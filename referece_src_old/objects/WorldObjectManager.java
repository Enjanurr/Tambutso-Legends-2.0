package objects;

import entities.WorldObject;
import main.Game;
import utils.Constants;
import utils.LoadSave;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class WorldObjectManager {
    // Decorative roadside objects can appear immediately at run start and can
    // also spawn from the right edge when a stop-count milestone is reached.
    private static final float SPAWN_X = Game.GAME_WIDTH;
    private static final float INITIAL_BUS_STOP_X = 250f;
    private static final int BUS_STOP_Y = 215;
    private static final int CLEANUP_INTERVAL_FRAMES = 60;
    private static final int CLEANUP_THRESHOLD = 8;

    private final List<WorldObject> worldObjects = new ArrayList<>();
    private final List<SpawnRule> spawnRules = new ArrayList<>();
    private int cleanupCounter = 0;

    public WorldObjectManager() {
        loadSpawnRules();
        spawnInitialObjects();
    }

    private void loadSpawnRules() {
        BufferedImage busStop = LoadSave.getSpriteAtlas(LoadSave.BUS_STOP);
        if (busStop != null) {
            // Bus stops appear at run start and again every time a stop sign spawns.
            spawnRules.add(new SpawnRule(
                    busStop,
                    INITIAL_BUS_STOP_X,
                    BUS_STOP_Y,
                    Constants.Environment.BUS_STOP_WIDTH,
                    Constants.Environment.BUS_STOP_HEIGHT,
                    true,
                    1,
                    1
            ));
        }

        // Register future decorative buildings here with their own stop-count cadence.
        // Example: spawn at run start only:
        // spawnRules.add(new SpawnRule(buildingImage, 50f, 140, width, height, true, 0, 0));
        // Example: first at stop 3, then every 3 stops after that:
        // spawnRules.add(new SpawnRule(buildingImage, 50f, 140, width, height, false, 3, 3));
    }

    public void onStopSignSpawned(int totalStopsSpawned) {
        // One stop-sign spawn can fan out into multiple decorative spawns.
        for (SpawnRule rule : spawnRules) {
            if (rule.matches(totalStopsSpawned)) {
                worldObjects.add(rule.spawn());
            }
        }
    }

    public void update(boolean worldScrolling, float scrollSpeed) {
        for (WorldObject obj : worldObjects) {
            obj.update(worldScrolling, scrollSpeed);
        }

        cleanupWorldObjects();
    }

    public void draw(Graphics g) {
        // Draw from a snapshot so list cleanup cannot interfere with rendering.
        List<WorldObject> snapshot = new ArrayList<>(worldObjects);
        for (WorldObject obj : snapshot) {
            obj.draw(g);
        }
    }

    public void reset() {
        // Restarting the run clears active roadside props, then restores any props
        // that should already be present at the beginning of a run.
        cleanupCounter = 0;
        worldObjects.clear();
        spawnInitialObjects();
    }

    private void cleanupWorldObjects() {
        // Large lists are cleaned immediately; smaller lists are cleaned in intervals
        // to avoid spending extra time every frame.
        if (worldObjects.size() > CLEANUP_THRESHOLD) {
            removeDisposableWorldObjects();
            return;
        }

        cleanupCounter++;
        if (cleanupCounter >= CLEANUP_INTERVAL_FRAMES) {
            cleanupCounter = 0;
            removeDisposableWorldObjects();
        }
    }

    private void removeDisposableWorldObjects() {
        worldObjects.removeIf(obj -> {
            if (!obj.isRemovable()) {
                return false;
            }

            obj.dispose();
            obj.markRemoved();
            return true;
        });
    }

    private void spawnInitialObjects() {
        // Startup props are restored on construction and after a run reset.
        for (SpawnRule rule : spawnRules) {
            if (rule.spawnAtStart()) {
                worldObjects.add(rule.spawnInitial());
            }
        }
    }

    private record SpawnRule(BufferedImage image, float initialX, int y, int width, int height,
                             boolean spawnAtStart,
                             int firstStopCount,
                             int repeatInterval) {

        private boolean matches(int totalStopsSpawned) {
                if (totalStopsSpawned < firstStopCount) {
                    return false;
                }

                if (repeatInterval <= 0) {
                    // Non-repeating landmarks spawn only once at their first threshold.
                    return totalStopsSpawned == firstStopCount;
                }

                return (totalStopsSpawned - firstStopCount) % repeatInterval == 0;
            }

            private WorldObject spawn() {
                return new WorldObject(SPAWN_X, y, width, height, image);
            }

            private WorldObject spawnInitial() {
                return new WorldObject(initialX, y, width, height, image);
            }
        }
}
