package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Draws the jeepney life bar in the upper-left corner.
 *
 * Sprite sheet: life_status.png — 1800 × 224, 2 rows × 6 columns
 *   Row 0  = Full bars  : col 0=5 bars … col 5=0 bars (dead)
 *   Row 1  = Half bars  : col 0=half of 5 … col 4=half of 1 (col 5 unused)
 *
 * Damage sequence (10 hits to die):
 *   R0C0 → R1C0 → R0C1 → R1C1 → R0C2 → R1C2 →
 *   R0C3 → R1C3 → R0C4 → R1C4 → R0C5 (DEAD)
 */
public class HealthBar {

    // ── Sprite sheet constants ────────────────────────────────
    private static final int COLS         = 6;
    private static final int ROWS         = 2;
    private static final int FRAME_W      = 1800 / COLS; // 300
    private static final int FRAME_H      = 224  / ROWS; // 112

    // ── Total damage steps before death ──────────────────────
    // 5 full + 5 half = 10 steps; step 10 = R0C5 = dead
    public static final int MAX_HITS = 10;

    // -------------------------------------------------------
    // POSITION & SIZE SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float BAR_X     = 10f;  // pre-scale screen X
    private static final float BAR_Y     = 10f;  // pre-scale screen Y
    private static final float BAR_SCALE = 0.5f; // render size multiplier
    // -------------------------------------------------------

    private final int drawX, drawY, drawW, drawH;

    // frames[row][col]
    private BufferedImage[][] frames;

    // Current damage step: 0 = full health, 10 = dead
    private int hitStep = 0;

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
        frames = new BufferedImage[ROWS][COLS];
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                frames[row][col] = sheet.getSubimage(
                        col * FRAME_W, row * FRAME_H, FRAME_W, FRAME_H);
    }

    // ─────────────────────────────────────────────────────────
    // DAMAGE / HEAL
    // ─────────────────────────────────────────────────────────

    /**
     * Advances one damage step.
     * @return true if the jeepney has died (hitStep == MAX_HITS)
     */
    public boolean takeDamage() {
        if (hitStep < MAX_HITS) hitStep++;
        return hitStep >= MAX_HITS;
    }

    /**
     * Reverses one damage step (Heal powerup).
     * Does nothing at full health.
     */
    public void heal() {
        if (hitStep > 0) hitStep--;
    }

    /** Resets to full health. */
    public void reset() {
        hitStep = 0;
    }

    public boolean isDead() {
        return hitStep >= MAX_HITS;
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (frames == null) return;

        // Map hitStep → (row, col):
        //   step 0         → R0 C0  (full 5 bars)
        //   step 1         → R1 C0  (half 5 bars)
        //   step 2         → R0 C1  (4 bars)
        //   step 3         → R1 C1  (half 4 bars)
        //   …
        //   step 10        → R0 C5  (dead)
        int row, col;
        if (hitStep >= MAX_HITS) {
            row = 0; col = COLS - 1; // dead frame
        } else {
            row = hitStep % 2;         // 0=full, 1=half
            col = hitStep / 2;         // which bar count column
        }

        row = Math.min(row, ROWS - 1);
        col = Math.min(col, COLS - 1);

        g.drawImage(frames[row][col], drawX, drawY, drawW, drawH, null);
    }
}