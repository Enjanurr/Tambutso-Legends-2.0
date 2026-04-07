package entities;

import gameStates.Playing;
import main.Game;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static utils.Constants.EnemyConstants.*;

/**
 * Spawns and manages road enemies (EnemyCar).
 * Rules:
 *  - Only 1 enemy at a time.
 *  - 20% spawn chance per cycle, only while world is scrolling.
 *  - Collision triggers car-struck animation AND deducts one health bar.
 */
public class EnemyManager {

    private final Playing playing;
    private final Random  rng = new Random();

    private final List<EnemyCar> enemies = new ArrayList<>();

    // ── Spawn settings ────────────────────────────────────────
    private static final float SPAWN_CHANCE       = 0.50f;
    private static final int   SPAWN_INTERVAL_MIN = 150;
    private static final int   SPAWN_INTERVAL_MAX = 400;
    private static final int   MAX_ENEMIES        = 1;

    // -------------------------------------------------------
    // LANE SETTINGS  ← ADJUST to match your road tiles
    // -------------------------------------------------------
    private static final int[] LANES_Y = { 460, 540, 620 };
    // -------------------------------------------------------

    private int spawnTimer;

    public EnemyManager(Playing playing) {
        this.playing = playing;
        spawnTimer   = nextSpawnInterval();
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update() {
        boolean scrolling = playing.isScrolling();
        float   speed     = playing.getScrollSpeed();
        Player  player    = playing.getPlayer();

        boolean playerWasHit = false;

        Iterator<EnemyCar> it = enemies.iterator();
        while (it.hasNext()) {
            EnemyCar e = it.next();
            e.update(scrolling, speed);

            // Collision — only when jeepney is NOT in ghost mode
            if (e.isActive() && !player.isGhost() && checkCollision(e, player)) {
                e.setActive(false);
                player.triggerCarStruck();  // starts struck animation + ghost mode
                playerWasHit = true;
            }

            if (!e.isActive()) it.remove();
        }


        if (playerWasHit) playing.onPlayerHit();

        // Spawn timer — only while scrolling
        if (scrolling) {
            spawnTimer--;
            if (spawnTimer <= 0) {
                trySpawnEnemy();
                spawnTimer = nextSpawnInterval();
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // SPAWN
    // ─────────────────────────────────────────────────────────
    private void trySpawnEnemy() {
        if (enemies.size() >= MAX_ENEMIES)   return;
        if (rng.nextFloat() >= SPAWN_CHANCE) return;

        EnemyCar.EnemyType type =
                EnemyCar.EnemyType.values()[rng.nextInt(EnemyCar.EnemyType.values().length)];

        float spawnX = Game.GAME_WIDTH + type.frameW * Game.SCALE;
        float spawnY = LANES_Y[rng.nextInt(LANES_Y.length)];

        enemies.add(new EnemyCar(spawnX, spawnY, type));
    }

    // ─────────────────────────────────────────────────────────
    // COLLISION
    // ─────────────────────────────────────────────────────────
    private boolean checkCollision(EnemyCar enemy, Player player) {
        Rectangle2D.Float eHB = enemy.getHitBox();
        Rectangle2D.Float pHB = player.getHitBox();
        if (eHB == null || pHB == null) return false;
        return eHB.intersects(pHB);
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────
    private int nextSpawnInterval() {
        return SPAWN_INTERVAL_MIN + rng.nextInt(SPAWN_INTERVAL_MAX - SPAWN_INTERVAL_MIN);
    }

    public void resetAll() {
        enemies.clear();
        spawnTimer = nextSpawnInterval();
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        List<EnemyCar> snapshot = new ArrayList<>(enemies);
        for (EnemyCar e : snapshot)
            e.render(g);
    }
}
