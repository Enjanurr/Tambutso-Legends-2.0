package entities;

import main.Game;
import main.GamePanel;
import utils.LoadSave;

import static utils.Constants.PlayerConstants.*;
import static utils.HelpMethods.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class Player extends Entity {

    private BufferedImage[][] animations;
    private int aniTick, aniIndex, aniSpeed = 25;
    private int playerAction = IDLE;
    private boolean moving = false, attacking = false;
    private boolean left, up, right, down;

    // ── Horizontal speed / decel ──────────────────────────────
    private float currentXSpeed = 0f;
    private float currentDecel  = 0f;

    private static final float X_ACCEL       = 0.005f;
    private static final float X_MAX_SPEED   = 1.5f;
    private static final float X_DECEL_START = 0.001f;
    private static final float X_DECEL_GROW  = 0.001f;  // slower buildup
    private static final float X_DECEL_MAX   = 0.01f;   // gentler cap

    // ── Vertical speed ────────────────────────────────────────
    private static final float Y_SPEED = 0.8f;

    public static final int STOP = Game.GAME_WIDTH;

    // Set by Playing / BossFightState every tick
    private boolean worldScrolling = false;
    private boolean worldLoopDone  = false;

    private float currentMaxSpeed = X_MAX_SPEED;

    // ── Car Struck state ─────────────────────────────────────
    // -------------------------------------------------------
    // STRUCK DURATION  ← ADJUST (seconds × UPS)
    // -------------------------------------------------------
    private static final int STRUCK_DURATION_TICKS = 1 * 200; // 1 s at 200 UPS
    // -------------------------------------------------------
    private boolean struckActive = false;
    private int     struckTimer  = 0;
    // Ghost mode: true while struck — disables collision detection
    private boolean ghost        = false;

    // ── Boss fight mode ───────────────────────────────────────
    // When true: running animation is always on, hit does NOT lock movement.
    private boolean bossMode = false;

    // ── Boss running animation speed ← ADJUST ─────────────────
    // -------------------------------------------------------
    // Multiplied by scroll speed to match world pace.
    // Higher = slower animation frames per unit of scroll speed.
    // -------------------------------------------------------
    private static final float BOSS_ANI_SPEED_DIVISOR = 0.8f;
    // -------------------------------------------------------

    private GamePanel gamePanel;
    private int[][] lvlData;
    private float xDrawOffset = 21 * Game.SCALE;
    private float yDrawOffset = 4  * Game.SCALE;

    // ── Boss fight hitbox offsets ← ADJUST ────────────────────
    // -------------------------------------------------------
    // These are used ONLY in boss mode. Normal hitbox is set in constructor.
    private static final float BOSS_HB_X_OFFSET = 4  * Game.SCALE;
    private static final float BOSS_HB_Y_OFFSET = 2  * Game.SCALE;
    private static final float BOSS_HB_W        = 54 * Game.SCALE; // hitbox width
    private static final float BOSS_HB_H        = 28 * Game.SCALE; // hitbox height
    // -------------------------------------------------------
    private java.util.List<String> activeAbilities = java.util.Collections.emptyList();

    public Player(float x, float y, int width, int height, GamePanel gamePanel) {
        super(x, y, width, height);
        this.gamePanel = gamePanel;
       // loadAnimations();
        initHitbox(x, y,
                54 * Game.SCALE,
                32 * Game.SCALE);
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (animations == null) return; // Guard: driver not applied yet
        g.drawImage(
                animations[playerAction][aniIndex],
                (int)(hitBox.x - xDrawOffset),
                (int)(hitBox.y - yDrawOffset),
                width, height, null
        );
        //drawHitBox(g);
    }

    public void render(Graphics g, int xLvlOffset) {
        if (animations == null) return; // Guard: driver not applied yet
        g.drawImage(
                animations[playerAction][aniIndex],
                (int)(hitBox.x - xDrawOffset) - xLvlOffset,
                (int)(hitBox.y - yDrawOffset),
                width, height, null
        );
        drawHitBox(g, xLvlOffset);
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update() {
        updateStruckState();
        updatePos();            // Fix 5: movement always runs, even during struck
        updateAnimationTick();
        setAnimation();
    }

    // ── Car-Struck ────────────────────────────────────────────
    /**
     * Triggers the car-struck animation and ghost mode.
     * Fix 5: in boss mode, does NOT lock W/A/S/D keys — player can still move.
     */
    public void triggerCarStruck() {
        struckActive = true;
        struckTimer  = STRUCK_DURATION_TICKS;
        ghost        = true;
        if (!bossMode) {
            // Normal mode: halt the jeep and clear key flags
            currentXSpeed = 0f;
            currentDecel  = X_DECEL_START;
            left = right = up = down = false;
        }
        // Boss mode: animation plays but movement is NOT locked
    }

    private void updateStruckState() {
        if (!struckActive) return;
        struckTimer--;
        if (struckTimer <= 0) {
            struckActive = false;
            ghost        = false;
        }
    }

    // ── Boss mode toggle ──────────────────────────────────────
    /**
     * Called by BossFightState when entering/exiting boss fight.
     * Switches hitbox to boss dimensions and enables always-running animation.
     */
    public void setBossMode(boolean enabled) {
        bossMode = enabled;
        if (enabled) {
            // Resize hitbox for boss fight
            hitBox.width  = BOSS_HB_W;
            hitBox.height = BOSS_HB_H;
        } else {
            // Restore normal hitbox
            hitBox.width  = 54 * Game.SCALE;
            hitBox.height = 32 * Game.SCALE;
        }
    }

    // ─────────────────────────────────────────────────────────
    // ANIMATION
    // ─────────────────────────────────────────────────────────
    private void setAnimation() {
        int prev = playerAction;

        if (struckActive) {
            playerAction = CAR_STRUCK;
        } else if (bossMode) {
            playerAction = RUNNING;

        } else if (moving || currentXSpeed > 0) {
            playerAction = RUNNING;

        } else {
            playerAction = IDLE;

        }

        if (prev != playerAction) resetAnimationTick();
    }

    private void resetAnimationTick() {
        aniTick  = 0;
        aniIndex = 0;
    }

    private void updateAnimationTick() {
        aniTick++;
        if (aniTick >= aniSpeed) {
            aniTick = 0;
            aniIndex++;
            if (aniIndex >= getSpriteAmount(playerAction)) {
                aniIndex  = 0;
                attacking = false;
            }
        }
    }
    // Add this field to Player class:


    // Update applyDriver():
    public void applyDriver(entities.DriverProfile profile) {
        this.currentMaxSpeed  = profile.maxSpeed;
        this.activeAbilities  = profile.abilities;   // store for future ability logic
        loadAnimationsFrom(profile.atlasPath);
        System.out.println("Driver applied: " + profile.displayName
                + " | Speed: " + profile.maxSpeed
                + " | Abilities: " + profile.abilities);
    }

    // Getter for BossFightState or powerup system to query abilities:
    public java.util.List<String> getActiveAbilities() { return activeAbilities; }

    private void loadAnimationsFrom(String atlasPath) {
        System.out.println("▼▼▼ LOADING SPRITE ▼▼▼");
        System.out.println("Atlas Path: " + atlasPath);

        // ✨ Add leading slash if missing (match LoadSave.getSpriteAtlas behavior)
        if (!atlasPath.startsWith("/")) {
            atlasPath = "/" + atlasPath;
        }

        System.out.println("Normalized Path: " + atlasPath);

        java.io.InputStream is = getClass().getResourceAsStream(atlasPath);

        if (is == null) {
            System.out.println("❌ STREAM IS NULL - FILE NOT FOUND!");
            System.out.println("▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲");
            return;
        }

        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(is);

            System.out.println("✓ Image loaded: " + img.getWidth() + "x" + img.getHeight());

            animations = new java.awt.image.BufferedImage[3][4];
            for (int row = 0; row < animations.length; row++) {
                for (int col = 0; col < animations[row].length; col++) {
                    animations[row][col] = img.getSubimage(col * 110, row * 40, 110, 40);
                }
            }

            System.out.println("✓ Animations array populated");
            System.out.println("▲▲▲ SPRITE LOADED ▲▲▲");

        } catch (Exception e) {
            System.out.println("❌ EXCEPTION DURING LOAD:");
            e.printStackTrace();
            System.out.println("▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲");
        } finally {
            try { if (is != null) is.close(); } catch (Exception ignored) {}
        }
    }
    /**
     * Fix 2: sets animation speed based on world scroll speed during boss fight.
     * Called each tick by BossFightState.
     * @param scrollSpeed current world scroll speed (pixels/tick)
     */
    public void updateBossAniSpeed(float scrollSpeed) {
        if (!bossMode) return;
        // Higher scroll speed → lower aniSpeed number → faster animation
        int computed = (scrollSpeed > 0)
                ? Math.max(4, (int)(BOSS_ANI_SPEED_DIVISOR / scrollSpeed))
                : 25;
        aniSpeed = computed;
    }

    // ─────────────────────────────────────────────────────────
    // MOVEMENT
    // ─────────────────────────────────────────────────────────
    private void updatePos() {
        if (gamePanel.isFading()) return;

        moving = false;

        // ── HORIZONTAL ───────────────────────────────────────
        if (right && !left) {
            currentXSpeed = Math.min(currentXSpeed + X_ACCEL, currentMaxSpeed);
            currentDecel  = X_DECEL_START;
        } else if (left && !right && !worldScrolling) {
            currentXSpeed = Math.max(currentXSpeed - X_ACCEL, -currentMaxSpeed);
            currentDecel  = X_DECEL_START;
        } else {
            currentDecel = Math.min(currentDecel + X_DECEL_GROW, X_DECEL_MAX);
            if (currentXSpeed > 0) {
                currentXSpeed -= currentDecel;
                if (currentXSpeed < 0) currentXSpeed = 0;
            } else if (currentXSpeed < 0) {
                currentXSpeed += currentDecel;
                if (currentXSpeed > 0) currentXSpeed = 0;
            } else {
                currentDecel = X_DECEL_START;
            }
        }

        if (!worldScrolling) {
            if (currentXSpeed != 0) {
                float nextX = hitBox.x + currentXSpeed;

                if (!worldLoopDone && currentXSpeed > 0) {
                    float centerClamp = Game.GAME_WIDTH / 2f - hitBox.width / 2f;
                    if (nextX >= centerClamp) {
                        hitBox.x      = centerClamp;
                        currentXSpeed = 0;
                        currentDecel  = X_DECEL_START;
                        return;
                    }
                }

                if (isAtRightBorder(nextX, hitBox.width)) {
                    hitBox.x = Game.GAME_WIDTH - hitBox.width;
                    gamePanel.triggerScreenFade(() -> {
                        hitBox.x = 0;
                        gamePanel.getGame().onJeepLooped();
                    });
                } else if (nextX < 0) {
                    hitBox.x      = 0;
                    currentXSpeed = 0;
                    currentDecel  = X_DECEL_START;
                } else {
                    hitBox.x = nextX;
                }
            }
        }

        // ── VERTICAL ─────────────────────────────────────────
        float ySpeed = 0;
        if (up && !down)      ySpeed = -Y_SPEED;
        else if (down && !up) ySpeed = Y_SPEED;

        if (ySpeed != 0 && canMoveHere(hitBox.x, hitBox.y + ySpeed, hitBox.width, hitBox.height, lvlData))
            hitBox.y += ySpeed;

        if ((!worldScrolling && currentXSpeed != 0) || ySpeed != 0)
            moving = true;
        if (worldScrolling && right)
            moving = true;
    }
    // ─────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────
    public void loadLvlData(int[][] lvlData)         { this.lvlData = lvlData; }
    public void setWorldScrolling(boolean scrolling) { this.worldScrolling = scrolling; }
    public void setWorldLoopDone(boolean done)        { this.worldLoopDone  = done; }

    public void resetDirBooleans() {
        left = false; right = false; up = false; down = false;
        currentXSpeed  = 0;
        currentDecel   = X_DECEL_START;
        worldScrolling = false;
    }

    public boolean isGhost()          { return ghost; }
    public boolean isStruckActive()   { return struckActive; }
    public boolean isBossMode()       { return bossMode; }
    public float   getCurrentXSpeed() { return currentXSpeed; }
    public void    setMaxSpeed(float maxSpeed) { this.currentMaxSpeed = maxSpeed; }

    public void setAttacking(boolean attacking) { this.attacking = attacking; }
    public void setLeft(boolean left)           { this.left = left; }
    public void setUp(boolean up)               { this.up = up; }
    public void setRight(boolean right)         { this.right = right; }
    public void setDown(boolean down)           { this.down = down; }
}
