package entities;

import main.Game;
import utils.LoadSave;

import static utils.Constants.PlayerConstants.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import static utils.HelpMethods.canMoveHere;
public class Player extends Entity{

    private BufferedImage[][] animations;
    private int aniTick,aniIndex,aniSpeed = 25;
    private int playerAction = IDLE;
    private boolean moving = false,attacking = false;
    private boolean left, up, right, down;
    private float playerSpeed = 1.0f * Game.SCALE;
    private int[][] lvlData;
    private float xDrawOffset = 21 * Game.SCALE;
    private float yDrawOffset = 4 * Game.SCALE;

   public Player(float x , float y, int width, int height){
       super(x,y,width,height);
       loadAnimations();
       initHitbox(x, y,    (int) (70 * Game.SCALE),(int) (32 * Game.SCALE));
   }

    // draws the image
    public void render(Graphics g) {
        g.drawImage(animations[playerAction][aniIndex], (int) (hitBox.x - xDrawOffset), (int) (hitBox.y - yDrawOffset), width, height, null);
        drawHitBox(g);
    }


   public void update(){
       updatePos();
       //updateHitBox();
       updateAnimationTick();
       setAnimation();
   }

    private void setAnimation() {

       int startAnimation = playerAction;
       if(moving){
           playerAction = RUNNING;
       }else {
           playerAction = IDLE;
       }
/*
       if(attacking){
           playerAction = ATTACK_1;
       } */

       if(startAnimation != playerAction){
           resetAnimationTick();
       }
    }

    private void resetAnimationTick() {
       aniTick = 0;
       aniIndex = 0;
    }

    private void updateAnimationTick() {
       aniTick++;
       if(aniTick >= aniSpeed){
           aniTick = 0;
           aniIndex++;
           if(aniIndex >= getSpriteAmount(playerAction)){
               aniIndex = 0 ;
               attacking = false;
           }
       }
    }

    private void updatePos() {

        moving = false;

        if (!left && !right && !up && !down)
            return;

        float xSpeed = 0;
        float ySpeed = 0;

        // W / S movement
        if (up && !down)
            ySpeed = -playerSpeed;
        else if (down && !up)
            ySpeed = playerSpeed;

        // A / D movement
        if (left && !right)
            xSpeed = -playerSpeed;
        else if (right && !left)
            xSpeed = playerSpeed;

        // Check collision BEFORE moving
        if (canMoveHere(hitBox.x + xSpeed, hitBox.y, hitBox.width, hitBox.height, lvlData)) {
            hitBox.x += xSpeed;
        }

        if (canMoveHere(hitBox.x, hitBox.y + ySpeed, hitBox.width, hitBox.height, lvlData)) {
            hitBox.y += ySpeed;
        }

        if (xSpeed != 0 || ySpeed != 0)
            moving = true;
    }


    private void loadAnimations() {

        BufferedImage img = LoadSave.getSpriteAtlas(LoadSave.PLAYER_ATLAS);

        animations = new BufferedImage[2][4];
        for (int j = 0; j < animations.length; j++)
            for (int i = 0; i < animations[j].length; i++)
                animations[j][i] = img.getSubimage(i * 110, j * 40, 110 , 40);

    }

    public void loadLvlData(int[][] lvlData){
       this.lvlData = lvlData;
    }
    public void resetDirBooleans(){
        left = false;
        right = false;
        up = false;
        down = false;
    }


    public void setAttacking(boolean attacking){
       this.attacking = attacking;
    }
    public boolean isLeft() {
        return left;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public boolean isUp() {
        return up;
    }

    public void setUp(boolean up) {
        this.up = up;
    }

    public boolean isRight() {
        return right;
    }

    public void setRight(boolean right) {
        this.right = right;
    }

    public boolean isDown() {
        return down;
    }

    public void setDown(boolean down) {
        this.down = down;
    }
}
