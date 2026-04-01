// WorldObject.java
package entities;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class WorldObject {
    private static final int HIDDEN_X = Integer.MIN_VALUE;
    private static final int NO_LOOP_SCHEDULED = -1;

    private final int worldX;
    private final int worldY;
    private final int width;
    private final int height;
    private final BufferedImage image;

    // 0 = never respawn, >0 = respawn after this many completed world loops.
    private final int loopsToRespawn;

    private int     nextAppearLoop   = NO_LOOP_SCHEDULED;
    private boolean gone             = false;
    // While respawning, the object ignores worldX and scrolls in from the right edge.
    private boolean respawning       = false;
    private boolean removed          = false;
    private boolean disposed         = false;
    private float   respawnScreenX   = 0f;
    private float   lastWorldOffset  = 0f;
    private boolean hasLastWorldTick = false;

    // Cached each update() so draw() doesn't need levelPixelWidth
    private int cachedScreenX = HIDDEN_X;

    public WorldObject(int worldX, int worldY, int width, int height,
                       BufferedImage image, int loopsToRespawn) {
        this.worldX         = worldX;
        this.worldY         = worldY;
        this.width          = width;
        this.height         = height;
        this.image          = image;
        this.loopsToRespawn = loopsToRespawn;
    }

    /** Reset to initial visible state on full game restart. */
    public void reset() {
        gone             = false;
        nextAppearLoop   = NO_LOOP_SCHEDULED;
        respawning       = false;
        removed          = false;
        disposed         = false;
        respawnScreenX   = 0f;
        lastWorldOffset  = 0f;
        hasLastWorldTick = false;
        cachedScreenX    = HIDDEN_X;
    }

    /**
     * Update visibility. Call this every tick.
     *
     * @param worldOffset      current world scroll offset in pixels
     * @param viewportWidth    screen width in pixels
     * @param currentLoopCount number of completed full level loops so far
     * @param levelPixelWidth  pixel width of one full level
     */
    public void update(float worldOffset, int viewportWidth,
                       int currentLoopCount, int levelPixelWidth) {
        // Track how far the world advanced this frame so respawning objects can move with it.
        float scrollDelta = getScrollDelta(worldOffset, levelPixelWidth);
        lastWorldOffset  = worldOffset;
        hasLastWorldTick = true;

        if (gone && loopsToRespawn > 0
                && nextAppearLoop != NO_LOOP_SCHEDULED
                && currentLoopCount >= nextAppearLoop) {
            // Start just off the right side so the sprite scrolls onto the screen naturally.
            gone           = false;
            respawning     = true;
            respawnScreenX = viewportWidth;
            nextAppearLoop = NO_LOOP_SCHEDULED;
        }

        if (gone) {
            cachedScreenX = HIDDEN_X;
            return;
        }

        if (respawning) {
            respawnScreenX -= scrollDelta;
            cachedScreenX = Math.round(respawnScreenX);

            if (cachedScreenX + width < 0) {
                gone          = true;
                respawning    = false;
                cachedScreenX = HIDDEN_X;
                if (loopsToRespawn > 0) {
                    nextAppearLoop = currentLoopCount + loopsToRespawn;
                }
            }
            return;
        }

        int unwrappedScreenX = getScreenX(worldOffset, levelPixelWidth);
        if (unwrappedScreenX + width < 0) {
            // Once the object fully exits left, hide it until its scheduled return loop.
            hideUntilLoop(currentLoopCount);
            return;
        }

        cachedScreenX = unwrappedScreenX;
    }

    private float getScrollDelta(float worldOffset, int levelPixelWidth) {
        if (!hasLastWorldTick) {
            return 0f;
        }

        float scrollDelta = worldOffset - lastWorldOffset;
        if (scrollDelta < 0) {
            scrollDelta += levelPixelWidth;
        }
        return scrollDelta;
    }

    private int getScreenX(float worldOffset, int levelPixelWidth) {
        int posInLevel = worldX % levelPixelWidth;
        return posInLevel - ((int) worldOffset % levelPixelWidth);
    }

    private void hideUntilLoop(int currentLoopCount) {
        // Schedule the next appearance relative to the current completed-loop count.
        gone          = true;
        respawning    = false;
        cachedScreenX = HIDDEN_X;
        if (loopsToRespawn > 0) {
            nextAppearLoop = currentLoopCount + loopsToRespawn;
        }
    }

    public void draw(Graphics g) {
        if (gone || cachedScreenX == HIDDEN_X) return;
        if (cachedScreenX + width > 0) {
            g.drawImage(image, cachedScreenX, worldY, width, height, null);
        }
    }

    /**
     * True only when permanently gone with no scheduled respawn.
     * Safe to remove from the list and dispose.
     */
    public boolean isRemovable() {
        return gone && loopsToRespawn == 0 && !removed;
    }

    public boolean isGone()   { return gone; }
    public void markRemoved() { removed = true; }
    public void dispose()     { if (!disposed) disposed = true; }
}
