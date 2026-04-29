package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays the jeepney's journey progress across world loops.
 *
 * Sprite sheets:
 *   Level 1: progress_bar.png  — 12390 × 79, 1 row × 15 columns
 *   Level 2: progress_bar2.png — 16520 × 79, 1 row × 20 columns
 *   Level 3: progress_bar3.png — 20650 × 79, 1 row × 25 columns
 *   Column 0 = 0 loops done (empty), last column = max loops (full)
 */
public class ProgressBar {

    // ── Sprite sheet constants ────────────────────────────────
    private static final int FRAME_W      = 826;   // All bars: 826px per column
    private static final int FRAME_H      = 79;

    // ── Max column index per level ────────────────────────────
    private static final int LEVEL1_MAX   = 14;  // 15 columns (0-14)
    private static final int LEVEL2_MAX   = 19;  // 20 columns (0-19)
    private static final int LEVEL3_MAX   = 24;  // 25 columns (0-24)

    // -------------------------------------------------------
    // POSITION & SIZE SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float RENDER_SCALE = 0.5f;  // shrink factor on top of Game.SCALE
    private static final float POS_Y        = 10f;    // pre-scale Y from top of screen
    // -------------------------------------------------------

    private final int drawW, drawH, drawX, drawY;
    private final int maxColumn;

    private BufferedImage[] frames;
    private int currentColumn = 0;

    public ProgressBar(int levelId) {
        drawW = (int)(FRAME_W * Game.SCALE * RENDER_SCALE);
        drawH = (int)(FRAME_H * Game.SCALE * RENDER_SCALE);
        drawX = (Game.GAME_WIDTH - drawW) / 2;   // centred horizontally
        drawY = (int)(POS_Y * Game.SCALE);

        // Set max column based on level
        switch (levelId) {
            case 2: maxColumn = LEVEL2_MAX; break;
            case 3: maxColumn = LEVEL3_MAX; break;
            default: maxColumn = LEVEL1_MAX; break;
        }
        loadFrames(levelId);
    }

    /** Legacy constructor for backward compatibility — defaults to Level 1 */
    public ProgressBar() {
        this(1);
    }

    private void loadFrames(int levelId) {
        String atlasPath;
        int numCols;
        switch (levelId) {
            case 2:
                atlasPath = LoadSave.PROGRESS_BAR2;
                numCols = LEVEL2_MAX + 1;  // 20 columns
                break;
            case 3:
                atlasPath = LoadSave.PROGRESS_BAR3;
                numCols = LEVEL3_MAX + 1;  // 25 columns
                break;
            default:
                atlasPath = LoadSave.PROGRESS_BAR;
                numCols = LEVEL1_MAX + 1;  // 15 columns
                break;
        }

        BufferedImage sheet = LoadSave.getSpriteAtlas(atlasPath);
        if (sheet == null) {
            System.err.println("[ProgressBar] Could not load " + atlasPath);
            return;
        }
        frames = new BufferedImage[numCols];
        for (int i = 0; i < numCols; i++)
            frames[i] = sheet.getSubimage(i * FRAME_W, 0, FRAME_W, FRAME_H);
    }

    // ─────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────

    /**
     * Call each time a world loop completes.
     * Advances one column, clamped to maxColumn.
     */
    public void onLoopCompleted() {
        if (currentColumn < maxColumn)
            currentColumn++;
    }

    /**
     * Sync directly from the world loop count (e.g. after restart).
     */
    public void setProgress(int loopCount) {
        currentColumn = Math.min(loopCount, maxColumn);
    }

    /** Resets to column 0 (empty bar). */
    public void reset() {
        currentColumn = 0;
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (frames == null) return;
        int col = Math.min(currentColumn, maxColumn);
        g.drawImage(frames[col], drawX, drawY, drawW, drawH, null);
    }
}