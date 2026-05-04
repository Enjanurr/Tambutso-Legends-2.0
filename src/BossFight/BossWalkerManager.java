package BossFight;

import entities.Person;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static utils.Constants.PersonConstants.*;

/**
 * Manages walkers during the boss fight.
 *
 * Differences from normal PersonManager:
 *  - No passenger spawning (only walkers).
 *  - World always scrolls (no right-movement fallback).
 *  - Receives a constant scroll speed from BossFightState instead of querying Playing.
 *  - Spawns on all 4 sidewalk lanes (top + bottom).
 */
public class BossWalkerManager {

    private final Random rng = new Random();

    private final List<Person> persons = new ArrayList<>();

    private static final String[] PERSON_ATLASES = {
            LoadSave.PERSON1_ATLAS,
            LoadSave.PERSON2_ATLAS,
    };

    // ── All four walker lanes ─────────────────────────────────
    private static final float[] WALKER_LANES = {
            LANE_1_Y,  // top sidewalk lane 1
            LANE_2_Y,  // top sidewalk lane 2
            LANE_3_Y,  // bottom sidewalk lane 3
            LANE_4_Y,  // bottom sidewalk lane 4
    };

    // -------------------------------------------------------
    // SPAWN SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float WALKER_SPAWN_CHANCE = 0.30f; // 30% per lane per attempt
    private static final int   WALKER_INTERVAL_MIN = 100;
    private static final int   WALKER_INTERVAL_MAX = 400;
    // -------------------------------------------------------

    private int walkerTimer;

    public BossWalkerManager() {
        walkerTimer = nextInterval();
    }

    /**
     * @param scrollSpeed constant boss fight world scroll speed (pixels/tick, pre-scale)
     */
    public void update(float scrollSpeed) {
        // Boss fight world always scrolls — walkers always move left
        float scaledSpeed = scrollSpeed * Game.SCALE;

        Iterator<Person> it = persons.iterator();
        while (it.hasNext()) {
            Person p = it.next();
            // Force scrolling=true so walker always moves left
            p.update(true, scaledSpeed);
            if (!p.isActive()) it.remove();
        }

        walkerTimer--;
        if (walkerTimer <= 0) {
            trySpawnWalkers();
            walkerTimer = nextInterval();
        }
    }

    private void trySpawnWalkers() {
        float spawnX = Game.GAME_WIDTH + PERSON_WIDTH;
        for (float laneY : WALKER_LANES) {
            if (rng.nextFloat() < WALKER_SPAWN_CHANCE)
                persons.add(new Person(spawnX, laneY * Game.SCALE,
                        Person.PersonType.WALKER, randomAtlas()));
        }
    }

    private int    nextInterval() { return WALKER_INTERVAL_MIN + rng.nextInt(WALKER_INTERVAL_MAX - WALKER_INTERVAL_MIN); }
    private String randomAtlas()  { return PERSON_ATLASES[rng.nextInt(PERSON_ATLASES.length)]; }

    public void resetAll() {
        persons.clear();
        walkerTimer = nextInterval();
    }

    // ── Render — same layered draw order as PersonManager ────
    public void render(Graphics g) {
        List<Person> snapshot = new ArrayList<>(persons);

        // Top Lane 1
        for (Person p : snapshot)
            if (p.getY() < LANE_2_Y * Game.SCALE)
                p.render(g);

        // Top Lane 2
        for (Person p : snapshot)
            if (p.getY() >= LANE_2_Y * Game.SCALE && p.getY() < LANE_3_Y * Game.SCALE)
                p.render(g);

        // Bottom Lane 3
        for (Person p : snapshot)
            if (p.getY() >= LANE_3_Y * Game.SCALE && p.getY() < LANE_4_Y * Game.SCALE)
                p.render(g);

        // Bottom Lane 4
        for (Person p : snapshot)
            if (p.getY() >= LANE_4_Y * Game.SCALE)
                p.render(g);
    }
}