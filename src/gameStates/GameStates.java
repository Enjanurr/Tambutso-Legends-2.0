package gameStates;

public enum GameStates {
    PLAYING,
    MENU,
    OPTIONS,
    INTRO,
    CHAR_SELECT,
     // also change from 1 to 2

    GREEN_JEEP_VS_BOSS3,   // ← ADD

    RED_JEEP_VS_BOSS2,
    BLUE_JEEP_VS_BOSS3,
    RED_JEEP_VS_BOSS3,
    QUIT;

    public static GameStates state = MENU;
}