package levels;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Manages level progression and configuration.
 * Handles loading level data and switching between levels.
 */
public class LevelManager {

    private Game game;
    private BufferedImage[] levelSprite;
    private Level levelOne;

    private int currentLevelId = 1;
    private LevelConfig currentLevelConfig;

    public LevelManager(Game game) {
        this.game = game;
        this.currentLevelConfig = LevelConfig.getLevel(1);
        importOutsideSprites();
        levelOne = new Level(LoadSave.GetLevelData());
    }

    private void importOutsideSprites() {
        BufferedImage img = LoadSave.getSpriteAtlas(LoadSave.LEVEL_ATLAS);
        levelSprite = new BufferedImage[10];
        for (int j = 0; j < 2; j++)
            for (int i = 0; i < 5; i++) {
                int index = j * 5 + i;
                levelSprite[index] = img.getSubimage(i * 32, j * 32, 32, 32);
            }
    }

    /**
     * Load a specific level by ID
     */
    public void loadLevel(int levelId) {
        this.currentLevelId = levelId;
        this.currentLevelConfig = LevelConfig.getLevel(levelId);
        System.out.println("[LevelManager] Loaded Level " + levelId +
                " - Loops: " + currentLevelConfig.maxWorldLoops +
                " - Passengers Required: " + currentLevelConfig.requiredPassengers +
                " - Fare Required: " + currentLevelConfig.requiredFare);
    }

    /**
     * Advance to the next level
     * @return true if advanced, false if already at max level
     */
    public boolean advanceToNextLevel() {
        if (currentLevelId < LevelConfig.ALL_LEVELS.length) {
            loadLevel(currentLevelId + 1);
            return true;
        }
        System.out.println("[LevelManager] Already at max level!");
        return false;
    }

    /**
     * Reset current level progress
     */
    public void resetLevel() {
        loadLevel(currentLevelId);
    }

    public void draw(Graphics g, int lvlOffset) {
        int levelCols  = levelOne.getLevelData()[0].length;
        int levelPixelW = levelCols * Game.TILES_SIZE;

        for (int row = 0; row < Game.TILES_IN_HEIGHT; row++) {
            for (int col = 0; col < levelCols; col++) {
                int index  = levelOne.getSpriteIndex(row, col);
                int drawX  = Game.TILES_SIZE * col - lvlOffset;

                g.drawImage(levelSprite[index], drawX, Game.TILES_SIZE * row,
                        Game.TILES_SIZE, Game.TILES_SIZE, null);

                g.drawImage(levelSprite[index], drawX + levelPixelW, Game.TILES_SIZE * row,
                        Game.TILES_SIZE, Game.TILES_SIZE, null);
            }
        }
    }

    public void update() {}

    public Level getCurrentLevel() { return levelOne; }

    public int getCurrentLevelId() { return currentLevelId; }

    public LevelConfig getCurrentLevelConfig() { return currentLevelConfig; }

    /**
     * Get max world loops for current level
     */
    public int getMaxWorldLoops() {
        return currentLevelConfig.maxWorldLoops;
    }

    /**
     * Get required passengers for current level
     */
    public int getRequiredPassengers() {
        return currentLevelConfig.requiredPassengers;
    }

    /**
     * Get required fare for current level
     */
    public int getRequiredFare() {
        return currentLevelConfig.requiredFare;
    }

    /**
     * Get stop name for current level
     */
    public String getStopName(int stopNumber) {
        return currentLevelConfig.getStopName(stopNumber);
    }
}
