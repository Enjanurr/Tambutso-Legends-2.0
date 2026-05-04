package Ui;

import entities.PassengerManager;
import entities.RidingPassenger;
import main.Game;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

import utils.LoadSave;

/**
 * Stop Here Indicator – Arrival Notification System
 *
 * Displays and animates an indicator above the jeepney when any seated
 * passenger has arrived at their destination stop.
 *
 * Trigger: ANY passenger has worldLoopCount >= destinationStop
 * Stop:    ALL arrived passengers have been dropped off
 */
public class StopHereIndicator {

    // =========================================================
    // SPRITE DIMENSIONS (original 660 × 40, 6 columns)
    // =========================================================
    private static final int SRC_WIDTH     = 660;
    private static final int SRC_HEIGHT    = 40;
    private static final int SRC_COLS      = 6;
    private static final int SRC_FRAMES    = 6;

    // =========================================================
    // RENDER SCALE  ← ADJUST
    // =========================================================
    private static final float RENDER_SCALE = Game.SCALE;

    // =========================================================
    // POSITION OFFSETS (relative to jeepney hitbox top)  ← ADJUST
    // =========================================================
    private static final int OFFSET_X = -40;   // negative = left of center
    private static final int OFFSET_Y = -80;   // negative = above jeepney

    // =========================================================
    // ANIMATION SPEED (ticks per frame)  ← ADJUST
    // =========================================================
    private static final int ANIMATION_SPEED = 20;  // lower = faster

    // =========================================================
    // INTERNAL
    // =========================================================
    private BufferedImage spriteSheet;
    private BufferedImage[] frames;

    private int currentFrame = 0;
    private int animationTick = 0;
    private boolean active = false;

    private int renderWidth;
    private int renderHeight;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================
    public StopHereIndicator() {
        loadSprite();
        calculateRenderDimensions();
    }

    // =========================================================
    // ASSET LOADING
    // =========================================================
    private void loadSprite() {
        spriteSheet = LoadSave.getSpriteAtlas(LoadSave.STOP_HERE);
        if (spriteSheet == null) {
            System.err.println("[StopHereIndicator] Failed to load " + LoadSave.STOP_HERE);
            frames = new BufferedImage[SRC_FRAMES];
            return;
        }

        int frameWidth = SRC_WIDTH / SRC_COLS;
        int frameHeight = SRC_HEIGHT;

        frames = new BufferedImage[SRC_FRAMES];
        for (int i = 0; i < SRC_FRAMES; i++) {
            frames[i] = spriteSheet.getSubimage(
                    i * frameWidth, 0, frameWidth, frameHeight
            );
        }
    }

    private void calculateRenderDimensions() {
        int frameWidth = SRC_WIDTH / SRC_COLS;
        renderWidth = (int)(frameWidth * RENDER_SCALE);
        renderHeight = (int)(SRC_HEIGHT * RENDER_SCALE);
    }

    // =========================================================
    // UPDATE LOGIC
    // =========================================================

    /**
     * Updates the indicator state based on passenger arrival status.
     * Called every frame from Playing.update().
     *
     * @param passengerManager Reference to the passenger manager
     * @param currentLoop Current world loop count
     */
    public void update(PassengerManager passengerManager, int currentLoop) {
        // Check if any passenger has arrived at their stop
        boolean hasArrivedPassenger = checkHasArrivedPassenger(passengerManager, currentLoop);

        if (hasArrivedPassenger) {
            active = true;
            updateAnimation();
        } else {
            active = false;
            resetAnimation();
        }
    }

    /**
     * Checks if ANY seated passenger has arrived at their destination.
     *
     * @param passengerManager Passenger manager containing all seated passengers
     * @param currentLoop Current world loop count
     * @return true if at least one passenger is ready to drop
     */
    private boolean checkHasArrivedPassenger(PassengerManager passengerManager, int currentLoop) {
        if (passengerManager == null) return false;

        List<RidingPassenger> seats = passengerManager.getSeatList();
        for (RidingPassenger passenger : seats) {
            if (passenger != null && passenger.isReadyToDrop(currentLoop)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the animation frame based on ANIMATION_SPEED.
     * Loops continuously through frames 0-5 while active.
     */
    private void updateAnimation() {
        animationTick++;
        if (animationTick >= ANIMATION_SPEED) {
            animationTick = 0;
            currentFrame = (currentFrame + 1) % SRC_FRAMES;
        }
    }

    private void resetAnimation() {
        animationTick = 0;
        currentFrame = 0;
    }

    // =========================================================
    // RENDER
    // =========================================================

    /**
     * Draws the indicator above the jeepney if active.
     *
     * @param g Graphics context
     * @param jeepneyX Jeepney hitbox X position
     * @param jeepneyY Jeepney hitbox Y position
     */
    public void render(Graphics g, float jeepneyX, float jeepneyY) {
        if (!active || frames == null || frames[currentFrame] == null) return;

        int drawX = (int)(jeepneyX + OFFSET_X);
        int drawY = (int)(jeepneyY + OFFSET_Y);

        g.drawImage(frames[currentFrame], drawX, drawY, renderWidth, renderHeight, null);
    }

    // =========================================================
    // GETTERS / SETTERS (for runtime adjustment)
    // =========================================================

    public boolean isActive() { return active; }

    /**
     * Set the X offset for the indicator position.
     * Useful for manual tweaking during testing.
     */
    public void setOffsetX(int offsetX) {
        // Would need to store as instance variable
    }

    /**
     * Set the Y offset for the indicator position.
     * Useful for manual tweaking during testing.
     */
    public void setOffsetY(int offsetY) {
        // Would need to store as instance variable
    }

    /**
     * Set the animation speed.
     * @param ticksPerFrame ticks between frame changes (lower = faster)
     */
    public void setAnimationSpeed(int ticksPerFrame) {
        // Would need to store as instance variable
    }
}