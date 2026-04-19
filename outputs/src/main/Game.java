package main;

import java.awt.Graphics;

import entities.DriverProfile;
import gameStates.*;
import Ui.IntroOverlay;
import BossFight.LevelOne.Blue.BlueJeepVsBoss1State;
import BossFight.LevelOne.Red.RedJeepVsBoss1State;
import BossFight.LevelOne.Green.GreenJeepVsBoss1State;
import utils.AudioPlayer;
import gameStates.BossFightMatchmaker;

public class Game implements Runnable {
    private GameWindow    gameWindow;
    private GamePanel     gamePanel;
    private Thread        gameThread;
    private final int     FPS_SET = 120;
    private final int     UPS_SET = 200;

    private Playing       playing;
    private Menu          menu;
    private Options       options;
    private IntroOverlay  introOverlay;
    private BlueJeepVsBoss1State blueJeepVsBoss1State;
    private RedJeepVsBoss1State redJeepVsBoss1State;
    private GreenJeepVsBoss1State greenJeepVsBoss1State;
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
    // ── Boss Fight Matchmaker ─────────────────────────────────
    private BossFightMatchmaker bossFightMatchmaker;
    private int currentBossLevel = 1; // Current boss level (1, 2, 3...)



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
        charSelectState = new CharSelectState(this);
        introOverlay  = new IntroOverlay(this::onIntroDone);

        // BossFightStates share the Player and HealthBar from Playing
        blueJeepVsBoss1State = new BlueJeepVsBoss1State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        // ── Initialize Red and Green boss fight states ← ADD ─────
        redJeepVsBoss1State = new RedJeepVsBoss1State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        greenJeepVsBoss1State = new GreenJeepVsBoss1State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        // ── Initialize matchmaker ← ADD ─────────────────────────
        bossFightMatchmaker = new BossFightMatchmaker(this,
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
        System.out.println("═══════════════════════════════");
        System.out.println("🏁 STARTING BOSS FIGHT");
        System.out.println("Selected Driver: " +
                (selectedDriver != null ? selectedDriver.displayName : "NULL"));
        System.out.println("═══════════════════════════════");

        // ── Use matchmaker to route to correct boss fight ← CHANGE ──
        startLevel1BossFight();
    }

    /**
     * Starts boss fight for the selected driver and level.
     * Called from IntroOverlay or level complete screen.
     */
    public void startBossFightWithLevel(int levelIndex) {
        if (selectedDriver == null) {
            System.err.println("❌ [Game] Cannot start boss fight - no driver selected!");
            GameStates.state = GameStates.MENU;
            return;
        }

        currentBossLevel = levelIndex;

        System.out.println("🎮 [Game] Starting Boss Fight - Level " + levelIndex +
                " - Driver: " + selectedDriver.displayName);

        // Route to correct state based on driver
        switch (selectedDriver.id) {
            case "driver_3": // Kuya Ben (Blue Jeep)
                blueJeepVsBoss1State.resetAll();
                blueJeepVsBoss1State.applyDriverAssets(selectedDriver);
                GameStates.state = GameStates.BLUE_JEEP_VS_BOSS1;
                break;

            case "driver_1": // Manong Ricky (Red Jeep)
                redJeepVsBoss1State.resetAll();
                redJeepVsBoss1State.applyDriverAssets(selectedDriver);
                GameStates.state = GameStates.RED_JEEP_VS_BOSS1;
                break;

            case "driver_2": // Ate Gloria (Green Jeep)
                greenJeepVsBoss1State.resetAll();
                greenJeepVsBoss1State.applyDriverAssets(selectedDriver);
                GameStates.state = GameStates.GREEN_JEEP_VS_BOSS1;
                break;

            default:
                System.err.println("❌ [Game] Unknown driver: " + selectedDriver.id);
                GameStates.state = GameStates.MENU;
                break;
        }
    }

    /**
     * Quick start Level 1 boss fight (most common).
     */
    public void startLevel1BossFight() {
        startBossFightWithLevel(1);
    }

    /**
     * Start character selection state.
     * Called from Menu when player clicks Play.
     */
    public void startCharSelect() {
        GameStates.state = GameStates.CHAR_SELECT;
    }

    /**
     * Start the intro overlay sequence.
     * Called from CharSelectState after driver selection.
     */
    public void startIntroOverlay() {
        GameStates.state = GameStates.INTRO;
        introOverlay.resetShown();
        introOverlay.open();
    }

    /**
     * Callback for IntroOverlay when complete.
     */
    private void onIntroDone() {
        // Transition is handled in update() method
    }

    public void update() {
        switch (GameStates.state) {
            case CHAR_SELECT:
                charSelectState.update();
                break;
            case MENU:
                menu.update();
                break;
            case INTRO:
                introOverlay.update();
                if (!introOverlay.isOpen()) {
                    GameStates.state = GameStates.PLAYING;

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

            // ── Boss Fight States ← CHANGE/ADD ──────────────────
            case BLUE_JEEP_VS_BOSS1:
                blueJeepVsBoss1State.update();
                break;

            case RED_JEEP_VS_BOSS1:
                redJeepVsBoss1State.update();
                break;

            case GREEN_JEEP_VS_BOSS1:
                greenJeepVsBoss1State.update();
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
        switch (GameStates.state) {
            case CHAR_SELECT:
                return "menu";
            case MENU:
            case OPTIONS:
                return "menu";
            case PLAYING:
                return playing.isPaused() ? "menu" : "main";
            case BLUE_JEEP_VS_BOSS1:
                return blueJeepVsBoss1State.isPaused() ? "menu" : "main";

            case RED_JEEP_VS_BOSS1:
                return redJeepVsBoss1State.isPaused() ? "menu" : "main";

            case GREEN_JEEP_VS_BOSS1:
                return greenJeepVsBoss1State.isPaused() ? "menu" : "main";

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

            // ── Boss Fight States ← CHANGE/ADD ──────────────────
            case BLUE_JEEP_VS_BOSS1:
                blueJeepVsBoss1State.draw(g);
                break;

            case RED_JEEP_VS_BOSS1:
                redJeepVsBoss1State.draw(g);
                break;

            case GREEN_JEEP_VS_BOSS1:
                greenJeepVsBoss1State.draw(g);
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
    public GamePanel       getGamePanel()      { return gamePanel; }
    public Menu            getMenu()           { return menu; }
    public Options         getOptions()        { return options; }
    public Playing         getPlaying()        { return playing; }
    public IntroOverlay    getIntroOverlay()   { return introOverlay; }
    public BlueJeepVsBoss1State getBossFightState() { return blueJeepVsBoss1State; }
    public AudioPlayer     getAudioPlayer()    { return audioPlayer; }
    public CharSelectState getCharSelectState() { return charSelectState; }

    public BossFightMatchmaker getBossFightMatchmaker() {
        return bossFightMatchmaker;
    }

    public int getCurrentBossLevel() {
        return currentBossLevel;
    }

    public RedJeepVsBoss1State getRedJeepVsBoss1State() {
        return redJeepVsBoss1State;
    }

    public GreenJeepVsBoss1State getGreenJeepVsBoss1State() {
        return greenJeepVsBoss1State;
    }

    // RENAME THIS GETTER for consistency:
// OLD: public BlueJeepVsBoss1State getBossFightState()
// NEW:
    public BlueJeepVsBoss1State getBlueJeepVsBoss1State() {
        return blueJeepVsBoss1State;
    }
}