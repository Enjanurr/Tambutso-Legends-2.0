package gameStates;

import Ui.DeathOverlay;
import Ui.HealthBar;
import Ui.IntroOverlay;
import Ui.PauseOverlay;
import Ui.ProgressBar;
import entities.EnemyManager;
import entities.PersonManager;
import entities.Player;
import entities.PowerupManager;
import objects.StopSignManager;
import levels.LevelManager;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

import static utils.Constants.Environment.*;

public class Playing extends State implements StateMethods {

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
    private IntroOverlay    introOverlay;

    private boolean paused     = false;
    private boolean playerDead = false;
    private boolean introShown = false; // Track if intro has been shown this session
    private boolean showIntro  = false; // Flag to show intro on game start

    // ── World scrolling ──────────────────────────────────────
    private float worldOffset = 0;
    private final int levelPixelWidth =
            LoadSave.GetLevelData()[0].length * Game.TILES_SIZE;

    // -------------------------------------------------------
    // WORLD SCROLL SETTINGS
    // -------------------------------------------------------
    private static final int MAX_WORLD_LOOPS = 15;
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

    // ── Background ───────────────────────────────────────────
    private BufferedImage backgroundImg, bigClouds, smallClouds;
    private int[] smallCloudsPos;
    private final Random rnd = new Random();

    public Playing(Game game) {
        super(game);
        initClasses();
        loadBackgroundAssets();
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

        personManager   = new PersonManager(this);
        enemyManager    = new EnemyManager(this);
        stopSignManager = new StopSignManager(this);
        pauseOverlay    = new PauseOverlay(this);
        powerupManager  = new PowerupManager(this);
        healthBar       = new HealthBar();
        deathOverlay    = new DeathOverlay(this);
        progressBar     = new ProgressBar();
        introOverlay    = new IntroOverlay();
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
        worldLoopDone  = false;
        dKeyHeld       = false;
        playerDead     = false;

        bigCloudOffset   = 0f;
        smallCloudOffset = 0f;

        for (int i = 0; i < smallCloudsPos.length; i++)
            smallCloudsPos[i] = (int)(20 * Game.SCALE) + rnd.nextInt((int)(100 * Game.SCALE));

        int jeepHitboxW = (int)(70 * Game.SCALE);
        int spawnX      = (Game.GAME_WIDTH - jeepHitboxW) / 2;
        player.getHitBox().x = spawnX;
        player.getHitBox().y = 520;
        player.resetDirBooleans();
        player.setWorldLoopDone(false);

        personManager.resetAll();
        enemyManager.resetAll();
        stopSignManager.resetAll();
        powerupManager.resetAll();
        healthBar.reset();
        progressBar.reset();

        paused = false;
    }

    // ── Start game with optional intro (called from Menu Play button) ─
    public void startGameWithOptionalIntro() {
        GameStates.state = GameStates.PLAYING;

        if (!introShown) {
            // First time: show intro, start paused
            showIntro = true;
            paused = true;
            introOverlay.reset();
        } else {
            // Subsequent times: skip intro, start unpaused
            showIntro = false;
            paused = false;
        }
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
        //if (true) { game.startBossFight(); return; }

        // ── Intro overlay handling ─────────────────────────────
        if (showIntro) {
            boolean introDone = introOverlay.update();
            if (introDone) {
                showIntro = false;
                introShown = true; // Mark as shown for this session
                paused = false;    // Unpause the game
            }
            return;
        }

        if (playerDead) {
            deathOverlay.update();
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
            enemyManager.update();
            stopSignManager.update();
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
        if (backgroundImg != null)
            g.drawImage(backgroundImg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        drawClouds(g);
        levelManager.draw(g, (int) worldOffset);
        personManager.render(g);
        enemyManager.render(g);
        stopSignManager.render(g);
        powerupManager.render(g);
        player.render(g);

        // ── UI layer — always on top ──────────────────────────
        healthBar.render(g);
        progressBar.render(g);

        if (playerDead) {
            deathOverlay.render(g);
            return;
        }

        if (paused) {
            // Show pause overlay dim only if not showing intro
            // (intro has its own fade overlay)
            if (!showIntro) {
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
                pauseOverlay.draw(g);
            }
        }

        // Draw intro overlay on top when active
        if (showIntro) {
            introOverlay.render(g);
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
        if (showIntro) return; // Block clicks during intro
        if (playerDead) return;
        if (e.getButton() == MouseEvent.BUTTON1) player.setAttacking(true);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (showIntro) { introOverlay.mousePressed(e); return; }
        if (playerDead) { deathOverlay.mousePressed(e); return; }
        if (paused) pauseOverlay.mousePressed(e);
    }

    public void mouseDragged(MouseEvent e) {
        if (showIntro) return;
        if (playerDead) return;
        if (paused) pauseOverlay.mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (showIntro) { introOverlay.mouseReleased(e); return; }
        if (playerDead) { deathOverlay.mouseReleased(e); return; }
        if (paused) pauseOverlay.mouseReleased(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (showIntro) { introOverlay.mouseMoved(e); return; }
        if (playerDead) { deathOverlay.mouseMoved(e); return; }
        if (paused) pauseOverlay.mouseMoved(e);
    }

    public void unPauseGame() { paused = false; }

    @Override
    public void keyPressed(KeyEvent e) {
        if (showIntro) return; // Block key inputs during intro
        if (playerDead) return;
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
        if (showIntro) return; // Block key inputs during intro
        if (playerDead) return;
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: player.setLeft(false);                       break;
            case KeyEvent.VK_D: player.setRight(false); dKeyHeld = false;    break;
            case KeyEvent.VK_W: player.setUp(false);                         break;
            case KeyEvent.VK_S: player.setDown(false);                       break;
        }
    }

    public void onJeepLooped() {
        personManager.resetAll();
        enemyManager.resetAll();
        stopSignManager.resetAll();
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
}