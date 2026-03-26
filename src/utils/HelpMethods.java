package utils;

import main.Game;

public class HelpMethods {


    public static boolean canMoveHere(float x, float y, float width, float height, int[][] levelData) {
        return !isSolid(x,         y,          levelData) &&
                !isSolid(x + width, y,          levelData) &&
                !isSolid(x,         y + height, levelData) &&
                !isSolid(x + width, y + height, levelData);
    }

    private static boolean isSolid(float x, float y, int[][] levelData) {

        int maxWidth = levelData[0].length * Game.TILES_SIZE;
        if (x < 0 || x >= maxWidth)      return true;
        if (y < 0 || y >= Game.GAME_HEIGHT) return true;

        int yIndex = (int)(y / Game.TILES_SIZE);

        // The driveable lanes are between row 10 (top rail) and row 18 (bottom rail).

        final int TOP_RAIL    = 9;
        final int BOTTOM_RAIL = (int) 17.5;
        if (yIndex <= TOP_RAIL || yIndex >= BOTTOM_RAIL)
            return true;

        return false;
    }

    public static boolean isAtRightBorder(float nextX, float hitBoxWidth) {
        return nextX + hitBoxWidth >= Game.GAME_WIDTH;
    }
}