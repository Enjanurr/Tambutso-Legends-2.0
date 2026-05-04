package objects;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;


public class StopSign {

    // -------------------------------------------------------
    // STOP SIGN SETTINGS  ← ADJUST
    // -------------------------------------------------------
    public static final float SPAWN_Y         = 186f;   // road Y position (pre-scale)
    private static final int  WIDTH_DEFAULT   = 30;
    private static final int  HEIGHT_DEFAULT  = 30;
    // -------------------------------------------------------

    private float x, y;
    private final int width, height;
    private boolean active = true;
    private BufferedImage image;

    public StopSign(float x) {
        this.x      = x;
        this.y      = SPAWN_Y * Game.SCALE;
        this.width  = (int)(WIDTH_DEFAULT  * Game.SCALE);
        this.height = (int)(HEIGHT_DEFAULT * Game.SCALE);
        loadImage();
    }

    private void loadImage() {
        image = LoadSave.getSpriteAtlas(LoadSave.STOP_SIGN);
        if (image == null)
            System.err.println("[StopSign] Could not load " + LoadSave.STOP_SIGN);
    }

    public void update(boolean worldScrolling, float scrollSpeed) {
        if (worldScrolling)
            x -= scrollSpeed;

        if (x + width < 0)
            active = false;
    }

    public void render(Graphics g) {
        if (!active || image == null) return;
        g.drawImage(image, (int) x, (int) y, width, height, null);
    }

    // ── Getters ───────────────────────────────────────────────
    public boolean isActive() { return active; }
    public float   getX()     { return x; }
    public float   getY()     { return y; }

    public Rectangle getHitbox() {
        return new Rectangle((int) x, (int) y, width, height);
    }
}