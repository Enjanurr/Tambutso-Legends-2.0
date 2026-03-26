package entities;

import gameStates.Playing;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static utils.Constants.PersonConstants.*;

public class PersonManager {

    private final Playing playing;
    private final Random  rng = new Random();

    private final List<Person> persons = new ArrayList<>();

    // ── Atlas pool ───────────────────────────────────────────
    private static final String[] PERSON_ATLASES = {
            LoadSave.PERSON1_ATLAS,
            LoadSave.PERSON2_ATLAS,
    };
    // WALKER & PASSENGER SPAWNER
    // -------------------------------------------------------
    // SPAWN SETTINGS
    // -------------------------------------------------------
    private static final float WALKER_SPAWN_CHANCE    = 0.60f; // ← ADJUST: 60% chance per attempt
    private static final float PASSENGER_SPAWN_CHANCE = 0.30f; // ← ADJUST: 30% chance per attempt

    // At 200 UPS: 100 ticks = 0.5s, 400 ticks = 2s
    private static final int WALKER_INTERVAL_MIN    = 100; // ← ADJUST: min ticks between walker spawn attempts
    private static final int WALKER_INTERVAL_MAX    = 400; // ← ADJUST: max ticks between walker spawn attempts
    private static final int PASSENGER_INTERVAL_MIN = 150; // ← ADJUST: min ticks between passenger spawn attempts
    private static final int PASSENGER_INTERVAL_MAX = 500; // ← ADJUST: max ticks between passenger spawn attempts
    // -------------------------------------------------------

    private int walkerTimer;
    private int passengerTimer;

    public PersonManager(Playing playing) {
        this.playing       = playing;
        walkerTimer        = nextWalkerInterval();
        passengerTimer     = nextPassengerInterval();
    }


    public void update() {
        boolean scrolling = playing.isScrolling();
        float   speed     = playing.getScrollSpeed();

        Iterator<Person> it = persons.iterator();
        while (it.hasNext()) {
            Person p = it.next();
            p.update(scrolling, speed);
            if (!p.isActive()) it.remove();
        }

        // ── Walker timer — runs always (scrolling OR stopped) ─
        walkerTimer--;
        if (walkerTimer <= 0) {
            trySpawnWalker();
            walkerTimer = nextWalkerInterval();
        }

        // ── Passenger timer — only spawn when world is scrolling
        if (scrolling) {
            passengerTimer--;
            if (passengerTimer <= 0) {
                trySpawnPassenger();
                passengerTimer = nextPassengerInterval();
            }
        }
    }

    // ── Walker spawn — always from right border ───────────────
    private void trySpawnWalker() {
        if (rng.nextFloat() >= WALKER_SPAWN_CHANCE) return;

        // Always spawn just off the RIGHT border regardless of scroll state
        float spawnX = Game.GAME_WIDTH + PERSON_WIDTH;
        float spawnY = (rng.nextBoolean() ? LANE_1_Y : LANE_2_Y) * Game.SCALE;

        persons.add(new Person(spawnX, spawnY, Person.PersonType.WALKER, randomAtlas()));
    }

    // ── Passenger spawn — only when scrolling ────────────────
    private void trySpawnPassenger() {
        if (rng.nextFloat() >= PASSENGER_SPAWN_CHANCE) return;

        float spawnX = Game.GAME_WIDTH + PERSON_WIDTH;
        float spawnY = PASSENGER_Y * Game.SCALE;

        persons.add(new Person(spawnX, spawnY, Person.PersonType.PASSENGER, randomAtlas()));
    }

    // ── Helpers ──────────────────────────────────────────────
    private int    nextWalkerInterval()    { return WALKER_INTERVAL_MIN + rng.nextInt(WALKER_INTERVAL_MAX - WALKER_INTERVAL_MIN); }
    private int    nextPassengerInterval() { return PASSENGER_INTERVAL_MIN + rng.nextInt(PASSENGER_INTERVAL_MAX - PASSENGER_INTERVAL_MIN); }
    private String randomAtlas()           { return PERSON_ATLASES[rng.nextInt(PERSON_ATLASES.length)]; }

    public void resetAll() {
        persons.clear();
        walkerTimer    = nextWalkerInterval();
        passengerTimer = nextPassengerInterval();
    }

    // ── Render — layered draw order ──────────────────────────
    public void render(Graphics g) {
        // Layer 1 — Lane 1 walkers (back)
        for (Person p : persons)
            if (p.getType() == Person.PersonType.WALKER
                    && p.getY() < LANE_2_Y * Game.SCALE)
                p.render(g);

        // Layer 2 — Lane 2 walkers (middle)
        for (Person p : persons)
            if (p.getType() == Person.PersonType.WALKER
                    && p.getY() >= LANE_2_Y * Game.SCALE)
                p.render(g);

        // Layer 3 — Passengers (front)
        for (Person p : persons)
            if (p.getType() == Person.PersonType.PASSENGER)
                p.render(g);
    }
}