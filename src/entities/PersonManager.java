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

    private static final String[] PERSON_ATLASES = {
            LoadSave.PERSON1_ATLAS,
            LoadSave.PERSON2_ATLAS,
    };

    // ── Walker lane Y positions ───────────────────────────────
    // All four lanes — top and bottom sidewalk
    private static final float[] WALKER_LANES = {
            LANE_1_Y, // top sidewalk lane 1
            LANE_2_Y, // top sidewalk lane 2
            LANE_3_Y, // bottom sidewalk lane 3  ← NEW
            LANE_4_Y, // bottom sidewalk lane 4  ← NEW
    };

    // -------------------------------------------------------
    // SPAWN SETTINGS
    // -------------------------------------------------------
    private static final float WALKER_SPAWN_CHANCE    = 0.30f; // 30% per lane per attempt
    private static final float PASSENGER_SPAWN_CHANCE = 0.30f;

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

    private static final int WALKER_INTERVAL_MIN    = 100; // ticks between spawn attempts
    private static final int WALKER_INTERVAL_MAX    = 400;
    private static final int PASSENGER_INTERVAL_MIN = 150;
    private static final int PASSENGER_INTERVAL_MAX = 500;

    private static final int MAX_WORLD_LOOPS = 15; // stop passengers on final loop
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

        public java.util.List<Person> getPersons() {
            return persons;
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
            trySpawnWalkers();
            walkerTimer = nextWalkerInterval();
        }

        // ── Passenger timer — only when scrolling ─────────────
        if (scrolling) {
            passengerTimer--;
            if (passengerTimer <= 0) {
                trySpawnPassenger();
                passengerTimer = nextPassengerInterval();
            }
        }
    }

        // ── Walker spawn — each lane rolls independently──────────
        private void trySpawnWalkers() {
            float spawnX = Game.GAME_WIDTH + PERSON_WIDTH;
            for (float laneY : WALKER_LANES) {
            if (rng.nextFloat() < WALKER_SPAWN_CHANCE)

            persons.add(new Person(spawnX, laneY * Game.SCALE, Person.PersonType.WALKER, randomAtlas()));
        }}

        // ── Passenger spawn ───────────────────────────────────────
        private void trySpawnPassenger() {
        if (playing.getWorldLoopCount() >= MAX_WORLD_LOOPS - 1) return;
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
        List<Person> snapshot = new ArrayList<>(persons);

        // Layer 1 — top Lane 1 walkers (furthest back)
        for (Person p : snapshot)
            if (p.getType() == Person.PersonType.WALKER
                    && p.getY() < LANE_2_Y * Game.SCALE)
                p.render(g);

        // Layer 2 — top Lane 2 walkers
        for (Person p : snapshot)
            if (p.getType() == Person.PersonType.WALKER
                    && p.getY() >= LANE_2_Y * Game.SCALE
                    && p.getY() <  LANE_3_Y * Game.SCALE)
                p.render(g);

        // Layer 3 — passengers
        for (Person p : snapshot)
            if (p.getType() == Person.PersonType.PASSENGER)
                p.render(g);

        // Layer 4 — bottom Lane 3 walkers
        for (Person p : snapshot)
            if (p.getType() == Person.PersonType.WALKER
                    && p.getY() >= LANE_3_Y * Game.SCALE
                    && p.getY() <  LANE_4_Y * Game.SCALE)
                p.render(g);

        // Layer 5 — bottom Lane 4 walkers (front)
        for (Person p : snapshot)
            if (p.getType() == Person.PersonType.WALKER
                    && p.getY() >= LANE_4_Y * Game.SCALE)
                p.render(g);
    }
}