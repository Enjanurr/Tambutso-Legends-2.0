package BossFight.LevelTwo;

import main.Game;
import java.awt.*;
import java.awt.image.BufferedImage;

public class NukeProjectile {

    // ── STATIONARY NUKE (spawned, stays in place, animated) ────
    public static class Nuke {
        public static final int FRAME_W = 60;
        public static final int FRAME_H = 60;
        public static final int ANI_SPEED = 8;
        public static final int FRAME_COUNT = 18;

        private float x, y;  // x, y are TOP-LEFT corner
        private final int width, height;
        private boolean active = true;

        private final BufferedImage[] frames;
        private int aniTick = 0;
        private int aniIndex = 0;

        public Nuke(float x, float y, BufferedImage[] frames) {
            this.x = x;
            this.y = y;  // y is already the top-left Y, no adjustment needed
            this.frames = frames;
            this.width = (int)(FRAME_W * Game.SCALE);
            this.height = (int)(FRAME_H * Game.SCALE);
        }

        public void update(float scrollSpeed) {
            // ── Animate in place ──
            aniTick++;
            if (aniTick >= ANI_SPEED) {
                aniTick = 0;
                aniIndex = (aniIndex + 1) % FRAME_COUNT;
            }

            // ── Scroll left with world ──
            x -= scrollSpeed;
            if (x + width < 0) active = false;
        }

        public void render(Graphics g) {
            if (!active || frames == null || aniIndex >= frames.length) return;
            if (frames[aniIndex] != null) {
                g.drawImage(frames[aniIndex], (int)x, (int)y, width, height, null);
            }
        }

        public Rectangle getHitbox() {
            // Standard hitbox: centered inset, not offset
            int insetX = (int)(width * 0.25f);   // 25% inset each side
            int insetY = (int)(height * 0.25f);  // 25% inset each side
            return new Rectangle(
                    (int)x + insetX,
                    (int)y + insetY,
                    width - (insetX * 2),
                    height - (insetY * 2));
        }

        public boolean isActive() { return active; }
        public void setActive(boolean v) { active = v; }
    }


    // ── SKILL 1 PROJECTILE (travels left, animated) ────────────
    public static class BossProjectile {
        public static final float TRAVEL_SPEED = 2f;
        public static final int ANI_SPEED = 6;
        public static final int FRAME_W = 36;
        public static final int FRAME_H = 34;
        public static final int FRAME_COUNT = 4;

        private float x, y;
        private final int width, height;
        private boolean active = true;

        private final BufferedImage[] frames;
        private int aniTick = 0;
        private int aniIndex = 0;

        public BossProjectile(float startX, float startY, BufferedImage[] frames) {
            this.x = startX;
            this.y = startY;
            this.width = (int)(FRAME_W * Game.SCALE);
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
            if (!active || frames == null || aniIndex >= frames.length) return;
            if (frames[aniIndex] != null) {
                g.drawImage(frames[aniIndex], (int)x, (int)y, width, height, null);
            }
        }

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

        public boolean isActive() { return active; }
        public void setActive(boolean v) { active = v; }
    }
}