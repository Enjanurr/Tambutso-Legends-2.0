package levels;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;


public class LevelManager {
    private Game game;
    private BufferedImage[] levelSprite;
    private Level levelOne;
    public LevelManager(Game game){
        this.game = game;
        //levelSprite = LoadSave.getSpriteAtlas(LoadSave.LEVEL_ATLAS);
         importOutsideSprites();

        levelOne = new Level(LoadSave.GetLevelData()); // gets the data base on the rgb
    }

    private void importOutsideSprites() {
        BufferedImage img = LoadSave.getSpriteAtlas(LoadSave.LEVEL_ATLAS);
        levelSprite = new BufferedImage[10]; // 2 rows x 5 columns
        for (int j = 0; j < 2; j++)
            for (int i = 0; i < 5; i++) {
                int index = j * 5 + i;
                levelSprite[index] = img.getSubimage(i * 32, j * 32, 32, 32);
            }
    }

    public void draw(Graphics g) {
        // getSpriteIndex in LoadSave
        for (int row = 0; row < Game.TILES_IN_HEIGHT; row++)
            for (int col = 0; col < Game.TILES_IN_WIDTH; col++) {
                int index = levelOne.getSpriteIndex(row, col);
                //System.out.print(index + " "); // debug
                g.drawImage(levelSprite[index], Game.TILES_SIZE * col, Game.TILES_SIZE * row, Game.TILES_SIZE, Game.TILES_SIZE, null);
            }
        System.out.println();
    }
    public void update(){

    }
    public Level getCurrentLevel() {
        return levelOne;
    }
}
