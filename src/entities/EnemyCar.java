package entities;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

import static utils.Constants.EnemyConstants.*;


public class EnemyCar extends Enemy {

    // ── Enemy type catalogue ─────────────────────────────────
    public enum EnemyType {
        TAXI(LoadSave.GSM_ATLAS,         90,     31,     4,     0.5f),
        JEEP(LoadSave.EJEEP_ATLAS,      90,     38,     4,     0.5f);

        public final String atlas;
        public final int    frameW, frameH, frameCount;
        public final float  speed;

        EnemyType(String atlas, int frameW, int frameH, int frameCount, float speed) {
            this.atlas      = atlas;
            this.frameW     = frameW;
            this.frameH     = frameH;
            this.frameCount = frameCount;
            this.speed      = speed;
        }
    }

    private final EnemyType     type;
    private       BufferedImage[][] frames; // [0][frame]

    private int     aniTick  = 0;
    private int     aniIndex = 0;
    private boolean active     = true;
    private boolean movingLeft = true;

    public EnemyCar(float x, float y, EnemyType type) {
        super(x, y,
                (int)(type.frameW * Game.SCALE),
                (int)(type.frameH * Game.SCALE),
                0);
        this.type = type;
        loadFrames();
        // Hitbox is a slightly inset rectangle for fair collisions
        initHitbox(x + 4 * Game.SCALE, y + 2 * Game.SCALE,
                type.frameW * Game.SCALE - 20 * Game.SCALE,// narrower width
                type.frameH * Game.SCALE - 8 * Game.SCALE); // shorter height
    }

    private void loadFrames() {
        BufferedImage sheet = LoadSave.getSpriteAtlas(type.atlas);
        if (sheet == null ||
                sheet.getWidth()  < type.frameW * type.frameCount ||
                sheet.getHeight() < type.frameH) {
            System.err.println("[EnemyCar] Bad atlas for " + type.name()
                    + " (" + type.atlas + ") — disabling.");
            active = false;
            return;
        }
        frames = new BufferedImage[1][type.frameCount];
        for (int i = 0; i < type.frameCount; i++)
            frames[0][i] = sheet.getSubimage(i * type.frameW, 0, type.frameW, type.frameH);
    }


    public void update(boolean worldScrolling, float scrollSpeed) {
        movingLeft = worldScrolling;

        if (movingLeft) {
            x -= type.speed * Game.SCALE;
            x -= scrollSpeed;
        } else {
            x += type.speed * Game.SCALE;
        }

        // Sync hitbox to sprite position
        if (hitBox != null) {
            hitBox.x = x + 4 * Game.SCALE;
            hitBox.y = y + 2 * Game.SCALE;
        }


        if  (movingLeft  && x + width  < 0)               active = false;
        if  (!movingLeft && x          > Game.GAME_WIDTH)  active = false;
        if  (x < -3000   || x          > Game.GAME_WIDTH + 3000) active = false;


        aniTick++;
        if (aniTick >= ENEMY_ANI_SPEED) {
            aniTick  = 0;
            aniIndex = (aniIndex + 1) % type.frameCount;
        }
    }

    public void render(Graphics g) {
        if (!active || frames == null) return;
        g.drawImage(frames[0][aniIndex], (int) x, (int) y, width, height, null);
        // Uncomment to debug hitboxes:
         //drawHitBox(g);
    }

    // ── Getters ───────────────────────────────────────────────
    public boolean   isActive()     { return active; }
    public void      setActive(boolean v) { active = v; }

}