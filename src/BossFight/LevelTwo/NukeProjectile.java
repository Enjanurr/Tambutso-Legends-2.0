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

        private float x, y;
        private final int width, height;
        private boolean active = true;

        private final BufferedImage[] frames;
        private int aniTick = 0;
        private int aniIndex = 0;

        public Nuke(float x, float y, BufferedImage[] frames) {
            this.x = x;
            this.y = y;
            this.frames = frames;
            this.width = (int)(FRAME_W * Game.SCALE);
            this.height = (int)(FRAME_H * Game.SCALE);
        }

        public void update(float scrollSpeed) {
            aniTick++;
            if (aniTick >= ANI_SPEED) {
                aniTick = 0;
                aniIndex = (aniIndex + 1) % FRAME_COUNT;
            }

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
            // Simple hitbox - full size for better collision detection
            return new Rectangle((int)x, (int)y, width, height);
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
            System.out.println("[BossProjectile] Created at: x=" + startX + ", y=" + startY + ", size=" + width + "x" + height);
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

        // FIXED: Simple hitbox with no offset
        public Rectangle getHitbox() {
            return new Rectangle((int)x, (int)y, width, height);
        }

        public boolean isActive() { return active; }
        public void setActive(boolean v) { active = v; }
    }
}