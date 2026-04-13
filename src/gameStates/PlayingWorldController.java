package gameStates;

import entities.Player;
import main.Game;
import utils.LoadSave;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Random;

import static utils.Constants.Environment.BIG_CLOUD_HEIGHT;
import static utils.Constants.Environment.BIG_CLOUD_WIDTH;
import static utils.Constants.Environment.SMALL_CLOUD_HEIGHT;
import static utils.Constants.Environment.SMALL_CLOUD_WIDTH;

public class PlayingWorldController {
    private static final float BIG_CLOUD_PARALLAX = 0.3f;
    private static final float SMALL_CLOUD_PARALLAX = 0.5f;
    private static final float CENTER_TOLERANCE = 10f * Game.SCALE;

    private final int levelPixelWidth = LoadSave.GetLevelData()[0].length * Game.TILES_SIZE;
    private final Random rnd = new Random();

    private BufferedImage backgroundImg;
    private BufferedImage bigClouds;
    private BufferedImage smallClouds;
    private int[] smallCloudsPos;

    private float worldOffset = 0f;
    private int worldLoopCount = 0;
    private boolean worldLoopDone = false;
    private boolean dKeyHeld = false;
    private float bigCloudOffset = 0f;
    private float smallCloudOffset = 0f;

    public PlayingWorldController() {
        loadBackgroundAssets();
    }

    private void loadBackgroundAssets() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PLAYING_BACKGROUND_IMG);
        bigClouds = LoadSave.getSpriteAtlas(LoadSave.BIG_CLOUDS);
        smallClouds = LoadSave.getSpriteAtlas(LoadSave.SMALL_CLOUDS);

        smallCloudsPos = new int[8];
        randomizeSmallClouds();
    }

    public void reset(Player player) {
        worldOffset = 0f;
        worldLoopCount = 0;
        worldLoopDone = false;
        dKeyHeld = false;
        bigCloudOffset = 0f;
        smallCloudOffset = 0f;

        randomizeSmallClouds();

        int jeepHitboxW = (int) (70 * Game.SCALE);
        player.getHitBox().x = (float) (Game.GAME_WIDTH - jeepHitboxW) / 2;
        player.getHitBox().y = 520;
        player.resetDirBooleans();
        player.setWorldLoopDone(false);
    }

    public boolean isScrolling(Player player, boolean paused, boolean playerDead) {
        boolean hasSpeed = player.getCurrentXSpeed() > 0;
        return (dKeyHeld || hasSpeed) && isJeepCentered(player) && !paused && !worldLoopDone
                && !player.isStruckActive() && !playerDead;
    }

    public boolean updateScroll(Player player, boolean paused, boolean playerDead) {
        boolean scrolling = isScrolling(player, paused, playerDead);
        player.setWorldScrolling(scrolling);
        player.setWorldLoopDone(worldLoopDone);

        if (!scrolling) {
            return false;
        }

        float spd = player.getCurrentXSpeed();
        if (spd <= 0) {
            return false;
        }

        worldOffset += spd;
        if (worldOffset >= levelPixelWidth) {
            worldOffset -= levelPixelWidth;
            worldLoopCount++;
            System.out.println("World loops: " + worldLoopCount);
            return true;
        }

        bigCloudOffset += spd * BIG_CLOUD_PARALLAX;
        if (bigCloudOffset >= BIG_CLOUD_WIDTH) {
            bigCloudOffset -= BIG_CLOUD_WIDTH;
        }

        smallCloudOffset += spd * SMALL_CLOUD_PARALLAX;
        if (smallCloudOffset >= SMALL_CLOUD_WIDTH) {
            smallCloudOffset -= SMALL_CLOUD_WIDTH;
        }

        return false;
    }

    public void drawBackground(Graphics g) {
        if (backgroundImg != null) {
            g.drawImage(backgroundImg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        }
    }

    public void drawClouds(Graphics g) {
        int bigTilesNeeded = (Game.GAME_WIDTH / BIG_CLOUD_WIDTH) + 2;
        for (int i = 0; i < bigTilesNeeded; i++) {
            int drawX = (int) (i * BIG_CLOUD_WIDTH - bigCloudOffset);
            g.drawImage(bigClouds, drawX, (int) (40 * Game.SCALE),
                    BIG_CLOUD_WIDTH, BIG_CLOUD_HEIGHT, null);
        }

        int smallTilesNeeded = (Game.GAME_WIDTH / SMALL_CLOUD_WIDTH) + 2;
        for (int i = 0; i < smallCloudsPos.length; i++) {
            int drawX = (int) (i * SMALL_CLOUD_WIDTH - smallCloudOffset);
            g.drawImage(smallClouds, drawX, smallCloudsPos[i % smallCloudsPos.length],
                    SMALL_CLOUD_WIDTH, SMALL_CLOUD_HEIGHT, null);
        }
        for (int i = 0; i < smallTilesNeeded - smallCloudsPos.length; i++) {
            int drawX = (int) ((smallCloudsPos.length + i) * SMALL_CLOUD_WIDTH - smallCloudOffset);
            g.drawImage(smallClouds, drawX, smallCloudsPos[i % smallCloudsPos.length],
                    SMALL_CLOUD_WIDTH, SMALL_CLOUD_HEIGHT, null);
        }
    }

    public void setDKeyHeld(boolean held) {
        dKeyHeld = held;
    }

    public void markLoopDone() {
        worldLoopDone = true;
        worldOffset = 0f;
    }

    public void clearLoopDone() {
        worldLoopDone = false;
    }

    public void onWindowFocusLost(Player player) {
        player.resetDirBooleans();
        dKeyHeld = false;
    }

    public float getWorldOffset() {
        return worldOffset;
    }

    public int getWorldLoopCount() {
        return worldLoopCount;
    }

    public boolean isWorldLoopDone() {
        return worldLoopDone;
    }

    public float getScrollSpeed(Player player) {
        return player.getCurrentXSpeed();
    }

    private boolean isJeepCentered(Player player) {
        float jeepCenterX = player.getHitBox().x + player.getHitBox().width / 2f;
        float screenCenterX = Game.GAME_WIDTH / 2f;
        return Math.abs(jeepCenterX - screenCenterX) <= CENTER_TOLERANCE;
    }

    private void randomizeSmallClouds() {
        for (int i = 0; i < smallCloudsPos.length; i++) {
            smallCloudsPos[i] = (int) (20 * Game.SCALE) + rnd.nextInt((int) (100 * Game.SCALE));
        }
    }
}
