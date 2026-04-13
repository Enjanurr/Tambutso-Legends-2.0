package BossFight.LevelOne.Green;

import main.Game;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Projectile fired by the jeepney's Shoot skill (E key).
 * Travels right, animated using Row 4, columns 0-3 of the jeepney sprite sheet.
 * Destroyed when it reaches the right border or hits the boss.
 */

public class GreenJeepProjectile {

    // -------------------------------------------------------
    // PROJECTILE SETTINGS
    // -------------------------------------------------------
    public static final float TRAVEL_SPEED   = 2f;      // pixels per tick (pre-scale)
    public static final int   ANI_SPEED      = 22;      // ticks per frame
    public static final int   FRAME_W        = 20;     // matches skill1 sprite cell
    public static final int   FRAME_H        = 20;      // matches skill1 sprite cell
    public static final int   FRAME_COUNT    = 6;       // 6 frames in skill1 sheet
    public static final int   SPRITE_ROW     = 0;       // Row 0 (only row in skill1 sheet)
    // -------------------------------------------------------

    private float x, y;
    private final int width, height;
    private boolean active = true;

    private final BufferedImage[] frames;
    private int aniTick  = 0;
    private int aniIndex = 0;

    public GreenJeepProjectile(float startX, float startY, BufferedImage[] frames) {
        this.x      = startX;
        this.y      = startY;
        this.width  = (int)(FRAME_W * Game.SCALE);
        this.height = (int)(FRAME_H * Game.SCALE);
        this.frames = frames;
    }

    public void update() {
        x += TRAVEL_SPEED * Game.SCALE;
        if (x > Game.GAME_WIDTH) active = false;

        aniTick++;
        if (aniTick >= ANI_SPEED) {
            aniTick = 0;
            aniIndex = (aniIndex + 1) % FRAME_COUNT;
        }
    }

    public void render(Graphics g) {
        if (!active || frames == null) return;
        if (aniIndex < frames.length && frames[aniIndex] != null) {
            g.drawImage(frames[aniIndex], (int) x, (int) y, width, height, null);
        }
    }

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
    public void setActive(boolean v) { active = v; }
    public float getX() { return x; }
    public float getY() { return y; }
}