package gameStates;

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

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

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

    private boolean paused              = false;
    private boolean playerDead          = false;
    private final PlayingDebugOverlay debugOverlay = new PlayingDebugOverlay();
    private PassengerInteractionController passengerInteractionController;
    private final PlayingWorldController worldController = new PlayingWorldController();

    // -------------------------------------------------------
    // WORLD SCROLL SETTINGS
    // -------------------------------------------------------
    public static final int MAX_WORLD_LOOPS = 15;
    // -------------------------------------------------------
    public int getMaxWorldLoops() { return  MAX_WORLD_LOOPS;}

    private PassengerCounter passengerCounter;
    // Decorative props such as bus stops and future buildings can exist at run start
    // and can also be scheduled from stop-count milestones.
    private WorldObjectManager worldObjectManager;

    // ── Current route position ────────────────────────────────
    private RouteMap currentMap = RouteMap.MAP_1;

    // ─────────────────────────────────────────────────────────
    public Playing(Game game) {
        super(game);
        initClasses();
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
        passengerInteractionController.resumeInteraction();
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
        worldObjectManager = new WorldObjectManager(currentMap);
        stopSignManager = new StopSignManager(this, worldObjectManager);
        pauseOverlay    = new PauseOverlay(this);
        powerupManager  = new PowerupManager(this);
        healthBar       = new HealthBar();
        passengerCounter = new PassengerCounter();
        deathOverlay    = new DeathOverlay(this);
        passengerInteractionController = new PassengerInteractionController(this, passengerCounter);
        progressBar     = new ProgressBar();
    }

    // ── Full game restart ────────────────────────────────────
    public void restartGame() {
        playerDead     = false;
        worldController.reset(player);

        worldObjectManager.reset();

        personManager.resetAll();
        enemyManager.resetAll();
        stopSignManager.resetAll();
        powerupManager.resetAll();
        healthBar.reset();
        passengerCounter.reset();
        progressBar.reset();

        // Reset interaction state
        passengerInteractionController.reset();
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
        worldController.markLoopDone();
        game.startBossFight();
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    @Override
    public void update() {
        // ── SKIP_TO_BOSS: uncomment the block below to skip loops ──
        //if (true) { game.startBossFight(); return; }

        if (playerDead) {
            deathOverlay.update();
            return;
        }

        // Keep modal ticking even while interaction is paused
        passengerInteractionController.updateOverlay();

        // ── Modal open — only update the modal, freeze everything else ──
        if (passengerInteractionController.isInteractionPaused()) {
            // Close modal if it shut itself (both buttons call close())
            passengerInteractionController.closeIfOverlayClosed();
            return;
        }

        if (!paused) {
            boolean loopCompleted = worldController.updateScroll(player, false, playerDead);
            boolean scrolling = worldController.isScrolling(player, paused, playerDead);

            if (loopCompleted) {
                progressBar.onLoopCompleted();
                if (worldController.getWorldLoopCount() >= MAX_WORLD_LOOPS) {
                    handleLoopComplete();
                }
            }

            levelManager.update();
            personManager.update();
            passengerInteractionController.checkPassengerInteractions(player, personManager);
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
    // DRAW
    // ─────────────────────────────────────────────────────────
    @Override
    public void draw(Graphics g) {
        // Always draw the frozen game world underneath
        worldController.drawBackground(g);
        worldController.drawClouds(g);
        levelManager.draw(g, (int) worldController.getWorldOffset());
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

        passengerInteractionController.renderOverlay(g);
        debugOverlay.draw(g, worldObjectManager, currentMap, worldController.getWorldLoopCount());

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

    // ─────────────────────────────────────────────────────────
    // INPUT
    // ─────────────────────────────────────────────────────────
    @Override
    public void mouseClicked(MouseEvent e) {
        if (playerDead) return;
        if (e.getButton() == MouseEvent.BUTTON1) player.setAttacking(true);
        if (playerDead)        return;
        if (passengerInteractionController.isInteractionPaused()) return;

        if (e.getButton() == MouseEvent.BUTTON1) {
            if (passengerInteractionController.handleMouseClick(e, personManager)) return;
            player.setAttacking(true);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (playerDead)        { deathOverlay.mousePressed(e); return; }
        if (passengerInteractionController.isInteractionPaused()) {
            passengerInteractionController.mousePressed(e);
            return;
        }
        if (paused)              pauseOverlay.mousePressed(e);
    }

    public void mouseDragged(MouseEvent e) {
        if (playerDead)          return;
        if (passengerInteractionController.isInteractionPaused()) {
            passengerInteractionController.mouseDragged(e);
            return;
        }
        if (paused)              pauseOverlay.mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (playerDead)        { deathOverlay.mouseReleased(e); return; }
        if (passengerInteractionController.isInteractionPaused()) {
            passengerInteractionController.mouseReleased(e);
            return;
        }
        if (paused)              pauseOverlay.mouseReleased(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (playerDead)        { deathOverlay.mouseMoved(e); return; }
        passengerInteractionController.mouseMoved(e);
        if (paused)              pauseOverlay.mouseMoved(e);
    }

    public void unPauseGame() { paused = false; }

    @Override
    public void keyPressed(KeyEvent e) {
        if (playerDead)        return;
        if (e.getKeyCode() == KeyEvent.VK_F3) {
            debugOverlay.toggleLandmarkDebug();
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_F4) {
            debugOverlay.toggleAlignmentGrid();
            return;
        }
        if (passengerInteractionController.isInteractionPaused()) return;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A:      player.setLeft(true);                   break;
            case KeyEvent.VK_D:      player.setRight(true); worldController.setDKeyHeld(true); break;
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
            case KeyEvent.VK_D: player.setRight(false); worldController.setDKeyHeld(false); break;
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

    public void setCurrentMap(RouteMap map) {
        currentMap = map;
        worldObjectManager.setCurrentMap(map);
    }

    public void windowFocusLost() {
        worldController.onWindowFocusLost(player);
    }

    // ── Getters ───────────────────────────────────────────────
    public Player       getPlayer()         { return player; }
    public HealthBar    getHealthBar()      { return healthBar; }
    public LevelManager getLevelManager()   { return levelManager; }
    public RouteMap     getCurrentMap()     { return currentMap; }
    public float        getWorldOffset()    { return worldController.getWorldOffset(); }
    public int          getWorldLoopCount() { return worldController.getWorldLoopCount(); }
    public boolean      isPaused()          { return paused; }
    public boolean      isScrolling()       { return worldController.isScrolling(player, paused, playerDead); }
    public float        getScrollSpeed()    { return worldController.getScrollSpeed(player); }
}
