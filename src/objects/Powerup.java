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

        PowerupType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static PowerupType fromValue(int value) {
            for (PowerupType type : PowerupType.values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return SPEED_BOOST;
        }
    }

    private float x, y;
    private final float width, height;
    private boolean active = true;
    private PowerupType type;
    private BufferedImage image;

    // Lane Y positions (same as passengers and walkers)
    public static final float LANE_1_Y = 200f;
    public static final float LANE_2_Y = 240f;
    public static final float LANE_3_Y = 280f;

    public Powerup(float x, float y, PowerupType type) {
        this.x = x;
        this.y = y;
        this.width = 44 * Game.SCALE;
        this.height = 44 * Game.SCALE;
        this.type = type;
        loadImage();
    }

    private void loadImage() {
        image = LoadSave.getSpriteAtlas("powerup.png");
    }

    public void update(boolean worldScrolling, float scrollSpeed) {
        // Powerup is stationary on ground, gets left behind when jeep moves forward
        if (worldScrolling) {
            x -= scrollSpeed;
        }

        // Remove if passed left border
        if (x + width < 0) {
            active = false;
        }
    }

    public void render(Graphics g) {
        if (!active || image == null) return;
        g.drawImage(image, (int)x, (int)y, (int)width, (int)height, null);
    }

    public Rectangle getHitbox() {
        return new Rectangle((int)x, (int)y, (int)width, (int)height);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public PowerupType getType() {
        return type;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}