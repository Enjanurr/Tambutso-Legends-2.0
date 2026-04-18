package utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class LoadSave {

    // ── Core atlases ─────────────────────────────────────────
   //  public static final String PLAYER_ATLAS = "/characters/player/red_jeep.png";
    public static final String PLAYER_ATLAS_1 = "characters/player/red_jeep.png";
    public static final String PLAYER_ATLAS_2 = "characters/player/green_jeep.png";
    public static final String PLAYER_ATLAS_3 = "characters/player/blue_jeep.png";
    public static final String CHAR_SELECT_BG = "backgrounds/menu/background_menu.png";
    public static final String CHAR_BG = "backgrounds/menu/menu_background.png";
    public static final String LEVEL_ATLAS  = "level/road_tiles.png";
    public static final String ROAD_DATA    = "level/road_data_new.png";

    // ── Enemy atlases ─────────────────────────────────────────
    public static final String GSM_ATLAS   = "characters/enemies/gsm-taxi.png";
    public static final String EJEEP_ATLAS = "characters/enemies/modern_jeep.png";

    // ── Boss atlases ──────────────────────────────────────────
    public static final String BOSS1_ATLAS = "boss/boss1.png";       // 550×316, 4 rows × 5 cols
    public static final String BOSS1_LIFE  = "boss/boss1_life.png";  // 2700×224, 2 rows × 9 cols
    public static final String BOSS_DEFEAT = "boss/BossDefeat.png";  // 434×323, single image

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
    public static final String BUS_STOP = "buildings/bus_stop.png";
    public static final String MAP1_KEPCO = "buildings/map1/kepco.png";
    public static final String MAP1_GAISANO = "buildings/map1/gaisano.png";
    public static final String MAP1_MARKETPLACE = "buildings/map1/marketplace.png";
    public static final String MAP1_UC = "buildings/map1/uc.png";
    public static final String MAP1_WILCON = "buildings/map1/wilcon.png";
    public static final String MAP2_CITU = "buildings/map2/citu.png";
    public static final String MAP2_EMALL = "buildings/map2/emall.png";
    public static final String MAP2_SHOPWISE = "buildings/map2/shopwise.png";
    public static final String MAP2_STARMALL = "buildings/map2/starmall.png";
    public static final String MAP2_USJR = "buildings/map2/usjr.png";
    public static final String MAP3_CATHEDRAL = "buildings/map3/cathedral.png";
    public static final String MAP3_CITYHALL = "buildings/map3/cityhall.png";
    public static final String MAP3_SMCITY = "buildings/map3/smcity.png";
    public static final String MAP3_AYALA_TERRACES = "buildings/map3/ayalaterraces.png";
    public static final String MAP3_AYALA_CENTRAL = "buildings/map3/ayalacentral.png";

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
        try (is) {
            try {
                if (is == null) {
                    System.err.println("[LoadSave] File not found on classpath: /" + fileName);
                    return null;
                }
                img = ImageIO.read(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return img;
    }

    public static int[][] GetLevelData() {
        BufferedImage img = getSpriteAtlas(ROAD_DATA);
        assert img != null;
        int[][] lvlData = new int[img.getHeight()][img.getWidth()];
        for (int j = 0; j < img.getHeight(); j++)
            for (int i = 0; i < img.getWidth(); i++) {
                Color color = new Color(img.getRGB(i, j));
                lvlData[j][i] = color.getRed();
            }
        return lvlData;
    }
}
