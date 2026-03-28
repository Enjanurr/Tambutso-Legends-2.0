package utils;

import main.Game;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class LoadSave {

    // ── Core atlases ─────────────────────────────────────────
    public static final String PLAYER_ATLAS = "/jeepney_sprites.png";
    public static final String LEVEL_ATLAS  = "road_tiles.png";
    public static final String ROAD_DATA    = "road_data_new.png";

    // ── Enemy atlases (road cars) ─────────────────────────────
    public static final String GSM_ATLAS   = "gsm-taxi.png";       // Taxi 4 frames
    public static final String EJEEP_ATLAS = "modern_jeep.png";    // Modern Jeep  4 frames
    // -------------------------------------------------------

    // ── Playing state background / environment ───────────────
    public static final String PLAYING_BACKGROUND_IMG = "playing_bg_img.png";
    public static final String BIG_CLOUDS             = "big_clouds.png";
    public static final String SMALL_CLOUDS           = "small_clouds.png";

    // ── Menu atlases ─────────────────────────────────────────
    public static final String MENU_BUTTONS        = "button_atlas.png";
    public static final String MENU_BACKGROUNDS    = "menu_background.png";
    public static final String MENU_BACKGROUND_IMG = "background_menu.png";

    // ── Pause UI atlases ─────────────────────────────────────
    public static final String PAUSE_BACKGROUNDS = "pause_menu.png";
    public static final String SOUND_BUTTONS     = "sound_button.png";
    public static final String URM_BUTTONS       = "urm_buttons.png";
    public static final String VOLUME_BUTTONS    = "volume_buttons.png";
    public static final String POWERUP           = "powerup.png";

    // ── Person sprite sheets ──────────────────────────────────
    public static final String PERSON1_ATLAS = "Person/Person1.png";
    public static final String PERSON2_ATLAS = "Person/Person2.png";

    // ─────────────────────────────────────────────────────────

    public static BufferedImage getSpriteAtlas(String fileName) {
        BufferedImage img = null;
        InputStream is = LoadSave.class.getResourceAsStream("/" + fileName);
        try {
            img = ImageIO.read(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { if (is != null) is.close(); } catch (IOException e) { e.printStackTrace(); }
        }
        return img;
    }

    public static int[][] GetLevelData() {
        BufferedImage img = getSpriteAtlas(ROAD_DATA);
        int[][] lvlData = new int[img.getHeight()][img.getWidth()];
        for (int j = 0; j < img.getHeight(); j++)
            for (int i = 0; i < img.getWidth(); i++) {
                Color color = new Color(img.getRGB(i, j));
                lvlData[j][i] = color.getRed();
            }
        return lvlData;
    }
}