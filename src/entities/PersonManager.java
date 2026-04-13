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

    // ── Person atlas paths with associated type IDs ───────────
    private static final PersonAtlas[] PERSON_ATLASES = {
            new PersonAtlas(LoadSave.PERSON1_ATLAS, 1),
            new PersonAtlas(LoadSave.PERSON2_ATLAS, 2),
            new PersonAtlas(LoadSave.PERSON3_ATLAS, 3),
            new PersonAtlas(LoadSave.PERSON4_ATLAS, 4),
            new PersonAtlas(LoadSave.PERSON5_ATLAS, 5),
            new PersonAtlas(LoadSave.PERSON6_ATLAS, 6),
    };

    // Helper class to pair atlas path with person type ID
    private static class PersonAtlas {
        final String path;
        final int typeId;
        PersonAtlas(String path, int typeId) {
            this.path = path;
            this.typeId = typeId;
        }
    }

    // ── Walker lane Y positions ───────────────────────────────
    // All four lanes — top and bottom sidewalk
    private static final float[] WALKER_LANES = {
            LANE_1_Y, // top sidewalk lane 1
            LANE_2_Y, // top sidewalk lane 2
            LANE_3_Y, // bottom sidewalk lane 3
            LANE_4_Y, // bottom sidewalk lane 4
    };

    // -------------------------------------------------------
    // SPAWN SETTINGS
    // -------------------------------------------------------
    private static final float WALKER_SPAWN_CHANCE    = 0.30f; // 30% per lane per attempt
    private static final float PASSENGER_SPAWN_CHANCE = 0.30f;

    private static final int WALKER_INTERVAL_MIN    = 100; // ticks between spawn attempts
    private static final int WALKER_INTERVAL_MAX    = 400;
    private static final int PASSENGER_INTERVAL_MIN = 150;
    private static final int PASSENGER_INTERVAL_MAX = 500;

    private static final int MAX_WORLD_LOOPS = 15; // stop passengers on final loop
    // -------------------------------------------------------

    private int walkerTimer;
    private int passengerTimer;

    public PersonManager(Playing playing) {
        this.playing = playing;
        walkerTimer = nextWalkerInterval();
        passengerTimer = nextPassengerInterval();
    }

    public List<Person> getPersons() {
        return persons;
    }

    public void update() {
        boolean scrolling = playing.isScrolling();
        float speed = playing.getScrollSpeed();

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
            if (rng.nextFloat() < WALKER_SPAWN_CHANCE) {
                PersonAtlas atlas = randomPersonAtlas();
                persons.add(new Person(spawnX, laneY * Game.SCALE,
                        Person.PersonType.WALKER, atlas.path, atlas.typeId));
            }
        }
    }

    // ── Passenger spawn ───────────────────────────────────────
    private void trySpawnPassenger() {
        if (playing.getWorldLoopCount() >= MAX_WORLD_LOOPS - 1) return;
        if (rng.nextFloat() >= PASSENGER_SPAWN_CHANCE) return;

        float spawnX = Game.GAME_WIDTH + PERSON_WIDTH;
        float spawnY = PASSENGER_Y * Game.SCALE;

        PersonAtlas atlas = randomPersonAtlas();
        persons.add(new Person(spawnX, spawnY,
                Person.PersonType.PASSENGER, atlas.path, atlas.typeId));
    }

    // ── Helpers ──────────────────────────────────────────────
    private int nextWalkerInterval() {
        return WALKER_INTERVAL_MIN + rng.nextInt(WALKER_INTERVAL_MAX - WALKER_INTERVAL_MIN);
    }

    private int nextPassengerInterval() {
        return PASSENGER_INTERVAL_MIN + rng.nextInt(PASSENGER_INTERVAL_MAX - PASSENGER_INTERVAL_MIN);
    }

    private PersonAtlas randomPersonAtlas() {
        return PERSON_ATLASES[rng.nextInt(PERSON_ATLASES.length)];
    }

    public void resetAll() {
        persons.clear();
        walkerTimer = nextWalkerInterval();
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