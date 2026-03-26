package entities;

import static utils.Constants.EnemyConstants.*;

public class EnemyJeep extends Enemy {

    public EnemyJeep(float x, float y) {
        super(x, y, ENEMY_WIDTH, ENEMY_HEIGHT, ENEMY_JEEP);
    }
}