package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Draws the life status bar in the upper-left corner.
 *
 * Sprite sheet: life_status.png — 2010 × 125, 1 row × 6 columns
 *   Column 0 = 5 bars (full health)
 *   Column 1 = 4 bars
 *   Column 2 = 3 bars
 *   Column 3 = 2 bars
 *   Column 4 = 1 bar
 *   Column 5 = 0 bars (dead)
 */
public class HealthBar {

    // ── Sprite sheet constants ────────────────────────────────
    private static final int SHEET_WIDTH    = 2010;
    private static final int SHEET_HEIGHT   = 125;
    private static final int FRAME_COUNT    = 6;
    private static final int FRAME_W        = SHEET_WIDTH  / FRAME_COUNT; // 335
    private static final int FRAME_H        = SHEET_HEIGHT;               // 125

    // ── Max health ────────────────────────────────────────────
    public static final int MAX_HEALTH      = 5; // column 0 = full, column 5 = dead

    // -------------------------------------------------------
    // POSITION & SIZE SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float BAR_X        = 10f;  // screen X (pre-scale)
    private static final float BAR_Y        = 10f;  // screen Y (pre-scale)
    private static final float BAR_SCALE    = 0.5f; // render at 50% of scaled size
    // -------------------------------------------------------

    private final int drawX, drawY, drawW, drawH;

    private BufferedImage[] frames;
    private int currentColumn = 0; // 0 = full health

    public HealthBar() {
        drawX = (int)(BAR_X * Game.SCALE);
        drawY = (int)(BAR_Y * Game.SCALE);
        drawW = (int)(FRAME_W * Game.SCALE * BAR_SCALE);
        drawH = (int)(FRAME_H * Game.SCALE * BAR_SCALE);
        loadFrames();
    }

    private void loadFrames() {
        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.LIFE_STATUS);
        if (sheet == null) {
            System.err.println("[HealthBar] Could not load " + LoadSave.LIFE_STATUS);
            return;
        }
        frames = new BufferedImage[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++)
            frames[i] = sheet.getSubimage(i * FRAME_W, 0, FRAME_W, FRAME_H);
    }

    // ─────────────────────────────────────────────────────────
    // DAMAGE / HEAL
    // ─────────────────────────────────────────────────────────


    public boolean takeDamage() {
        if (currentColumn < MAX_HEALTH) {
            currentColumn++;
        }
        return currentColumn >= MAX_HEALTH; // true = dead
    }


    public void heal() {
        if (currentColumn > 0)
            currentColumn--;
    }


    public void reset() {
        currentColumn = 0;
    }

    public boolean isDead() {
        return currentColumn >= MAX_HEALTH;
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (frames == null) return;
        int col = Math.min(currentColumn, FRAME_COUNT - 1);
        g.drawImage(frames[col], drawX, drawY, drawW, drawH, null);
    }
}