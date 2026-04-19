package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Draws the boss life bar in the upper-right corner.
 *
 * Sprite sheet: boss1_life.png — 2700 × 224, 2 rows
 *   Row 0  = Full bars : 9 columns (0-8)  → col 0=8 bars, col 8=0 bars (defeated)
 *   Row 1  = Half bars : 8 columns (0-7)  → col 0=half8 … col 7=half1; col 8 unused
 *
 * Damage sequence (16 hits to defeat):
 *   R0C0 → R1C0 → R0C1 → R1C1 → … → R0C7 → R1C7 → R0C8 (DEFEATED)
 */
public class BossHealthBar {

    // ── Sprite sheet constants ────────────────────────────────
    private static final int COLS     = 9;
    private static final int ROWS     = 2;
    private static final int FRAME_W  = 2700 / COLS; // 300
    private static final int FRAME_H  = 224  / ROWS; // 112

    // ── Total hits to defeat boss ─────────────────────────────
    // 8 full + 8 half = 16 steps; step 16 = R0C8 = defeated
    public static final int MAX_HITS = 16;

    // -------------------------------------------------------
    // POSITION & SIZE SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float BAR_Y     = 10f;  // pre-scale screen Y (same as jeep bar)
    private static final float BAR_SCALE = 0.5f; // render size multiplier
    // -------------------------------------------------------

    private final int drawX, drawY, drawW, drawH;

    private BufferedImage[][] frames; // [row][col]
    private int hitStep = 0; // 0 = full health, 16 = defeated

    public BossHealthBar() {
        drawW = (int)(FRAME_W * Game.SCALE * BAR_SCALE);
        drawH = (int)(FRAME_H * Game.SCALE * BAR_SCALE);
        drawY = (int)(BAR_Y * Game.SCALE);
        drawX = Game.GAME_WIDTH - drawW - (int)(10 * Game.SCALE); // right-aligned with margin
        loadFrames();
    }

    private void loadFrames() {
        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.BOSS1_LIFE);
        if (sheet == null) {
            System.err.println("[BossHealthBar] Could not load " + LoadSave.BOSS1_LIFE);
            return;
        }
        frames = new BufferedImage[ROWS][COLS];
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++) {
                // Row 1 col 8 is unused — guard against subimage out-of-bounds
                if (row == 1 && col == COLS - 1) continue;
                frames[row][col] = sheet.getSubimage(
                        col * FRAME_W, row * FRAME_H, FRAME_W, FRAME_H);
            }
    }

    // ─────────────────────────────────────────────────────────
    // DAMAGE
    // ─────────────────────────────────────────────────────────

    /**
     * Advances one damage step.
     * @return true when boss is defeated (hitStep == MAX_HITS)
     */
    public boolean takeDamage() {
        if (hitStep < MAX_HITS) hitStep++;
        return hitStep >= MAX_HITS;
    }

    public boolean isDefeated() {
        return hitStep >= MAX_HITS;
    }

    public void reset() {
        hitStep = 0;
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (frames == null) return;

        int row, col;
        if (hitStep >= MAX_HITS) {
            row = 0; col = COLS - 1; // R0C8 — defeated (0 bars)
        } else {
            row = hitStep % 2;       // 0=full, 1=half
            col = hitStep / 2;       // which bar count column
        }

        row = Math.min(row, ROWS - 1);
        col = Math.min(col, COLS - 1);

        // Safety: row 1 col 8 has no frame
        if (frames[row][col] == null) return;

        g.drawImage(frames[row][col], drawX, drawY, drawW, drawH, null);
    }
}