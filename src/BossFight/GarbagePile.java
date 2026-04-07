package BossFight;

import main.Game;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A stationary garbage pile laid by Boss Skill 2.
 * Moves left with the world scroll. Damages the jeepney on contact.
 */
public class GarbagePile {

    // -------------------------------------------------------
    // PILE SIZE SETTINGS  ← ADJUST (pixels pre-scale)
    // -------------------------------------------------------
    public static final int PILE_W = 55;
    public static final int PILE_H = 40;

    // Fix 3 — pile hitbox insets ← ADJUST


    private float x, y;
    private final int width, height;
    private boolean active = true;

    private final BufferedImage image;

    public GarbagePile(float x, float y, BufferedImage image) {
        this.x      = x;
        this.y      = y;
        this.width  = (int)(PILE_W * Game.SCALE);
        this.height = (int)(PILE_H * Game.SCALE);
        this.image  = image;
    }

    public void update(float scrollSpeed) {
        x -= scrollSpeed;
        if (x + width < 0) active = false;
    }

    public void render(Graphics g) {
        if (!active) return;
        if (image != null)
            g.drawImage(image, (int) x, (int) y, width, height, null);
        else {
            g.setColor(new Color(60, 140, 60));
            g.fillRect((int) x, (int) y, width, height);
            g.setColor(Color.BLACK);
            g.drawRect((int) x, (int) y, width, height);
        }

    }

    /** Inset hitbox for fairer collision feel. */
    private static final float HB_INSET_PERCENT = 0.5f;
    private static final int X_OFFSET = 0;  // Negative = left, Positive = right
    private static final int Y_OFFSET = 15; // Negative = up, Positive = down

    public Rectangle getHitbox() {
        int insetX = (int)(width * HB_INSET_PERCENT / 2);
        int insetY = (int)(height * HB_INSET_PERCENT / 2);
        return new Rectangle(
                (int) x + insetX + X_OFFSET,  // ← Add X_OFFSET here
                (int) y + insetY + Y_OFFSET,  // ← Add Y_OFFSET here
                width - (insetX * 2),
                height - (insetY * 2));
    }

    public boolean isActive()           { return active; }
    public void    setActive(boolean v) { active = v; }
}