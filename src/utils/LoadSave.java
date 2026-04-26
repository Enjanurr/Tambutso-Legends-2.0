package utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class LoadSave {

    // ── Core atlases ─────────────────────────────────────────

    public static final String PLAYER_ATLAS_1 = "characters/player/red_jeep.png";
    public static final String PLAYER_ATLAS_2 = "characters/player/green_jeep.png";
    public static final String GREEN_JEEP_SKILL1 = "characters/player/greenjeep_skill1.png";
    public static final String GREEN_JEEP_SKILL2 = "characters/player/greenjeep_skill2.png";
    public static final String PLAYER_ATLAS_3 = "characters/player/blue_jeep.png";
    public static final String BOSS3_SKILL2_GSM = "boss/gsm-obstacle.png";
    public static final String BOSS3_SKILL2_EJEEP = "boss/modernjeep.png";
    
    public static final String LEVEL_ATLAS  = "level/road_tiles.png";
    public static final String ROAD_DATA    = "level/road_data_new.png";

    // ── Enemy atlases ─────────────────────────────────────────
    public static final String GSM_ATLAS   = "characters/enemies/gsm-taxi.png";
    public static final String EJEEP_ATLAS = "characters/enemies/modern_jeep.png";

    // ── Boss atlases ──────────────────────────────────────────
    public static final String BOSS1_ATLAS = "boss/boss1.png";       // 550×316, 4 rows × 5 cols
    public static final String BOSS1_LIFE  = "boss/boss1_life.png";  // 2700×224, 2 rows × 9 cols
    public static final String BOSS_DEFEAT = "boss/BossDefeat.png";  // 434×323, single image
    public static final String BOSS2_ATLAS = "boss/boss2.png";       // 550×316, 4 rows × 5 cols
    public static final String BOSS2_SKILL1 = "boss/boss2_skill1.png";       // 550×316, 4 rows × 5 cols
    public static final String BOSS2_SKILL2 = "boss/boss2_skill2.png";       // 550×316, 4 rows × 5 cols
    public static final String BOSS3_ATLAS = "boss/boss3.png";       // 550×316, 4 rows × 5 cols

    // ── Objects ───────────────────────────────────────────────
    public static final String STOP_SIGN        = "objects/stop_sign.png";
    public static final String LIFE_STATUS      = "ui/hud/life_status.png";
    public static final String DEATH_SCREEN     = "ui/overlays/death_screen.png";
    public static final String TUTORIAL_IMG     = "ui/overlays/tutorial.png";
    public static final String MISSION_MAP1_IMG = "ui/overlays/mission_map1.png";
    public static final String PROGRESS_BAR     = "ui/hud/progress_bar.png";

    // ── Playing state background / environment ───────────────
    public static final String PLAYING_BACKGROUND_IMG = "backgrounds/playing/playing_bg_img.png";
    public static final String BIG_CLOUDS             = "backgrounds/playing/big_clouds.png";
    public static final String SMALL_CLOUDS           = "backgrounds/playing/small_clouds.png";

    // ── Playing state background / stops ───────────────
    public static final String BUS_STOP = "objects/bus_stop.png";

    // ── Menu atlases ─────────────────────────────────────────
    public static final String MENU_BUTTONS        = "ui/buttons/button_atlas.png";
    public static final String MENU_BACKGROUNDS    = "backgrounds/menu/menu_background.png";
    public static final String MENU_BACKGROUND_IMG = "backgrounds/menu/background_menu.png";

    // ── Pause UI atlases ─────────────────────────────────────
    public static final String PAUSE_BACKGROUNDS = "ui/overlays/pause_menu.png";
    public static final String OPTIONS_BACKGROUND = "ui/overlays/options_menu.png";
    public static final String SOUND_BUTTONS     = "ui/buttons/sound_button.png";
    public static final String URM_BUTTONS       = "ui/buttons/urm_buttons.png";
    public static final String VOLUME_BUTTONS    = "ui/buttons/volume_buttons.png";
    public static final String POWERUP           = "objects/powerup.png";

    // ── Person sprite sheets ──────────────────────────────────
    public static final String PERSON1_ATLAS = "characters/npc/Person1.png";
    public static final String PERSON2_ATLAS = "characters/npc/Person2.png";

    // ─────────────────────────────────────────────────────────
    public static final String ACCEPT_PASSENGER_BACKGROUND = "ui/overlays/accept_passenger.png";
    public static final String ACCEPT_PASSENGER_BUTTONS = "ui/buttons/accept_passenger_buttons.png";
    public static final String PASSENGER_COUNTER = "ui/hud/passenger_counter.png";



    public static BufferedImage getSpriteAtlas(String fileName) {
        BufferedImage img = null;
        InputStream is = LoadSave.class.getResourceAsStream("/" + fileName);
        if (is == null) {
            System.err.println("[LoadSave] File not found on classpath: /" + fileName);
            return null;
        }
        try {
            img = ImageIO.read(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { is.close(); } catch (IOException e) { e.printStackTrace(); }
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
