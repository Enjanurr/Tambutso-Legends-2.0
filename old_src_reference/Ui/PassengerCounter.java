package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Draws the dropped passenger counter in the upper-right corner.
 * Shows count of passengers successfully dropped at their stops (not seated).
 *
 * Sprite sheet: passenger_counter.png — arranged in a grid, 4 columns
 *   Frame 0  = 0/16   Frame 1  = 1/16   Frame 2  = 2/16   Frame 3  = 3/16
 *   Frame 4  = 4/16   Frame 5  = 5/16   Frame 6  = 6/16   Frame 7  = 7/16
 *   ...
 *   Frame 16 = 16/16
 */
public class PassengerCounter {

    // ── Sprite sheet constants ────────────────────────────────
    private static final int COUNTER_COLS  = 4;
    private static final int FRAME_COUNT   = 17;   // 0–16 inclusive

    // ── Max passengers (for display frame limiting) ────────────
    public static final int MAX_DISPLAY = 16;

    // ── Position & size settings ← ADJUST ────────────────────
    private static final float COUNTER_SCALE = 0.2f;
    // ─────────────────────────────────────────────────────────

    private final int drawW, drawH;
    private final int drawX, drawY;

    private BufferedImage[] frames;
    private int currentCount = 0;   // 0 = no passengers yet

    public PassengerCounter() {
        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.PASSENGER_COUNTER);

        int rows   = (int) Math.ceil((double) FRAME_COUNT / COUNTER_COLS);
        int frameW = sheet.getWidth()  / COUNTER_COLS;
        int frameH = sheet.getHeight() / rows;

        drawW = (int)(frameW * Game.SCALE * COUNTER_SCALE);
        drawH = (int)(frameH * Game.SCALE * COUNTER_SCALE);

        // Upper-right corner — mirrors HealthBar's top-left placement
        int paddingX = (int)(40 * Game.SCALE);
        int paddingY = (int)(10 * Game.SCALE);
        drawX = Game.GAME_WIDTH - drawW - paddingX;
        drawY = paddingY;

        loadFrames(sheet, frameW, frameH);
    }

    private void loadFrames(BufferedImage sheet, int frameW, int frameH) {
        if (sheet == null) {
            System.err.println("[PassengerCounter] Could not load " + LoadSave.PASSENGER_COUNTER);
            return;
        }
        frames = new BufferedImage[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            int col = i % COUNTER_COLS;
            int row = i / COUNTER_COLS;
            frames[i] = sheet.getSubimage(
                    col * frameW,
                    row * frameH,
                    frameW,
                    frameH);
        }
    }

    // ─────────────────────────────────────────────────────────
    // INCREMENT / RESET
    // ─────────────────────────────────────────────────────────

    /** Call when a passenger is successfully dropped. */
    public void increment() {
        if (currentCount < MAX_DISPLAY)
            currentCount++;
    }

    public void reset() {
        currentCount = 0;
    }

    public int  getCount()  { return currentCount; }
    public int getDrawY() { return drawY; }
    public int getDrawH() { return drawH; }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (frames == null) return;
        int index = Math.min(currentCount, FRAME_COUNT - 1);
        g.drawImage(frames[index], drawX, drawY, drawW, drawH, null);
    }
}