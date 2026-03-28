package utils;

import entities.Enemy;
import main.Game;

public class    Constants {

    // ── Environment (clouds / background) ───────────────────
    public static class Environment {
        public static final int BIG_CLOUD_WIDTH_DEFAULT   = 448;
        public static final int BIG_CLOUD_HEIGHT_DEFAULT  = 101;
        public static final int SMALL_CLOUD_WIDTH_DEFAULT = 72;
        public static final int SMALL_CLOUD_HEIGHT_DEFAULT = 24;
        public static final int BUS_STOP_WIDTH_DEFAULT = 793;
        public static final int BUS_STOP_HEIGHT_DEFAULT = 261;

        public static final int BIG_CLOUD_WIDTH    = (int)(BIG_CLOUD_WIDTH_DEFAULT   * Game.SCALE);
        public static final int BIG_CLOUD_HEIGHT   = (int)(BIG_CLOUD_HEIGHT_DEFAULT  * Game.SCALE);
        public static final int SMALL_CLOUD_WIDTH  = (int)(SMALL_CLOUD_WIDTH_DEFAULT * Game.SCALE);
        public static final int SMALL_CLOUD_HEIGHT = (int)(SMALL_CLOUD_HEIGHT_DEFAULT * Game.SCALE);
        public static final int BUS_STOP_WIDTH = (int)(BUS_STOP_WIDTH_DEFAULT * (Game.SCALE - 1.2));
        public static final int BUS_STOP_HEIGHT = (int)(BUS_STOP_HEIGHT_DEFAULT * (Game.SCALE - 1.2));
    }

    // ── Person sprites ───────────────────────────────────────
    public static class PersonConstants {


        // Sprite sheet per person: 610 × 120 — 10 columns, 2 rows, each cell 61 × 60 px
        // Row 0 = IDLE / PASSENGER  (top row)
        // Row 1 = WALK              (bottom row)
        public static final int PERSON_FRAME_COUNT    = 10;
        public static final int PERSON_WIDTH_DEFAULT  = 61;  // 610 / 10
        public static final int PERSON_HEIGHT_DEFAULT = 60;  // 120 / 2

        public static final int ROW_IDLE = 0;
        public static final int ROW_WALK = 1;

        public static final int PERSON_WIDTH  = (int)(PERSON_WIDTH_DEFAULT  * Game.SCALE);
        public static final int PERSON_HEIGHT = (int)(PERSON_HEIGHT_DEFAULT * Game.SCALE);



        // ── Walker settings ──────────────────────────────────
        public static final float WALK_SPEED       = 0.3333f;
        public static final int   WALKER_ANI_SPEED = 25;

        // FOR WALKERS Lane Y positions
        public static final float LANE_1_Y = 128f; // top sidewalk lane
        public static final float LANE_2_Y = 140f; // bottom sidewalk lane



        // ── Passenger settings ───────────────────────────────
        public static final float PASSENGER_Y         = 154f;
        public static final int   PASSENGER_ANI_SPEED = 25;



    }


    public static class EnemyConstants {
        public static final int ENEMY_JEEP = 0;

        public static final int IDLE = 1;
        public static final int RUNNING = 0;


        // FOR ENEMIES WITH VARIOUS SIZES
        public static final int ENEMY_WIDTH_DEFAULT = 110;
        public static final int ENEMY_HEIGHT_DEFAULT = 40;


        public static final int ENEMY_WIDTH = (int)(ENEMY_WIDTH_DEFAULT * Game.SCALE);
        public static final int ENEMY_HEIGHT = (int)(ENEMY_HEIGHT_DEFAULT * Game.SCALE);

        public static int getSpriteAmountEnemy(int enemyType, int enemyState){

            // as of now return 4
            return 4;
         }


    }
    public static class UI{
        public static class Buttons{
            public static final int B_WIDTH_DEFAULT = 140;
            public static final int B_HEIGHT_DEFAULT = 56;
            public static final int B_WIDTH = (int)(B_WIDTH_DEFAULT * Game.SCALE);
            public static final int B_HEIGHT = (int)(B_HEIGHT_DEFAULT * Game.SCALE);

        }
        public static class PauseButtons{
            public static final int SOUND_SIZE_DEFAULT = 42;
            public static final int SOUND_SIZE = (int) (SOUND_SIZE_DEFAULT * Game.SCALE);

        }

        public static class URMButtons{
            public static  final int URM_DEFAULT_SIZE = 56;
            public static final int URM_SIZE = (int) (URM_DEFAULT_SIZE * Game.SCALE);
        }
        public static class VolumeButtons{
            public static final int VOLUME_DEFAULT_WIDTH = 28;
            public static final int VOLUME_DEFAULT_HEIGHT = 44;
            public static final int SLIDER_DEFAULT_WIDTH = 215;

            public static final int VOLUME_WIDTH =(int)(VOLUME_DEFAULT_WIDTH * Game.SCALE);
            public static final int VOLUME_HEIGHT =(int)(VOLUME_DEFAULT_HEIGHT * Game.SCALE);
            public static final int SLIDER_WIDTH =(int)(SLIDER_DEFAULT_WIDTH * Game.SCALE);
        }
    }
    public static class Directions {
        public static final int LEFT = 0;
        public static final int UP = 1;
        public static final int RIGHT = 2;
        public static final int DOWN = 3;
    }

    public static class PlayerConstants {
        public static final int IDLE = 1;
        public static final int RUNNING = 0;
        public static int getSpriteAmount(int player_action) {
            return 4;
            /*
            switch (player_action) {

                case RUNNING:
                    return 6;
                case IDLE:
                    return 5;
              case HIT:
                    return 4;
                case JUMP:
                case ATTACK_1:
                case ATTACK_JUMP_1:
                case ATTACK_JUMP_2:
                    return 3;
                case GROUND:
                    return 2;
                case FALLING:
                default:
                    return 1;


            }
            */

        /*

        public static final int IDLE = 0;
        public static final int RUNNING = 1;
        public static final int JUMP = 2;
        public static final int FALLING = 3;
        public static final int GROUND = 4;
        public static final int HIT = 5;
        public static final int ATTACK_1 = 6;
        public static final int ATTACK_JUMP_1 = 7;
        public static final int ATTACK_JUMP_2 = 8;

        public static int getSpriteAmount(int player_action) {
            switch (player_action) {
                case RUNNING:
                    return 6;
                case IDLE:
                    return 5;
                case HIT:
                    return 4;
                case JUMP:
                case ATTACK_1:
                case ATTACK_JUMP_1:
                case ATTACK_JUMP_2:
                    return 3;
                case GROUND:
                    return 2;
                case FALLING:
                default:
                    return 1;
            }
            */

        }

    }
}
