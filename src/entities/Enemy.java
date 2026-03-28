package entities;

import static utils.Constants.EnemyConstants.getSpriteAmountEnemy;
import static utils.HelpMethods.canMoveHere;

public abstract class Enemy extends Entity {

    private int aniIndex, aniTick = 0, aniSpeed = 25;
    protected float carSpeed = 2.0f;
    private final int enemyType;

    public Enemy(float x, float y, int width, int height, int enemyType) {
        super(x, y, width, height);
        this.enemyType = enemyType;

        initHitbox(x, y, width, height);
    }

    private void updateAnimation() {
        aniTick++;
        if (aniTick >= aniSpeed) {
            aniTick = 0;
            aniIndex++;
            if (aniIndex >= getSpriteAmountEnemy(enemyType, 0)) aniIndex = 0;
        }
    }

    public void update(int[][] lvlData) {
        updateAnimation();
        updateMove(lvlData);
    }

    public void updateMove(int[][] lvlData) {
        float xSpeed = -carSpeed;
        if (canMoveHere(hitBox.x + xSpeed, hitBox.y, hitBox.width, hitBox.height, lvlData)) {
            hitBox.x += xSpeed;
            x += xSpeed;
        }
    }

    public int getAniIndex() { return aniIndex; }
}