package BossFight.render;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static utils.Constants.Environment.BUILDING_BASE_Y;

public class BuildingRenderer {

    // Building data structure (matching WorldObjectManager's BuildingSpawn)
    private static class BuildingData {
        final String path;
        final int originalWidth;
        final int originalHeight;
        final float scale;
        final int yOffset;
        BufferedImage image;

        BuildingData(String path, int origWidth, int origHeight, float scale, int yOffset) {
            this.path = path;
            this.originalWidth = origWidth;
            this.originalHeight = origHeight;
            this.scale = scale;
            this.yOffset = yOffset;
            loadImage();
        }

        private void loadImage() {
            image = LoadSave.getSpriteAtlas(path);
            if (image == null) {
                System.err.println("[BuildingRenderer] Failed to load: " + path);
            }
        }

        int getWidth() {
            return (int)(originalWidth * scale * Game.SCALE);
        }

        int getHeight() {
            return (int)(originalHeight * scale * Game.SCALE);
        }

        int getAnchorY() {
            return BUILDING_BASE_Y + yOffset;
        }
    }

    // Individual building instance
    private static class Building {
        BufferedImage image;
        float x, y;
        int width, height;
        int typeIndex;  // Track which building type this is

        Building(BufferedImage image, float x, float y, int width, int height, int typeIndex) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.typeIndex = typeIndex;
        }

        void update(boolean worldScrolling, float scrollSpeed) {
            if (worldScrolling) {
                x -= scrollSpeed;
            }
        }

        void draw(Graphics g) {
            if (image != null) {
                g.drawImage(image, (int)x, (int)y, width, height, null);
            }
        }

        boolean isOffScreen() {
            return x + width < 0;
        }
    }

    private final List<Building> buildings = new ArrayList<>();
    private final List<Rectangle> blockedZones = new ArrayList<>();
    private final Random random = new Random();

    // Spawn settings - BUILDINGS TOUCHING (NO SPACE)
    private static final int FIXED_GAP = 0;
    private static final float SPAWN_CHANCE = 1.0f;

    // Track the last building type to prevent duplicates side by side
    private int lastBuildingType = -1;

    // Building types
    private final BuildingData[] buildingTypes = {
            new BuildingData("backgrounds/buildings/house.png", 444, 676, 0.2f, 20),
            new BuildingData("backgrounds/buildings/generic.png", 320, 540, 0.25f, 15),
            new BuildingData("backgrounds/buildings/cinema.png", 888, 612, 0.15f, 25),
            new BuildingData("backgrounds/buildings/cafe.png", 652, 716, 0.18f, 10)
    };

    public BuildingRenderer() {
        init();
    }

    private void init() {
        buildings.clear();
        lastBuildingType = -1;  // Reset last building type
        float currentX = 0;

        // Fill screen with initial buildings
        while (currentX < Game.GAME_WIDTH + 500) {
            float nextX = addRandomBuilding(currentX);
            currentX = nextX > currentX ? nextX : currentX + 100;
        }
    }

    private float addRandomBuilding(float x) {
        if (random.nextFloat() > SPAWN_CHANCE) return x + 100;

        // Choose a random building type that is NOT the same as the last one
        int typeIndex;
        do {
            typeIndex = random.nextInt(buildingTypes.length);
        } while (typeIndex == lastBuildingType && buildingTypes.length > 1);

        lastBuildingType = typeIndex;
        BuildingData data = buildingTypes[typeIndex];

        if (data.image == null) return x + 100;

        int width = data.getWidth();
        int height = data.getHeight();
        int anchorY = data.getAnchorY();
        float spawnX = resolveSpawnX(x, width);
        float y = anchorY - height;

        buildings.add(new Building(data.image, spawnX, y, width, height, typeIndex));
        return spawnX + width + FIXED_GAP;
    }

    private float resolveSpawnX(float x, int width) {
        float candidateX = x;
        boolean adjusted;

        do {
            adjusted = false;
            float candidateRight = candidateX + width;

            for (Rectangle zone : blockedZones) {
                float zoneLeft = zone.x;
                float zoneRight = zone.x + zone.width;
                if (candidateRight <= zoneLeft || candidateX >= zoneRight) {
                    continue;
                }

                candidateX = zoneRight + FIXED_GAP;
                adjusted = true;
                break;
            }
        } while (adjusted);

        return candidateX;
    }

    public void update(boolean worldScrolling, float scrollSpeed) {
        // Update all buildings
        for (Building b : buildings) {
            b.update(worldScrolling, scrollSpeed);
        }

        // Remove off-screen buildings
        buildings.removeIf(Building::isOffScreen);

        // Add new buildings to the right
        if (!buildings.isEmpty()) {
            Building last = buildings.get(buildings.size() - 1);
            float rightEdge = last.x + last.width;

            while (rightEdge < Game.GAME_WIDTH + 300) {
                float nextRightEdge = addRandomBuilding(rightEdge + FIXED_GAP);
                if (nextRightEdge <= rightEdge) {
                    break;
                }
                rightEdge = nextRightEdge;
            }
        } else {
            init();
        }
    }

    public void setBlockedZones(List<Rectangle> zones) {
        blockedZones.clear();
        if (zones == null || zones.isEmpty()) {
            return;
        }

        for (Rectangle zone : zones) {
            if (zone == null || zone.width <= 0) {
                continue;
            }
            blockedZones.add(new Rectangle(zone));
        }

        blockedZones.sort(Comparator.comparingInt(zone -> zone.x));
        removeBlockedBuildings();
    }

    private void removeBlockedBuildings() {
        buildings.removeIf(building -> {
            Rectangle bounds = new Rectangle(
                    Math.round(building.x),
                    Math.round(building.y),
                    building.width,
                    building.height
            );

            for (Rectangle zone : blockedZones) {
                if (bounds.intersects(zone)) {
                    return true;
                }
            }

            return false;
        });
    }

    public void render(Graphics g) {
        // Create a snapshot copy to avoid ConcurrentModificationException
        List<Building> snapshot = new ArrayList<>(buildings);
        for (Building b : snapshot) {
            b.draw(g);
        }
    }

    public void reset() {
        init();
    }
}








