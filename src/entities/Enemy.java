package entities;

import static utils.Constants.EnemyConstants.getSpriteAmountEnemy;

public abstract class Enemy extends Entity{
   private int aniIndex, enemyState, enemyType;
   private int aniTick,aniSpeed = 25;

    public Enemy(float x, float y, int width, int height, int enemyType){
        super(x,y,width,height);
        this.enemyType = enemyType;
        initHitbox(x,y,width,height);
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
            if(aniIndex >= getSpriteAmountEnemy(enemyType,enemyState)){
                aniIndex = 0 ;

                //attacking = false;
            }
        }
    }

    public void update(){
        updateAnimationTick();
    }
    public int getAniIndex(){
        return aniIndex;
    }

    public int getEnemyState(){
        return enemyState;
    }


}
