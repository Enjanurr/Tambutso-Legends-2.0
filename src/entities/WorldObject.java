package entities;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class WorldObject {
    public record DebugInfo(String label, float scale, int anchorY, float xOffset) {}

    // Decorative roadside props are lightweight screen-space sprites that
    // enter from the right and scroll off with the world.
    private float x;
    private final int anchorY;
    private final int width;
    private final int height;
    private final BufferedImage image;
    private final DebugInfo debugInfo;
    private final boolean bottomAligned;
    private boolean active = true;
    private boolean removed = false;
    private boolean disposed = false;
    
    public WorldObject(float x, int y, int width, int height, BufferedImage image) {
        this(x, y, width, height, image, null, false);
    }

    public WorldObject(float x, int y, int width, int height, BufferedImage image, DebugInfo debugInfo) {
        this(x, y, width, height, image, debugInfo, false);
    }

    public WorldObject(float x, int anchorY, int width, int height, BufferedImage image,
                       DebugInfo debugInfo, boolean bottomAligned) {
        this.x = x;
        this.anchorY = anchorY;
        this.width = width;
        this.height = height;
        this.image = image;
        this.debugInfo = debugInfo;
        this.bottomAligned = bottomAligned;
    }

    public void update(boolean worldScrolling, float scrollSpeed) {
        if (!active) {
            return;
        }

        if (worldScrolling) {
            x -= scrollSpeed;
        }

        if (x + width < 0) {
            active = false;
        }
    }

    public void draw(Graphics g) {
        if (!active || image == null) {
            return;
        }

        g.drawImage(image, Math.round(x), getDrawY(), width, height, null);
    }

    public boolean isRemovable() {
        // Once a prop has fully left the screen, the manager can drop it.
        return !active && !removed;
    }

    public void markRemoved() {
        removed = true;
    }

    public void dispose() {
        if (!disposed) {
            disposed = true;
        }
    }

    public boolean hasDebugInfo() {
        return debugInfo != null;
    }

    public DebugInfo getDebugInfo() {
        return debugInfo;
    }

    public float getX() {
        return x;
    }

    public int getY() {
        return anchorY;
    }

    public int getDrawY() {
        if (!bottomAligned) {
            return anchorY;
        }

        return anchorY - height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isBottomAligned() {
        return bottomAligned;
    }
}
