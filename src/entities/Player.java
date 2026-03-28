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
    private static final float X_DECEL_GROW  = 0.003f;
    private static final float X_DECEL_MAX   = 0.15f;

    // ── Vertical speed ────────────────────────────────────────
    private static final float Y_SPEED = 1f;

    public static final int STOP = Game.GAME_WIDTH;

    // Set by Playing every tick
    private boolean worldScrolling = false;
    private boolean worldLoopDone  = false;

    private float currentMaxSpeed = X_MAX_SPEED;

    // ── Car Struck state ─────────────────────────────────────

    private static final int STRUCK_DURATION_TICKS = 2 * 200; // 4 s at 200 UPS
    // -------------------------------------------------------
    private boolean struckActive = false;
    private int     struckTimer  = 0;
    // Ghost mode: true while struck — disables collision detection
    private boolean ghost        = false;

    private GamePanel gamePanel;
    private int[][] lvlData;
    private float xDrawOffset = 21 * Game.SCALE;
    private float yDrawOffset = 4  * Game.SCALE;

    public Player(float x, float y, int width, int height, GamePanel gamePanel) {
        super(x, y, width, height);
        this.gamePanel = gamePanel;
        loadAnimations();
        initHitbox(x, y, 70 * Game.SCALE, 32 * Game.SCALE);
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        g.drawImage(
                animations[playerAction][aniIndex],
                (int)(hitBox.x - xDrawOffset),
                (int)(hitBox.y - yDrawOffset),
                width, height, null
        );
        drawHitBox(g);
    }

    public void render(Graphics g, int xLvlOffset) {
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
        if (!struckActive) {
            updatePos();
        }
        updateAnimationTick();
        setAnimation();
    }


    public void triggerCarStruck() {
        struckActive  = true;
        struckTimer   = STRUCK_DURATION_TICKS;
        ghost         = true;
        // Kill speed so jeep stops instantly
        currentXSpeed = 0f;
        currentDecel  = X_DECEL_START;
        // Force key flags off so input can't resume movement mid-animation
        left = right = up = down = false;
    }

    private void updateStruckState() {
        if (!struckActive) return;
        struckTimer--;
        if (struckTimer <= 0) {
            struckActive = false;
            ghost        = false;
        }
    }

    // ─────────────────────────────────────────────────────────
    // ANIMATION
    // ─────────────────────────────────────────────────────────
    private void setAnimation() {
        int prev = playerAction;

        if (struckActive) {
            playerAction = CAR_STRUCK;
        } else if (moving) {
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
    // LOAD ANIMATIONS
    //   Row 0 = RUNNING
    //   Row 1 = IDLE
    //   Row 2 = CAR_STRUCK
    // ─────────────────────────────────────────────────────────
    private void loadAnimations() {
        InputStream is = getClass().getResourceAsStream(LoadSave.PLAYER_ATLAS);
        try {
            BufferedImage img = ImageIO.read(is);
            animations = new BufferedImage[3][4]; // 3 rows, 4 frames
            for (int row = 0; row < animations.length; row++)
                for (int col = 0; col < animations[row].length; col++)
                    animations[row][col] = img.getSubimage(col * 110, row * 40, 110, 40);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { if (is != null) is.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public void loadLvlData(int[][] lvlData) { this.lvlData = lvlData; }

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
    public float   getCurrentXSpeed() { return currentXSpeed; }

    public void setMaxSpeed(float maxSpeed)      { this.currentMaxSpeed = maxSpeed; }


    public void setAttacking(boolean attacking)  { this.attacking = attacking; }
    public boolean isLeft()  { return left; }
    public void setLeft(boolean left)            { this.left = left; }
    public boolean isUp()    { return up; }
    public void setUp(boolean up)                { this.up = up; }
    public boolean isRight() { return right; }
    public void setRight(boolean right)          { this.right = right; }
    public boolean isDown()  { return down; }
    public void setDown(boolean down)            { this.down = down; }
}