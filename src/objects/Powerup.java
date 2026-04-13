package objects;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Powerup {

    public enum PowerupType {
        SPEED_BOOST(1),
        HEAL(2),
        DAMAGE_AMPLIFIER(3);

        private final int value;

        PowerupType(int value) { this.value = value; }

        public int getValue() { return value; }

        public static PowerupType fromValue(int value) {
            for (PowerupType type : PowerupType.values())
                if (type.value == value) return type;
            return SPEED_BOOST;
        }
    }

    // ── Sprite sheet constants ────────────────────────────────
    private static final int FRAME_COUNT  = 6;
    private static final int FRAME_W      = 44;   // 264 / 6
    private static final int FRAME_H      = 44;

    // -------------------------------------------------------
    // ANIMATION SPEED  ← ADJUST (ticks per frame at 200 UPS)
    // -------------------------------------------------------
    private static final int ANI_SPEED = 15;
    // -------------------------------------------------------

    // ── Lane Y positions ──────────────────────────────────────
    public static final float LANE_1_Y = 200f;
    public static final float LANE_2_Y = 240f;
    public static final float LANE_3_Y = 280f;

    private float x, y;
    private final float width, height;
    private boolean active = true;
    private PowerupType type;

    private BufferedImage[] frames;
    private int aniTick  = 0;
    private int aniIndex = 0;

    public Powerup(float x, float y, PowerupType type) {
        this.x      = x;
        this.y      = y;
        this.width  = FRAME_W * Game.SCALE;
        this.height = FRAME_H * Game.SCALE;
        this.type   = type;
        loadFrames();
    }

    private void loadFrames() {
        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.POWERUP);
        if (sheet == null) {
            System.err.println("[Powerup] Could not load " + LoadSave.POWERUP);
            return;
        }
        frames = new BufferedImage[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++)
            frames[i] = sheet.getSubimage(i * FRAME_W, 0, FRAME_W, FRAME_H);
    }

    public void update(boolean worldScrolling, float scrollSpeed) {
        // Move left when world scrolls
        if (worldScrolling)
            x -= scrollSpeed;

        // Cull when off left edge
        if (x + width < 0)
            active = false;

        // Animate always (regardless of scroll state)
        aniTick++;
        if (aniTick >= ANI_SPEED) {
            aniTick = 0;
            aniIndex = (aniIndex + 1) % FRAME_COUNT;
        }
    }

    public void render(Graphics g) {
        if (!active || frames == null) return;
        g.drawImage(frames[aniIndex], (int) x, (int) y, (int) width, (int) height, null);
    }

    public Rectangle getHitbox() {
        return new Rectangle((int) x, (int) y, (int) width, (int) height);
    }

    public boolean     isActive()              { return active; }
    public void        setActive(boolean v)    { active = v; }
    public PowerupType getType()               { return type; }
    public float       getX()                  { return x; }
    public float       getY()                  { return y; }
}