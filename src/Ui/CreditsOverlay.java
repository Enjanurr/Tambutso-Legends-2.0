package Ui;

import main.Game;
import utils.LoadSave;
import utils.Constants.AboutButtons;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Credits overlay with scrolling animation.
 * Image: 233 × 1029 pixels.
 * Scrolls from bottom to center (showing last part at center) once, then stops.
 */
public class CreditsOverlay {
    private static final int CREDITS_WIDTH_DEFAULT = 233;
    private static final int CREDITS_HEIGHT_DEFAULT = 1029;
    private static final float CREDITS_RENDER_SCALE = 0.9f;  // Make credits 20% bigger (adjust as needed)
    private static final float SCROLL_SPEED = 1f;  // pixels per frame

    // Scaled dimensions
    private final int CREDITS_WIDTH;
    private final int CREDITS_HEIGHT;

    private BufferedImage originalImage;
    private BufferedImage scaledImage;
    private float yOffset;      // Current Y position (starts at bottom, moves up)
    private float targetY;
    private boolean isOpen = false;
    private boolean isAnimating = true;
    private float endY;         // Final position (can be negative for tall images)

    // Exit button
    private AboutButton exitButton;

    // Callback
    private Runnable onClose;

    public CreditsOverlay(Runnable onClose) {
        this.onClose = onClose;

        // Calculate scaled dimensions with Game.SCALE and CREDITS_RENDER_SCALE
        CREDITS_WIDTH = (int)(CREDITS_WIDTH_DEFAULT * Game.SCALE * CREDITS_RENDER_SCALE);
        CREDITS_HEIGHT = (int)(CREDITS_HEIGHT_DEFAULT * Game.SCALE * CREDITS_RENDER_SCALE);

        loadImage();
    }

    private void loadImage() {
        originalImage = LoadSave.getSpriteAtlas(LoadSave.CREDITS_IMG);
        if (originalImage == null) {
            System.err.println("[CreditsOverlay] Failed to load credits.png");
            return;
        }

        // Scale the image with render scale
        scaledImage = new BufferedImage(CREDITS_WIDTH, CREDITS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, CREDITS_WIDTH, CREDITS_HEIGHT, null);
        g2d.dispose();

        System.out.println("[CreditsOverlay] Scaled credits: " + CREDITS_WIDTH_DEFAULT + "x" + CREDITS_HEIGHT_DEFAULT +
                " -> " + CREDITS_WIDTH + "x" + CREDITS_HEIGHT +
                " (Game.SCALE=" + Game.SCALE + ", renderScale=" + CREDITS_RENDER_SCALE + ")");
    }

    private void initButton() {
        // Exit button: top-right corner
        int exitX = Game.GAME_WIDTH - AboutButtons.BUTTON_WIDTH - 40;
        int exitY = 20;
        exitButton = new AboutButton(exitX, exitY, AboutButtons.EXIT_ROW);
    }

    public boolean open() {
        if (isOpen) return false;

        initButton();

        // Start offscreen (below bottom)
        yOffset = Game.GAME_HEIGHT;

        // Calculate target: stop when the image's BOTTOM edge reaches screen center
        // This ensures the last part of the credits is centered
        float screenCenterY = Game.GAME_HEIGHT / 2f;
        endY = screenCenterY - CREDITS_HEIGHT;

        // Don't scroll past the top of the screen
        if (endY > 0) {
            // If image fits entirely, center it normally
            endY = (Game.GAME_HEIGHT - CREDITS_HEIGHT) / 2f;
        }

        targetY = endY;
        isAnimating = true;
        isOpen = true;

        resetButtons();
        System.out.println("[CreditsOverlay] Opened - scrolling from " + yOffset + " to " + targetY);
        System.out.println("[CreditsOverlay] Scaled image height=" + CREDITS_HEIGHT + ", screenCenter=" + screenCenterY);
        return true;
    }

    public void close() {
        isOpen = false;
        isAnimating = false;
        if (onClose != null) onClose.run();
        System.out.println("[CreditsOverlay] Closed");
    }

    private void resetButtons() {
        if (exitButton != null) exitButton.resetBools();
    }

    public void update() {
        if (!isOpen) return;

        // Update animation
        if (isAnimating) {
            yOffset -= SCROLL_SPEED;

            // Stop when reached target or passed it
            if (yOffset <= targetY) {
                yOffset = targetY;
                isAnimating = false;
                System.out.println("[CreditsOverlay] Scrolling complete - reached Y=" + yOffset);
            }
        }

        // Update exit button
        if (exitButton != null) exitButton.update();
    }

    public void draw(Graphics g) {
        if (!isOpen) return;

        // Draw semi-transparent background
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);

        // Draw credits image at current Y position
        if (scaledImage != null) {
            int x = (Game.GAME_WIDTH - CREDITS_WIDTH) / 2;
            g.drawImage(scaledImage, x, (int) yOffset, CREDITS_WIDTH, CREDITS_HEIGHT, null);
        } else if (originalImage != null) {
            // Fallback: draw original unscaled
            int x = (Game.GAME_WIDTH - CREDITS_WIDTH_DEFAULT) / 2;
            g.drawImage(originalImage, x, (int) yOffset, CREDITS_WIDTH_DEFAULT, CREDITS_HEIGHT_DEFAULT, null);
        } else {
            // Fallback: draw placeholder
            int x = (Game.GAME_WIDTH - CREDITS_WIDTH) / 2;
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x, (int) yOffset, CREDITS_WIDTH, CREDITS_HEIGHT);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, (int)(20 * Game.SCALE)));
            String text = "CREDITS";
            FontMetrics fm = g.getFontMetrics();
            int textX = x + (CREDITS_WIDTH - fm.stringWidth(text)) / 2;
            int textY = (int) yOffset + CREDITS_HEIGHT / 2;
            g.drawString(text, textX, textY);
        }

        // Draw exit button (visible always)
        if (exitButton != null) exitButton.draw(g);
    }

    public void mousePressed(MouseEvent e) {
        if (!isOpen || exitButton == null) return;

        if (exitButton.getBounds().contains(e.getX(), e.getY())) {
            exitButton.setMousePressed(true);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (!isOpen || exitButton == null) return;

        if (exitButton.getBounds().contains(e.getX(), e.getY()) && exitButton.isMousePressed()) {
            System.out.println("[CreditsOverlay] Exit clicked - closing");
            close();
            exitButton.setMousePressed(false);
        }
        exitButton.setMousePressed(false);
    }

    public void mouseMoved(MouseEvent e) {
        if (!isOpen || exitButton == null) return;

        exitButton.setMouseOver(false);
        if (exitButton.getBounds().contains(e.getX(), e.getY())) {
            exitButton.setMouseOver(true);
        }
    }

    public void handleEsc() {
        if (isOpen) {
            close();
        }
    }

    public boolean isOpen() { return isOpen; }
    public boolean isAnimating() { return isAnimating; }
}