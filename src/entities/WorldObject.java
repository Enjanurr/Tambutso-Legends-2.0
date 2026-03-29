// WorldObject.java
package entities;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class WorldObject {
    private final int worldX;
    private final int worldY;
    private final int width;
    private final int height;
    private final BufferedImage image;

    // respawn control: 0 = never respawn, >0 = respawn after this many loops
    private final int loopsToRespawn;
    private int nextAppearLoop = -1;      // loop index when object should next appear
    private int scheduledWorldX = -1;     // world X to use when scheduling a respawn
    private boolean pendingRespawn = false;
    private boolean gone = false;

    // resource / lifecycle flags
    private boolean removed = false;      // true once removed from list
    private boolean disposed = false;     // true once resources released

    public WorldObject(int worldX, int worldY, int width, int height,
                       BufferedImage image, int loopsToRespawn) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.width = width;
        this.height = height;
        this.image = image;
        this.loopsToRespawn = loopsToRespawn;
    }

    public boolean isGone() { return gone; }

    /**
     * Reset object to initial visible state (used on full game restart).
     */
    public void reset() {
        gone = false;
        nextAppearLoop = -1;
        pendingRespawn = false;
        scheduledWorldX = -1;
        removed = false;
        disposed = false;
    }

    /**
     * Update visibility and respawn scheduling.
     *
     * @param worldOffset      current world offset in pixels
     * @param viewportWidth    screen width in pixels
     * @param currentLoopCount number of completed full level loops
     * @param levelPixelWidth  width of one level in pixels (used to schedule respawn ahead)
     */
    public void update(float worldOffset, int viewportWidth, int currentLoopCount, int levelPixelWidth) {
        // If object was gone and its scheduled loop has arrived, mark pending respawn.
        if (gone && nextAppearLoop >= 0 && currentLoopCount >= nextAppearLoop) {
            if (scheduledWorldX < 0) {
                scheduledWorldX = worldX + levelPixelWidth;
            }
            pendingRespawn = true;
            nextAppearLoop = -1; // clear schedule; now waiting for off-right condition
        }

        // If pending respawn, wait until the scheduled world X is off the right side.
        // pending respawn check (keep scheduledWorldX; do not clear it)
        if (pendingRespawn) {
            int screenX = scheduledWorldX - (int) worldOffset;
            if (screenX >= viewportWidth) {   // use >= for safety
                gone = false;
                pendingRespawn = false;
                // DO NOT clear scheduledWorldX here — keep it as the effective position
            } else {
                return; // still waiting offscreen to the right
            }
        }


        if (gone) return;

        // Determine effective world X (use scheduledWorldX if set and not pending)
        int effectiveWorldX = (scheduledWorldX > 0 && !pendingRespawn) ? scheduledWorldX : worldX;
        int screenX = effectiveWorldX - (int) worldOffset;

        // inside update(...), replace the scheduling branch with this:

        // If fully offscreen to the left or right, mark gone and schedule respawn if configured
        if (screenX + width < 0 || screenX > viewportWidth) {
            gone = true;
            if (loopsToRespawn > 0) {
                // schedule reappearance after `loopsToRespawn` full loops
                nextAppearLoop = currentLoopCount + loopsToRespawn;

                // place the scheduled world X several levels ahead so it will be off the right
                // when the loop count arrives: effectiveWorldX + levelPixelWidth * loopsToRespawn
                scheduledWorldX = effectiveWorldX + levelPixelWidth * loopsToRespawn;

                // pendingRespawn will be set when the loop count arrives
                pendingRespawn = false;
            } else {
                nextAppearLoop = -1; // never respawn
            }
        }

    }

    /**
     * Draw the object if any part is inside the viewport.
     */
    public void draw(Graphics g, float worldOffset, int viewportWidth) {
        if (gone) return;
        int effectiveWorldX = (scheduledWorldX > 0 && !pendingRespawn) ? scheduledWorldX : worldX;
        int screenX = effectiveWorldX - (int) worldOffset;
        if (screenX + width > 0 && screenX < viewportWidth) {
            g.drawImage(image, screenX, worldY, width, height, null);
        }
    }

    /**
     * True when the object is permanently removable (gone and not scheduled to reappear).
     */
    public boolean isRemovable() {
        return gone && nextAppearLoop < 0 && !pendingRespawn && !removed;
    }

    public void markRemoved() {
        removed = true;
    }

    /**
     * Free heavy resources held by this object.
     * If the image is shared globally (recommended), do not null it here.
     * If this object owns the image, call image.flush() and drop the reference.
     */
    public void dispose() {
        if (disposed) return;
        // If this object owns the image, uncomment the following:
        // if (image != null) {
        //     image.flush();
        //     // Note: image reference is final; only null if you change field mutability.
        // }
        disposed = true;
    }
}
