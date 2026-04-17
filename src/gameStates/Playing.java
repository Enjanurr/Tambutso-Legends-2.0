package gameStates;

import Ui.*;
import Ui.StatusCheckOverlay;
import entities.EnemyManager;
import entities.PassengerManager;
import entities.Person;
import entities.PersonManager;
import entities.Player;
import entities.PowerupManager;
import entities.RidingPassenger;
import objects.StopSignManager;
import objects.WorldObjectManager;
import levels.LevelManager;
import main.Game;
import utils.LoadSave;

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
 *     1. introPaused      → IntroOverlay
 *     2. playerDead       → DeathOverlay
 *     3. interactionPaused→ AcceptPassengerOverlay
 *     4. listPopupPaused  → PassengerListOverlay (popup open)
 *     5. paused           → PauseOverlay
 *     6. none             → normal game + Open button of PassengerListOverlay
 *
 *   ESC behaviour:
 *     - AcceptPassengerOverlay open  → close it (acts like NO)
 *     - PassengerListOverlay open    → close it
 *     - PauseOverlay open            → resume
 *     - None of the above            → open PauseOverlay
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
    private AcceptPassengerOverlay acceptPassengerOverlay;
    private PassengerListOverlay   passengerListOverlay;
    private IntroOverlay           introOverlay;
    private StopHereIndicator stopHereIndicator;
    private StatusCheckOverlay statusCheckOverlay;
    // ── Overlay-state flags ───────────────────────────────────
    private boolean paused            = false;
    private boolean playerDead        = false;
    private boolean interactionPaused = false;   // AcceptPassengerOverlay is open
    private boolean listPopupPaused   = false;   // PassengerListOverlay popup is open
    private boolean introPaused       = false;
    private boolean statusCheckPaused = false;   // StatusCheckOverlay is active

    // ── World ─────────────────────────────────────────────────
    private float worldOffset    = 0;
    private final int levelPixelWidth =
            LoadSave.GetLevelData()[0].length * Game.TILES_SIZE;

    public static final int MAX_WORLD_LOOPS = 15;
    public int getMaxWorldLoops() { return MAX_WORLD_LOOPS; }

    private static final float CENTER_TOLERANCE = 10f * Game.SCALE;

    private int     worldLoopCount = 0;
    private boolean worldLoopDone  = false;
    private boolean dKeyHeld       = false;
    private int     passengersDroppedCount = 0;

    private float bigCloudOffset   = 0f;
    private float smallCloudOffset = 0f;
    private static final float BIG_CLOUD_PARALLAX   = 0.3f;
    private static final float SMALL_CLOUD_PARALLAX = 0.5f;

    // ── UI ────────────────────────────────────────────────────
    private PassengerCounter passengerCounter;

    private BufferedImage backgroundImg, bigClouds, smallClouds;
    private int[] smallCloudsPos;
    private final Random rnd = new Random();
    private WorldObjectManager worldObjectManager;

    @SuppressWarnings("unused")
    private int currentStopIndex = 0;

    // ─────────────────────────────────────────────────────────
    public Playing(Game game) {
        super(game);
        initClasses();
        loadBackgroundAssets();
    }

    public void resumeFromInteraction() {
        interactionPaused = false;
        acceptPassengerOverlay.close();
    }

    private void initClasses() {
        levelManager = new LevelManager(game);

        int jeepHitboxW = (int)(70 * Game.SCALE);
        int spawnX      = (Game.GAME_WIDTH - jeepHitboxW) / 2;
        int spawnY      = 520;

        player = new Player(spawnX, spawnY,
                (int)(110 * Game.SCALE), (int)(40 * Game.SCALE),
                game.getGamePanel());
        player.loadLvlData(levelManager.getCurrentLevel().getLevelData());

        personManager    = new PersonManager(this);
        passengerManager = new PassengerManager(this);
        enemyManager     = new EnemyManager(this);
        worldObjectManager = new WorldObjectManager();
        stopSignManager  = new StopSignManager(this, worldObjectManager);
        pauseOverlay     = new PauseOverlay(this);
        powerupManager   = new PowerupManager(this);
        healthBar        = new HealthBar();
        passengerCounter = new PassengerCounter();
        deathOverlay     = new DeathOverlay(this);
        progressBar      = new ProgressBar();
        stopHereIndicator = new StopHereIndicator();
        acceptPassengerOverlay = new AcceptPassengerOverlay(this, passengerCounter);
        acceptPassengerOverlay.setPassengerManager(passengerManager);

        passengerListOverlay = new PassengerListOverlay(
                this::handlePassengerDrop,
                this::handlePopupClose,
                this::handlePopupOpen
        );

        introOverlay = new IntroOverlay(this::onIntroDone);
        statusCheckOverlay = new StatusCheckOverlay(this);
    }

    private void loadBackgroundAssets() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PLAYING_BACKGROUND_IMG);
        bigClouds     = LoadSave.getSpriteAtlas(LoadSave.BIG_CLOUDS);
        smallClouds   = LoadSave.getSpriteAtlas(LoadSave.SMALL_CLOUDS);

        smallCloudsPos = new int[8];
        for (int i = 0; i < smallCloudsPos.length; i++)
            smallCloudsPos[i] = (int)(20 * Game.SCALE) + rnd.nextInt((int)(100 * Game.SCALE));
    }

    // ─────────────────────────────────────────────────────────
    // OVERLAY PRIORITY HELPERS
    // ─────────────────────────────────────────────────────────

    /**
     * Returns a token identifying the single topmost active overlay,
     * or NONE if no overlay is active.  All mouse routing is driven by this.
     */
    private enum ActiveOverlay { STATUS_CHECK, INTRO, DEATH, ACCEPT, LIST_POPUP, PAUSE, NONE }

    private ActiveOverlay activeOverlay() {
        if (statusCheckPaused) return ActiveOverlay.STATUS_CHECK;
        if (introPaused)       return ActiveOverlay.INTRO;
        if (playerDead)        return ActiveOverlay.DEATH;
        if (interactionPaused) return ActiveOverlay.ACCEPT;
        if (listPopupPaused)   return ActiveOverlay.LIST_POPUP;
        if (paused)            return ActiveOverlay.PAUSE;
        return ActiveOverlay.NONE;
    }

    // ─────────────────────────────────────────────────────────
    // PASSENGER LIST CALLBACKS
    // ─────────────────────────────────────────────────────────
    private void handlePopupOpen()  { listPopupPaused = true; }
    private void handlePopupClose() { listPopupPaused = false; }

    /**
     * Drop the selected passenger.
     * PassengerManager enforces the worldLoopCount >= assignedStop condition
     * and returns -1 if not allowed.
     */
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
            System.out.println("[Playing] Dropped passenger — fare \u20B1" + fare
                    + "  total \u20B1" + passengerListOverlay.getTotalFareEarned()
                    + "  dropped " + passengersDroppedCount + "/12");

            // Check if status check should trigger (e.g., last passenger dropped at Stop 15)
            tryTriggerStatusCheck();
        }
    }

    /**
     * Attempts to trigger the status check overlay if all conditions are met.
     * Call this after passenger drops when worldLoopDone is true.
     *
     * @return true if status check was triggered
     */
    private boolean tryTriggerStatusCheck() {
        if (!worldLoopDone) return false;
        if (statusCheckPaused) return false;

        int occupied = passengerManager.occupiedCount();
        if (occupied > 0) {
            System.out.println("[Playing] Cannot proceed to status check - " + occupied + " passengers still seated");
            return false;
        }

        // All passengers dropped - show status check
        int totalFare = passengerManager.getTotalFareEarned();
        if (statusCheckOverlay.open(passengersDroppedCount, totalFare)) {
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
        if (introOverlay.open()) introPaused = true;
    }

    private void onIntroDone() { introPaused = false; }

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
        interactionPaused = false;
        introPaused      = false;
        statusCheckPaused = false;

        bigCloudOffset   = 0f;
        smallCloudOffset = 0f;

        for (int i = 0; i < smallCloudsPos.length; i++)
            smallCloudsPos[i] = (int)(20 * Game.SCALE) + rnd.nextInt((int)(100 * Game.SCALE));

        int jeepHitboxW = (int)(70 * Game.SCALE);
        player.getHitBox().x = (float)(Game.GAME_WIDTH - jeepHitboxW) / 2;
        player.getHitBox().y = 520;
        player.resetDirBooleans();
        player.setWorldLoopDone(false);
        worldObjectManager.reset();

        personManager.resetAll();
        passengerManager.resetAll();
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
                && !introPaused && !listPopupPaused && !interactionPaused
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
        if (statusCheckPaused) { statusCheckOverlay.update(); return; }
        if (introPaused) { introOverlay.update(); return; }

        if (listPopupPaused) {
            passengerListOverlay.update();
            return;
        }

        acceptPassengerOverlay.update();

        if (interactionPaused) {
            if (!acceptPassengerOverlay.isOpen()) interactionPaused = false;
            return;
        }

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
                        if (worldLoopCount >= MAX_WORLD_LOOPS) {
                            worldLoopDone = true;
                            worldOffset   = 0;

                            // Status Check: Trigger if all passengers already dropped
                            tryTriggerStatusCheck();
                        }
                    }

                    bigCloudOffset += spd * BIG_CLOUD_PARALLAX;
                    if (bigCloudOffset >= BIG_CLOUD_WIDTH)     bigCloudOffset -= BIG_CLOUD_WIDTH;
                    smallCloudOffset += spd * SMALL_CLOUD_PARALLAX;
                    if (smallCloudOffset >= SMALL_CLOUD_WIDTH) smallCloudOffset -= SMALL_CLOUD_WIDTH;
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
        // HUD
        healthBar.render(g);
        passengerCounter.render(g);
        progressBar.render(g);

        // Passenger list (open button always visible when popup closed)
        List<RidingPassenger> seats = passengerManager.getSeatList();
        passengerListOverlay.render(g, seats, worldLoopCount);

        // Overlays — rendered on top in priority order
        if (statusCheckPaused) { statusCheckOverlay.render(g);   return; }
        if (introPaused)       { introOverlay.render(g);         return; }
        acceptPassengerOverlay.render(g);
        if (playerDead)        { deathOverlay.render(g);         return; }
        if (paused) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
            pauseOverlay.draw(g);
        }
    }

    private void drawClouds(Graphics g) {
        int bigTilesNeeded = (Game.GAME_WIDTH / BIG_CLOUD_WIDTH) + 2;
        for (int i = 0; i < bigTilesNeeded; i++) {
            int drawX = (int)(i * BIG_CLOUD_WIDTH - bigCloudOffset);
            g.drawImage(bigClouds, drawX, (int)(40 * Game.SCALE), BIG_CLOUD_WIDTH, BIG_CLOUD_HEIGHT, null);
        }
        int smallTilesNeeded = (Game.GAME_WIDTH / SMALL_CLOUD_WIDTH) + 2;
        for (int i = 0; i < smallCloudsPos.length; i++) {
            int drawX = (int)(i * SMALL_CLOUD_WIDTH - smallCloudOffset);
            g.drawImage(smallClouds, drawX, smallCloudsPos[i % smallCloudsPos.length],
                    SMALL_CLOUD_WIDTH, SMALL_CLOUD_HEIGHT, null);
        }
        for (int i = 0; i < smallTilesNeeded - smallCloudsPos.length; i++) {
            int drawX = (int)((smallCloudsPos.length + i) * SMALL_CLOUD_WIDTH - smallCloudOffset);
            g.drawImage(smallClouds, drawX, smallCloudsPos[i % smallCloudsPos.length],
                    SMALL_CLOUD_WIDTH, SMALL_CLOUD_HEIGHT, null);
        }
    }

    // ─────────────────────────────────────────────────────────
    // INPUT — strict priority stack
    // ─────────────────────────────────────────────────────────

    /**
     * mouseClicked only fires world interactions when NO overlay is active.
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (activeOverlay() != ActiveOverlay.NONE) return;

        if (e.getButton() == MouseEvent.BUTTON1) {
            for (Person p : personManager.getPersons()) {
                if (!p.isInteractable()) continue;
                Rectangle2D.Float pHB = p.getHitBox();
                if (pHB != null && pHB.contains(e.getX(), e.getY())) {
                    if (passengerManager.isFull()) {
                        System.out.println("[Playing] Jeepney full");
                        return;
                    }
                    if (worldLoopCount >= MAX_WORLD_LOOPS) {
                        System.out.println("[Playing] Last loop — no future stops");
                        return;
                    }
                    interactionPaused = true;
                    acceptPassengerOverlay.open(p);
                    return;
                }
            }
            player.setAttacking(true);
        }
    }

    /**
     * Strict overlay routing — the topmost active overlay gets the event,
     * all other layers are silently blocked.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        switch (activeOverlay()) {
            case STATUS_CHECK: statusCheckOverlay.mousePressed(e);                    return;
            case INTRO:      introOverlay.mousePressed(e);                            return;
            case DEATH:      deathOverlay.mousePressed(e);                            return;
            case ACCEPT:     acceptPassengerOverlay.mousePressed(e);                  return;
            case LIST_POPUP: passengerListOverlay.mousePressed(e, passengerManager.getSeatList()); return;
            case PAUSE:      pauseOverlay.mousePressed(e);                            return;
            case NONE:
                // No blocking overlay — let the Open button work
                passengerListOverlay.mousePressed(e, passengerManager.getSeatList());
                break;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        switch (activeOverlay()) {
            case STATUS_CHECK: statusCheckOverlay.mouseReleased(e);             return;
            case INTRO:      introOverlay.mouseReleased(e);                  return;
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
            case STATUS_CHECK: statusCheckOverlay.mouseMoved(e);           return;
            case INTRO:      introOverlay.mouseMoved(e);              return;
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

    /**
     * ESC behaviour:
     *   StatusCheckOverlay open     → do nothing (must click button)
     *   AcceptPassengerOverlay open → close it (NO action)
     *   PassengerListOverlay open   → close it
     *   PauseOverlay open           → resume
     *   None                        → open PauseOverlay
     *
     *   Intro, Death, and StatusCheck overlays block ESC entirely (they have their own buttons).
     */
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            switch (activeOverlay()) {
                case STATUS_CHECK:
                case INTRO:
                case DEATH:
                    // These overlays consume ESC — do nothing
                    break;
                case ACCEPT:
                    acceptPassengerOverlay.handleEsc();
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

        // All non-ESC keys are blocked while any overlay is active
        if (activeOverlay() != ActiveOverlay.NONE) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: player.setLeft(true);                   break;
            case KeyEvent.VK_D:
                // Block forward movement after reaching MAX_WORLD_LOOPS
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
        // Always release movement keys so the jeep doesn't get stuck
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: player.setLeft(false);                    break;
            case KeyEvent.VK_D: player.setRight(false); dKeyHeld = false; break;
            case KeyEvent.VK_W: player.setUp(false);                      break;
            case KeyEvent.VK_S: player.setDown(false);                    break;
        }
    }

    public void onJeepLooped() {
        personManager.resetAll();
        enemyManager.resetAll();
        stopSignManager.resetAll();
        worldObjectManager.reset();
    }

    public void windowFocusLost() {
        player.resetDirBooleans();
        dKeyHeld = false;
    }

    public void unPauseGame() { paused = false; }

    /**
     * Called by StatusCheckOverlay when player clicks Next button.
     * Proceeds to boss fight.
     */
    public void startBossFight() {
        game.startBossFight();
    }

    // ── Getters ───────────────────────────────────────────────
    public Player           getPlayer()           { return player; }
    public HealthBar        getHealthBar()         { return healthBar; }
    public LevelManager     getLevelManager()      { return levelManager; }
    public float            getWorldOffset()       { return worldOffset; }
    public int              getWorldLoopCount()    { return worldLoopCount; }
    public boolean          isPaused()             { return paused; }
    public PassengerManager getPassengerManager()  { return passengerManager; }
}