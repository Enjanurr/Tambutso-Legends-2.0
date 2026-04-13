package objects;

import entities.WorldObject;
import main.Game;
import utils.Constants;
import utils.LoadSave;
import utils.RouteMap;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldObjectManager {
    public record LandmarkDebugEntry(String label, float x, int y, int width, int height,
                                     float scale, float xOffset, int anchorY) {}

    // Decorative roadside objects can appear immediately at run start and can
    // also spawn from the right edge when a stop-count milestone is reached.
    private static final float SPAWN_X = Game.GAME_WIDTH;
    private static final float INITIAL_BUS_STOP_X = 250f;
    private static final int BUS_STOP_Y = 175;
    private static final int CLEANUP_INTERVAL_FRAMES = 60;
    private static final int CLEANUP_THRESHOLD = 8;

    private final List<WorldObject> worldObjects = new ArrayList<>();
    private final Map<RouteMap, Map<Integer, StopSpawnDefinition>> stopSpawnDefinitions = new EnumMap<>(RouteMap.class);
    private final BufferedImage busStopImage;
    private RouteMap currentMap;
    private int cleanupCounter = 0;

    public WorldObjectManager(RouteMap initialMap) {
        currentMap = initialMap;
        busStopImage = LoadSave.getSpriteAtlas(LoadSave.BUS_STOP);
        loadStopSpawnDefinitions();
        spawnInitialObjects();
    }

    private void loadStopSpawnDefinitions() {
        for (RouteMap map : RouteMap.values()) {
            stopSpawnDefinitions.put(map, new HashMap<>());
        }

        registerBuilding(RouteMap.MAP_1, 1, true, LoadSave.MAP1_KEPCO, Constants.Landmarks.MAP1_KEPCO);
        registerBuilding(RouteMap.MAP_1, 3, true, LoadSave.MAP1_MARKETPLACE, Constants.Landmarks.MAP1_MARKETPLACE);
        registerBuilding(RouteMap.MAP_1, 5, true, LoadSave.MAP1_GAISANO, Constants.Landmarks.MAP1_GAISANO);

        registerBuilding(RouteMap.MAP_2, 1, true, LoadSave.MAP2_CITU, Constants.Landmarks.MAP2_CITU);
        registerBuilding(RouteMap.MAP_2, 2, true, LoadSave.MAP2_USJR, Constants.Landmarks.MAP2_USJR);
        registerBuilding(RouteMap.MAP_2, 3, true, LoadSave.MAP2_EMALL, Constants.Landmarks.MAP2_EMALL);
        registerBuilding(RouteMap.MAP_2, 4, true, LoadSave.MAP2_SHOPWISE, Constants.Landmarks.MAP2_SHOPWISE);
        registerBuilding(RouteMap.MAP_2, 5, true, LoadSave.MAP2_STARMALL, Constants.Landmarks.MAP2_STARMALL);

        registerBuilding(RouteMap.MAP_3, 2, true, LoadSave.MAP3_CATHEDRAL, Constants.Landmarks.MAP3_CATHEDRAL);
        registerBuilding(RouteMap.MAP_3, 4, true, LoadSave.MAP3_SMCITY, Constants.Landmarks.MAP3_SMCITY);
    }

    public void setCurrentMap(RouteMap map) {
        currentMap = map;
        reset();
    }

    public RouteMap getCurrentMap() {
        return currentMap;
    }

    public void onStopSignSpawned(int stopIndex) {
        StopSpawnDefinition definition = getDefinition(currentMap, stopIndex);

        if (definition == null || !definition.overrideBusStop()) {
            spawnBusStop(SPAWN_X);
        }

        if (definition != null) {
            worldObjects.addAll(definition.spawnBuildings());
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

    public List<LandmarkDebugEntry> getActiveLandmarkDebugEntries() {
        List<LandmarkDebugEntry> entries = new ArrayList<>();
        for (WorldObject obj : worldObjects) {
            if (!obj.hasDebugInfo() || obj.isRemovable()) {
                continue;
            }

            WorldObject.DebugInfo info = obj.getDebugInfo();
            entries.add(new LandmarkDebugEntry(
                    info.label(),
                    obj.getX(),
                    obj.getDrawY(),
                    obj.getWidth(),
                    obj.getHeight(),
                    info.scale(),
                    info.xOffset(),
                    info.anchorY()
            ));
        }
        return entries;
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
        if (busStopImage != null) {
            spawnBusStop(INITIAL_BUS_STOP_X);
        }
    }

    private void spawnBusStop(float x) {
        worldObjects.add(new WorldObject(
                x,
                BUS_STOP_Y,
                Constants.Environment.BUS_STOP_WIDTH,
                Constants.Environment.BUS_STOP_HEIGHT,
                busStopImage
        ));
    }

    private StopSpawnDefinition getDefinition(RouteMap map, int stopIndex) {
        Map<Integer, StopSpawnDefinition> byStop = stopSpawnDefinitions.get(map);
        if (byStop == null) {
            return null;
        }

        return byStop.get(stopIndex);
    }

    private void registerBuilding(RouteMap map, int stopIndex, boolean overrideBusStop,
                                  String imagePath, Constants.Landmarks.LandmarkTuning tuning) {
        BufferedImage image = LoadSave.getSpriteAtlas(imagePath);
        if (image == null) {
            return;
        }

        int width = Math.max(1, Math.round(image.getWidth() * Game.SCALE * tuning.scale()));
        int height = Math.max(1, Math.round(image.getHeight() * Game.SCALE * tuning.scale()));
        BuildingSpawn buildingSpawn = new BuildingSpawn(imagePath, image, tuning.y(), width, height,
                tuning.scale(), tuning.xOffset());

        Map<Integer, StopSpawnDefinition> byStop = stopSpawnDefinitions.get(map);
        StopSpawnDefinition existing = byStop.get(stopIndex);
        if (existing == null) {
            List<BuildingSpawn> buildings = new ArrayList<>();
            buildings.add(buildingSpawn);
            byStop.put(stopIndex, new StopSpawnDefinition(overrideBusStop, buildings));
            return;
        }

        existing.buildings().add(buildingSpawn);
        if (overrideBusStop) {
            existing.setOverrideBusStop(true);
        }
    }

    private static final class StopSpawnDefinition {
        private boolean overrideBusStop;
        private final List<BuildingSpawn> buildings;

        private StopSpawnDefinition(boolean overrideBusStop, List<BuildingSpawn> buildings) {
            this.overrideBusStop = overrideBusStop;
            this.buildings = buildings;
        }

        private boolean overrideBusStop() {
            return overrideBusStop;
        }

        private void setOverrideBusStop(boolean overrideBusStop) {
            this.overrideBusStop = overrideBusStop;
        }

        private List<BuildingSpawn> buildings() {
            return buildings;
        }

        private List<WorldObject> spawnBuildings() {
            List<WorldObject> spawned = new ArrayList<>();
            for (BuildingSpawn building : buildings) {
                spawned.add(building.spawn());
            }
            return spawned;
        }
    }

    private record BuildingSpawn(String label, BufferedImage image, int y, int width, int height,
                                 float scale, float xOffset) {
        private WorldObject spawn() {
            return new WorldObject(
                    SPAWN_X + xOffset,
                    y,
                    width,
                    height,
                    image,
                    new WorldObject.DebugInfo(label, scale, y, xOffset),
                    true
            );
        }
    }
}
