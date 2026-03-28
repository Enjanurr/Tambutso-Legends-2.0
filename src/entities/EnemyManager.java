package entities;

import gameStates.Playing;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static utils.Constants.EnemyConstants.*;

public class EnemyManager {

    private final Playing playing;
    private final Random  rng = new Random();

    private final List<EnemyCar> enemies = new ArrayList<>();

    // ── Atlas pool — mirrors PERSON_ATLASES in PersonManager ─
    private static final EnemyCar.EnemyType[] ENEMY_TYPES = EnemyCar.EnemyType.values();

    // ── Lanes — same idea as LANE_1_Y / LANE_2_Y ─────────────
    private static final int[] LANES_Y = {460, 520, 580};

    // ── Spawn settings — mirrors PersonManager constants ──────
    private static final float SPAWN_CHANCE       = 0.70f;
    private static final int   SPAWN_INTERVAL_MIN = 100;
    private static final int   SPAWN_INTERVAL_MAX = 300;
    private static final int MAX_ENEMIES = 20; // ← safety cap
    private int spawnTimer;

    public EnemyManager(Playing playing) {
        this.playing   = playing;
        spawnTimer     = nextSpawnInterval();
    }

    public void update() {
        boolean scrolling = playing.isScrolling();
        float   speed     = playing.getScrollSpeed();

        Iterator<EnemyCar> it = enemies.iterator();
        while (it.hasNext()) {
            EnemyCar e = it.next();
            e.update(scrolling, speed);
            if (!e.isActive()) it.remove();
        }

        // ── Spawn timer — only when scrolling, mirrors PASSENGER logic ──
        if (scrolling) {
            spawnTimer--;
            if (spawnTimer <= 0) {
                trySpawnEnemy();
                spawnTimer = nextSpawnInterval();
            }
        }
    }

    private void trySpawnEnemy() {
        if (enemies.size() >= MAX_ENEMIES) return; // ← add this
        if (rng.nextFloat() >= SPAWN_CHANCE) return;

        EnemyCar.EnemyType type = ENEMY_TYPES[rng.nextInt(ENEMY_TYPES.length)];

        // Screen-space spawn — mirrors trySpawnWalker / trySpawnPassenger
        float spawnX = Game.GAME_WIDTH + type.frameW * Game.SCALE;
        float spawnY = LANES_Y[rng.nextInt(LANES_Y.length)];

        enemies.add(new EnemyCar(spawnX, spawnY, type));
    }

    private int nextSpawnInterval() {
        return SPAWN_INTERVAL_MIN + rng.nextInt(SPAWN_INTERVAL_MAX - SPAWN_INTERVAL_MIN);
    }

    public void resetAll() {
        enemies.clear();
        spawnTimer = nextSpawnInterval();
    }

    // ── Render — mirrors PersonManager layered draw ───────────
    public void render(Graphics g) {
        for (EnemyCar e : enemies)
            e.render(g);
    }
}