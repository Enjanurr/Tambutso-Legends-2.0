package entities;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class WorldObject {
    // Decorative roadside props are lightweight screen-space sprites that
    // enter from the right and scroll off with the world.
    private float x;
    private final int y;
    private final int width;
    private final int height;
    private final BufferedImage image;
    private boolean active = true;
    private boolean removed = false;
    private boolean disposed = false;
    
    public WorldObject(float x, int y, int width, int height, BufferedImage image) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.image = image;
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

        g.drawImage(image, Math.round(x), y, width, height, null);
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
}
