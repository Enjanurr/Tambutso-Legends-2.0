package gameStates;

import Ui.AcceptPassengerOverlay;
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
import utils.LoadSave;
import utils.RouteConstants;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import static utils.Constants.Environment.*;

public class Playing extends State implements StateMethods {
    // Playing orchestrates the active run and delegates specialized behavior
    // to managers for enemies, powerups, interactive stop signs, and
    // decorative roadside objects.

    // =======================================================
    // ── DEBUG TOGGLES — comment / uncomment as needed ──────
    // =======================================================

    // Uncomment the line below to skip all 15 loops and jump straight to boss fight.
    // SKIP_TO_BOSS takes priority over DISABLE_BOSS_FIGHT.
    // private static final boolean SKIP_TO_BOSS      = true;

    // Uncomment the line below to disable the boss fight entirely.
    // After loop 15 the game will loop indefinitely instead of transitioning.
    // private static final boolean DISABLE_BOSS_FIGHT = true;

    // =======================================================

    private PowerupManager  powerupManager;
    private Player          player;
    private PersonManager   personManager;
    private EnemyManager    enemyManager;
    private StopSignManager stopSignManager;
    private LevelManager    levelManager;
    private PauseOverlay    pauseOverlay;
    private HealthBar       healthBar;
    private DeathOverlay    deathOverlay;
    private ProgressBar     progressBar;
    private AcceptPassengerOverlay acceptPassengerOverlay;       // ← modal

    private boolean paused              = false;
    private boolean playerDead          = false;
    private boolean interactionPaused   = false;   // ← replaces pickUpPassenger

    // ── World scrolling ──────────────────────────────────────
    private float worldOffset    = 0;
    private final int levelPixelWidth =
            LoadSave.GetLevelData()[0].length * Game.TILES_SIZE;

    // -------------------------------------------------------
    // WORLD SCROLL SETTINGS
    // -------------------------------------------------------
    public static final int MAX_WORLD_LOOPS = 15;
    // -------------------------------------------------------
    public int getMaxWorldLoops() { return  MAX_WORLD_LOOPS;}
    private static final float CENTER_TOLERANCE = 10f * Game.SCALE;

    private int     worldLoopCount = 0;
    private boolean worldLoopDone  = false;
    private boolean dKeyHeld       = false;


    // ── Cloud scroll accumulators ────────────────────────────
    private float bigCloudOffset   = 0f;
    private float smallCloudOffset = 0f;

    private static final float BIG_CLOUD_PARALLAX   = 0.3f;
    private static final float SMALL_CLOUD_PARALLAX = 0.5f;

    private PassengerCounter passengerCounter;

    // ── Background ───────────────────────────────────────────
    private BufferedImage backgroundImg, bigClouds, smallClouds;
    private int[] smallCloudsPos;
    private final Random rnd = new Random();
    // Decorative props such as bus stops and future buildings can exist at run start
    // and can also be scheduled from stop-count milestones.
    private WorldObjectManager worldObjectManager;

    // ── Current route position ────────────────────────────────
    private int    currentStopIndex = 0;
    private final java.util.Random rng = new java.util.Random();

    // ─────────────────────────────────────────────────────────
    public Playing(Game game) {
        super(game);
        initClasses();
        loadBackgroundAssets();
    }

    /** Called by CharSelectState before gameplay begins. */
    public void applyDriver(entities.DriverProfile profile) {
        player.applyDriver(profile);
        System.out.println("Driver selected: " + profile.displayName
                + " | Speed: " + profile.maxSpeed);
    }

    /** Used by Game.getDesiredMusicTrack(). */
    //public boolean isPaused() { return paused; }

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
                (int)(110 * Game.SCALE), (int)(40 * Game.SCALE), game.getGamePanel());
        player.loadLvlData(levelManager.getCurrentLevel().getLevelData());

        // ✨ NEW: Apply default driver immediately
        // This ensures animations[] is never null during char select/menu
        DriverProfile defaultDriver = DriverProfile.ALL[0];  // Kuya Ben
        player.applyDriver(defaultDriver);


        personManager   = new PersonManager(this);
        enemyManager    = new EnemyManager(this);
        worldObjectManager = new WorldObjectManager();
        stopSignManager = new StopSignManager(this, worldObjectManager);
        pauseOverlay    = new PauseOverlay(this);
        powerupManager  = new PowerupManager(this);
        healthBar       = new HealthBar();
        passengerCounter = new PassengerCounter();
        deathOverlay    = new DeathOverlay(this);
        acceptPassengerOverlay = new AcceptPassengerOverlay(this, passengerCounter);  // ← init modal
        progressBar     = new ProgressBar();
    }

    private void loadBackgroundAssets() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PLAYING_BACKGROUND_IMG);
        bigClouds     = LoadSave.getSpriteAtlas(LoadSave.BIG_CLOUDS);
        smallClouds   = LoadSave.getSpriteAtlas(LoadSave.SMALL_CLOUDS);

        smallCloudsPos = new int[8];
        for (int i = 0; i < smallCloudsPos.length; i++)
            smallCloudsPos[i] = (int)(20 * Game.SCALE) + rnd.nextInt((int)(100 * Game.SCALE));
    }

    // ── Full game restart ────────────────────────────────────
    public void restartGame() {
        worldOffset    = 0;
        worldLoopCount = 0;
        currentStopIndex = 0;
        worldLoopDone  = false;
        dKeyHeld       = false;
        playerDead     = false;

        bigCloudOffset   = 0f;
        smallCloudOffset = 0f;

        for (int i = 0; i < smallCloudsPos.length; i++)
            smallCloudsPos[i] = (int)(20 * Game.SCALE) + rnd.nextInt((int)(100 * Game.SCALE));

        int jeepHitboxW = (int)(70 * Game.SCALE);
        player.getHitBox().x = (float) (Game.GAME_WIDTH - jeepHitboxW) / 2;
        player.getHitBox().y = 520;
        player.resetDirBooleans();
        player.setWorldLoopDone(false);

        worldObjectManager.reset();

        personManager.resetAll();
        enemyManager.resetAll();
        stopSignManager.resetAll();
        powerupManager.resetAll();
        healthBar.reset();
        passengerCounter.reset();
        progressBar.reset();

        // Reset interaction state
        interactionPaused = false;
        acceptPassengerOverlay.close();
        acceptPassengerOverlay.resetPassengerCount(); //
        acceptPassengerOverlay.resetEarnings();
        paused = false;
    }

    // ── Health callbacks ─────────────────────────────────────
    public void onPlayerHit() {
        boolean dead = healthBar.takeDamage();
        if (dead) {
            playerDead = true;
            deathOverlay.reset();
        }
    }

    public void onPlayerHeal() {
        healthBar.heal();
    }

    // ── Scrolling condition ──────────────────────────────────
    public boolean isScrolling() {
        boolean hasSpeed = player.getCurrentXSpeed() > 0;
        return (dKeyHeld || hasSpeed) && isJeepCentered() && !paused && !worldLoopDone
                && !player.isStruckActive() && !playerDead;
    }

    public float getScrollSpeed() { return player.getCurrentXSpeed(); }

    private boolean isJeepCentered() {
        float jeepCenterX   = player.getHitBox().x + player.getHitBox().width / 2f;
        float screenCenterX = Game.GAME_WIDTH / 2f;
        return Math.abs(jeepCenterX - screenCenterX) <= CENTER_TOLERANCE;
    }

    // ── Boss fight transition helper ─────────────────────────
    /**
     * Central method that applies all three toggle rules:
     *   1. SKIP_TO_BOSS uncommented  → go to boss immediately (ignore disable)
     *   2. DISABLE_BOSS_FIGHT uncommented → loop forever, never go to boss
     *   3. Both commented (normal)   → go to boss after MAX_WORLD_LOOPS
     */
    private void handleLoopComplete() {
        // ── SKIP_TO_BOSS check ──────────────────────────────
        // (field won't exist when the constant is commented — compile-time toggle)
        // This block is always evaluated; the skip only fires when the constant exists.


        worldLoopDone = true;
        worldOffset   = 0;
        game.startBossFight();
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    @Override
    public void update() {
        // ── SKIP_TO_BOSS: uncomment the block below to skip loops ──
        if (true) { game.startBossFight(); return; }

        if (playerDead) {
            deathOverlay.update();
            return;
        }

        // Keep modal ticking even while interaction is paused
        acceptPassengerOverlay.update();

        // ── Modal open — only update the modal, freeze everything else ──
        if (interactionPaused) {
            // Close modal if it shut itself (both buttons call close())
            if (!acceptPassengerOverlay.isOpen()) {
                interactionPaused = false;
            }
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

                        // Debug output: prints once per full level wrap.
                        System.out.println("World loops: " + worldLoopCount);
                        if (worldLoopCount >= MAX_WORLD_LOOPS) {

                            worldLoopDone = true;
                            worldOffset   = 0;
                            game.startBossFight(); // ← comment out if DISABLE_BOSS active
                        }
                    }

                    bigCloudOffset += spd * BIG_CLOUD_PARALLAX;
                    if (bigCloudOffset >= BIG_CLOUD_WIDTH)
                        bigCloudOffset -= BIG_CLOUD_WIDTH;

                    smallCloudOffset += spd * SMALL_CLOUD_PARALLAX;
                    if (smallCloudOffset >= SMALL_CLOUD_WIDTH)
                        smallCloudOffset -= SMALL_CLOUD_WIDTH;
                }
            }

            levelManager.update();
            personManager.update();
            checkPassengerInteractions();   // ← renamed + expanded
            enemyManager.update();
            stopSignManager.update();
            worldObjectManager.update(scrolling, getScrollSpeed());
            powerupManager.update();
            player.update();

        } else {
            pauseOverlay.update();
        }
    }

    // ─────────────────────────────────────────────────────────
    // PASSENGER INTERACTION SCAN
    // ─────────────────────────────────────────────────────────
    private void checkPassengerInteractions() {
        // if full 9/9 early return
        if(passengerCounter.isFull()) return;

        Rectangle2D.Float jeepHB = player.getHitBox();
        if (jeepHB == null) return;

        for (Person p : personManager.getPersons()) {
            if (p.getType() != Person.PersonType.PASSENGER) continue;
            if (!p.isActive()) continue;

            Rectangle2D.Float pHB = p.getHitBox();
            if (pHB == null) continue;

            boolean overlapping = jeepHB.intersects(pHB);

            // Print only on the rising edge (was false, now true)
            if (overlapping && !p.isInteractable()) {
                System.out.println("Passenger ready for interaction");
            }

            p.setInteractable(overlapping);
        }
    }

    // ─────────────────────────────────────────────────────────
    // DRAW
    // ─────────────────────────────────────────────────────────
    @Override
    public void draw(Graphics g) {
        // Always draw the frozen game world underneath
        if (backgroundImg != null)
            g.drawImage(backgroundImg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        drawClouds(g);
        levelManager.draw(g, (int) worldOffset);
        // Decorative roadside props should sit behind characters in the scene.
        worldObjectManager.draw(g);
        enemyManager.render(g);
        personManager.render(g);
        stopSignManager.render(g);
        powerupManager.render(g);

        player.render(g);

        // ── UI layer — always on top ──────────────────────────
        healthBar.render(g);
        passengerCounter.render(g);

        // Modal renders on top of the frozen world
        progressBar.render(g);

        acceptPassengerOverlay.render(g);

        if (playerDead) {
            deathOverlay.render(g);
            return;
        }

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
            g.drawImage(bigClouds, drawX, (int)(40 * Game.SCALE),
                    BIG_CLOUD_WIDTH, BIG_CLOUD_HEIGHT, null);
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
    // INPUT
    // ─────────────────────────────────────────────────────────
    @Override
    public void mouseClicked(MouseEvent e) {
        if (playerDead) return;
        if (e.getButton() == MouseEvent.BUTTON1) player.setAttacking(true);
        if (playerDead)        return;
        if (interactionPaused) return;

        if (e.getButton() == MouseEvent.BUTTON1) {
            for (Person p : personManager.getPersons()) {
                if (!p.isInteractable()) continue;

                Rectangle2D.Float pHB = p.getHitBox();
                if (pHB != null && pHB.contains(e.getX(), e.getY())) {
                    System.out.println("Passenger clicked");
                    System.out.println("Game paused");

                    // ── Assign destination and fare on click ──────
                    int destIndex = RouteConstants.randomForwardStopIndex(currentStopIndex, rng);
                    p.setDestinationStop(RouteConstants.STOPS[destIndex]);
                    p.setFare(RouteConstants.randomFare(rng));

                    System.out.println("Destination: " + p.getDestinationStop());
                    System.out.println("Fare: ₱" + p.getFare());

                    interactionPaused = true;
                    acceptPassengerOverlay.open(p);
                    return;
                }
            }
            player.setAttacking(true);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (playerDead)        { deathOverlay.mousePressed(e); return; }
        if (interactionPaused) { acceptPassengerOverlay.mousePressed(e); return; }
        if (paused)              pauseOverlay.mousePressed(e);
    }

    public void mouseDragged(MouseEvent e) {
        if (playerDead)          return;
        if (interactionPaused) { acceptPassengerOverlay.mouseDragged(e); return; }
        if (paused)              pauseOverlay.mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (playerDead)        { deathOverlay.mouseReleased(e); return; }
        if (interactionPaused) { acceptPassengerOverlay.mouseReleased(e); return; }
        if (paused)              pauseOverlay.mouseReleased(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (playerDead)        { deathOverlay.mouseMoved(e); return; }
        acceptPassengerOverlay.mouseMoved(e);   // always forward — needed for button hover
        if (paused)              pauseOverlay.mouseMoved(e);
    }

    public void unPauseGame() { paused = false; }

    @Override
    public void keyPressed(KeyEvent e) {
        if (playerDead)        return;
        if (interactionPaused) return;   // block key input during modal
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A:      player.setLeft(true);                   break;
            case KeyEvent.VK_D:      player.setRight(true); dKeyHeld = true; break;
            case KeyEvent.VK_W:      player.setUp(true);                     break;
            case KeyEvent.VK_S:      player.setDown(true);                   break;
            case KeyEvent.VK_ESCAPE: paused = !paused;                       break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (playerDead) return;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: player.setLeft(false);                       break;
            case KeyEvent.VK_D: player.setRight(false); dKeyHeld = false;    break;
            case KeyEvent.VK_W: player.setUp(false);                         break;
            case KeyEvent.VK_S: player.setDown(false);                       break;
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

    public void windowFocusLost() {
        player.resetDirBooleans();
        dKeyHeld = false;
    }

    // ── Getters ───────────────────────────────────────────────
    public Player       getPlayer()         { return player; }
    public HealthBar    getHealthBar()      { return healthBar; }
    public LevelManager getLevelManager()   { return levelManager; }
    public float        getWorldOffset()    { return worldOffset; }
    public int          getWorldLoopCount() { return worldLoopCount; }
    public boolean      isPaused()          { return paused; }
}
