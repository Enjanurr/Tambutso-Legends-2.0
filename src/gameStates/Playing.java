package gameStates;

import Ui.PauseOverlay;
import entities.PersonManager;
import entities.Player;
import entities.WorldObject;
import levels.LevelManager;
import utils.LoadSave;
import main.Game;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

import static utils.Constants.Environment.*;

public class Playing extends State implements StateMethods {


    private Player        player;
    private PersonManager personManager;
    private LevelManager  levelManager;
    private PauseOverlay  pauseOverlay;
    private boolean paused = false;

    // ── World scrolling ──────────────────────────────────────
    private float worldOffset = 0;
    private final int levelPixelWidth =
            LoadSave.GetLevelData()[0].length * Game.TILES_SIZE;

    // -------------------------------------------------------
    // WORLD SCROLL SETTINGS
    // -------------------------------------------------------
    private static final float WORLD_SCROLL_SPEED = 1.6f; // ← ADJUST: pixels per tick
    private static final int   MAX_WORLD_LOOPS    = 15;   // ← ADJUST: total world loops
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
    private BufferedImage backgroundImg, bigClouds, smallClouds, busStop;
    private int[] smallCloudsPos;
    private final Random rnd = new Random();

    // World objects like bus stops, signs, etc.
    private final List<WorldObject> worldObjects = new ArrayList<>();

    // Resouce cleaner
    private int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL_FRAMES = 60; // run heavy cleanup every 60 frames
    private static final int CLEANUP_THRESHOLD = 8; // run immediate cleanup if list grows beyond this

    public Playing(Game game) {
        super(game);
        initClasses();
        loadBackgroundAssets();
    }

    private void initClasses() {
        levelManager = new LevelManager(game);

        // Spawn jeep at horizontal screen centre
        int jeepHitboxW = (int)(70 * Game.SCALE);
        int spawnX      = (Game.GAME_WIDTH - jeepHitboxW) / 2;
        int spawnY      = 520;

        player = new Player(spawnX, spawnY,
                (int)(110 * Game.SCALE), (int)(40 * Game.SCALE),
                game.getGamePanel());
        player.loadLvlData(levelManager.getCurrentLevel().getLevelData());

        personManager = new PersonManager(this);
        pauseOverlay  = new PauseOverlay(this);
    }

    private void loadBackgroundAssets() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PLAYING_BACKGROUND_IMG);
        bigClouds     = LoadSave.getSpriteAtlas(LoadSave.BIG_CLOUDS);
        smallClouds   = LoadSave.getSpriteAtlas(LoadSave.SMALL_CLOUDS);
        busStop       = LoadSave.getSpriteAtlas(LoadSave.BUS_STOP);

        // create bus stop world object
        if (busStop != null) {
            int busW = busStop.getWidth();
            int busH = busStop.getHeight();
            int busWorldX = 50;   // world X coordinate for the bus stop
            int busWorldY = 175;  // world Y coordinate for the bus stop
            int loopsToRespawn = 3; // reappear after N full loops
            worldObjects.add(new WorldObject(busWorldX, busWorldY, busW, busH, busStop, loopsToRespawn));
        }

        smallCloudsPos = new int[8];
        for (int i = 0; i < smallCloudsPos.length; i++)
            smallCloudsPos[i] = (int)(20 * Game.SCALE) + rnd.nextInt((int)(100 * Game.SCALE));
    }

    // ── Full game restart — called by PauseOverlay restart button ──
    public void restartGame() {
        // Reset world scroll state
        worldOffset    = 0;
        worldLoopCount = 0;
        worldLoopDone  = false;
        dKeyHeld       = false;

        // Reset cloud offsets
        bigCloudOffset   = 0f;
        smallCloudOffset = 0f;

        // Randomise new cloud positions
        for (int i = 0; i < smallCloudsPos.length; i++)
            smallCloudsPos[i] = (int)(20 * Game.SCALE) + rnd.nextInt((int)(100 * Game.SCALE));

        // reset world objects so they reappear on a full restart
        for (WorldObject obj : worldObjects) obj.reset();

        // Reset player to centre screen with no speed
        int jeepHitboxW = (int)(70 * Game.SCALE);
        int spawnX      = (Game.GAME_WIDTH - jeepHitboxW) / 2;
        player.getHitBox().x = spawnX;
        player.getHitBox().y = 520;
        player.resetDirBooleans();

        // Clear all persons and reset spawn timers
        personManager.resetAll();

        // Close pause menu and resume play
        paused = false;
    }

    public boolean isScrolling() {
        return dKeyHeld && isJeepCentered() && !paused && !worldLoopDone;
    }

    public float getScrollSpeed() { return WORLD_SCROLL_SPEED; }

    private boolean isJeepCentered() {
        float jeepCenterX   = player.getHitBox().x + player.getHitBox().width / 2f;
        float screenCenterX = Game.GAME_WIDTH / 2f;
        return Math.abs(jeepCenterX - screenCenterX) <= CENTER_TOLERANCE;
    }

    @Override
    public void update() {
        if (!paused) {
            boolean scrolling = isScrolling();
            player.setWorldScrolling(scrolling);

            if (scrolling) {
                worldOffset += WORLD_SCROLL_SPEED;
                if (worldOffset >= levelPixelWidth) {
                    worldOffset -= levelPixelWidth;
                    worldLoopCount++;
                    if (worldLoopCount >= MAX_WORLD_LOOPS) {
                        worldLoopDone = true;
                        worldOffset   = 0;
                    }
                }

                bigCloudOffset += WORLD_SCROLL_SPEED * BIG_CLOUD_PARALLAX;
                if (bigCloudOffset >= BIG_CLOUD_WIDTH)
                    bigCloudOffset -= BIG_CLOUD_WIDTH;

                smallCloudOffset += WORLD_SCROLL_SPEED * SMALL_CLOUD_PARALLAX;
                if (smallCloudOffset >= SMALL_CLOUD_WIDTH)
                    smallCloudOffset -= SMALL_CLOUD_WIDTH;
            }

            // update world objects with current loop count
            for (WorldObject obj : worldObjects) {
                obj.update(worldOffset, Game.GAME_WIDTH, worldLoopCount, levelPixelWidth);
            }

            // quick removal of permanently removable objects
            if (worldObjects.size() > CLEANUP_THRESHOLD) {
                // immediate cleanup when list is large
                worldObjects.removeIf(obj -> {
                    if (obj.isRemovable()) {
                        obj.dispose();
                        obj.markRemoved();
                        return true;
                    }
                    return false;
                });
            } else {
                // periodic cleanup to reduce per-frame cost
                cleanupCounter++;
                if (cleanupCounter >= CLEANUP_INTERVAL_FRAMES) {
                    cleanupCounter = 0;
                    worldObjects.removeIf(obj -> {
                        if (obj.isRemovable()) {
                            obj.dispose();
                            obj.markRemoved();
                            return true;
                        }
                        return false;
                    });
                }
            }

            levelManager.update();
            personManager.update();
            player.update();
        } else {
            pauseOverlay.update();
        }
    }

    @Override
    public void draw(Graphics g) {
        if (backgroundImg != null)
            g.drawImage(backgroundImg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        drawClouds(g);
        levelManager.draw(g, (int) worldOffset);
        personManager.render(g);
        player.render(g);

        // take a single snapshot to avoid ConcurrentModificationException
        List<WorldObject> snapshot = new ArrayList<>(worldObjects);
        for (WorldObject obj : snapshot) {
            obj.draw(g, worldOffset, Game.GAME_WIDTH);
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

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) player.setAttacking(true);
    }

    @Override public void mousePressed(MouseEvent e)  { if (paused) pauseOverlay.mousePressed(e); }
    public    void mouseDragged(MouseEvent e)         { if (paused) pauseOverlay.mouseDragged(e); }
    @Override public void mouseReleased(MouseEvent e) { if (paused) pauseOverlay.mouseReleased(e); }
    @Override public void mouseMoved(MouseEvent e)    { if (paused) pauseOverlay.mouseMoved(e); }

    public void unPauseGame() { paused = false; }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A:      player.setLeft(true);                break;
            case KeyEvent.VK_D:      player.setRight(true); dKeyHeld = true; break;
            case KeyEvent.VK_W:      player.setUp(true);                  break;
            case KeyEvent.VK_S:      player.setDown(true);                break;
            case KeyEvent.VK_ESCAPE: paused = !paused;                    break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: player.setLeft(false);                     break;
            case KeyEvent.VK_D: player.setRight(false); dKeyHeld = false;  break;
            case KeyEvent.VK_W: player.setUp(false);                       break;
            case KeyEvent.VK_S: player.setDown(false);                     break;
        }
    }

    public void onJeepLooped() { personManager.resetAll(); }

    public void windowFocusLost() {
        player.resetDirBooleans();
        dKeyHeld = false;
    }

    public Player getPlayer()      { return player; }
    public float  getWorldOffset() { return worldOffset; }
}