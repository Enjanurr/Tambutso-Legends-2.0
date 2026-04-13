package gameStates;

public enum GameStates {
    PLAYING,
    MENU,
    OPTIONS,
    INTRO,
    CHAR_SELECT,
    // Level 1
    BLUE_JEEP_VS_BOSS1,
    RED_JEEP_VS_BOSS1,
    GREEN_JEEP_VS_BOSS1,
    // Level 2 ← ADD
    BLUE_JEEP_VS_BOSS2,
    RED_JEEP_VS_BOSS2,
    GREEN_JEEP_VS_BOSS2,
    // Level 3 ← ADD
    BLUE_JEEP_VS_BOSS3,
    RED_JEEP_VS_BOSS3,
    GREEN_JEEP_VS_BOSS3,
    QUIT;

    public static GameStates state = MENU;
}