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


    private float currentXSpeed = 0f;
    private float currentDecel  = 0f;

    private static final float X_ACCEL       = 0.005f;
    private static final float X_MAX_SPEED   = 1.5f;
    private static final float X_DECEL_START = 0.001f;
    private static final float X_DECEL_GROW  = 0.003f;
    private static final float X_DECEL_MAX   = 0.15f;
    // -------------------------------------------------------

    // UP/DOWN SPEED
    private static final float Y_SPEED = 1f;

    public static final int STOP = Game.GAME_WIDTH;

    // Set by Playing every tick — true = world is scrolling, freeze jeep X.
    private boolean worldScrolling = false;

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

    public void update() {
        updatePos();
        updateAnimationTick();
        setAnimation();
    }

    private void setAnimation() {
        int startAnimation = playerAction;
        if (moving)  playerAction = RUNNING;
        else         playerAction = IDLE;
        if (startAnimation != playerAction)
            resetAnimationTick();
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

    private void updatePos() {
        if (gamePanel.isFading()) return;

        moving = false;

        // ── HORIZONTAL (A / D) ───────────────────────────────
        if (!worldScrolling) {

            if (right && !left) {
                currentXSpeed = Math.min(currentXSpeed + X_ACCEL, X_MAX_SPEED);
                currentDecel  = X_DECEL_START;
            } else if (left && !right) {
                currentXSpeed = Math.max(currentXSpeed - X_ACCEL, -X_MAX_SPEED);
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

            if (currentXSpeed != 0) {
                float nextX = hitBox.x + currentXSpeed;

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

        } else {

            currentXSpeed = 0;
            currentDecel  = X_DECEL_START;
        }

        // ── VERTICAL (W / S) — always allowed ───────────────
        float ySpeed = 0;
        if (up && !down)      ySpeed = -Y_SPEED;
        else if (down && !up) ySpeed = Y_SPEED;

        if (ySpeed != 0 && canMoveHere(hitBox.x, hitBox.y + ySpeed, hitBox.width, hitBox.height, lvlData))
            hitBox.y += ySpeed;

        // Mark moving for animation purposes
        if ((!worldScrolling && currentXSpeed != 0) || ySpeed != 0)
            moving = true;
        if (worldScrolling && right)
            moving = true;
    }

    private void loadAnimations() {
        InputStream is = getClass().getResourceAsStream(LoadSave.PLAYER_ATLAS);
        try {
            BufferedImage img = ImageIO.read(is);
            animations = new BufferedImage[2][4];
            for (int j = 0; j < animations.length; j++)
                for (int i = 0; i < animations[j].length; i++)
                    animations[j][i] = img.getSubimage(i * 110, j * 40, 110, 40);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { is.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public void loadLvlData(int[][] lvlData) { this.lvlData = lvlData; }


    public void setWorldScrolling(boolean scrolling) { this.worldScrolling = scrolling; }

    public void resetDirBooleans() {
        left = false; right = false; up = false; down = false;
        currentXSpeed  = 0;
        currentDecel   = X_DECEL_START;
        worldScrolling = false;
    }

    public void setAttacking(boolean attacking) { this.attacking = attacking; }
    public boolean isLeft()  { return left; }
    public void setLeft(boolean left)     { this.left = left; }
    public boolean isUp()    { return up; }
    public void setUp(boolean up)         { this.up = up; }
    public boolean isRight() { return right; }
    public void setRight(boolean right)   { this.right = right; }
    public boolean isDown()  { return down; }
    public void setDown(boolean down)     { this.down = down; }
}