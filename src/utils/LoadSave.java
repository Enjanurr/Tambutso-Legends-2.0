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

    public static final String BOSS_DEFEAT = "boss/BossDefeat.png";
    public static final String BOSS1_ATLAS = "boss/boss1.png";       // 550×316, 4 rows × 5 cols
    public static final String BOSS1_LIFE  = "boss/boss1_life.png";  // 2700×224, 2 rows × 9 cols

    public static final String BOSS2_ATLAS = "boss/boss2.png";       // 550×316, 4 rows × 5 cols
    public static final String BOSS2_SKILL1 = "boss/boss2_skill1.png";       // 550×316, 4 rows × 5 cols
    public static final String BOSS2_SKILL2 = "boss/boss2_skill2.png";       // 550×316, 4 rows × 5 cols
    public static final String BOSS2_LIFE  = "boss/boss2_life.png";  // 3300×224, Row0=11cols, Row1=10cols
    public static final String BOSS3_ATLAS = "boss/boss3.png";       // 550×316, 4 rows × 5 cols
    public static final String BOSS3_LIFE  = "boss/boss3_life.png";  // 4147×348, Row0=13cols, Row1=12cols

    // ── Objects ───────────────────────────────────────────────
    public static final String STOP_SIGN        = "objects/stop_sign.png";
    public static final String LIFE_STATUS      = "ui/hud/life_status.png";
    public static final String DEATH_SCREEN     = "ui/overlays/death_screen.png";
    public static final String TUTORIAL_IMG     = "ui/overlays/tutorial.png";
    public static final String MISSION_MAP1_IMG = "ui/overlays/mission_map1.png";
    public static final String MISSION_MAP2_IMG = "ui/overlays/mission_map2.png";
    public static final String MISSION_MAP3_IMG = "ui/overlays/mission_map3.png";
    public static final String PROGRESS_BAR     = "ui/hud/progress_bar.png";
    public static final String PROGRESS_BAR2    = "ui/hud/progress_bar2.png";  // Level 2: 20 stops
    public static final String PROGRESS_BAR3    = "ui/hud/progress_bar3.png";  // Level 3: 25 stops



    // ── Playing state background / environment ───────────────
    public static final String PLAYING_BACKGROUND_IMG = "backgrounds/playing/playing_bg_img.png";
    public static final String BIG_CLOUDS             = "backgrounds/playing/big_clouds.png";
    public static final String SMALL_CLOUDS           = "backgrounds/playing/small_clouds.png";

    // ── Playing state background / stops ────────────────────
    public static final String BUS_STOP = "objects/bus_stop.png";

    // ── Menu atlases ─────────────────────────────────────────
    public static final String MENU_BUTTONS        = "ui/buttons/button_atlas.png";
    public static final String MENU_BACKGROUNDS    = "backgrounds/menu/menu_background.png";
    public static final String MENU_BACKGROUND_IMG = "backgrounds/menu/background_menu.png";
    // Add this line to LoadSave.java with the other constants
    public static final String STOP_HERE = "characters/npc/stop_here.png";

    // ── Status Check overlays ─────────────────────────────────
    public static final String BEFORE_BOSS_PASSED = "ui/overlays/before_boss_passed.png";
    public static final String BEFORE_BOSS_FAILED = "ui/overlays/before_boss_failed.png";

    // ── Pause UI atlases ─────────────────────────────────────
    public static final String PAUSE_BACKGROUNDS  = "ui/overlays/pause_menu.png";
    public static final String OPTIONS_BACKGROUND = "ui/overlays/options_menu.png";
    public static final String SOUND_BUTTONS      = "ui/buttons/sound_button.png";
    public static final String URM_BUTTONS        = "ui/buttons/urm_buttons.png";
    public static final String VOLUME_BUTTONS     = "ui/buttons/volume_buttons.png";
    public static final String POWERUP            = "objects/powerup.png";

    // ── Person sprite sheets ──────────────────────────────────
    public static final String PERSON1_ATLAS = "characters/npc/Person1.png";
    public static final String PERSON2_ATLAS = "characters/npc/Person2.png";
    public static final String PERSON3_ATLAS = "characters/npc/Person3.png";
    public static final String PERSON4_ATLAS = "characters/npc/Person4.png";
    public static final String PERSON5_ATLAS = "characters/npc/Person5.png";
    public static final String PERSON6_ATLAS = "characters/npc/Person6.png";

    // ── Passenger system UI ───────────────────────────────────
    public static final String ACCEPT_PASSENGER_BACKGROUND = "ui/overlays/accept_passenger.png";
    public static final String ACCEPT_PASSENGER_BUTTONS    = "ui/buttons/accept_passenger_buttons.png";
    public static final String PASSENGER_COUNTER           = "ui/hud/passenger_counter.png";

    // ── NEW: Passenger ride & drop system ─────────────────────
    /** 135 × 270 — 6 rows × 3 cols (seated + get-out animation per person ID) */
    public static final String RIDING_PASSENGER = "ui/hud/riding_passenger.png";
    /** 123 × 120 — 10 rows × 3 cols → 30 stop-number frames */
    public static final String STOP_INDICATOR   = "ui/hud/stop_indicator.png";
    /** 500 × 299 — top-down jeepney seat layout background */
    public static final String PASSENGER_LIST   = "ui/overlays/passenger_list.png";
    /** 420 × 168 — 3 rows × 3 cols (Drop row, Exit row, Open-popup row) */
    public static final String PASSENGER_BUTTON = "ui/buttons/passenger_button.png";
    /** 168 × 336 — 6 rows × 3 cols (Jeep skill buttons for boss fights) */
    public static final String JEEP_SKILL_BUTTONS = "ui/buttons/jeeps_skill_button.png";
    /** 235 × 225 — Payment overlay background */
    public static final String PAYMENT_OVERLAY = "ui/overlays/popupfare.png";
    /** 426 × 56 — Change button sprite sheet */
    public static final String CHANGE_BUTTON = "ui/buttons/change_button.png";

    // ─────────────────────────────────────────────────────────


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