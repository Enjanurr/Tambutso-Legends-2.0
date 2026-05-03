package gameStates;

import Ui.*;
import entities.EnemyManager;
import entities.PassengerManager;
import entities.Person;
import entities.PersonManager;
import entities.Player;
import entities.PowerupManager;
import entities.RidingPassenger;
import Ui.DeathOverlay;
import Ui.HealthBar;
import Ui.PassengerCounter;
import Ui.PauseOverlay;
import Ui.ProgressBar;
import entities.*;
import objects.StopSignManager;
import objects.WorldObjectManager;
import levels.LevelManager;
import main.Game;
import utils.RouteMap;
import utils.LoadSave;
import utils.ScrollingCloudLayer;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

import static utils.Constants.Environment.*;

/**
 * Playing game-state.
 *
 * Input-blocking contract (Part 2):
 *   All mouse events are routed through activeOverlay() which returns the
 *   single topmost overlay that is currently open.  Only that overlay
 *   receives input; everything else is silently swallowed.
 *
 *   Priority order (highest → lowest):
 *     1. paymentPaused                   → PaymentOverlay
 *     2. statusCheckPaused               → StatusCheckOverlay
 *     3. introPaused                     → IntroOverlay
 *     4. playerDead                      → DeathOverlay
 *     5. acceptPassengerOverlay.isOpen() → AcceptPassengerOverlay
 *     6. listPopupPaused                 → PassengerListOverlay (popup open)
 *     7. paused                          → PauseOverlay
 *     8. none                            → normal game + Open button of PassengerListOverlay
 *
 *   ESC behaviour:
 *     - AcceptPassengerOverlay open  → close it (acts like NO)
 *     - PassengerListOverlay open    → close it
 *     - PauseOverlay open            → resume
 *     - None of the above            → open PauseOverlay
 *
 * IntroOverlay ownership:
 *   Playing is the SINGLE owner of IntroOverlay. Game.java no longer
 *   constructs its own instance.
 */
public class Playing extends State implements StateMethods {

    // ── Sub-systems ───────────────────────────────────────────
    private PowerupManager       powerupManager;
    private Player               player;
    private PersonManager        personManager;
    private PassengerManager     passengerManager;
    private EnemyManager         enemyManager;
    private StopSignManager      stopSignManager;
    private LevelManager         levelManager;
    private PauseOverlay         pauseOverlay;
    private HealthBar            healthBar;
    private DeathOverlay         deathOverlay;
    private ProgressBar          progressBar;
    // Add with other UI fields (around line 100)
    private LevelBanner levelBanner;
    private AcceptPassengerOverlay acceptPassengerOverlay;
    private PassengerListOverlay   passengerListOverlay;
    private IntroOverlay           introOverlay;
    private StopHereIndicator      stopHereIndicator;
    private StatusCheckOverlay     statusCheckOverlay;
    private GameClock              gameClock;
    private SkipOverlay            skipOverlay;
    private PaymentOverlay         paymentOverlay;
    private MissionOverlay         missionOverlay;

    // ── Overlay-state flags ───────────────────────────────────
    private boolean paused            = false;
    private boolean playerDead        = false;
    private final PlayingDebugOverlay debugOverlay = new PlayingDebugOverlay();
    private PassengerInteractionController passengerInteractionController;
    private final PlayingWorldController worldController = new PlayingWorldController();
    private boolean interactionPaused = false;
    private boolean listPopupPaused   = false;
    private boolean introPaused       = false;
    private boolean statusCheckPaused = false;
    private boolean paymentPaused     = false;
    private boolean missionShowing    = false;

    // ── Interaction safety timeout ────────────────────────────
    // Counts frames while interactionPaused == true.
    // Auto-recovers if the flag is stuck for more than 2 seconds (400 frames @ 200 UPS).
    private int interactionStuckTimer = 0;

    // ── Driver reference backup ───────────────────────────────
    private entities.DriverProfile currentDriver = null;

    // ── World ─────────────────────────────────────────────────
    private float worldOffset    = 0;
    private final int levelPixelWidth =
            LoadSave.GetLevelData()[0].length * Game.TILES_SIZE;

    public static final int MAX_WORLD_LOOPS = 15;
    // -------------------------------------------------------
    public int getMaxWorldLoops() { return MAX_WORLD_LOOPS; }

    private static final float CENTER_TOLERANCE = 10f * Game.SCALE;

    private int     worldLoopCount = 0;
    private boolean worldLoopDone  = false;
    private boolean dKeyHeld       = false;
    private int     passengersDroppedCount = 0;

    private static final float BIG_CLOUD_PARALLAX   = 0.3f;
    private static final float SMALL_CLOUD_PARALLAX = 0.5f;

    // ── UI ────────────────────────────────────────────────────
    private PassengerCounter passengerCounter;

    private BufferedImage backgroundImg, bigClouds, smallClouds;
    private int[] smallCloudsPos;
    private ScrollingCloudLayer bigCloudLayer;
    private ScrollingCloudLayer smallCloudLayer;
    private final Random rnd = new Random();
    private WorldObjectManager worldObjectManager;

    // ── Current route position ────────────────────────────────
    private RouteMap currentMap = RouteMap.MAP_2;
    @SuppressWarnings("unused")
    private int currentStopIndex = 0;

    // ── Boss fight state ──────────────────────────────────────
    private boolean bossFightActive = false;

    public void setBossFightActive(boolean active) {
        this.bossFightActive = active;
        if (!active) {
            player.setBossMode(false);
        }
    }

    public boolean isBossFightActive() { return bossFightActive; }

    // ─────────────────────────────────────────────────────────
    public Playing(Game game) {
        super(game);
        System.out.println("[Playing] Constructor called");
        initClasses();
        loadBackgroundAssets();
        System.out.println("[Playing] Constructor complete");
    }

    /** Called by CharSelectState before gameplay begins. */
    public void applyDriver(entities.DriverProfile profile) {
        this.currentDriver = profile;  // Store reference for recovery
        player.applyDriver(profile);
        System.out.println("Driver selected: " + profile.displayName
                + " | Speed: " + profile.maxSpeed);
    }

    public entities.DriverProfile getCurrentDriver() { return currentDriver; }

    // Add this method in Playing class
    public void refreshLevelBanner() {
        int currentLevel = levelManager.getCurrentLevelId();
        levelBanner = new LevelBanner(currentLevel);
        System.out.println("[Playing] Banner refreshed to Level " + currentLevel);
    }
    public void resumeFromInteraction() {
        if (!interactionPaused) return;  // Already resumed — nothing to do
        System.out.println("[Playing] resumeFromInteraction() - clearing interactionPaused");
        // Overlay is already closed by handleYes/handleNo before this is called;
        // call close() defensively in case we got here via a direct ESC path.
        acceptPassengerOverlay.close();
        interactionPaused = false;
        System.out.println("[Playing] interactionPaused=" + interactionPaused + ", activeOverlay=" + activeOverlay());
    }

    public boolean isInteractionPaused() { return interactionPaused; }

    private void initClasses() {
        System.out.println("[Playing] initClasses() started");
        levelManager = new LevelManager(game);

        int jeepHitboxW = (int)(70 * Game.SCALE);
        int spawnX      = (Game.GAME_WIDTH - jeepHitboxW) / 2;
        int spawnY      = 520;

        player = new Player(spawnX, spawnY,
                (int)(110 * Game.SCALE), (int)(40 * Game.SCALE), game.getGamePanel());
        player.loadLvlData(levelManager.getCurrentLevel().getLevelData());

        personManager    = new PersonManager(this);
        passengerManager = new PassengerManager(this);
        enemyManager     = new EnemyManager(this);
        worldObjectManager = new WorldObjectManager(currentMap);
        stopSignManager  = new StopSignManager(this, worldObjectManager);
        pauseOverlay     = new PauseOverlay(this);
        powerupManager   = new PowerupManager(this);
        healthBar        = new HealthBar();
        passengerCounter = new PassengerCounter();
        deathOverlay     = new DeathOverlay(this);

        progressBar      = new ProgressBar(levelManager.getCurrentLevelId());
        levelBanner = new LevelBanner(levelManager.getCurrentLevelId());

        stopHereIndicator = new StopHereIndicator();
        acceptPassengerOverlay = new AcceptPassengerOverlay(this, passengerCounter);
        acceptPassengerOverlay.setPassengerManager(passengerManager);

        paymentOverlay = new PaymentOverlay(
                this::confirmPassengerDrop,
                this::handlePaymentClose
        );

        passengerListOverlay = new PassengerListOverlay(
                this::handlePassengerDrop,
                this::handlePopupClose,
                this::handlePopupOpen,
                this::handleOpenPayment
        );

        introOverlay = new IntroOverlay(this::onIntroDone);
        statusCheckOverlay = new StatusCheckOverlay(this);
        gameClock = new GameClock();
        skipOverlay = new SkipOverlay(game, this);
        missionOverlay = null;  // Created on-demand for current level
        System.out.println("[Playing] initClasses() complete - Level " + levelManager.getCurrentLevelId() + " loaded");
        passengerInteractionController = new PassengerInteractionController(this, passengerCounter);
    }

    private void loadBackgroundAssets() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PLAYING_BACKGROUND_IMG);
        bigClouds     = LoadSave.getSpriteAtlas(LoadSave.BIG_CLOUDS);
        smallClouds   = LoadSave.getSpriteAtlas(LoadSave.SMALL_CLOUDS);

        smallCloudsPos = new int[8];
        randomizeSmallCloudRows();
        initCloudLayers();
    }

    // ─────────────────────────────────────────────────────────
    // OVERLAY PRIORITY HELPERS
    // ─────────────────────────────────────────────────────────

    private enum ActiveOverlay { PAYMENT, STATUS_CHECK, INTRO, DEATH, ACCEPT, LIST_POPUP, PAUSE, MISSION, NONE }

    private ActiveOverlay activeOverlay() {
        if (paymentPaused)                    return ActiveOverlay.PAYMENT;
        if (statusCheckPaused)                return ActiveOverlay.STATUS_CHECK;
        if (introPaused)                      return ActiveOverlay.INTRO;
        if (playerDead)                       return ActiveOverlay.DEATH;
        if (acceptPassengerOverlay.isOpen())  return ActiveOverlay.ACCEPT;
        if (listPopupPaused)                  return ActiveOverlay.LIST_POPUP;
        if (paused)                           return ActiveOverlay.PAUSE;
        if (missionOverlay != null && missionOverlay.isOpen()) return ActiveOverlay.MISSION;
        return ActiveOverlay.NONE;
    }

    // ─────────────────────────────────────────────────────────
    // PASSENGER LIST CALLBACKS
    // ─────────────────────────────────────────────────────────
    private void handlePopupOpen()  { listPopupPaused = true; }
    private void handlePopupClose() { listPopupPaused = false; }

    private void handlePassengerDrop() {
        int slot = passengerListOverlay.getSelectedSlot();
        if (slot < 0) return;

        RidingPassenger rp = passengerManager.getSeat(slot);
        if (rp == null) return;

        if (!rp.isReadyToDrop(worldLoopCount)) {
            System.out.println("[Playing] Cannot drop — not at stop yet (stop "
                    + rp.getAssignedStop() + ", loop " + worldLoopCount + ")");
            return;
        }

        int fare = passengerManager.dropPassenger(
                slot, worldLoopCount, player.getHitBox().x, player.getHitBox().y);
        if (fare >= 0) {
            passengerListOverlay.addFare(fare);
            passengerListOverlay.clearSelection();
            passengersDroppedCount++;
            passengerCounter.increment();
            System.out.println("[Playing] Dropped passenger — fare ₱" + fare
                    + "  total ₱" + passengerListOverlay.getTotalFareEarned()
                    + "  dropped " + passengersDroppedCount + "/12");

            tryTriggerStatusCheck();
        }
    }

    private void handleOpenPayment() {
        int slot = passengerListOverlay.getSelectedSlot();
        System.out.println("[Playing] handleOpenPayment() STARTED - selectedSlot=" + slot);
        if (slot < 0) {
            System.out.println("[Playing] handleOpenPayment() - slot < 0, returning");
            return;
        }

        RidingPassenger rp = passengerManager.getSeat(slot);
        if (rp == null) {
            System.out.println("[Playing] handleOpenPayment() - rp is null (empty seat), returning");
            return;
        }

        int expectedFare = rp.calculateFare(worldLoopCount);
        System.out.println("[Playing] handleOpenPayment() - calling paymentOverlay.open(" + expectedFare + ", rp)");
        paymentOverlay.open(expectedFare, rp);
        paymentPaused = true;
        System.out.println("[Playing] handleOpenPayment() - paymentPaused=" + paymentPaused + ", activeOverlay=" + activeOverlay());
    }

    private void confirmPassengerDrop() {
        int slot = passengerListOverlay.getSelectedSlot();
        if (slot < 0) return;

        RidingPassenger rp = passengerManager.getSeat(slot);
        if (rp == null) return;

        int fare = passengerManager.confirmDrop(
                slot, worldLoopCount, player.getHitBox().x, player.getHitBox().y);

        if (fare >= 0) {
            passengerListOverlay.addFare(fare);
            passengerListOverlay.clearSelection();
            passengersDroppedCount++;
            passengerCounter.increment();
            System.out.println("[Playing] Dropped passenger — fare ₱" + fare
                    + "  total ₱" + passengerListOverlay.getTotalFareEarned()
                    + "  dropped " + passengersDroppedCount + "/12");

            tryTriggerStatusCheck();
        }
    }

    private void handlePaymentClose() {
        paymentPaused = false;
    }

    private boolean tryTriggerStatusCheck() {
        if (!worldLoopDone) return false;
        if (statusCheckPaused) return false;

        int occupied = passengerManager.occupiedCount();
        if (occupied > 0) {
            System.out.println("[Playing] Cannot proceed to status check - " + occupied + " passengers still seated");
            return false;
        }

        int totalFare = passengerManager.getTotalFareEarned();
        int requiredPassengers = levelManager.getRequiredPassengers();
        int requiredFare = levelManager.getRequiredFare();

        if (statusCheckOverlay.open(passengersDroppedCount, totalFare, requiredPassengers, requiredFare)) {
            statusCheckPaused = true;
            System.out.println("[Playing] Status check triggered after all passengers dropped");
            return true;
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────
    // INTRO OVERLAY
    // ─────────────────────────────────────────────────────────

    public void tryShowIntro() {
        if (introOverlay.open()) {
            introPaused = true;
            GameStates.state = GameStates.INTRO;
            System.out.println("[Playing] Intro opened — state=INTRO, introPaused=true");
        } else {
            System.out.println("[Playing] Intro already shown, skipping");
        }
    }

    public void resetIntroShown() {
        introOverlay.resetShown();
    }

    public void onIntroDone() {
        introPaused = false;
        setBossFightActive(false);  // ← ADD THIS
        player.setBossMode(false);
        gameClock.setCurrentLevel(levelManager.getCurrentLevelId());
        gameClock.start();
        System.out.println("[Playing] onIntroDone() — introPaused=false, clock started, notifying Game");
        game.onIntroComplete();
    }

    // ─────────────────────────────────────────────────────────
    // RESTART
    // ─────────────────────────────────────────────────────────
    public void restartGame() {
        worldOffset      = 0;
        worldLoopCount   = 0;
        currentStopIndex = 0;
        worldLoopDone    = false;
        dKeyHeld         = false;
        playerDead       = false;
        listPopupPaused  = false;
        introPaused      = false;
        statusCheckPaused = false;
        paymentPaused    = false;
        paymentOverlay.close();

        randomizeSmallCloudRows();
        initCloudLayers();

        int jeepHitboxW = (int)(70 * Game.SCALE);
        player.getHitBox().x = (float)(Game.GAME_WIDTH - jeepHitboxW) / 2;
        player.getHitBox().y = 520;
        player.resetDirBooleans();
        player.setWorldLoopDone(false);
        player.setBossMode(false);
        worldObjectManager.reset();

        player.setRight(false);
        player.setLeft(false);
        player.setUp(false);
        player.setDown(false);
        player.setWorldScrolling(false);

        personManager.resetAll();
        passengerManager.resetAll();  // Clear seated passengers
        enemyManager.resetAll();
        stopSignManager.resetAll();
        powerupManager.resetAll();

        healthBar.reset();
        passengerCounter.reset();
        progressBar.reset();

        passengerListOverlay.closePopup();
        passengerListOverlay.resetFare();

        acceptPassengerOverlay.close();
        acceptPassengerOverlay.resetPassengerCount();
        acceptPassengerOverlay.resetEarnings();
        statusCheckOverlay.close();
        passengersDroppedCount = 0;
        paused = false;

        gameClock.setCurrentLevel(levelManager.getCurrentLevelId());
        // Clock keeps running - no reset on restart, only on level advance
    }

    public void resetGame() {
        restartGame();
    }

    // ── Health callbacks ─────────────────────────────────────
    public void onPlayerHit() {
        boolean dead = healthBar.takeDamage();
        if (dead) { playerDead = true; deathOverlay.reset(); }
    }

    public void onPlayerHeal() { healthBar.heal(); }

    // ── Scrolling condition ──────────────────────────────────
    public boolean isScrolling() {
        boolean hasSpeed = player.getCurrentXSpeed() > 0;
        return (dKeyHeld || hasSpeed) && isJeepCentered() && !paused && !worldLoopDone
                && !player.isStruckActive() && !playerDead
                && !introPaused && !listPopupPaused && !acceptPassengerOverlay.isOpen()
                && !statusCheckPaused;
    }

    public float getScrollSpeed() { return player.getCurrentXSpeed(); }

    private boolean isJeepCentered() {
        float jeepCenterX   = player.getHitBox().x + player.getHitBox().width / 2f;
        float screenCenterX = Game.GAME_WIDTH / 2f;
        return Math.abs(jeepCenterX - screenCenterX) <= CENTER_TOLERANCE;
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    @Override
    public void update() {
        if (playerDead)  { deathOverlay.update(); return; }
        if (paymentPaused) { paymentOverlay.update(); return; }
        if (statusCheckPaused) { statusCheckOverlay.update(); return; }
        if (introPaused) { introOverlay.update(); return; }
        if (missionOverlay != null && missionOverlay.isOpen()) { missionOverlay.update(); return; }

        if (listPopupPaused) {
            passengerListOverlay.update();
            return;
        }

        acceptPassengerOverlay.update();

        if (!paused) {
            boolean scrolling = isScrolling();
            player.setWorldScrolling(scrolling);
            player.setWorldLoopDone(worldLoopDone);

            if (scrolling) {
                float spd = getScrollSpeed();

                if (spd > 0) {
                    worldOffset += spd;
                    if (worldOffset >= levelPixelWidth) {
                        worldOffset -= levelPixelWidth;
                        worldLoopCount++;
                        progressBar.onLoopCompleted();
                        System.out.println("World loops: " + worldLoopCount);

                        // Check if we've reached the final loop
                        int maxLoops = levelManager.getMaxWorldLoops();
                        if (worldLoopCount >= maxLoops) {
                            worldLoopDone = true;
                            worldOffset = 0;
                            player.setCurrentXSpeed(0);
                            dKeyHeld = false;
                            player.setRight(false);
                            System.out.println("[Playing] Final loop reached - auto stopped");
                            tryTriggerStatusCheck();
                        }
                    }

                    bigCloudLayer.update(spd);
                    smallCloudLayer.update(spd);
                }
            }

            levelManager.update();
            personManager.update();
            passengerManager.update(worldLoopCount, scrolling, getScrollSpeed());
            checkPassengerInteractions();
            enemyManager.update();
            stopSignManager.update();
            worldObjectManager.update(scrolling, getScrollSpeed());
            powerupManager.update();
            stopHereIndicator.update(passengerManager, worldLoopCount);
            player.update();
            gameClock.update();

        } else {
            pauseOverlay.update();
        }
    }

    // ─────────────────────────────────────────────────────────
    // PASSENGER INTERACTION SCAN
    // ─────────────────────────────────────────────────────────
    private void checkPassengerInteractions() {
        Rectangle2D.Float jeepHB = player.getHitBox();
        if (jeepHB == null) return;
        for (Person p : personManager.getPersons()) {
            if (p.getType() != Person.PersonType.PASSENGER) continue;
            if (!p.isActive()) continue;
            Rectangle2D.Float pHB = p.getHitBox();
            if (pHB == null) continue;
            p.setInteractable(jeepHB.intersects(pHB));
        }
    }

    // ─────────────────────────────────────────────────────────
    // DRAW
    // ─────────────────────────────────────────────────────────
    @Override
    public void draw(Graphics g) {
        if (backgroundImg != null)
            g.drawImage(backgroundImg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        drawClouds(g);
        levelManager.draw(g, (int) worldOffset);
        worldObjectManager.draw(g);
        enemyManager.render(g);
        personManager.render(g);
        stopSignManager.render(g);
        powerupManager.render(g);
        passengerManager.renderDropAnimations(g);
        player.render(g);
        stopHereIndicator.render(g, player.getHitBox().x, player.getHitBox().y);
        healthBar.render(g);
        passengerCounter.render(g);

        // Update banner position based on progress bar position
        levelBanner.updatePosition(progressBar.getDrawY(), progressBar.getDrawHeight());
        progressBar.render(g);

        levelBanner.render(g);  // ← RENDER BANNER AFTER PROGRESS BAR

        gameClock.render(g);

        List<RidingPassenger> seats = passengerManager.getSeatList();
        passengerListOverlay.render(g, seats, worldLoopCount);

        if (paymentPaused)     { paymentOverlay.render(g);       return; }
        if (statusCheckPaused) { statusCheckOverlay.render(g);   return; }
        if (introPaused)       { introOverlay.render(g);         return; }
        if (missionOverlay != null && missionOverlay.isOpen()) { missionOverlay.render(g); return; }
        acceptPassengerOverlay.render(g);
        if (playerDead)        { deathOverlay.render(g);         return; }
        if (paused) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
            pauseOverlay.draw(g);
        }

        skipOverlay.render(g);
    }

    private void drawClouds(Graphics g) {
        bigCloudLayer.draw(g);
        smallCloudLayer.draw(g);
    }

    private void initCloudLayers() {
        int bigCloudCount = (Game.GAME_WIDTH / BIG_CLOUD_WIDTH) + 3;
        int smallCloudCount = (Game.GAME_WIDTH / SMALL_CLOUD_WIDTH) + 3;
        bigCloudLayer = new ScrollingCloudLayer(
                bigClouds, BIG_CLOUD_WIDTH, BIG_CLOUD_HEIGHT,
                BIG_CLOUD_PARALLAX, bigCloudCount, (int)(40 * Game.SCALE));
        smallCloudLayer = new ScrollingCloudLayer(
                smallClouds, SMALL_CLOUD_WIDTH, SMALL_CLOUD_HEIGHT,
                SMALL_CLOUD_PARALLAX, smallCloudCount, smallCloudsPos);
    }

    private void randomizeSmallCloudRows() {
        for (int i = 0; i < smallCloudsPos.length; i++)
            smallCloudsPos[i] = (int)(20 * Game.SCALE) + rnd.nextInt((int)(100 * Game.SCALE));
    }

    // ─────────────────────────────────────────────────────────
    // INPUT — strict priority stack
    // ─────────────────────────────────────────────────────────

    @Override
    public void mouseClicked(MouseEvent e) {
        if (activeOverlay() != ActiveOverlay.NONE) return;

        if (e.getButton() == MouseEvent.BUTTON1) {
            // Require jeep idle before accepting passenger
            if (player.getCurrentXSpeed() > 0 || dKeyHeld) {
                System.out.println("[Playing] Cannot accept passenger while jeep is moving");
                return;
            }
            for (Person p : personManager.getPersons()) {
                if (!p.isInteractable()) continue;
                Rectangle2D.Float pHB = p.getHitBox();
                if (pHB != null && pHB.contains(e.getX(), e.getY())) {
                    if (passengerManager.isFull()) {
                        System.out.println("[Playing] Jeepney full");
                        return;
                    }
                    int maxLoops = levelManager.getMaxWorldLoops();
                    if (worldLoopCount >= maxLoops) {
                        System.out.println("[Playing] Last loop — no future stops");
                        return;
                    }
                    acceptPassengerOverlay.open(p);
                    return;
                }
            }
        }
        player.setAttacking(true);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (skipOverlay.isVisible()) {
            skipOverlay.mousePressed(e);
            return;
        }

        switch (activeOverlay()) {
            case PAYMENT:    paymentOverlay.mousePressed(e);                          return;
            case STATUS_CHECK: statusCheckOverlay.mousePressed(e);                    return;
            case INTRO:      introOverlay.mousePressed(e);                            return;
            case MISSION:    missionOverlay.mousePressed(e);                          return;
            case DEATH:      deathOverlay.mousePressed(e);                            return;
            case ACCEPT:     acceptPassengerOverlay.mousePressed(e);                  return;
            case LIST_POPUP: passengerListOverlay.mousePressed(e, passengerManager.getSeatList()); return;
            case PAUSE:      pauseOverlay.mousePressed(e);                            return;
            case NONE:
                passengerListOverlay.mousePressed(e, passengerManager.getSeatList());
                break;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (skipOverlay.isVisible()) {
            return;
        }

        switch (activeOverlay()) {
            case PAYMENT:    paymentOverlay.mouseReleased(e);            return;
            case STATUS_CHECK: statusCheckOverlay.mouseReleased(e);             return;
            case INTRO:      introOverlay.mouseReleased(e);                  return;
            case MISSION:    missionOverlay.mouseReleased(e);            return;
            case DEATH:      deathOverlay.mouseReleased(e);                  return;
            case ACCEPT:     acceptPassengerOverlay.mouseReleased(e);        return;
            case LIST_POPUP: passengerListOverlay.mouseReleased(e);          return;
            case PAUSE:      pauseOverlay.mouseReleased(e);                  return;
            case NONE:
                passengerListOverlay.mouseReleased(e);
                break;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        switch (activeOverlay()) {
            case PAYMENT:    paymentOverlay.mouseMoved(e);             return;
            case STATUS_CHECK: statusCheckOverlay.mouseMoved(e);           return;
            case INTRO:      introOverlay.mouseMoved(e);              return;
            case MISSION:    missionOverlay.mouseMoved(e);            return;
            case DEATH:      deathOverlay.mouseMoved(e);              return;
            case ACCEPT:     acceptPassengerOverlay.mouseMoved(e);    return;
            case LIST_POPUP: passengerListOverlay.mouseMoved(e);      return;
            case PAUSE:      pauseOverlay.mouseMoved(e);              return;
            case NONE:
                passengerListOverlay.mouseMoved(e);
                break;
        }
    }

    public void mouseDragged(MouseEvent e) {
        switch (activeOverlay()) {
            case ACCEPT: acceptPassengerOverlay.mouseDragged(e); return;
            case PAUSE:  pauseOverlay.mouseDragged(e);           return;
            default: break;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_S && e.isControlDown() && e.isShiftDown()) {
            skipOverlay.toggleEnabled();
            return;
        }

        if (skipOverlay.isEnabled()) {
            skipOverlay.keyPressed(e);
            if (skipOverlay.isVisible()) return;
        }

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (acceptPassengerOverlay.isOpen()) {
                System.out.println("[Playing] ESC closing AcceptPassengerOverlay");
                acceptPassengerOverlay.handleEsc();  // calls handleNo() → close()
                return;
            }
            if (paymentPaused) {
                System.out.println("[Playing] ESC force closed PaymentOverlay");
                paymentOverlay.close();
                handlePaymentClose();
                return;
            }
            if (listPopupPaused) {
                System.out.println("[Playing] ESC force closed PassengerListOverlay");
                passengerListOverlay.closePopup();
                listPopupPaused = false;
                return;
            }

            // Normal ESC routing for overlays that don't need force-close
            switch (activeOverlay()) {
                case PAYMENT:
                    paymentOverlay.close();
                    handlePaymentClose();
                    break;
                case STATUS_CHECK:
                case INTRO:
                case DEATH:
                    break;
                case LIST_POPUP:
                    passengerListOverlay.handleEsc();
                    break;
                case PAUSE:
                    paused = false;
                    break;
                case NONE:
                    paused = true;
                    break;
            }
            return;
        }

        if (paymentPaused) {
            if (e.getKeyCode() >= KeyEvent.VK_0 && e.getKeyCode() <= KeyEvent.VK_9) {
                paymentOverlay.inputDigit(e.getKeyCode() - KeyEvent.VK_0);
                return;
            } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                paymentOverlay.backspace();
                return;
            }
            return;
        }

        if (activeOverlay() != ActiveOverlay.NONE) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: player.setLeft(true);                   break;
            case KeyEvent.VK_D:
                if (worldLoopDone) {
                    System.out.println("[Playing] D key ignored - world loop complete, drop remaining passengers");
                    break;
                }
                player.setRight(true);
                dKeyHeld = true;
                break;
            case KeyEvent.VK_W: player.setUp(true);                     break;
            case KeyEvent.VK_S: player.setDown(true);                   break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: player.setLeft(false);                    break;
            case KeyEvent.VK_D: player.setRight(false); dKeyHeld = false; break;
            case KeyEvent.VK_W: player.setUp(false);                      break;
            case KeyEvent.VK_S: player.setDown(false);                    break;
        }
    }

    public void onJeepLooped() {
        // A hard jeep loop reset clears dynamic roadside spawns and restores any
        // props that should be visible at the start of a fresh run.
        personManager.resetAll();
        enemyManager.resetAll();
        stopSignManager.resetAll();
        worldObjectManager.reset();
    }

    public void setCurrentMap(RouteMap map) {
        currentMap = map;
        worldObjectManager.setCurrentMap(map);
    }

    public void windowFocusLost() {
        player.resetDirBooleans();
        dKeyHeld = false;
    }

    public void unPauseGame() { paused = false; }

    public void startBossFight() {
        setBossFightActive(true);
        int currentLevel = levelManager.getCurrentLevelId();
        if (currentLevel == 1) {
            game.startLevel1BossFight();
        } else if (currentLevel == 2) {
            game.startLevel2BossFight();
        } else {
            game.startBossFightWithLevel(currentLevel);
        }
        gameClock.start();  // Keep clock running during boss fight
    }

    public boolean advanceToNextLevel() {
        int currentLevel = levelManager.getCurrentLevelId();

        if (currentLevel >= 3) {
            System.out.println("[Playing] Already at max level!");
            return false;
        }

        System.out.println("[Playing] advanceToNextLevel() - Level " + currentLevel + " -> " + (currentLevel + 1));
        gameClock.stop();
        gameClock.saveLevelRecord();
        System.out.println("[Playing] Level " + currentLevel + " completed in " + gameClock.getFormattedTime());

        levelManager.advanceToNextLevel();

        // Recreate progress bar for new level
        progressBar = new ProgressBar(levelManager.getCurrentLevelId());
        levelBanner = new LevelBanner(levelManager.getCurrentLevelId());  // ← RECREATE BANNER

        gameClock.reset();
        gameClock.setCurrentLevel(levelManager.getCurrentLevelId());

        restartGame();
        player.setBossMode(false);
        gameClock.start();

        System.out.println("[Playing] Advanced to Level " + levelManager.getCurrentLevelId());
        return true;
    }

    public void restartCurrentLevel() {
        restartGame();
    }

    public void completeLevelForDebug() {
        System.out.println("[Playing] Debug: Completing current level");
        worldLoopDone = true;
        statusCheckPaused = true;
        int totalFare = passengerManager.getTotalFareEarned();
        int requiredPassengers = levelManager.getRequiredPassengers();
        int requiredFare = levelManager.getRequiredFare();
        statusCheckOverlay.open(passengersDroppedCount, totalFare, requiredPassengers, requiredFare);
    }

    // ─────────────────────────────────────────────────────────
    // MISSION OVERLAY
    // ─────────────────────────────────────────────────────────
    /**
     * Shows mission screen for current level before starting gameplay.
     * Called after boss defeat (NEXT button) or SkipOverlay (Next Level).
     */
    public void showMissionForCurrentLevel() {
        int levelId = levelManager.getCurrentLevelId();

        // Guard: prevent duplicate mission shows
        if (missionShowing) {
            System.out.println("[Playing] Mission already showing for Level " + levelId + ", skipping");
            return;
        }
        if (missionOverlay != null && missionOverlay.isOpen()) {
            System.out.println("[Playing] Mission overlay already open, skipping");
            return;
        }

        System.out.println("[Playing] Showing mission for Level " + levelId);
        missionShowing = true;
        missionOverlay = new MissionOverlay(levelId, this::onMissionDone);
        missionOverlay.open();

        // Game clock stopped - will start when mission completes
        gameClock.stop();
    }

    private void onMissionDone() {
        System.out.println("[Playing] Mission complete - starting gameplay");
        missionOverlay = null;
        missionShowing = false;
        gameClock.start();
        GameStates.state = GameStates.PLAYING;
    }

    // ── Getters ───────────────────────────────────────────────
    public Player           getPlayer()           { return player; }
    public HealthBar        getHealthBar()        { return healthBar; }
    public LevelManager     getLevelManager()     { return levelManager; }
    public float            getWorldOffset()      { return worldOffset; }
    public int              getWorldLoopCount()   { return worldLoopCount; }
    public boolean          isPaused()            { return paused; }
    public PassengerManager getPassengerManager() { return passengerManager; }
    public IntroOverlay     getIntroOverlay()     { return introOverlay; }
    public GameClock        getGameClock()        { return gameClock; }
    public ProgressBar      getProgressBar()      { return progressBar; }

    // ── Setters ──────────────────────────────────────────────
    public void setProgressBar(ProgressBar bar) { this.progressBar = bar; }
}
