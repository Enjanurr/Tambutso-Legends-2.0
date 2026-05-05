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
 *   3. Next button (URM row 0), Restart button (URM row 1), and Menu button (URM row 2)
 */
public class BossDefeatOverlay {

    // ── Image scaling ─────────────────────────────────────────
    private static final float DEFEAT_IMG_RENDER_SCALE = 0.7f;  // ← ADJUST: 0.5f = 50%, 0.7f = 70%, 1.0f = 100%

    // ── Button positions (same Y, individual X) ───────────────
    private static final int BUTTONS_Y = (int)(230 * Game.SCALE);  // ← ADJUST: Y position for all buttons

    // Individual X positions for each button
    private static final int NEXT_BUTTON_X    = (int)(370 * Game.SCALE);   // ← ADJUST: Next button X
    private static final int RESTART_BUTTON_X = (int)(290 * Game.SCALE);   // ← ADJUST: Restart button X
    private static final int MENU_BUTTON_X    = (int)(450 * Game.SCALE);   // ← ADJUST: Menu button X

    // Alternative: Use spacing between buttons (uncomment to use)
    // private static final int BUTTON_SPACING = 130;  // Spacing between buttons
    // Then calculate: NEXT_X = centerX - BUTTON_SPACING, etc.

    // ── Fade settings ─────────────────────────────────────────
    private static final float FADE_SPEED = 0.03f;  // ← ADJUST: Fade speed

    private BufferedImage originalDefeatImg;
    private BufferedImage scaledDefeatImg;
    private int imgW, imgH, imgX, imgY;

    private UrmButton nextBtn;
    private UrmButton restartBtn;
    private UrmButton menuBtn;

    private float overlayAlpha = 0f;
    private boolean fadeComplete = false;
    private boolean processingNext = false;

    // Callbacks
    private Runnable onRestart;
    private Runnable onMenu;
    private Runnable onNext;

    public BossDefeatOverlay(Runnable onNext, Runnable onRestart, Runnable onMenu) {
        this.onNext = onNext;
        this.onRestart = onRestart;
        this.onMenu = onMenu;
        loadImage();
        buildLayout();
    }

    private void loadImage() {
        originalDefeatImg = LoadSave.getSpriteAtlas(LoadSave.BOSS_DEFEAT);
        if (originalDefeatImg == null) {
            System.err.println("[BossDefeatOverlay] Could not load " + LoadSave.BOSS_DEFEAT);
            return;
        }

        // Scale the image using DEFEAT_IMG_RENDER_SCALE
        int scaledWidth = (int)(434 * Game.SCALE * DEFEAT_IMG_RENDER_SCALE);
        int scaledHeight = (int)(323 * Game.SCALE * DEFEAT_IMG_RENDER_SCALE);

        scaledDefeatImg = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledDefeatImg.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalDefeatImg, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        System.out.println("[BossDefeatOverlay] Defeat image scaled: 434x323 -> " +
                scaledWidth + "x" + scaledHeight + " (scale=" + DEFEAT_IMG_RENDER_SCALE + ")");
    }

    private void buildLayout() {
        if (scaledDefeatImg != null) {
            imgW = scaledDefeatImg.getWidth();
            imgH = scaledDefeatImg.getHeight();
        } else {
            imgW = (int)(434 * Game.SCALE * DEFEAT_IMG_RENDER_SCALE);
            imgH = (int)(323 * Game.SCALE * DEFEAT_IMG_RENDER_SCALE);
        }

        imgX = (Game.GAME_WIDTH - imgW) / 2;
        imgY = (Game.GAME_HEIGHT - imgH) / 2;

        // Create buttons with individual X positions but same Y
        nextBtn    = new UrmButton(NEXT_BUTTON_X,    BUTTONS_Y, URM_SIZE, URM_SIZE, 0); // row 0 = next
        restartBtn = new UrmButton(RESTART_BUTTON_X, BUTTONS_Y, URM_SIZE, URM_SIZE, 1); // row 1 = restart
        menuBtn    = new UrmButton(MENU_BUTTON_X,    BUTTONS_Y, URM_SIZE, URM_SIZE, 2); // row 2 = home

        System.out.println("[BossDefeatOverlay] Buttons at Y=" + BUTTONS_Y +
                ", X positions: Next=" + NEXT_BUTTON_X +
                ", Restart=" + RESTART_BUTTON_X +
                ", Menu=" + MENU_BUTTON_X);
    }

    public void reset() {
        overlayAlpha = 0f;
        fadeComplete = false;
        nextBtn.resetBools();
        restartBtn.resetBools();
        menuBtn.resetBools();
    }

    public void update() {
        if (!fadeComplete) {
            overlayAlpha = Math.min(overlayAlpha + FADE_SPEED, 0.85f);
            if (overlayAlpha >= 0.85f) fadeComplete = true;
        }
        if (fadeComplete) {
            nextBtn.update();
            restartBtn.update();
            menuBtn.update();
        }
    }

    public void render(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // 1 — semi-transparent black overlay
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // 2 — defeat image (scaled)
        if (scaledDefeatImg != null) {
            float imgAlpha = Math.min(overlayAlpha / 0.85f, 1f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, imgAlpha));
            g2d.drawImage(scaledDefeatImg, imgX, imgY, imgW, imgH, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        } else if (originalDefeatImg != null) {
            // Fallback to original unscaled
            float imgAlpha = Math.min(overlayAlpha / 0.85f, 1f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, imgAlpha));
            int x = (Game.GAME_WIDTH - (int)(434 * Game.SCALE)) / 2;
            int y = (Game.GAME_HEIGHT - (int)(323 * Game.SCALE)) / 2;
            g2d.drawImage(originalDefeatImg, x, y, (int)(434 * Game.SCALE), (int)(323 * Game.SCALE), null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // 3 — buttons appear only after fade completes
        if (fadeComplete) {
            nextBtn.draw(g);
            restartBtn.draw(g);
            menuBtn.draw(g);
        }
    }

    public void mouseMoved(MouseEvent e) {
        if (!fadeComplete) return;
        nextBtn.setMouseOver(nextBtn.getBounds().contains(e.getX(), e.getY()));
        restartBtn.setMouseOver(restartBtn.getBounds().contains(e.getX(), e.getY()));
        menuBtn.setMouseOver(menuBtn.getBounds().contains(e.getX(), e.getY()));
    }

    public void mousePressed(MouseEvent e) {
        if (!fadeComplete) return;
        if (nextBtn.getBounds().contains(e.getX(), e.getY()))    nextBtn.setMousePressed(true);
        if (restartBtn.getBounds().contains(e.getX(), e.getY())) restartBtn.setMousePressed(true);
        if (menuBtn.getBounds().contains(e.getX(), e.getY()))    menuBtn.setMousePressed(true);
    }

    public void mouseReleased(MouseEvent e) {
        if (!fadeComplete || processingNext) return;
        if (nextBtn.isMousePressed() && nextBtn.getBounds().contains(e.getX(), e.getY())) {
            System.out.println("[BossDefeatOverlay] NEXT clicked - calling onNext");
            processingNext = true;
            onNext.run();
            processingNext = false;
        }
        if (restartBtn.isMousePressed() && restartBtn.getBounds().contains(e.getX(), e.getY()))
            onRestart.run();
        if (menuBtn.isMousePressed() && menuBtn.getBounds().contains(e.getX(), e.getY()))
            onMenu.run();
        nextBtn.resetBools();
        restartBtn.resetBools();
        menuBtn.resetBools();
    }
}