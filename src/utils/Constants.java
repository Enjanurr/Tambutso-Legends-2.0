package utils;

import main.Game;

public class Constants {

    // ── Environment (clouds / background) ───────────────────
    public static class Environment {
        public static final int BIG_CLOUD_WIDTH_DEFAULT    = 448;
        public static final int BIG_CLOUD_HEIGHT_DEFAULT   = 101;
        public static final int SMALL_CLOUD_WIDTH_DEFAULT  = 72;
        public static final int SMALL_CLOUD_HEIGHT_DEFAULT = 24;

        public static final int BUS_STOP_WIDTH_DEFAULT     = 793;
        public static final int BUS_STOP_HEIGHT_DEFAULT    = 261;
        public static final float BUS_STOP_SCALE           = 0.4f;

        public static final int BIG_CLOUD_WIDTH    = (int)(BIG_CLOUD_WIDTH_DEFAULT   * Game.SCALE);
        public static final int BIG_CLOUD_HEIGHT   = (int)(BIG_CLOUD_HEIGHT_DEFAULT  * Game.SCALE);
        public static final int SMALL_CLOUD_WIDTH  = (int)(SMALL_CLOUD_WIDTH_DEFAULT * Game.SCALE);
        public static final int SMALL_CLOUD_HEIGHT = (int)(SMALL_CLOUD_HEIGHT_DEFAULT * Game.SCALE);

        public static final int BUS_STOP_WIDTH    = (int)(BUS_STOP_WIDTH_DEFAULT  * Game.SCALE * BUS_STOP_SCALE);
        public static final int BUS_STOP_HEIGHT   = (int)(BUS_STOP_HEIGHT_DEFAULT * Game.SCALE * BUS_STOP_SCALE);
    }

    // ── Person sprites ───────────────────────────────────────
    public static class PersonConstants {
        // Base dimensions for person sprites (pre-scale)
        public static final int PERSON_WIDTH_DEFAULT  = 61;
        public static final int PERSON_HEIGHT_DEFAULT = 60;

        // Person3: 10 frames per row, 61px width
        public static final int PERSON3_FRAME_COUNT = 10;
        public static final int PERSON3_WIDTH_DEFAULT = 61;

        // Person4, Person5, Person6: 12 frames per row, 61px width (732/12 = 61)
        public static final int PERSON4_FRAME_COUNT = 12;
        public static final int PERSON4_WIDTH_DEFAULT = 61;

        // Default for backward compatibility
        public static final int PERSON_FRAME_COUNT = 10;
        public static final int PERSON_WIDTH_DEFAULT_LEGACY = 61;

        public static final int ROW_IDLE = 0;
        public static final int ROW_WALK = 1;

        public static final int PERSON_WIDTH  = (int)(PERSON_WIDTH_DEFAULT  * Game.SCALE);
        public static final int PERSON_HEIGHT = (int)(PERSON_HEIGHT_DEFAULT * Game.SCALE);

        public static final float WALK_SPEED       = 0.3333f;
        public static final int   WALKER_ANI_SPEED = 25;

        // ── Top sidewalk walker lanes ─────────────────────────
        public static final float LANE_1_Y = 128f;
        public static final float LANE_2_Y = 140f;

        // ── Bottom sidewalk walker lanes ← ADJUST ─────────────
        // -------------------------------------------------------
        public static final float LANE_3_Y = 320f; // top bottom-sidewalk lane
        public static final float LANE_4_Y = 330f; // bottom bottom-sidewalk lane
        // -------------------------------------------------------

        public static final float PASSENGER_Y         = 154f;
        public static final int   PASSENGER_ANI_SPEED = 25;

        // Helper method to get frame count by person type
        public static int getFrameCountForPerson(int personType) {
            switch(personType) {
                case 3: return PERSON3_FRAME_COUNT;
                case 4: return PERSON4_FRAME_COUNT;
                case 5: return PERSON4_FRAME_COUNT;
                case 6: return PERSON4_FRAME_COUNT;
                default: return PERSON_FRAME_COUNT;
            }
        }

        // Helper method to get default width by person type
        public static int getWidthDefaultForPerson(int personType) {
            switch(personType) {
                case 3: return PERSON3_WIDTH_DEFAULT;
                case 4: return PERSON4_WIDTH_DEFAULT;
                case 5: return PERSON4_WIDTH_DEFAULT;
                case 6: return PERSON4_WIDTH_DEFAULT;
                default: return PERSON_WIDTH_DEFAULT_LEGACY;
            }
        }
    }

    // ── Enemy sprites ────────────────────────────────────────
    public static class EnemyConstants {
        public static final int ENEMY_JEEP = 0;

        public static final int ENEMY_WIDTH_DEFAULT  = 110;
        public static final int ENEMY_HEIGHT_DEFAULT = 40;

        public static final int ENEMY_WIDTH  = (int)(ENEMY_WIDTH_DEFAULT  * Game.SCALE);
        public static final int ENEMY_HEIGHT = (int)(ENEMY_HEIGHT_DEFAULT * Game.SCALE);

        // -------------------------------------------------------
        // ANI SPEED  ← ADJUST
        // -------------------------------------------------------
        public static final int ENEMY_ANI_SPEED = 20;
        // -------------------------------------------------------

        public static int getSpriteAmountEnemy(int enemyType, int enemyState) { return 4; }
    }

    // ── UI ───────────────────────────────────────────────────
    public static class UI {
        public static class Buttons {
            public static final int B_WIDTH_DEFAULT  = 140;
            public static final int B_HEIGHT_DEFAULT = 56;
            public static final int B_WIDTH  = (int)(B_WIDTH_DEFAULT  * Game.SCALE);
            public static final int B_HEIGHT = (int)(B_HEIGHT_DEFAULT * Game.SCALE);
        }

        public static class PauseButtons {
            public static final int SOUND_SIZE_DEFAULT = 42;
            public static final int SOUND_SIZE = (int)(SOUND_SIZE_DEFAULT * Game.SCALE);
        }

        public static class URMButtons {
            public static final int URM_DEFAULT_SIZE = 56;
            public static final int URM_SIZE = (int)(URM_DEFAULT_SIZE * Game.SCALE);
        }

        public static class VolumeButtons {
            public static final int VOLUME_DEFAULT_WIDTH  = 28;
            public static final int VOLUME_DEFAULT_HEIGHT = 44;
            public static final int SLIDER_DEFAULT_WIDTH  = 215;
            public static final int VOLUME_WIDTH  = (int)(VOLUME_DEFAULT_WIDTH  * Game.SCALE);
            public static final int VOLUME_HEIGHT = (int)(VOLUME_DEFAULT_HEIGHT * Game.SCALE);
            public static final int SLIDER_WIDTH  = (int)(SLIDER_DEFAULT_WIDTH  * Game.SCALE);
        }
    }

    // ── Player ───────────────────────────────────────────────
    public static class PlayerConstants {
        // Row indices in the jeepney sprite sheet (440 × 200, 5 rows)
        public static final int RUNNING    = 0;
        public static final int IDLE       = 1;
        public static final int CAR_STRUCK = 2;
        public static final int SHIELD     = 3; // 2 cols: col0=full, col1=half
        public static final int SHOOT      = 4; // 4 cols: animated projectile source

        // Frame counts per row
        public static int getSpriteAmount(int playerAction) {
            switch (playerAction) {
                case SHIELD: return 2;
                default:     return 4;
            }
        }
    }

    // ── Boss 1 (Garbage Truck) ───────────────────────────────
    public static class BossConstants {
        // boss1.png  550 × 316,  4 rows × 5 cols  (each cell 110 × 79)
        public static final int BOSS1_FRAME_W   = 110;
        public static final int BOSS1_FRAME_H   = 79;
        public static final int BOSS1_COLS      = 5;
        public static final int BOSS1_ROWS      = 4;

        public static final int ROW_SKILL1      = 0;
        public static final int ROW_RUNNING     = 1;
        public static final int ROW_SKILL2      = 2;
        public static final int ROW_HIT         = 3;
    }
}