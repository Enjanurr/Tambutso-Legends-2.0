package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static utils.Constants.UI.URMButtons.*;

/**
 * Shown when the boss is defeated (BossHealthBar reaches 0).
 *
 * Draw order:
 *   1. Semi-transparent black overlay
 *   2. BossDefeat.png centered on screen (434 × 323)
 *   3. Restart button (URM row 1) and Menu button (URM row 2)
 */
public class BossDefeatOverlay {

    // ── Defeat image ──────────────────────────────────────────
    private BufferedImage defeatImg;
    private int imgW, imgH, imgX, imgY;

    // -------------------------------------------------------
    // BUTTON POSITIONS  ← ADJUST
    // -------------------------------------------------------
    private static final int RESTART_X = (int)(295 * Game.SCALE);
    private static final int MENU_X    = (int)(451 * Game.SCALE);
    private static final int BTN_Y     = (int)(325 * Game.SCALE);
    // -------------------------------------------------------
    private UrmButton restartBtn;
    private UrmButton menuBtn;

    // ── Fade ──────────────────────────────────────────────────
    // -------------------------------------------------------
    // FADE SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float FADE_SPEED = 0.03f;
    // -------------------------------------------------------
    private float   overlayAlpha = 0f;
    private boolean fadeComplete = false;

    // Callbacks set by BossFightState
    private Runnable onRestart;
    private Runnable onMenu;

    public BossDefeatOverlay(Runnable onRestart, Runnable onMenu) {
        this.onRestart = onRestart;
        this.onMenu    = onMenu;
        loadImage();
        buildLayout();
    }

    private void loadImage() {
        defeatImg = LoadSave.getSpriteAtlas(LoadSave.BOSS_DEFEAT);
        if (defeatImg == null)
            System.err.println("[BossDefeatOverlay] Could not load " + LoadSave.BOSS_DEFEAT);
    }

    private void buildLayout() {
        // Scale the 434×323 source image
        imgW = (int)(434 * Game.SCALE * 0.5f); // ← ADJUST 0.5f to resize
        imgH = (int)(323 * Game.SCALE * 0.5f);
        imgX = (Game.GAME_WIDTH  - imgW) / 2;
        imgY = (Game.GAME_HEIGHT - imgH) / 2;

        restartBtn = new UrmButton(RESTART_X, BTN_Y, URM_SIZE, URM_SIZE, 1); // row 1 = restart
        menuBtn    = new UrmButton(MENU_X,    BTN_Y, URM_SIZE, URM_SIZE, 2); // row 2 = home
    }

    // ─────────────────────────────────────────────────────────
    // RESET
    // ─────────────────────────────────────────────────────────
    public void reset() {
        overlayAlpha = 0f;
        fadeComplete = false;
        restartBtn.resetBools();
        menuBtn.resetBools();
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update() {
        if (!fadeComplete) {
            overlayAlpha = Math.min(overlayAlpha + FADE_SPEED, 0.85f);
            if (overlayAlpha >= 0.85f) fadeComplete = true;
        }
        if (fadeComplete) {
            restartBtn.update();
            menuBtn.update();
        }
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // 1 — semi-transparent black overlay
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // 2 — defeat image
        if (defeatImg != null) {
            float imgAlpha = Math.min(overlayAlpha / 0.85f, 1f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, imgAlpha));
            g2d.drawImage(defeatImg, imgX, imgY, imgW, imgH, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // 3 — buttons appear only after fade completes
        if (fadeComplete) {
            restartBtn.draw(g);
            menuBtn.draw(g);
        }
    }

    // ─────────────────────────────────────────────────────────
    // MOUSE INPUT
    // ─────────────────────────────────────────────────────────
    public void mouseMoved(MouseEvent e) {
        if (!fadeComplete) return;
        restartBtn.setMouseOver(restartBtn.getBounds().contains(e.getX(), e.getY()));
        menuBtn.setMouseOver(menuBtn.getBounds().contains(e.getX(), e.getY()));
    }

    public void mousePressed(MouseEvent e) {
        if (!fadeComplete) return;
        if (restartBtn.getBounds().contains(e.getX(), e.getY())) restartBtn.setMousePressed(true);
        if (menuBtn.getBounds().contains(e.getX(), e.getY()))    menuBtn.setMousePressed(true);
    }

    public void mouseReleased(MouseEvent e) {
        if (!fadeComplete) return;
        if (restartBtn.isMousePressed() && restartBtn.getBounds().contains(e.getX(), e.getY()))
            onRestart.run();
        if (menuBtn.isMousePressed() && menuBtn.getBounds().contains(e.getX(), e.getY()))
            onMenu.run();
        restartBtn.resetBools();
        menuBtn.resetBools();
    }
}