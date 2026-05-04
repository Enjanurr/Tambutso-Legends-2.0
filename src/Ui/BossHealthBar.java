package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Draws the boss life bar in the upper-right corner.
 *
 * Supports two sprite configurations:
 *   BOSS1: boss1_life.png — 2700 × 224, 2 rows
 *     Row 0  = Full bars : 9 columns (0-8)  → col 0=8 bars, col 8=0 bars (defeated)
 *     Row 1  = Half bars : 8 columns (0-7)  → col 0=half8 … col 7=half1; col 8 unused
 *     Damage sequence (16 hits): R0C0 → R1C0 → R0C1 → R1C1 → … → R0C8 (DEFEATED)
 *
 *   BOSS2: boss2_life.png — 3300 × 224
 *     Row 0  = Full bars : 11 columns (0-10) → col 0=10 bars, col 10=0 bars (defeated)
 *     Row 1  = Half bars : 10 columns (0-9)  → col 0=9.5 bars … col 9=0.5 bars
 *     Damage sequence (20 hits): R0C0 → R1C0 → R0C1 → R1C1 → … → R0C10 (DEFEATED)
 */
public class BossHealthBar {

    public enum LifeBarType {
        BOSS1(9, 9, 16, LoadSave.BOSS1_LIFE),      // Row0:9cols, Row1:9cols(8used), 16hits
        BOSS2(11, 10, 20, LoadSave.BOSS2_LIFE),    // Row0:11cols, Row1:10cols, 20hits
        BOSS3(13, 12, 24, LoadSave.BOSS3_LIFE);    // Row0:13cols, Row1:12cols, 24hits

        final int row0Cols;
        final int row1Cols;
        final int maxHits;
        final String spritePath;

        LifeBarType(int row0Cols, int row1Cols, int maxHits, String spritePath) {
            this.row0Cols = row0Cols;
            this.row1Cols = row1Cols;
            this.maxHits = maxHits;
            this.spritePath = spritePath;
        }
    }

    // ── Configuration ──────────────────────────────────────────
    private final LifeBarType type;
    private final int frameW;
    private final int frameH;
    private final int maxHits;

    // ── Position & size ───────────────────────────────────────
    private static final float BAR_Y     = 10f;  // pre-scale screen Y
    private static final float BAR_SCALE = 0.5f; // render size multiplier
    private final int drawX, drawY, drawW, drawH;

    private BufferedImage[][] frames; // [row][col]
    private int hitStep = 0; // 0 = full health

    /**
     * Creates a BossHealthBar with the specified life bar type.
     * @param type BOSS1 or BOSS2 life bar configuration
     */
    public BossHealthBar(LifeBarType type) {
        this.type = type;
        this.maxHits = type.maxHits;

        // Calculate frame dimensions based on sprite sheet size
        BufferedImage sheet = LoadSave.getSpriteAtlas(type.spritePath);
        if (sheet != null) {
            frameW = sheet.getWidth() / type.row0Cols;  // Use row0 columns for width calc
            frameH = sheet.getHeight() / 2;              // Always 2 rows
        } else {
            // Fallback dimensions
            frameW = (type == LifeBarType.BOSS1) ? 300 : 300;
            frameH = 112;
        }

        drawW = (int)(frameW * Game.SCALE * BAR_SCALE);
        drawH = (int)(frameH * Game.SCALE * BAR_SCALE);
        drawY = (int)(BAR_Y * Game.SCALE);
        drawX = Game.GAME_WIDTH - drawW - (int)(10 * Game.SCALE); // right-aligned

        loadFrames();
    }

    /**
     * Default constructor — uses BOSS1 life bar for backward compatibility.
     */
    public BossHealthBar() {
        this(LifeBarType.BOSS1);
    }

    private void loadFrames() {
        BufferedImage sheet = LoadSave.getSpriteAtlas(type.spritePath);
        if (sheet == null) {
            System.err.println("[BossHealthBar] Could not load " + type.spritePath);
            return;
        }

        // Row 0 has row0Cols, Row 1 has row1Cols
        frames = new BufferedImage[2][];
        frames[0] = new BufferedImage[type.row0Cols];
        frames[1] = new BufferedImage[type.row1Cols];

        // Load Row 0 (full bars)
        for (int col = 0; col < type.row0Cols; col++) {
            frames[0][col] = sheet.getSubimage(col * frameW, 0, frameW, frameH);
        }

        // Load Row 1 (half bars)
        for (int col = 0; col < type.row1Cols; col++) {
            frames[1][col] = sheet.getSubimage(col * frameW, frameH, frameW, frameH);
        }
    }

    // ─────────────────────────────────────────────────────────
    // DAMAGE
    // ─────────────────────────────────────────────────────────

    /**
     * Advances one damage step.
     * @return true when boss is defeated (hitStep >= maxHits)
     */
    public boolean takeDamage() {
        if (hitStep < maxHits) hitStep++;
        return hitStep >= maxHits;
    }

    public boolean isDefeated() {
        return hitStep >= maxHits;
    }

    public void reset() {
        hitStep = 0;
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (frames == null || frames[0] == null) return;

        int row, col;
        if (hitStep >= maxHits) {
            // Defeated — show final frame (0 bars)
            row = 0;
            col = type.row0Cols - 1;
        } else {
            row = hitStep % 2;           // 0=full, 1=half
            col = hitStep / 2;           // which bar count column
        }

        // Safety bounds check
        if (row >= frames.length) row = frames.length - 1;
        if (row == 0 && col >= frames[0].length) col = frames[0].length - 1;
        if (row == 1 && col >= frames[1].length) col = frames[1].length - 1;

        if (frames[row][col] == null) return;

        g.drawImage(frames[row][col], drawX, drawY, drawW, drawH, null);
    }

    /**
     * Gets the max hits for this life bar type.
     */
    public int getMaxHits() {
        return maxHits;
    }

    /**
     * Gets the life bar type.
     */
    public LifeBarType getType() {
        return type;
    }
}
