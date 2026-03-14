package utils;

import main.Game;

public class HelpMethods {
    public static boolean canMoveHere(float x, float y, float width , float height, int[][] levelData){
        return !isSolid(x, y, levelData) &&
                !isSolid(x + width, y, levelData) &&
                !isSolid(x, y + height, levelData) &&
                !isSolid(x + width, y + height, levelData);
    }

    private static boolean isSolid(float x, float y, int[][] levelData){

        if(x < 0 || x >= Game.GAME_WIDTH) return true;
        if(y < 0 || y >= Game.GAME_HEIGHT) return true;

        int yIndex = (int)(y / Game.TILES_SIZE);

        int TOP_RAIL = 10;     // row with 7
        int BOTTOM_RAIL = 18;  // row with 2

        if(yIndex <= TOP_RAIL || yIndex >= BOTTOM_RAIL)
            return true;

        return false;
    }
}