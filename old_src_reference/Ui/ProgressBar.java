package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays the jeepney's journey progress across 15 world loops.
 *
 * Sprite sheet: progress_bar.png — 12390 × 79, 1 row × 15 columns
 *   Column 0  = 0/15 loops done (empty)
 *   Column 14 = 15/15 loops done (full)
 */
public class ProgressBar {

    // ── Sprite sheet constants ────────────────────────────────
    private static final int SHEET_COLS   = 15;
    private static final int FRAME_W      = 826;   // 12390 / 15
    private static final int FRAME_H      = 79;

    // ── Max column index ──────────────────────────────────────
    private static final int MAX_COL      = SHEET_COLS - 1; // 14

    // -------------------------------------------------------
    // POSITION & SIZE SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float RENDER_SCALE = 0.5f;  // shrink factor on top of Game.SCALE
    private static final float POS_Y        = 10f;    // pre-scale Y from top of screen
    // -------------------------------------------------------

    private final int drawW, drawH, drawX, drawY;

    private BufferedImage[] frames;
    private int currentColumn = 0;

    public ProgressBar() {
        drawW = (int)(FRAME_W * Game.SCALE * RENDER_SCALE);
        drawH = (int)(FRAME_H * Game.SCALE * RENDER_SCALE);
        drawX = (Game.GAME_WIDTH - drawW) / 2;   // centred horizontally
        drawY = (int)(POS_Y * Game.SCALE);
        loadFrames();
    }

    private void loadFrames() {
        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.PROGRESS_BAR);
        if (sheet == null) {
            System.err.println("[ProgressBar] Could not load " + LoadSave.PROGRESS_BAR);
            return;
        }
        frames = new BufferedImage[SHEET_COLS];
        for (int i = 0; i < SHEET_COLS; i++)
            frames[i] = sheet.getSubimage(i * FRAME_W, 0, FRAME_W, FRAME_H);
    }

    // ─────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────

    /**
     * Call each time a world loop completes.
     * Advances one column, clamped to MAX_COL (14).
     */
    public void onLoopCompleted() {
        if (currentColumn < MAX_COL)
            currentColumn++;
    }

    /**
     * Sync directly from the world loop count (e.g. after restart).
     */
    public void setProgress(int loopCount) {
        currentColumn = Math.min(loopCount, MAX_COL);
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
        int col = Math.min(currentColumn, MAX_COL);
        g.drawImage(frames[col], drawX, drawY, drawW, drawH, null);
    }
}