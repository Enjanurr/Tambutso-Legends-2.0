package utils;

import main.Game;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class LoadSave {
    public static final String PLAYER_ATLAS = "jeepney_sprites.png";
    public static final String LEVEL_ATLAS = "road_tiles.png";
    public static final String ROAD_DATA = "road_data_new.png";
    public static final String ROAD_SAMPLE = "level_one_data.png";
    public static final String ENEMY = "jeepney_sprites.png";
    public static BufferedImage getSpriteAtlas(String fileName) {
        BufferedImage img = null;
        InputStream is = LoadSave.class.getResourceAsStream("/" + fileName);

        try {
            img = ImageIO.read(is);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return img;
    }

    public static int[][] GetLevelData() {

        BufferedImage img = getSpriteAtlas(ROAD_DATA);

        int[][] lvlData = new int[img.getHeight()][img.getWidth()];
        for (int j = 0; j < img.getHeight(); j++) {
            for (int i = 0; i < img.getWidth(); i++) {

                Color color = new Color(img.getRGB(i, j));

                int index = color.getRed();
                lvlData[j][i] = index;

               System.out.print(lvlData[j][i] + " ");
            }
            System.out.println();
        }
        return lvlData;
    }
}