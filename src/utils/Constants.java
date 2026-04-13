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

    public static class Landmarks {
        public record LandmarkTuning(float scale, int y, float xOffset) {}

        // These values are intentionally centralized so landmark alignment can be
        // tuned without touching spawn logic.
        public static final LandmarkTuning MAP1_KEPCO =
                new LandmarkTuning(0.70f, 360, -10f); // 1012x598
        public static final LandmarkTuning MAP1_GAISANO =
                new LandmarkTuning(0.70f, 132, 0f);   // 1256x417
        public static final LandmarkTuning MAP1_MARKETPLACE =
                new LandmarkTuning(0.50f, 120, -12f); // 1480x552

        public static final LandmarkTuning MAP2_CITU =
                new LandmarkTuning(0.25f, 132, 0f);   // 827x431
        public static final LandmarkTuning MAP2_EMALL =
                new LandmarkTuning(0.23f, 132, -6f);  // 968x377
        public static final LandmarkTuning MAP2_SHOPWISE =
                new LandmarkTuning(0.23f, 134, -6f);  // 915x370
        public static final LandmarkTuning MAP2_STARMALL =
                new LandmarkTuning(0.18f, 106, -14f); // 1325x641
        public static final LandmarkTuning MAP2_USJR =
                new LandmarkTuning(0.22f, 128, -6f);  // 1015x395

        public static final LandmarkTuning MAP3_CATHEDRAL =
                new LandmarkTuning(0.16f, 82, -18f);  // 1181x837
        public static final LandmarkTuning MAP3_SMCITY =
                new LandmarkTuning(0.18f, 124, -10f); // 1734x530
    }

    // ── Person sprites ───────────────────────────────────────
    public static class PersonConstants {
        // Sprite sheet per person: 610 × 120 — 10 columns, 2 rows, each cell 61 × 60 px
        // Row 0 = IDLE / PASSENGER
        // Row 1 = WALK
        public static final int PERSON_FRAME_COUNT    = 10;
        public static final int PERSON_WIDTH_DEFAULT  = 61;
        public static final int PERSON_HEIGHT_DEFAULT = 60;

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
            return switch (playerAction) {
                case SHIELD -> 2;
                default -> 4;
            };
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
