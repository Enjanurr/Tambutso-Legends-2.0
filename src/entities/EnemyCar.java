package entities;

import utils.LoadSave;
import main.Game;
import java.awt.*;
import java.awt.image.BufferedImage;

import static utils.Constants.EnemyConstants.*;

public class EnemyCar extends Enemy {

    public enum EnemyType {
        TAXI (LoadSave.GSM_ATLAS,        90, 31, 4, 0.5f),
        JEEP (LoadSave.EJEEP_ATLAS, 110, 40, 4, 0.5f);


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

    private final EnemyType      type;
    private       BufferedImage[][] frames;

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
        loadFrames(type.atlas, type.frameW, type.frameH, type.frameCount);
    }

    private void loadFrames(String atlasPath, int frameW, int frameH, int frameCount) {
        BufferedImage sheet = LoadSave.getSpriteAtlas(atlasPath);

        // ── Safety check — catch wrong frame dimensions early ────
        if (sheet.getWidth() < frameW * frameCount || sheet.getHeight() < frameH) {
            System.err.println("Atlas too small for " + atlasPath +
                    " — sheet: " + sheet.getWidth() + "x" + sheet.getHeight() +
                    " needed: " + (frameW * frameCount) + "x" + frameH);
            active = false; // disable this car so it doesn't crash render
            return;
        }

        frames = new BufferedImage[1][frameCount];
        for (int i = 0; i < frameCount; i++)
            frames[0][i] = sheet.getSubimage(i * frameW, 0, frameW, frameH);
    }

    /**
     * @param worldScrolling true when the jeepney is moving (D held + centered)
     * @param scrollSpeed    world scroll speed in pixels/tick
     */
    public void update(boolean worldScrolling, float scrollSpeed) {

        // ── Direction: LEFT when jeep moving, RIGHT when stopped ──
        movingLeft = worldScrolling;

        if (movingLeft) {
            x -= type.speed * Game.SCALE;
            x -= scrollSpeed;
        } else {
            x += type.speed * Game.SCALE;
        }

        // ── Cull ─────────────────────────────────────────────
        if (movingLeft  && x + width < 0)            active = false;
        if (!movingLeft && x         > Game.GAME_WIDTH) active = false;
        if (x < -2000 || x > Game.GAME_WIDTH + 2000)         active = false; // ← absolute safety net
        // ── Animation ────────────────────────────────────────
        aniTick++;
        if (aniTick >= ENEMY_ANI_SPEED) {
            aniTick  = 0;
            aniIndex = (aniIndex + 1) % type.frameCount;
        }
    }

    public void render(Graphics g) {
        if (!active) return;
        g.drawImage(frames[0][aniIndex], (int) x, (int) y, width, height, null);
    }

    public boolean    isActive()     { return active; }
    public EnemyType  getType()      { return type; }
    public boolean    isMovingLeft() { return movingLeft; }
    public int        getAniIndex()  { return aniIndex; }
}