package Ui;

import gameStates.Playing;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static utils.Constants.UI.URMButtons.*;

/**
 * Status Check Overlay - displayed after MAX_WORLD_LOOPS is reached.
 * Checks if player met requirements to fight the boss.
 *
 * Requirements to Pass:
 *   - Total Passenger Dropped: 12 / 12
 *   - Total Fare Earned: 500+
 *
 * Pass: Shows before_boss_passed.png with Next button → Proceed to boss fight
 * Fail: Shows before_boss_failed.png with Restart button → Restart game
 */
public class StatusCheckOverlay {

    private final Playing playing;

    // ── Overlay state ───────────────────────────────────────────
    private boolean active = false;
    private boolean passed = false;

    // ── Overlay images ─────────────────────────────────────────
    private BufferedImage passedImg;
    private BufferedImage failedImg;

    // ── Adjustable overlay positioning ─────────────────────────
    // PASSED overlay position
    private static final float BEFORE_BOSS_PASSED_SCALE = 1.0f;
    private static int beforeBossPassedX;
    private static int beforeBossPassedY;
    private static int passedImgW, passedImgH;

    // FAILED overlay position
    private static final float BEFORE_BOSS_FAILED_SCALE = 1.0f;
    private static int beforeBossFailedX;
    private static int beforeBossFailedY;
    private static int failedImgW, failedImgH;

    // ── Background overlay ─────────────────────────────────────
    private static final Color OVERLAY_BG_COLOR = new Color(0, 0, 0, 180);

    // ── Statistics to display ──────────────────────────────────
    private int passengersDropped = 0;
    private int totalFare = 0;
    private int requiredPassengers = 12;
    private int requiredFare = 500;

    // ── Text display settings ──────────────────────────────────
    // Passenger count text
    private static final int PASSENGER_TEXT_X_OFFSET = 30;  // Offset from image center
    private static final int PASSENGER_TEXT_Y_OFFSET = 40;
    private static final Color PASSENGER_TEXT_COLOR = Color.WHITE;
    private static final int PASSENGER_FONT_SIZE = 15;

    // Fare text
    private static final int FARE_TEXT_X_OFFSET = 30;
    private static final int FARE_TEXT_Y_OFFSET = 55;
    private static final Color FARE_TEXT_COLOR = new Color(255, 215, 0); // Gold #FFD700
    private static final int FARE_FONT_SIZE = 15;

    // ── Buttons ─────────────────────────────────────────────────
    private UrmButton nextButton;      // Row 0 for PASSED
    private UrmButton restartButton;   // Row 1 for FAILED

    // Button positions (adjustable)
    private static final int NEXT_BUTTON_X_OFFSET = 0;
    private static final int NEXT_BUTTON_Y_OFFSET = 15;
    private static final int RESTART_BUTTON_X_OFFSET = 0;
    private static final int RESTART_BUTTON_Y_OFFSET = 15;

    // ── Fade-in effect ──────────────────────────────────────────
    private static final float FADE_SPEED = 0.05f;
    private float overlayAlpha = 0f;
    private boolean fadeComplete = false;

    // ─────────────────────────────────────────────────────────────
    public StatusCheckOverlay(Playing playing) {
        this.playing = playing;
        loadImages();
        buildLayout();
        createButtons();
    }

    // ─────────────────────────────────────────────────────────────
    // SETUP
    // ─────────────────────────────────────────────────────────────
    private void loadImages() {
        passedImg = LoadSave.getSpriteAtlas(LoadSave.BEFORE_BOSS_PASSED);
        failedImg = LoadSave.getSpriteAtlas(LoadSave.BEFORE_BOSS_FAILED);

        if (passedImg == null)
            System.err.println("[StatusCheckOverlay] Could not load " + LoadSave.BEFORE_BOSS_PASSED);
        if (failedImg == null)
            System.err.println("[StatusCheckOverlay] Could not load " + LoadSave.BEFORE_BOSS_FAILED);
    }

    private void buildLayout() {
        // Calculate image dimensions based on scale
        // Source images are 224 x 204
        int srcW = 224;
        int srcH = 204;

        // PASSED image layout
        passedImgW = (int)(srcW * Game.SCALE * BEFORE_BOSS_PASSED_SCALE);
        passedImgH = (int)(srcH * Game.SCALE * BEFORE_BOSS_PASSED_SCALE);
        beforeBossPassedX = (Game.GAME_WIDTH - passedImgW) / 2;
        beforeBossPassedY = (Game.GAME_HEIGHT - passedImgH) / 2;

        // FAILED image layout
        failedImgW = (int)(srcW * Game.SCALE * BEFORE_BOSS_FAILED_SCALE);
        failedImgH = (int)(srcH * Game.SCALE * BEFORE_BOSS_FAILED_SCALE);
        beforeBossFailedX = (Game.GAME_WIDTH - failedImgW) / 2;
        beforeBossFailedY = (Game.GAME_HEIGHT - failedImgH) / 2;
    }

    private void createButtons() {
        // Next button (Row 0) - for PASSED state
        int nextBtnX = beforeBossPassedX + passedImgW / 2 - URM_SIZE / 2 + (int)(NEXT_BUTTON_X_OFFSET * Game.SCALE);
        int nextBtnY = beforeBossPassedY + passedImgH + (int)(NEXT_BUTTON_Y_OFFSET * Game.SCALE);
        nextButton = new UrmButton(nextBtnX, nextBtnY, URM_SIZE, URM_SIZE, 0);

        // Restart button (Row 1) - for FAILED state
        int restartBtnX = beforeBossFailedX + failedImgW / 2 - URM_SIZE / 2 + (int)(RESTART_BUTTON_X_OFFSET * Game.SCALE);
        int restartBtnY = beforeBossFailedY + failedImgH + (int)(RESTART_BUTTON_Y_OFFSET * Game.SCALE);
        restartButton = new UrmButton(restartBtnX, restartBtnY, URM_SIZE, URM_SIZE, 1);
    }

    // ─────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────

    /**
     * Opens the status check overlay and evaluates if player passed.
     * Call this when MAX_WORLD_LOOPS is reached and all passengers are dropped.
     *
     * @param passengersDropped number of passengers dropped
     * @param totalFare total fare earned
     * @return true if overlay was opened
     */
    public boolean open(int passengersDropped, int totalFare) {
        return open(passengersDropped, totalFare, 12, 500);
    }

    /**
     * Opens the status check overlay with level-specific requirements.
     *
     * @param passengersDropped number of passengers dropped
     * @param totalFare total fare earned
     * @param requiredPassengers minimum passengers required to pass
     * @param requiredFare minimum fare required to pass
     * @return true if overlay was opened
     */
    public boolean open(int passengersDropped, int totalFare, int requiredPassengers, int requiredFare) {
        if (active) return false;

        this.passengersDropped = passengersDropped;
        this.totalFare = totalFare;
        this.requiredPassengers = requiredPassengers;
        this.requiredFare = requiredFare;

        // Check requirements
        passed = (passengersDropped >= requiredPassengers) && (totalFare >= requiredFare);

        active = true;
        overlayAlpha = 0f;
        fadeComplete = false;

        // Reset button states
        nextButton.resetBools();
        restartButton.resetBools();

        System.out.println("[StatusCheck] Opened - Passengers: " + passengersDropped + "/" + requiredPassengers +
                ", Fare: " + totalFare + "/" + requiredFare + " - " + (passed ? "PASSED" : "FAILED"));

        return true;
    }

    public void close() {
        active = false;
        overlayAlpha = 0f;
        fadeComplete = false;
    }

    public boolean isActive() { return active; }
    public boolean isPassed() { return passed; }

    // ─────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────
    public void update() {
        if (!active) return;

        // Fade in background
        if (!fadeComplete) {
            overlayAlpha = Math.min(overlayAlpha + FADE_SPEED, 1.0f);
            if (overlayAlpha >= 1.0f) fadeComplete = true;
        }

        // Update appropriate button
        if (fadeComplete) {
            if (passed) {
                nextButton.update();
            } else {
                restartButton.update();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (!active) return;

        Graphics2D g2d = (Graphics2D) g;

        // 1 - Semi-transparent black overlay
        g2d.setColor(OVERLAY_BG_COLOR);
        g2d.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);

        // 2 - Status image with fade-in
        BufferedImage img = passed ? passedImg : failedImg;
        int imgX = passed ? beforeBossPassedX : beforeBossFailedX;
        int imgY = passed ? beforeBossPassedY : beforeBossFailedY;
        int imgW = passed ? passedImgW : failedImgW;
        int imgH = passed ? passedImgH : failedImgH;

        if (img != null) {
            float imgAlpha = Math.min(overlayAlpha / 0.5f, 1f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, imgAlpha));
            g2d.drawImage(img, imgX, imgY, imgW, imgH, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // 3 - Draw statistics text on top of image
        if (fadeComplete) {
            drawStatsText(g2d, imgX, imgY, imgW, imgH);

            // 4 - Draw appropriate button
            if (passed) {
                nextButton.draw(g);
            } else {
                restartButton.draw(g);
            }
        }
    }

    private void drawStatsText(Graphics2D g2d, int imgX, int imgY, int imgW, int imgH) {
        int centerX = imgX + imgW / 2;
        int baseY = imgY + imgH / 2;

        // Passenger count text
        g2d.setFont(new Font("SansSerif", Font.BOLD, (int)(PASSENGER_FONT_SIZE * Game.SCALE)));
        g2d.setColor(PASSENGER_TEXT_COLOR);
        String passengerText = passengersDropped + " / " + requiredPassengers;
        int passengerTextX = centerX - g2d.getFontMetrics().stringWidth(passengerText) / 2 +
                (int)(PASSENGER_TEXT_X_OFFSET * Game.SCALE);
        int passengerTextY = baseY + (int)(PASSENGER_TEXT_Y_OFFSET * Game.SCALE);
        g2d.drawString(passengerText, passengerTextX, passengerTextY);

        // Fare text
        g2d.setFont(new Font("SansSerif", Font.BOLD, (int)(FARE_FONT_SIZE * Game.SCALE)));
        g2d.setColor(FARE_TEXT_COLOR);
        String fareText = totalFare + " / " + requiredFare;
        int fareTextX = centerX - g2d.getFontMetrics().stringWidth(fareText) / 2 +
                (int)(FARE_TEXT_X_OFFSET * Game.SCALE);
        int fareTextY = baseY + (int)(FARE_TEXT_Y_OFFSET * Game.SCALE);
        g2d.drawString(fareText, fareTextX, fareTextY);
    }

    // ─────────────────────────────────────────────────────────────
    // INPUT HANDLING
    // ─────────────────────────────────────────────────────────────

    public void mouseMoved(MouseEvent e) {
        if (!active || !fadeComplete) return;

        if (passed) {
            nextButton.setMouseOver(nextButton.getBounds().contains(e.getX(), e.getY()));
        } else {
            restartButton.setMouseOver(restartButton.getBounds().contains(e.getX(), e.getY()));
        }
    }

    public void mousePressed(MouseEvent e) {
        if (!active || !fadeComplete) return;

        if (passed) {
            if (nextButton.getBounds().contains(e.getX(), e.getY()))
                nextButton.setMousePressed(true);
        } else {
            if (restartButton.getBounds().contains(e.getX(), e.getY()))
                restartButton.setMousePressed(true);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (!active || !fadeComplete) return;

        if (passed) {
            if (nextButton.isMousePressed() && nextButton.getBounds().contains(e.getX(), e.getY())) {
                // Proceed to boss fight
                close();
                playing.startBossFight();
            }
            nextButton.resetBools();
        } else {
            if (restartButton.isMousePressed() && restartButton.getBounds().contains(e.getX(), e.getY())) {
                // Restart the game
                close();
                playing.restartGame();
                playing.getGameClock().start();  // Restart clock after reset
            }
            restartButton.resetBools();
        }
    }

    /**
     * Returns true if the given point is within the overlay's interactive area.
     * Used for input blocking.
     */
    public boolean contains(int x, int y) {
        if (!active) return false;

        // Check if within the status image
        int imgX = passed ? beforeBossPassedX : beforeBossFailedX;
        int imgY = passed ? beforeBossPassedY : beforeBossFailedY;
        int imgW = passed ? passedImgW : failedImgW;
        int imgH = passed ? passedImgH : failedImgH;

        Rectangle imgBounds = new Rectangle(imgX, imgY, imgW, imgH);
        if (imgBounds.contains(x, y)) return true;

        // Check if within button bounds
        if (passed) {
            return nextButton.getBounds().contains(x, y);
        } else {
            return restartButton.getBounds().contains(x, y);
        }
    }
}
