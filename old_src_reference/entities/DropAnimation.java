package entities;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

import static utils.Constants.PersonConstants.*;

/**
 * A stationary drop animation that plays at the jeepney's position when
 * a passenger is dropped off. Uses Row 2 of the passenger's sprite sheet.
 * Moves left with world scroll until it exits the left border.
 */
public class DropAnimation {

    // ── Row 2 frame counts per person type ───────────────────
    // Person1/2/3 → 10 frames; Person4/5/6 → 8 frames (spec says same as walk)
    private static int dropFrameCount(int personTypeId) {
        switch (personTypeId) {
            case 4: case 5: case 6: return 8;
            default: return 10;
        }
    }

    // Row index for drop animation
    private static final int ROW_DROP = 2;

    // ── Animation speed ← ADJUST ──────────────────────────────
    private static final int ANI_SPEED = 20; // ticks per frame

    // ── Fields ────────────────────────────────────────────────
    private float x, y;
    private final int width, height;

    private boolean active = true;

    private final BufferedImage[] frames;
    private int aniTick  = 0;
    private int aniIndex = 0;
    private final int frameCount;

    // ─────────────────────────────────────────────────────────
    public DropAnimation(float spawnX, float spawnY, String atlasPath, int personTypeId) {
        this.x      = spawnX;
        this.y      = spawnY;
        this.width  = PERSON_WIDTH;
        this.height = PERSON_HEIGHT;

        this.frameCount = dropFrameCount(personTypeId);
        int cellW       = getWidthDefaultForPerson(personTypeId);

        BufferedImage sheet = LoadSave.getSpriteAtlas(atlasPath);
        if (sheet != null && sheet.getHeight() > ROW_DROP * PERSON_HEIGHT_DEFAULT) {
            frames = new BufferedImage[frameCount];
            for (int i = 0; i < frameCount; i++)
                frames[i] = sheet.getSubimage(
                        i * cellW,
                        ROW_DROP * PERSON_HEIGHT_DEFAULT,
                        cellW,
                        PERSON_HEIGHT_DEFAULT);
        } else {
            System.err.println("[DropAnimation] Cannot load row 2 from " + atlasPath);
            frames = null;
        }
    }

    /** Call every tick. Provide current world scroll speed so the sprite drifts left. */
    public void update(boolean worldScrolling, float scrollSpeed) {
        if (!active) return;

        // Move left with world scroll (stationary relative to road)
        if (worldScrolling)
            x -= scrollSpeed;

        if (x + width < 0)
            active = false;

        // Animate
        aniTick++;
        if (aniTick >= ANI_SPEED) {
            aniTick = 0;
            aniIndex = (aniIndex + 1) % frameCount;
        }
    }

    public void render(Graphics g) {
        if (!active || frames == null) return;
        g.drawImage(frames[aniIndex], (int) x, (int) y, width, height, null);
    }

    public boolean isActive() { return active; }
}