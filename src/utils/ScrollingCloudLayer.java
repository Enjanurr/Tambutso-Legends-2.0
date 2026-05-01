package utils;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class ScrollingCloudLayer {
    private final BufferedImage sprite;
    private final int spriteWidth;
    private final int spriteHeight;
    private final float parallax;
    private final float[] xPositions;
    private final int[] yCycle;
    private final int[] yPositions;
    private int nextYIndex;

    public ScrollingCloudLayer(BufferedImage sprite, int spriteWidth, int spriteHeight,
                               float parallax, int count, int fixedY) {
        this(sprite, spriteWidth, spriteHeight, parallax, createFixedYArray(count, fixedY));
    }

    public ScrollingCloudLayer(BufferedImage sprite, int spriteWidth, int spriteHeight,
                               float parallax, int count, int[] yCycleValues) {
        this.sprite = sprite;
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        this.parallax = parallax;
        this.yCycle = yCycleValues.clone();
        this.xPositions = new float[count];
        this.yPositions = new int[count];
        reset();
    }

    public ScrollingCloudLayer(BufferedImage sprite, int spriteWidth, int spriteHeight,
                               float parallax, int[] initialYPositions) {
        this(sprite, spriteWidth, spriteHeight, parallax, initialYPositions.length, initialYPositions);
    }

    public void reset() {
        for (int i = 0; i < xPositions.length; i++) {
            xPositions[i] = i * spriteWidth;
            yPositions[i] = yCycle[i % yCycle.length];
        }
        nextYIndex = 0;
    }

    public void update(float scrollSpeed) {
        float delta = scrollSpeed * parallax;
        if (delta <= 0f) {
            return;
        }

        for (int i = 0; i < xPositions.length; i++) {
            xPositions[i] -= delta;
        }

        for (int i = 0; i < xPositions.length; i++) {
            if (xPositions[i] + spriteWidth < 0f) {
                xPositions[i] = getMaxX() + spriteWidth;
                yPositions[i] = yCycle[nextYIndex];
                nextYIndex = (nextYIndex + 1) % yCycle.length;
            }
        }
    }

    public void draw(Graphics g) {
        if (sprite == null) {
            return;
        }

        for (int i = 0; i < xPositions.length; i++) {
            g.drawImage(sprite, Math.round(xPositions[i]), yPositions[i], spriteWidth, spriteHeight, null);
        }
    }

    private float getMaxX() {
        float maxX = xPositions[0];
        for (int i = 1; i < xPositions.length; i++) {
            if (xPositions[i] > maxX) {
                maxX = xPositions[i];
            }
        }
        return maxX;
    }

    private static int[] createFixedYArray(int count, int fixedY) {
        int[] yPositions = new int[count];
        for (int i = 0; i < count; i++) {
            yPositions[i] = fixedY;
        }
        return yPositions;
    }
}
