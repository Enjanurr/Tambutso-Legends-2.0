package BossFight;

import main.Game;
import utils.LoadSave;
import utils.ScrollingCloudLayer;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Random;

import static utils.Constants.Environment.*;

public class CloudRenderer {
    private static final float BIG_CLOUD_PARALLAX = 0.3f;
    private static final float SMALL_CLOUD_PARALLAX = 0.5f;

    private BufferedImage backgroundImg;
    private ScrollingCloudLayer bigCloudLayer;
    private ScrollingCloudLayer smallCloudLayer;
    private int[] smallCloudsPos;
    private final Random rnd = new Random();

    public CloudRenderer() {
        loadAssets();
    }

    private void loadAssets() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PLAYING_BACKGROUND_IMG);
        BufferedImage bigClouds = LoadSave.getSpriteAtlas(LoadSave.BIG_CLOUDS);
        BufferedImage smallClouds = LoadSave.getSpriteAtlas(LoadSave.SMALL_CLOUDS);

        smallCloudsPos = new int[8];
        randomizeSmallClouds();

        int bigCloudCount = (Game.GAME_WIDTH / BIG_CLOUD_WIDTH) + 3;
        int smallCloudCount = (Game.GAME_WIDTH / SMALL_CLOUD_WIDTH) + 3;

        bigCloudLayer = new ScrollingCloudLayer(
                bigClouds, BIG_CLOUD_WIDTH, BIG_CLOUD_HEIGHT,
                BIG_CLOUD_PARALLAX, bigCloudCount, (int)(40 * Game.SCALE));

        smallCloudLayer = new ScrollingCloudLayer(
                smallClouds, SMALL_CLOUD_WIDTH, SMALL_CLOUD_HEIGHT,
                SMALL_CLOUD_PARALLAX, smallCloudCount, smallCloudsPos);
    }

    public void update(float scrollSpeed) {
        bigCloudLayer.update(scrollSpeed);
        smallCloudLayer.update(scrollSpeed);
    }

    public void drawBackground(Graphics g) {
        if (backgroundImg != null) {
            g.drawImage(backgroundImg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        }
    }

    public void drawClouds(Graphics g) {
        bigCloudLayer.draw(g);
        smallCloudLayer.draw(g);
    }

    public void reset() {
        // Re-initialize layers to reset offsets
        loadAssets();
    }

    private void randomizeSmallClouds() {
        for (int i = 0; i < smallCloudsPos.length; i++) {
            smallCloudsPos[i] = (int) (20 * Game.SCALE) + rnd.nextInt((int) (100 * Game.SCALE));
        }
    }
}