package BossFight.LevelTwo.Red;

import main.Game;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Projectile fired by Red Jeep Skill 2 (Slow Ball).
 * IDENTICAL to RedJeepProjectile — only difference is the effect on boss hit.
 */
public class SlowBallProjectile {

    // -------------------------------------------------------
    // PROJECTILE SETTINGS (MATCH SKILL 1)
    // -------------------------------------------------------
    public static final float TRAVEL_SPEED   = 2f;   // MATCH RedJeepProjectile
    public static final int   ANI_SPEED      = 22;   // MATCH RedJeepProjectile
    public static final int   FRAME_W        = 110;  // MATCH RedJeepProjectile
    public static final int   FRAME_H        = 40;   // MATCH RedJeepProjectile
    public static final int   FRAME_COUNT    = 4;    // MATCH RedJeepProjectile
    public static final int   SPRITE_ROW     = 3;    // Different row (slow ball row)
    // -------------------------------------------------------

    private float x, y;
    private final int width, height;
    private boolean active = true;

    private final BufferedImage[] frames;
    private int aniTick  = 0;
    private int aniIndex = 0;

    public SlowBallProjectile(float startX, float startY, BufferedImage[] frames) {
        this.x      = startX;
        this.y      = startY;
        this.width  = (int)(FRAME_W * Game.SCALE);
        this.height = (int)(FRAME_H * Game.SCALE);
        this.frames = frames;
    }

    public void update() {
        // MATCH RedJeepProjectile logic exactly
        x += TRAVEL_SPEED * Game.SCALE;
        if (x > Game.GAME_WIDTH) active = false;

        aniTick++;
        if (aniTick >= ANI_SPEED) {
            aniTick = 0;
            aniIndex = (aniIndex + 1) % FRAME_COUNT;
        }
    }

    public void render(Graphics g) {
        // MATCH RedJeepProjectile logic exactly
        if (!active || frames == null) return;
        g.drawImage(frames[aniIndex], (int) x, (int) y, width, height, null);
    }

    // MATCH RedJeepProjectile hitbox exactly
    private static final float HB_INSET_PERCENT = 0.7f;
    private static final int X_OFFSET = -30;
    private static final int Y_OFFSET = 20;

    public Rectangle getHitbox() {
        int insetX = (int)(width * HB_INSET_PERCENT / 2);
        int insetY = (int)(height * HB_INSET_PERCENT / 2);
        return new Rectangle(
                (int) x + insetX + X_OFFSET,
                (int) y + insetY + Y_OFFSET,
                width - (insetX * 2),
                height - (insetY * 2));
    }

    public boolean isActive() { return active; }
    public void    setActive(boolean v) { active = v; }
    public float   getX() { return x; }
    public float   getY() { return y; }
}