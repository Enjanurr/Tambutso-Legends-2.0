package utils;

import main.Game;

public class Constants {

    // ── Environment (clouds / background) ───────────────────
    public static class Environment {
        public static final int BIG_CLOUD_WIDTH_DEFAULT    = 448;
        public static final int BIG_CLOUD_HEIGHT_DEFAULT   = 101;
        public static final int SMALL_CLOUD_WIDTH_DEFAULT  = 72;
        public static final int SMALL_CLOUD_HEIGHT_DEFAULT = 24;

        public static final int BIG_CLOUD_WIDTH    = (int)(BIG_CLOUD_WIDTH_DEFAULT   * Game.SCALE);
        public static final int BIG_CLOUD_HEIGHT   = (int)(BIG_CLOUD_HEIGHT_DEFAULT  * Game.SCALE);
        public static final int SMALL_CLOUD_WIDTH  = (int)(SMALL_CLOUD_WIDTH_DEFAULT * Game.SCALE);
        public static final int SMALL_CLOUD_HEIGHT = (int)(SMALL_CLOUD_HEIGHT_DEFAULT * Game.SCALE);
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

        // ── Walker settings ──────────────────────────────────
        public static final float WALK_SPEED       = 0.3333f;
        public static final int   WALKER_ANI_SPEED = 25;

        // Lane Y positions (walkers)
        public static final float LANE_1_Y = 128f;
        public static final float LANE_2_Y = 140f;

        // ── Passenger settings ───────────────────────────────
        public static final float PASSENGER_Y         = 154f;
        public static final int   PASSENGER_ANI_SPEED = 25;
    }

    // ── Enemy sprites ────────────────────────────────────────
    public static class EnemyConstants {

        // Legacy jeep enemy (unused stub — kept for compatibility)
        public static final int ENEMY_JEEP = 0;

        public static final int ENEMY_WIDTH_DEFAULT  = 110;
        public static final int ENEMY_HEIGHT_DEFAULT = 40;

        public static final int ENEMY_WIDTH  = (int)(ENEMY_WIDTH_DEFAULT  * Game.SCALE);
        public static final int ENEMY_HEIGHT = (int)(ENEMY_HEIGHT_DEFAULT * Game.SCALE);

        // -------------------------------------------------------
        // ANI SPEED  ← ADJUST: ticks per frame for road enemies
        // -------------------------------------------------------
        public static final int ENEMY_ANI_SPEED = 20;
        // -------------------------------------------------------

        public static int getSpriteAmountEnemy(int enemyType, int enemyState) {
            return 4;
        }
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
        // Row indices in the sprite sheet
        public static final int RUNNING    = 0;
        public static final int IDLE       = 1;
        public static final int CAR_STRUCK = 2; // NEW — Row 2

        public static int getSpriteAmount(int playerAction) {
            return 4; // all rows have 4 frames
        }
    }
}