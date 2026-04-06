package gameStates;

import Ui.DeathOverlay;
import Ui.HealthBar;
import Ui.PauseOverlay;
import entities.EnemyManager;
import entities.PersonManager;
import entities.Player;
import entities.PowerupManager;
import objects.StopSignManager;
import objects.WorldObjectManager;
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
    // Playing orchestrates the active run and delegates specialized behavior
    // to managers for enemies, powerups, interactive stop signs, and
    // decorative roadside objects.
    private PowerupManager  powerupManager;
    private Player          player;
    private PersonManager   personManager;
    private EnemyManager    enemyManager;
    private StopSignManager stopSignManager;
    private LevelManager    levelManager;
    private PauseOverlay    pauseOverlay;
    private HealthBar       healthBar;
    private DeathOverlay    deathOverlay;

    private boolean paused     = false;
    private boolean playerDead = false;     // true while death screen is showing

    // ── World scrolling ──────────────────────────────────────
    private float worldOffset = 0;
    private final int levelPixelWidth =
            LoadSave.GetLevelData()[0].length * Game.TILES_SIZE;

    // -------------------------------------------------------
    // WORLD SCROLL SETTINGS
    // -------------------------------------------------------
    public static final int MAX_WORLD_LOOPS = 15;
    // -------------------------------------------------------

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
    // Decorative props such as bus stops and future buildings can exist at run start
    // and can also be scheduled from stop-count milestones.
    private WorldObjectManager worldObjectManager;

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
        worldObjectManager = new WorldObjectManager();
        stopSignManager = new StopSignManager(this, worldObjectManager);
        pauseOverlay    = new PauseOverlay(this);
        powerupManager  = new PowerupManager(this);
        healthBar       = new HealthBar();
        deathOverlay    = new DeathOverlay(this);
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
        playerDead     = false;         // close death screen

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

        paused = false;
    }

    // ── Health callbacks ─────────────────────────────────────
    /**
     * Called by EnemyManager after a collision.
     * Deducts one bar — shows death screen if health reaches 0.
     */
    public void onPlayerHit() {
        boolean dead = healthBar.takeDamage();
        if (dead) {
            playerDead = true;
            deathOverlay.reset();   // restart the fade-in fresh
        }
    }

    /**
     * Called by PowerupManager when Heal powerup is collected.
     */
    public void onPlayerHeal() {
        healthBar.heal();
    }

    // ── Scrolling condition ──────────────────────────────────
    public boolean isScrolling() {
        // Freeze world while death screen is showing
        return dKeyHeld && isJeepCentered() && !paused && !worldLoopDone
                && !player.isStruckActive() && !playerDead;
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
        // Death screen active — only update the overlay, freeze everything else
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
                        // Debug output: prints once per full level wrap.
                        System.out.println("World loops: " + worldLoopCount);
                        if (worldLoopCount >= MAX_WORLD_LOOPS) {
                            worldLoopDone = true;
                            worldOffset   = 0;
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
        if (backgroundImg != null)
            g.drawImage(backgroundImg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        drawClouds(g);
        levelManager.draw(g, (int) worldOffset);
        // Decorative roadside props should sit behind characters in the scene.
        worldObjectManager.draw(g);
        personManager.render(g);
        enemyManager.render(g);
        stopSignManager.render(g);
        powerupManager.render(g);

        player.render(g);

        // ── UI layer ──────────────────────────────────────────
        healthBar.render(g);

        // Death screen drawn on top of everything
        if (playerDead) {
            deathOverlay.render(g);
            return;                 // skip pause overlay while dead
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
        if (playerDead) return;     // ignore game clicks during death screen
        if (e.getButton() == MouseEvent.BUTTON1) player.setAttacking(true);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (playerDead) { deathOverlay.mousePressed(e); return; }
        if (paused) pauseOverlay.mousePressed(e);
    }

    public void mouseDragged(MouseEvent e) {
        if (playerDead) return;
        if (paused) pauseOverlay.mouseDragged(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (playerDead) { deathOverlay.mouseReleased(e); return; }
        if (paused) pauseOverlay.mouseReleased(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (playerDead) { deathOverlay.mouseMoved(e); return; }
        if (paused) pauseOverlay.mouseMoved(e);
    }

    public void unPauseGame() { paused = false; }

    @Override
    public void keyPressed(KeyEvent e) {
        if (playerDead) return;     // ignore all key input during death screen
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
    public Player getPlayer()         { return player; }
    public float  getWorldOffset()    { return worldOffset; }
    public int    getWorldLoopCount() { return worldLoopCount; }
}
