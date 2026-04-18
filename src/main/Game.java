package main;

import java.awt.Graphics;

import entities.DriverProfile;
import gameStates.*;
import Ui.IntroOverlay;
import BossFight.BossFightState;
import utils.AudioPlayer;

public class Game implements Runnable {
    private GameWindow    gameWindow;
    private GamePanel     gamePanel;
    private Thread        gameThread;
    private final int     FPS_SET = 120;
    private final int     UPS_SET = 200;

    private Playing       playing;
    private Menu          menu;
    private Options       options;
    private GameIntroState gameIntroState;
    private IntroOverlay  introOverlay;
    private BossFightState bossFightState;
    private final AudioPlayer audioPlayer;
    private CharSelectState charSelectState;
    private String activeMusicTrack;

    public final static int   TILES_DEFAULT_SIZE = 20;
    public final static float SCALE              = 2f;
    public final static int   TILES_SIZE         = (int)(TILES_DEFAULT_SIZE * SCALE);
    public final static int   TILES_IN_WIDTH     = 40;
    public final static int   TILES_IN_HEIGHT    = 20;
    public final static int   GAME_WIDTH         = TILES_SIZE * TILES_IN_WIDTH;
    public final static int   GAME_HEIGHT        = TILES_SIZE * TILES_IN_HEIGHT;
    private DriverProfile selectedDriver;  // add this field

    public Game() {
        audioPlayer = new AudioPlayer();
        gamePanel    = new GamePanel(this);
        initClasses();
        gameWindow   = new GameWindow(gamePanel);
        gamePanel.requestFocus();
        syncMusicToState();
        startGameLoop();
    }


    public void setSelectedDriver(DriverProfile driver) {
        this.selectedDriver = driver;
    }

    public DriverProfile getSelectedDriver() {
        return selectedDriver;
    }

    private void initClasses() {
        menu          = new Menu(this);
        options       = new Options(this);
        playing       = new Playing(this);
        gameIntroState = new GameIntroState(this);
        charSelectState = new CharSelectState(this);
        introOverlay  = new IntroOverlay();
        // BossFightState shares the Player and HealthBar from Playing
        bossFightState = new BossFightState(this,
                playing.getPlayer(),
                playing.getHealthBar());
    }

    private void startGameLoop() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    /** Called by Menu PLAY button — goes to char select first. */
    public void startCharSelect() {
        GameStates.state = GameStates.CHAR_SELECT;
    }

    /** Called by CharSelectState after confirming a driver. */
    public void startIntroOverlay() {
        introOverlay.reset();
        GameStates.state = GameStates.INTRO;
    }

    /** Called by Playing when all 15 loops complete. */
    public void startBossFight() {
        System.out.println("═══════════════════════════════");
        System.out.println("🏁 STARTING BOSS FIGHT");
        System.out.println("Selected Driver: " +
                (selectedDriver != null ? selectedDriver.displayName : "NULL"));
        System.out.println("═══════════════════════════════");

        bossFightState.resetAll();

        if (selectedDriver != null) {
            bossFightState.applyDriverAssets(selectedDriver);
        }

        GameStates.state = GameStates.BOSS_FIGHT;
    }

    public void update() {
        switch (GameStates.state) {
            case GAME_INTRO:
                gameIntroState.update();
                break;
            case CHAR_SELECT:
                charSelectState.update();
                break;
            case MENU:
                menu.update();
                break;
            case INTRO:
                boolean done = introOverlay.update();
                if (done) {
                    GameStates.state = GameStates.PLAYING;

                    // ✨ DEBUG OUTPUT
                    System.out.println("───────────────────────────────");
                    System.out.println("INTRO COMPLETE");
                    System.out.println("Selected Driver: " +
                            (selectedDriver != null ? selectedDriver.displayName : "NULL"));
                    System.out.println("───────────────────────────────");

                    if (selectedDriver != null) {
                        playing.applyDriver(selectedDriver);
                    }
                }
                break;
            case PLAYING:
                gamePanel.updateFade();
                playing.update();
                break;
            case BOSS_FIGHT:
                bossFightState.update();
                break;
            case OPTIONS:
                options.update();
                break;
            case QUIT:
            default:
                System.exit(0);
                break;
        }
        syncMusicToState();
    }

    private void syncMusicToState() {
        String desiredTrack = getDesiredMusicTrack();
        if (desiredTrack.equals(activeMusicTrack))
            return;

        switch (desiredTrack) {
            case "menu":
                audioPlayer.playMenuTheme();
                break;
            case "main":
                audioPlayer.playMainTheme();
                break;
            default:
                audioPlayer.stop();
                break;
        }

        activeMusicTrack = desiredTrack;
    }

    private String getDesiredMusicTrack() {
        return switch (GameStates.state) {
            case GAME_INTRO -> gameIntroState.hasLogoRevealStarted() ? "menu" : "none";
            case CHAR_SELECT -> "menu";
            case MENU, INTRO, OPTIONS -> "menu";
            case PLAYING -> playing.isPaused() ? "menu" : "main";
            case BOSS_FIGHT -> bossFightState.isPaused() ? "menu" : "main";
            default -> "none";
        };
    }

    public void render(Graphics g) {
        switch (GameStates.state) {
            case GAME_INTRO:
                gameIntroState.draw(g);
                break;
            case CHAR_SELECT:
                charSelectState.draw(g);
                break;
            case MENU:
                menu.draw(g);
                break;
            case INTRO:
                menu.draw(g);                  // optional background/menu layer
                introOverlay.render(g);
                break;
            case PLAYING:
                playing.draw(g);
                break;
            case BOSS_FIGHT:
                bossFightState.draw(g);
                break;
            case OPTIONS:
                options.draw(g);
                break;
            default:
                break;
        }
    }
// In Game.java


    public void onJeepLooped() {
        playing.onJeepLooped();
    }

    @Override
    public void run() {
        double timePerFrame  = 1000000000.0 / FPS_SET;
        double timePerUpdate = 1000000000.0 / UPS_SET;
        long previousTime    = System.nanoTime();
        int frames = 0, updates = 0;
        long lastCheck = System.currentTimeMillis();
        double deltaU = 0, deltaF = 0;

        while (true) {
            long currentTime = System.nanoTime();
            deltaU += (currentTime - previousTime) / timePerUpdate;
            deltaF += (currentTime - previousTime) / timePerFrame;
            previousTime = currentTime;

            if (deltaU >= 1) { update(); updates++; deltaU--; }
            if (deltaF >= 1) { gamePanel.repaint(); frames++; deltaF--; }

            if (System.currentTimeMillis() - lastCheck >= 1000) {
                lastCheck = System.currentTimeMillis();
                frames = 0; updates = 0;
            }
        }
    }

    public void windowFocusLost() {
        if (GameStates.state == GameStates.PLAYING)
            playing.windowFocusLost();
    }

    // ── Getters ───────────────────────────────────────────────
    public GamePanel       getGamePanel()      { return gamePanel; }
    public Menu            getMenu()           { return menu; }
    public Options         getOptions()        { return options; }
    public Playing         getPlaying()        { return playing; }
    public GameIntroState  getGameIntroState() { return gameIntroState; }
    public IntroOverlay    getIntroOverlay()   { return introOverlay; }
    public BossFightState  getBossFightState() { return bossFightState; }
    public AudioPlayer     getAudioPlayer()    { return audioPlayer; }
    public CharSelectState getCharSelectState() { return charSelectState; }
}
