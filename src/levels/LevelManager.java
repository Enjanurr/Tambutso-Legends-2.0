package levels;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

public class LevelManager {

    private Game game;
    private BufferedImage[] levelSprite;
    private Level levelOne;

    public LevelManager(Game game) {
        this.game = game;
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
}