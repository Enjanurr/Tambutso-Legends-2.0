package main;

import java.awt.Graphics;
import java.awt.event.MouseMotionListener;

import BossFight.LevelOne.Green.GreenJeepVsBoss1State;
import BossFight.LevelOne.Red.RedJeepVsBoss1State;
import BossFight.LevelOne.Blue.BlueJeepVsBoss1State;

import BossFight.LevelThree.Blue.BlueJeepVsBoss3State;
import BossFight.LevelThree.Green.GreenJeepVsBoss3State;
import BossFight.LevelThree.Red.RedJeepVsBoss3State;

import BossFight.LevelTwo.Blue.BlueJeepVsBoss2State;
import BossFight.LevelTwo.Green.GreenJeepVsBoss2State;
import BossFight.LevelTwo.Red.RedJeepVsBoss2State;

import entities.DriverProfile;
import gameStates.*;
import Ui.IntroOverlay;
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
    // level 1
    private BlueJeepVsBoss1State blueJeepVsBoss1State;
    private RedJeepVsBoss1State redJeepVsBoss1State;
    private GreenJeepVsBoss1State greenJeepVsBoss1State;

    // level 2
    private BlueJeepVsBoss2State blueJeepVsBoss2State;
    private RedJeepVsBoss2State redJeepVsBoss2State;
    private GreenJeepVsBoss2State greenJeepVsBoss2State;

    // level 3 ← ADD THESE
    private BlueJeepVsBoss3State blueJeepVsBoss3State;
    private RedJeepVsBoss3State redJeepVsBoss3State;
    private GreenJeepVsBoss3State greenJeepVsBoss3State;

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

    private int currentGameLevel = 2;  // ← ADD THIS at class level


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
        introOverlay  = new IntroOverlay();
       
        // level 1
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
        
        //-----------------------
           // level 2

        // BossFightStates share the Player and HealthBar from Playing
         blueJeepVsBoss2State = new BlueJeepVsBoss2State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        // ── Initialize Red and Green boss fight states ← ADD ─────
         redJeepVsBoss2State = new RedJeepVsBoss2State(this,
                playing.getPlayer(),
                playing.getHealthBar());

         greenJeepVsBoss2State = new GreenJeepVsBoss2State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        //------ level 3
        //-----------------------
// level 3 ← ADD THIS

        blueJeepVsBoss3State = new BlueJeepVsBoss3State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        redJeepVsBoss3State = new RedJeepVsBoss3State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        greenJeepVsBoss3State = new GreenJeepVsBoss3State(this,
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
    /** Called by Playing when all 15 loops complete. */
    public void startBossFight() {
        System.out.println("═══════════════════════════════");
        System.out.println("🏁 STARTING BOSS FIGHT");
        System.out.println("Selected Driver: " +
                (selectedDriver != null ? selectedDriver.displayName : "NULL"));
        System.out.println("Current Level: " + currentGameLevel);
        System.out.println("═══════════════════════════════");


        startBossFightWithLevel(currentGameLevel);
    }


    /**
     * Starts boss fight for the selected driver and level.
     * Called from IntroOverlay or level complete screen.
     */
    /**
     * Starts boss fight for the selected driver and level.
     * Called from Playing when level loop completes.
     */
    public void setCurrentGameLevel(int level) {
        currentGameLevel = level;
        System.out.println("════════════════════════════════════════");
        System.out.println("[Game] ⚙️  Level forced to: " + level);
        System.out.println("════════════════════════════════════════");
    }
    public void startBossFightWithLevel(int levelIndex) {
        if (selectedDriver == null) {
            System.err.println("❌ [Game] Cannot start boss fight - no driver selected!");
            GameStates.state = GameStates.MENU;
            return;
        }

        currentGameLevel = levelIndex;

        System.out.println("════════════════════════════════════════");
        System.out.println("🎮 [Game] Starting Boss Fight");
        System.out.println("   Level: " + levelIndex);
        System.out.println("   Driver: " + selectedDriver.displayName);
        System.out.println("   Driver ID: " + selectedDriver.id);
        System.out.println("════════════════════════════════════════");

        // ── Level 1 Routing ──────────────────────────────────────
        if (levelIndex == 1) {
            System.out.println("✓ Routing to LEVEL 1 BOSS");
            switch (selectedDriver.id) {
                case "driver_3": // Kuya Ben (Blue Jeep)
                    System.out.println("  → Blue Jeep vs Boss 1");
                    blueJeepVsBoss1State.resetAll();
                    blueJeepVsBoss1State.applyDriverAssets(selectedDriver);
                    GameStates.state = GameStates.BLUE_JEEP_VS_BOSS1;
                    break;
                case "driver_1": // Manong Ricky (Red Jeep)
                    System.out.println("  → Red Jeep vs Boss 1");
                    redJeepVsBoss1State.resetAll();
                    redJeepVsBoss1State.applyDriverAssets(selectedDriver);
                    GameStates.state = GameStates.RED_JEEP_VS_BOSS1;
                    break;
                case "driver_2": // Ate Gloria (Green Jeep)
                    System.out.println("  → Green Jeep vs Boss 1");
                    greenJeepVsBoss1State.resetAll();
                    greenJeepVsBoss1State.applyDriverAssets(selectedDriver);
                    GameStates.state = GameStates.GREEN_JEEP_VS_BOSS1;
                    break;
                default:
                    System.err.println("❌ [Game] Unknown driver: " + selectedDriver.id);
                    GameStates.state = GameStates.MENU;
            }
        }

        // ── Level 2 Routing ──────────────────────────────────────
        else if (levelIndex == 2) {
            System.out.println("✓ Routing to LEVEL 2 BOSS");
            switch (selectedDriver.id) {
                case "driver_3": // Kuya Ben (Blue Jeep)
                    System.out.println("  → Blue Jeep vs Boss 2");
                    blueJeepVsBoss2State.resetAll();
                    blueJeepVsBoss2State.applyDriverAssets(selectedDriver);
                    GameStates.state = GameStates.BLUE_JEEP_VS_BOSS2;
                    break;
                case "driver_1": // Manong Ricky (Red Jeep)
                    System.out.println("  → Red Jeep vs Boss 2");
                    redJeepVsBoss2State.resetAll();
                    redJeepVsBoss2State.applyDriverAssets(selectedDriver);
                    GameStates.state = GameStates.RED_JEEP_VS_BOSS2;
                    break;
                case "driver_2": // Ate Gloria (Green Jeep)
                    System.out.println("  → Green Jeep vs Boss 2");
                    greenJeepVsBoss2State.resetAll();
                    greenJeepVsBoss2State.applyDriverAssets(selectedDriver);
                    GameStates.state = GameStates.GREEN_JEEP_VS_BOSS2;
                    break;
                default:
                    System.err.println("❌ [Game] Unknown driver: " + selectedDriver.id);
                    GameStates.state = GameStates.MENU;
            }
        }

        // ── Level 3 Routing ──────────────────────────────────────
        else if (levelIndex == 3) {
            System.out.println("✓ Routing to LEVEL 3 BOSS");
            switch (selectedDriver.id) {
                case "driver_3": // Kuya Ben (Blue Jeep)
                    System.out.println("  → Blue Jeep vs Boss 3");
                    blueJeepVsBoss3State.resetAll();
                    blueJeepVsBoss3State.applyDriverAssets(selectedDriver);
                    GameStates.state = GameStates.BLUE_JEEP_VS_BOSS3;
                    break;
                case "driver_1": // Manong Ricky (Red Jeep)
                    System.out.println("  → Red Jeep vs Boss 3");
                    redJeepVsBoss3State.resetAll();
                    redJeepVsBoss3State.applyDriverAssets(selectedDriver);
                    GameStates.state = GameStates.RED_JEEP_VS_BOSS3;
                    break;
                case "driver_2": // Ate Gloria (Green Jeep)
                    System.out.println("  → Green Jeep vs Boss 3");
                    greenJeepVsBoss3State.resetAll();
                    greenJeepVsBoss3State.applyDriverAssets(selectedDriver);
                    GameStates.state = GameStates.GREEN_JEEP_VS_BOSS3;
                    break;
                default:
                    System.err.println("❌ [Game] Unknown driver: " + selectedDriver.id);
                    GameStates.state = GameStates.MENU;
            }
        }

        else {
            System.err.println("❌ [Game] Unknown level: " + levelIndex);
            GameStates.state = GameStates.MENU;
        }
    }
    // After boss defeat:
    public void progressToNextLevel() {
        currentGameLevel++;
        if (currentGameLevel > 3) {
            // Game Over / Credits
        } else {
            GameStates.state = GameStates.PLAYING;
            getPlaying().restartGame();
        }
    }
    /**
     * Quick start Level 1 boss fight (most common).
     */


    public void update() {
        switch (GameStates.state) {
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

            // ── LEVEL 1 ← RESTORE THESE ────────────────────────
            case BLUE_JEEP_VS_BOSS1:
                blueJeepVsBoss1State.update();
                break;
            case RED_JEEP_VS_BOSS1:
                redJeepVsBoss1State.update();
                break;
            case GREEN_JEEP_VS_BOSS1:
                greenJeepVsBoss1State.update();
                break;

            // ── LEVEL 2 ← ADD THESE ────────────────────────────
            case BLUE_JEEP_VS_BOSS2:
                blueJeepVsBoss2State.update();
                break;
            case RED_JEEP_VS_BOSS2:
                redJeepVsBoss2State.update();
                break;
            case GREEN_JEEP_VS_BOSS2:
                greenJeepVsBoss2State.update();
                break;

            // ── LEVEL 3 ← KEEP THESE ───────────────────────────
            case BLUE_JEEP_VS_BOSS3:
                blueJeepVsBoss3State.update();
                break;
            case RED_JEEP_VS_BOSS3:
                redJeepVsBoss3State.update();
                break;
            case GREEN_JEEP_VS_BOSS3:
                greenJeepVsBoss3State.update();
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
            case INTRO:
            case OPTIONS:
                return "menu";
            case PLAYING:
                return playing.isPaused() ? "menu" : "main";

            // ── LEVEL 1 ← KEEP THESE ───────────────────────────
            case BLUE_JEEP_VS_BOSS1:
                return blueJeepVsBoss1State.isPaused() ? "menu" : "main";
            case RED_JEEP_VS_BOSS1:
                return redJeepVsBoss1State.isPaused() ? "menu" : "main";
            case GREEN_JEEP_VS_BOSS1:
                return greenJeepVsBoss1State.isPaused() ? "menu" : "main";

            // ── LEVEL 2 ← ADD THESE ────────────────────────────
            case BLUE_JEEP_VS_BOSS2:
                return blueJeepVsBoss2State.isPaused() ? "menu" : "main";
            case RED_JEEP_VS_BOSS2:
                return redJeepVsBoss2State.isPaused() ? "menu" : "main";
            case GREEN_JEEP_VS_BOSS2:
                return greenJeepVsBoss2State.isPaused() ? "menu" : "main";

            // ── LEVEL 3 ← KEEP THESE ───────────────────────────
            case BLUE_JEEP_VS_BOSS3:
                return blueJeepVsBoss3State.isPaused() ? "menu" : "main";
            case RED_JEEP_VS_BOSS3:
                return redJeepVsBoss3State.isPaused() ? "menu" : "main";
            case GREEN_JEEP_VS_BOSS3:
                return greenJeepVsBoss3State.isPaused() ? "menu" : "main";

            case QUIT:
            default:
                return "none";
        }
    }

    public void render(Graphics g) {
        switch (GameStates.state) {
            case CHAR_SELECT:
                charSelectState.draw(g);
                break;
            case MENU:
                menu.draw(g);
                break;
            case INTRO:
                menu.draw(g);
                introOverlay.render(g);
                break;
            case PLAYING:
                playing.draw(g);
                break;

            // ── LEVEL 1 ← KEEP THESE ───────────────────────────
            case BLUE_JEEP_VS_BOSS1:
                blueJeepVsBoss1State.draw(g);
                break;
            case RED_JEEP_VS_BOSS1:
                redJeepVsBoss1State.draw(g);
                break;
            case GREEN_JEEP_VS_BOSS1:
                greenJeepVsBoss1State.draw(g);
                break;

            // ── LEVEL 2 ← ADD THESE ────────────────────────────
            case BLUE_JEEP_VS_BOSS2:
                blueJeepVsBoss2State.draw(g);
                break;
            case RED_JEEP_VS_BOSS2:
                redJeepVsBoss2State.draw(g);
                break;
            case GREEN_JEEP_VS_BOSS2:
                greenJeepVsBoss2State.draw(g);
                break;

            // ── LEVEL 3 ← KEEP THESE ───────────────────────────
            case BLUE_JEEP_VS_BOSS3:
                blueJeepVsBoss3State.draw(g);
                break;
            case RED_JEEP_VS_BOSS3:
                redJeepVsBoss3State.draw(g);
                break;
            case GREEN_JEEP_VS_BOSS3:
                greenJeepVsBoss3State.draw(g);
                break;

            case OPTIONS:
                options.draw(g);
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

    public int getCurrentGameLevel() {
        return currentGameLevel;
    }
    public void returnToPlayingAfterBossFight() {
        System.out.println("[Game] Returning to Playing state");
        GameStates.state = GameStates.PLAYING;
        // Playing.restartGame() will be called to reset for next level
    }



    public BlueJeepVsBoss1State getBlueJeepVsBoss1State() {
        return blueJeepVsBoss1State;
    }

    public RedJeepVsBoss1State getRedJeepVsBoss1State() {
        return redJeepVsBoss1State;
    }

    public GreenJeepVsBoss1State getGreenJeepVsBoss1State() {
        return greenJeepVsBoss1State;
    }
    public BlueJeepVsBoss2State getBlueJeepVsBoss2State() {
        return blueJeepVsBoss2State;
    }

    public RedJeepVsBoss2State getRedJeepVsBoss2State() {
        return redJeepVsBoss2State;
    }

    public GreenJeepVsBoss2State getGreenJeepVsBoss2State() {
        return greenJeepVsBoss2State;
    }
    public BlueJeepVsBoss3State getBlueJeepVsBoss3State() {
        return blueJeepVsBoss3State;
    }

    public RedJeepVsBoss3State getRedJeepVsBoss3State() {
        return redJeepVsBoss3State;
    }

    public GreenJeepVsBoss3State getGreenJeepVsBoss3State() {
        return greenJeepVsBoss3State;
    }
}