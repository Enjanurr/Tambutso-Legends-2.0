package BossFight.LevelOne;

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

    /**
     * Projectile fired by Boss Skill 1.
     * Travels left, animated using Row 0 columns 0-4 of boss1.png.
     * Destroyed when it reaches the left border or hits the jeepney.
     */
    public static class BossProjectile {

        // -------------------------------------------------------
        // PROJECTILE SETTINGS  ← ADJUST
        // -------------------------------------------------------
        public static final float TRAVEL_SPEED = 2f;  // pixels per tick (pre-scale)
        public static final int   ANI_SPEED    = 6;   // ticks per frame ← ADJUST
        // Boss bullet frame dimensions (from boss1.png Row 0)
        public static final int   FRAME_W     = 110;
        public static final int   FRAME_H     = 79;
        public static final int   FRAME_COUNT = 5;

        // -------------------------------------------------------

        private float x, y;
        private final int width, height;
        private boolean active = true;

        private final BufferedImage[] frames;
        private int aniTick  = 0;
        private int aniIndex = 0;

        public BossProjectile(float startX, float startY, BufferedImage[] frames) {
            this.x      = startX;
            this.y      = startY;
            this.width  = (int)(FRAME_W * Game.SCALE);
            this.height = (int)(FRAME_H * Game.SCALE);
            this.frames = frames;
        }

        public void update() {
            x -= TRAVEL_SPEED * Game.SCALE;
            if (x + width < 0) active = false;

            aniTick++;
            if (aniTick >= ANI_SPEED) {
                aniTick = 0;
                aniIndex = (aniIndex + 1) % FRAME_COUNT;
            }
        }

        public void render(Graphics g) {
            if (!active || frames == null) return;
            g.drawImage(frames[aniIndex], (int) x, (int) y, width, height, null);

        }

        /** Inset hitbox for fairer collision feel. */
        private static final float HB_INSET_PERCENT = 0.8f;
        private static final int X_OFFSET = 0;  // Negative = left, Positive = right
        private static final int Y_OFFSET = 30; // Negative = up, Positive = down

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
}