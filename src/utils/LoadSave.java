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
    //public static final String ROAD_DATA = "road_data_new.png";
    public static final String ROAD_DATA = "long_road_data.png";
    public static final String ROAD_SAMPLE = "level_one_data.png";
    public static final String MENU_BUTTONS = "button_atlas.png";
    public static final String MENU_BACKGROUNDS = "menu_background.png";
    public static final String PAUSE_BACKGROUND = "pause_menu.png";
    public static final String SOUND_BUTTONS = "sound_button.png";
    public static final String URM_BUTTONS = "urm_buttons.png";
    public static final String VOLUME_BUTTONS = "volume_buttons.png";
    public static final String MENU_BACKGROUND_IMG = "background_menu.png";
    public static final String PLAYING_BACKGROUND_IMG = "playing_bg_img.png";
    public static final String BIG_CLOUDS = "big_clouds.png";
    public static final String SMALL_CLOUDS  = "small_clouds.png";

    // Menu

    public static final String ENEMY = "jeepney_sprites.png"    ;
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