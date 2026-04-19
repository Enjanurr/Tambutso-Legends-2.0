package main;

import java.awt.Graphics;
import gameStates.GameStates;
import gameStates.Menu;
import gameStates.Options;
import gameStates.Playing;
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
    private BossFightState bossFightState;
    private final AudioPlayer audioPlayer;
    private String activeMusicTrack;

    public final static int   TILES_DEFAULT_SIZE = 20;
    public final static float SCALE              = 2f;
    public final static int   TILES_SIZE         = (int)(TILES_DEFAULT_SIZE * SCALE);
    public final static int   TILES_IN_WIDTH     = 40;
    public final static int   TILES_IN_HEIGHT    = 20;
    public final static int   GAME_WIDTH         = TILES_SIZE * TILES_IN_WIDTH;
    public final static int   GAME_HEIGHT        = TILES_SIZE * TILES_IN_HEIGHT;

    public Game() {
        audioPlayer = new AudioPlayer();
        gamePanel    = new GamePanel(this);
        initClasses();
        gameWindow   = new GameWindow(gamePanel);
        gamePanel.requestFocus();
        syncMusicToState();
        startGameLoop();
    }

    private void initClasses() {
        menu          = new Menu(this);
        options       = new Options(this);
        playing       = new Playing(this);
        // BossFightState shares the Player and HealthBar from Playing
        bossFightState = new BossFightState(this,
                playing.getPlayer(),
                playing.getHealthBar());
    }

    private void startGameLoop() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    /**
     * Called from Menu / keyboard when the player presses Play.
     *
     * Always transitions directly to PLAYING — no INTRO game state needed.
     * Playing.tryShowIntro() handles the show-once logic internally:
     *   • First call  → game starts paused, intro overlay fades in over the world.
     *   • Later calls → game starts immediately unpaused, intro is skipped.
     */
    public void startPlayingOrIntro() {
        GameStates.state = GameStates.PLAYING;
        playing.tryShowIntro();
    }

    /** Called by Playing when all 15 loops complete. */
    public void startBossFight() {
        bossFightState.resetAll();
        GameStates.state = GameStates.BOSS_FIGHT;
    }

    public void update() {
        switch (GameStates.state) {
            case MENU:
                menu.update();
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
            // INTRO state is no longer used; kept in enum for safety
            case INTRO:
                GameStates.state = GameStates.PLAYING;
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
        switch (GameStates.state) {
            case MENU:
            case OPTIONS:
                return "menu";
            case PLAYING:
                return playing.isPaused() ? "menu" : "main";
            case BOSS_FIGHT:
                return bossFightState.isPaused() ? "menu" : "main";
            case QUIT:
            default:
                return "none";
        }
    }

    public void render(Graphics g) {
        switch (GameStates.state) {
            case MENU:
                menu.draw(g);
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
            // INTRO is no longer used as a standalone state
            case INTRO:
                playing.draw(g);
                break;
            default:
                break;
        }
    }

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
    public GamePanel      getGamePanel()      { return gamePanel; }
    public Menu           getMenu()           { return menu; }
    public Options        getOptions()        { return options; }
    public Playing        getPlaying()        { return playing; }
    public BossFightState getBossFightState() { return bossFightState; }
    public AudioPlayer    getAudioPlayer()    { return audioPlayer; }
}