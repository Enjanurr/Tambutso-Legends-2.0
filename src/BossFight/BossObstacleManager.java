package BossFight;

import entities.EnemyCar;
import main.Game;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BossObstacleManager {

    private final Random rng = new Random();
    private final List<EnemyCar> obstacles = new ArrayList<>();

    // Spawn settings (adjust as needed)
    private static final float SPAWN_CHANCE = 0.35f;
    private static final int SPAWN_INTERVAL_MIN = 100;
    private static final int SPAWN_INTERVAL_MAX = 250;
    private static final int MAX_OBSTACLES = 2;

    // Lane Y positions matching your road
    private static final int[] LANES_Y = { 460, 520, 580 };

    private int spawnTimer;
    private static final int BULLET_DAMAGE = 1;

    public BossObstacleManager() {
        spawnTimer = nextSpawnInterval();
    }

    public void update(boolean scrolling, float scrollSpeed) {
        // Update existing obstacles
        Iterator<EnemyCar> it = obstacles.iterator();
        while (it.hasNext()) {
            EnemyCar obstacle = it.next();
            obstacle.update(scrolling, scrollSpeed);
            if (!obstacle.isActive()) {
                it.remove();
                System.out.println("[BossObstacleManager] Obstacle removed (destroyed or off-screen)");
            }
        }

        // Spawn new obstacles only when scrolling
        if (scrolling) {
            spawnTimer--;
            if (spawnTimer <= 0) {
                trySpawnObstacle();
                spawnTimer = nextSpawnInterval();
            }
        }
    }

    private void trySpawnObstacle() {
        if (obstacles.size() >= MAX_OBSTACLES) return;
        if (rng.nextFloat() >= SPAWN_CHANCE) return;

        EnemyCar.EnemyType type = EnemyCar.EnemyType.values()[rng.nextInt(EnemyCar.EnemyType.values().length)];
        float spawnX = Game.GAME_WIDTH + (type.frameW * Game.SCALE * type.scale);
        float spawnY = LANES_Y[rng.nextInt(LANES_Y.length)];

        EnemyCar newObstacle = new EnemyCar(spawnX, spawnY, type);
        newObstacle.setShowHealthBar(true);  // ← ENABLE HEALTH BAR FOR BOSS FIGHTS
        obstacles.add(newObstacle);
        System.out.println("[BossObstacleManager] Spawned: " + type.name() + " (Health: " + type.maxHealth + ")");
    }

    private int nextSpawnInterval() {
        return SPAWN_INTERVAL_MIN + rng.nextInt(SPAWN_INTERVAL_MAX - SPAWN_INTERVAL_MIN);
    }

    public void checkCollision(Rectangle playerHitbox, Runnable onHit) {
        for (EnemyCar obstacle : obstacles) {
            if (obstacle.isActive() && obstacle.getHitBox().intersects(playerHitbox)) {
                obstacle.setActive(false);
                onHit.run();
                System.out.println("[BossObstacleManager] Player collided with obstacle!");
                break;
            }
        }
    }

    public void checkBulletCollision(Rectangle bulletHitbox, Runnable onBulletHit) {
        for (EnemyCar obstacle : obstacles) {
            if (obstacle.isActive() && obstacle.getHitBox().intersects(bulletHitbox)) {
                boolean destroyed = obstacle.takeDamage(BULLET_DAMAGE);
                onBulletHit.run();  // This should set bullet to inactive

                if (destroyed) {
                    System.out.println("[BossObstacleManager] Obstacle destroyed! Remaining obstacles: " + (obstacles.size() - 1));
                } else {
                    System.out.println("[BossObstacleManager] Obstacle hit! Remaining health: " + obstacle.getCurrentHealth() + "/" + obstacle.getMaxHealth());
                }
                break;  // One bullet hits only one obstacle
            }
        }
    }

    public void render(Graphics g) {
        for (EnemyCar obstacle : obstacles) {
            obstacle.render(g);
        }
    }

    public void reset() {
        obstacles.clear();
        spawnTimer = nextSpawnInterval();
        System.out.println("[BossObstacleManager] Reset - all obstacles cleared");
    }

    public List<EnemyCar> getActiveObstacles() {
        return obstacles;
    }
}