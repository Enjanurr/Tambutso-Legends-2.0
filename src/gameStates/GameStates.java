package gameStates;

public enum GameStates {
    PLAYING,
    MENU,
    OPTIONS,
    INTRO,
    CHAR_SELECT,
     // also change from 1 to 2
    BLUE_JEEP_VS_BOSS1,    // ← ADD
    RED_JEEP_VS_BOSS1,     // ← ADD
    GREEN_JEEP_VS_BOSS2,   // ← ADD
    BLUE_JEEP_VS_BOSS2,
    RED_JEEP_VS_BOSS2,
    QUIT;

    public static GameStates state = MENU;
}