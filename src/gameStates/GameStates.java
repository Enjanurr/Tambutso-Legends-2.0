package gameStates;

public enum GameStates {
    PLAYING,
    MENU,
    OPTIONS,
    INTRO,
    CHAR_SELECT,

    BLUE_JEEP_VS_BOSS1,    // ← ADD
    RED_JEEP_VS_BOSS1,     // ← ADD
    GREEN_JEEP_VS_BOSS1,   // ← ADD
    QUIT;

    public static GameStates state = MENU;
}