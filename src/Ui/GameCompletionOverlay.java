package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static utils.Constants.UI.URMButtons.*;

/**
 * Game completion overlay shown after defeating Boss 3.
 *
 * Single image (game_completed.png) with only NEXT button.
 * No RESTART or MENU buttons.
 */
public class GameCompletionOverlay {

    // ── Image scaling ─────────────────────────────────────────
    private static final float COMPLETED_IMG_RENDER_SCALE = 0.5f;

    // ── Button position ─────────────────────────────────────
    private static final int NEXT_BUTTON_Y = (int)(243 * Game.SCALE);
    private static final int NEXT_BUTTON_X = (int)(374 * Game.SCALE);

    // ── Fade settings ───────────────────────────────────────
    private static final float FADE_SPEED = 0.03f;

    private BufferedImage originalImage;
    private BufferedImage scaledImage;
    private int imgW, imgH, imgX, imgY;

    private UrmButton nextBtn;

    private float overlayAlpha = 0f;
    private boolean fadeComplete = false;
    private boolean isOpen = false;
    private boolean isActive = false;  // FIX: tracks whether overlay should be visible

    private Runnable onNext;

    public GameCompletionOverlay(Runnable onNext) {
        this.onNext = onNext;
        loadImage();
        buildLayout();
    }

    private void loadImage() {
        originalImage = LoadSave.getSpriteAtlas(LoadSave.GAME_COMPLETED);
        if (originalImage == null) {
            System.err.println("[GameCompletionOverlay] Could not load " + LoadSave.GAME_COMPLETED);
            return;
        }

        // Scale the image using COMPLETED_IMG_RENDER_SCALE
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int scaledWidth = (int)(originalWidth * Game.SCALE * COMPLETED_IMG_RENDER_SCALE);
        int scaledHeight = (int)(originalHeight * Game.SCALE * COMPLETED_IMG_RENDER_SCALE);

        scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        System.out.println("[GameCompletionOverlay] Image scaled: " +
                originalWidth + "x" + originalHeight + " -> " +
                scaledWidth + "x" + scaledHeight + " (scale=" + COMPLETED_IMG_RENDER_SCALE + ")");
    }

    private void buildLayout() {
        if (scaledImage != null) {
            imgW = scaledImage.getWidth();
            imgH = scaledImage.getHeight();
        } else {
            // Fallback if image failed to load
            imgW = (int)(500 * Game.SCALE * COMPLETED_IMG_RENDER_SCALE);
            imgH = (int)(500 * Game.SCALE * COMPLETED_IMG_RENDER_SCALE);
        }

        imgX = (Game.GAME_WIDTH - imgW) / 2;
        imgY = (Game.GAME_HEIGHT - imgH) / 2;

        // Create NEXT button only
        nextBtn = new UrmButton(NEXT_BUTTON_X, NEXT_BUTTON_Y, URM_SIZE, URM_SIZE, 0);
    }

    public void reset() {
        overlayAlpha = 0f;
        fadeComplete = false;
        isOpen = true;
        isActive = true;  // FIX: mark active immediately so update/render work on first frame
        nextBtn.resetBools();
    }

    public void update() {
        if (!isActive) return;  // FIX: don't update if not active

        if (!fadeComplete) {
            overlayAlpha = Math.min(overlayAlpha + FADE_SPEED, 0.85f);
            if (overlayAlpha >= 0.85f) {
                fadeComplete = true;
                isOpen = true;
            }
        }
        if (fadeComplete) {
            nextBtn.update();
        }
    }

    public void render(Graphics g) {
        if (!isActive) return;  // FIX: don't render if not active
        Graphics2D g2d = (Graphics2D) g;

        // 1 — semi-transparent black overlay
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // 2 — completion image (scaled)
        if (scaledImage != null) {
            float imgAlpha = Math.min(overlayAlpha / 0.85f, 1f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, imgAlpha));
            g2d.drawImage(scaledImage, imgX, imgY, imgW, imgH, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        } else if (originalImage != null) {
            // Fallback to original unscaled
            float imgAlpha = Math.min(overlayAlpha / 0.85f, 1f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, imgAlpha));
            g2d.drawImage(originalImage, imgX, imgY, (int)(500 * Game.SCALE), (int)(500 * Game.SCALE), null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // 3 — NEXT button appears only after fade completes
        if (fadeComplete) {
            nextBtn.draw(g);
        }
    }

    public void mouseMoved(MouseEvent e) {
        if (!fadeComplete) return;
        nextBtn.setMouseOver(nextBtn.getBounds().contains(e.getX(), e.getY()));
    }

    public void mousePressed(MouseEvent e) {
        if (!fadeComplete) return;
        if (nextBtn.getBounds().contains(e.getX(), e.getY())) {
            nextBtn.setMousePressed(true);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (!fadeComplete || !isActive) return;  // FIX: guard on isActive
        if (nextBtn.isMousePressed() && nextBtn.getBounds().contains(e.getX(), e.getY())) {
            System.out.println("[GameCompletionOverlay] NEXT clicked - calling onNext");
            isActive = false;  // FIX: deactivate before callback to prevent double-firing
            isOpen = false;
            if (onNext != null) onNext.run();
        }
        nextBtn.resetBools();
    }

    public boolean isOpen() {
        return isActive;  // FIX: use isActive as the single source of truth
    }

    /** Forcibly closes the overlay (e.g. on fullReset). */
    public void close() {
        isActive = false;
        isOpen = false;
        fadeComplete = false;
        overlayAlpha = 0f;
    }

    public boolean isFadeComplete() {
        return fadeComplete;
    }
}