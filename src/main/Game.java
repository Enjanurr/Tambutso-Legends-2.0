package main;

import java.awt.Graphics;

import BossFight.LevelThree.Blue.BlueJeepVsBoss3State;
import BossFight.LevelThree.Green.GreenJeepVsBoss3State;
import BossFight.LevelThree.Red.RedJeepVsBoss3State;
import BossFight.LevelTwo.Blue.BlueJeepVsBoss2State;
import BossFight.LevelTwo.Green.GreenJeepVsBoss2State;
import entities.DriverProfile;
import gameStates.*;
import Ui.IntroOverlay;

import BossFight.LevelOne.Blue.BlueJeepVsBoss1State;
import BossFight.LevelOne.Red.RedJeepVsBoss1State;
import BossFight.LevelOne.Green.GreenJeepVsBoss1State;

import BossFight.LevelTwo.Red.RedJeepVsBoss2State;
import utils.AudioPlayer;
import gameStates.BossFightMatchmaker;

import static gameStates.GameStates.RED_JEEP_VS_BOSS2;
import static gameStates.GameStates.GREEN_JEEP_VS_BOSS2;

public class Game implements Runnable {
    private GameWindow    gameWindow;
    private GamePanel     gamePanel;
    private Thread        gameThread;
    private final int     FPS_SET = 120;
    private final int     UPS_SET = 200;

    private Playing       playing;
    private Menu          menu;
    private Options       options;

    private final AudioPlayer audioPlayer;
    private GameIntroState gameIntroState;
    private CharSelectState charSelectState;

    private String activeMusicTrack;

    // ── Level 1 Boss States ───────────────────────────────────
    private BlueJeepVsBoss1State blueJeepVsBoss1State;
    private RedJeepVsBoss1State redJeepVsBoss1State;
    private GreenJeepVsBoss1State greenJeepVsBoss1State;

    // ── Level 2 Boss States ───────────────────────────────────
    private BlueJeepVsBoss2State blueJeepVsBoss2State;
    private RedJeepVsBoss2State redJeepVsBoss2State;
    private GreenJeepVsBoss2State greenJeepVsBoss2State;

    // ── Level 3 Boss States ───────────────────────────────────
    private BlueJeepVsBoss3State blueJeepVsBoss3State;
    private RedJeepVsBoss3State redJeepVsBoss3State;
    private GreenJeepVsBoss3State greenJeepVsBoss3State;

    public final static int   TILES_DEFAULT_SIZE = 20;
    public final static float SCALE              = 2f;
    public final static int   TILES_SIZE         = (int)(TILES_DEFAULT_SIZE * SCALE);
    public final static int   TILES_IN_WIDTH     = 40;
    public final static int   TILES_IN_HEIGHT    = 20;
    public final static int   GAME_WIDTH         = TILES_SIZE * TILES_IN_WIDTH;
    public final static int   GAME_HEIGHT        = TILES_SIZE * TILES_IN_HEIGHT;
    private DriverProfile selectedDriver;
    // ── Boss Fight Matchmaker ─────────────────────────────────
    private BossFightMatchmaker bossFightMatchmaker;
    private int currentBossLevel = 1;

    // ── Game State Persistence ─────────────────────────────────
    /** True when an active game exists that can be resumed (after IntroOverlay completes). */
    private boolean hasActiveGame = false;

    /** Stores the last active game state (PLAYING or boss fight) for resume after menu. */
    private GameStates lastActiveGameState = null;


    public Game() {
        audioPlayer = new AudioPlayer();
        gamePanel    = new GamePanel(this);
        initClasses();
        gameWindow   = new GameWindow(gamePanel);
        reclaimInputFocus();
        syncMusicToState();
        startGameLoop();
    }


    public void setSelectedDriver(DriverProfile driver) {
        this.selectedDriver = driver;
    }

    public DriverProfile getSelectedDriver() {
        return selectedDriver;
    }

    // ── Game State Persistence ─────────────────────────────────
    public boolean hasActiveGame() {
        return hasActiveGame;
    }

    public void setHasActiveGame(boolean active) {
        this.hasActiveGame = active;
        if (active) {
            this.lastActiveGameState = GameStates.state;
        }
    }

    /**
     * Explicitly set the last active game state for resume purposes.
     * Called when entering boss fights or other special game states.
     */
    public void setLastActiveGameState(GameStates state) {
        this.lastActiveGameState = state;
        this.hasActiveGame = true;
    }

    /**
     * Called by Menu Play button.
     * Resumes existing game if available, otherwise starts new game flow.
     * Handles both normal gameplay and boss fight states.
     */
    public void startOrResumeGame() {
        if (hasActiveGame && lastActiveGameState != null) {
            System.out.println("[Game] Resuming active game: " + lastActiveGameState);
            GameStates.state = lastActiveGameState;
        } else {
            System.out.println("[Game] No active game — starting new game flow");
            startCharSelect();
        }
    }

    /**
     * Resets game state for a fresh start.
     * Called on death or when player chooses to restart.
     */
    public void resetGameState() {
        hasActiveGame = false;
        lastActiveGameState = null;
        selectedDriver = null;
        currentBossLevel = 1;
        playing.restartGame();
        System.out.println("[Game] Game state reset for fresh start");
    }

    private void initClasses() {
        System.out.println("[Game] Initializing classes...");
        menu          = new Menu(this);
        options       = new Options(this);
        playing       = new Playing(this);
        gameIntroState = new GameIntroState(this);
        charSelectState = new CharSelectState(this);
        System.out.println("[Game] All classes initialized");

        // ── Initialize Level 1 BossFightStates ─────────────────
        blueJeepVsBoss1State = new BlueJeepVsBoss1State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        redJeepVsBoss1State = new RedJeepVsBoss1State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        greenJeepVsBoss1State = new GreenJeepVsBoss1State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        // ── Initialize Level 2 BossFightStates ─────────────────
        blueJeepVsBoss2State = new BlueJeepVsBoss2State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        redJeepVsBoss2State = new RedJeepVsBoss2State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        greenJeepVsBoss2State = new GreenJeepVsBoss2State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        // ── Initialize Level 3 BossFightStates ─────────────────
        blueJeepVsBoss3State = new BlueJeepVsBoss3State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        redJeepVsBoss3State = new RedJeepVsBoss3State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        greenJeepVsBoss3State = new GreenJeepVsBoss3State(this,
                playing.getPlayer(),
                playing.getHealthBar());

        // ── Initialize matchmaker ──────────────────────────────
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
        System.out.println("[Game] Setting state to CHAR_SELECT");
        GameStates.state = GameStates.CHAR_SELECT;
        reclaimInputFocus();
    }

    /**
     * Called by CharSelectState after confirming a driver.
     */
    public void startIntroOverlay() {
        System.out.println("[Game] startIntroOverlay() — delegating to Playing");
        playing.resetIntroShown();
        playing.tryShowIntro();
        if (GameStates.state != GameStates.INTRO) {
            System.out.println("[Game] Intro skipped — going straight to PLAYING");
            onIntroComplete();
        }
    }

    /** Called by Playing.onIntroDone() when the overlay finishes. */
    public void onIntroComplete() {
        System.out.println("[Game] Intro complete, moving to PLAYING");

        GameStates.state = GameStates.PLAYING;
        hasActiveGame = true;
        lastActiveGameState = GameStates.PLAYING;
        System.out.println("───────────────────────────────");
        System.out.println("INTRO COMPLETE");
        System.out.println("Selected Driver: " +
                (selectedDriver != null ? selectedDriver.displayName : "NULL"));
        System.out.println("───────────────────────────────");

        if (selectedDriver != null) {
            System.out.println("[Game] Applying driver to Playing");
            playing.applyDriver(selectedDriver);
        }
        reclaimInputFocus();
    }

    /**
     * Legacy method — use startBossFightWithLevel(levelIndex) instead.
     */
    public void startBossFight() {
        System.out.println("═══════════════════════════════");
        System.out.println("🏁 STARTING BOSS FIGHT");
        System.out.println("Selected Driver: " +
                (selectedDriver != null ? selectedDriver.displayName : "NULL"));
        System.out.println("═══════════════════════════════");

        startBossFightWithLevel(currentBossLevel);
    }

    /**
     * Starts boss fight for the selected driver and level.
     */
    public void startBossFightWithLevel(int levelIndex) {
        if (selectedDriver == null) {
            // Attempt to recover driver from Playing
            DriverProfile playingDriver = playing.getCurrentDriver();
            if (playingDriver != null) {
                System.out.println("[Game] Recovered driver from Playing: " + playingDriver.displayName);
                selectedDriver = playingDriver;
            } else {
                System.err.println("❌ [Game] Cannot start boss fight - no driver selected!");
                System.err.println("   Current state: " + GameStates.state);
                System.err.println("   Has active game: " + hasActiveGame);
                GameStates.state = GameStates.MENU;
                return;
            }
        }
        System.out.println("════════════════════════════════════════");
        System.out.println("BEFORE: GameStates.state = " + GameStates.state);

        currentBossLevel = levelIndex;

        System.out.println("🎮 [Game] Starting Boss Fight - Level " + levelIndex +
                " - Driver: " + selectedDriver.displayName);

        if (levelIndex == 1) {
            startLevel1BossFightInternal();
        } else if (levelIndex == 2) {
            startLevel2BossFightInternal();
        } else if (levelIndex == 3) {
            startLevel3BossFightInternal();
        } else {
            System.err.println("❌ [Game] Invalid level: " + levelIndex);
            GameStates.state = GameStates.MENU;
        }

        System.out.println("AFTER: GameStates.state = " + GameStates.state);
        System.out.println("════════════════════════════════════════");
    }

    private void startLevel1BossFightInternal() {
        switch (selectedDriver.id) {
            case "driver_3":
                blueJeepVsBoss1State.resetAll();
                blueJeepVsBoss1State.applyDriverAssets(selectedDriver);
                GameStates.state = GameStates.BLUE_JEEP_VS_BOSS1;
                setLastActiveGameState(GameStates.BLUE_JEEP_VS_BOSS1);
                break;
            case "driver_1":
                redJeepVsBoss1State.resetAll();
                redJeepVsBoss1State.applyDriverAssets(selectedDriver);
                GameStates.state = GameStates.RED_JEEP_VS_BOSS1;
                setLastActiveGameState(GameStates.RED_JEEP_VS_BOSS1);
                break;
            case "driver_2":
                greenJeepVsBoss1State.resetAll();
                greenJeepVsBoss1State.applyDriverAssets(selectedDriver);
                GameStates.state = GameStates.GREEN_JEEP_VS_BOSS1;
                setLastActiveGameState(GameStates.GREEN_JEEP_VS_BOSS1);
                break;
            default:
                System.err.println("❌ Unknown driver id: " + selectedDriver.id);
                GameStates.state = GameStates.MENU;
        }
        reclaimInputFocus();
    }

    private void startLevel2BossFightInternal() {
        switch (selectedDriver.id) {
            case "driver_3":
                blueJeepVsBoss2State.resetAll();
                blueJeepVsBoss2State.applyDriverAssets(selectedDriver);
                GameStates.state = GameStates.BLUE_JEEP_VS_BOSS2;
                setLastActiveGameState(GameStates.BLUE_JEEP_VS_BOSS2);
                break;
            case "driver_1":
                redJeepVsBoss2State.resetAll();
                redJeepVsBoss2State.applyDriverAssets(selectedDriver);
                GameStates.state = RED_JEEP_VS_BOSS2;
                setLastActiveGameState(GameStates.RED_JEEP_VS_BOSS2);
                break;
            case "driver_2":
                greenJeepVsBoss2State.resetAll();
                greenJeepVsBoss2State.applyDriverAssets(selectedDriver);
                GameStates.state = GameStates.GREEN_JEEP_VS_BOSS2;
                setLastActiveGameState(GameStates.GREEN_JEEP_VS_BOSS2);
                break;
            default:
                System.err.println("❌ Unknown driver id: " + selectedDriver.id);
                GameStates.state = GameStates.MENU;
        }
    }

    private void startLevel3BossFightInternal() {
        switch (selectedDriver.id) {
            case "driver_3":
                blueJeepVsBoss3State.resetAll();
                blueJeepVsBoss3State.applyDriverAssets(selectedDriver);
                GameStates.state = GameStates.BLUE_JEEP_VS_BOSS3;
                setLastActiveGameState(GameStates.BLUE_JEEP_VS_BOSS3);
                break;
            case "driver_1":
                redJeepVsBoss3State.resetAll();
                redJeepVsBoss3State.applyDriverAssets(selectedDriver);
                GameStates.state = GameStates.RED_JEEP_VS_BOSS3;
                setLastActiveGameState(GameStates.RED_JEEP_VS_BOSS3);
                break;
            case "driver_2":
                greenJeepVsBoss3State.resetAll();
                greenJeepVsBoss3State.applyDriverAssets(selectedDriver);
                GameStates.state = GameStates.GREEN_JEEP_VS_BOSS3;
                setLastActiveGameState(GameStates.GREEN_JEEP_VS_BOSS3);
                break;
            default:
                System.err.println("❌ Unknown driver id: " + selectedDriver.id);
                GameStates.state = GameStates.MENU;
        }
    }

    public void startLevel1BossFight() { startBossFightWithLevel(1); }
    public void startLevel2BossFight() { startBossFightWithLevel(2); }
    public void startLevel3BossFight() { startBossFightWithLevel(3); }

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
                playing.update();
                break;
            case PLAYING:
                gamePanel.updateFade();
                playing.update();
                break;
            case BLUE_JEEP_VS_BOSS1:
                blueJeepVsBoss1State.update();
                break;
            case RED_JEEP_VS_BOSS1:
                redJeepVsBoss1State.update();
                break;
            case GREEN_JEEP_VS_BOSS1:
                greenJeepVsBoss1State.update();
                break;
            case BLUE_JEEP_VS_BOSS2:
                System.out.println("[Game.update()] Updating BLUE_JEEP_VS_BOSS2");
                blueJeepVsBoss2State.update();
                break;
            case RED_JEEP_VS_BOSS2:
                redJeepVsBoss2State.update();
                break;
            case GREEN_JEEP_VS_BOSS2:
                greenJeepVsBoss2State.update();
                break;
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
            case GAME_INTRO:
                return gameIntroState.hasLogoRevealStarted() ? "menu" : "none";
            case CHAR_SELECT:
                return "menu";
            case MENU:
            case INTRO:
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
            case BLUE_JEEP_VS_BOSS2:
                return blueJeepVsBoss2State.isPaused() ? "menu" : "main";
            case RED_JEEP_VS_BOSS2:
                return redJeepVsBoss2State.isPaused() ? "menu" : "main";
            case GREEN_JEEP_VS_BOSS2:
                return greenJeepVsBoss2State.isPaused() ? "menu" : "main";
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
                playing.draw(g);
                break;
            case PLAYING:
                playing.draw(g);
                break;
            case BLUE_JEEP_VS_BOSS1:
                blueJeepVsBoss1State.draw(g);
                break;
            case RED_JEEP_VS_BOSS1:
                redJeepVsBoss1State.draw(g);
                break;
            case GREEN_JEEP_VS_BOSS1:
                greenJeepVsBoss1State.draw(g);
                break;
            case BLUE_JEEP_VS_BOSS2:
                System.out.println("[Game.render()] Drawing BLUE_JEEP_VS_BOSS2");
                blueJeepVsBoss2State.draw(g);
                break;
            case RED_JEEP_VS_BOSS2:
                redJeepVsBoss2State.draw(g);
                break;
            case GREEN_JEEP_VS_BOSS2:
                greenJeepVsBoss2State.draw(g);
                break;
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

    public void reclaimInputFocus() {
        gamePanel.reclaimFocus();
    }

    // ── Getters ───────────────────────────────────────────────
    public GamePanel       getGamePanel()      { return gamePanel; }
    public Menu            getMenu()           { return menu; }
    public Options         getOptions()        { return options; }
    public Playing         getPlaying()        { return playing; }
    public IntroOverlay    getIntroOverlay()   { return playing.getIntroOverlay(); }
    public GameIntroState  getGameIntroState() { return gameIntroState; }
    public AudioPlayer     getAudioPlayer()    { return audioPlayer; }
    public CharSelectState getCharSelectState() { return charSelectState; }

    public BossFightMatchmaker getBossFightMatchmaker() {
        return bossFightMatchmaker;
    }

    public int getCurrentBossLevel() {
        return currentBossLevel;
    }

    // ── Level 1 Boss State Getters ────────────────────────────
    public BlueJeepVsBoss1State getBlueJeepVsBoss1State() {
        return blueJeepVsBoss1State;
    }

    public RedJeepVsBoss1State getRedJeepVsBoss1State() {
        return redJeepVsBoss1State;
    }

    public GreenJeepVsBoss1State getGreenJeepVsBoss1State() {
        return greenJeepVsBoss1State;
    }

    // ── Level 2 Boss State Getters ────────────────────────────
    public BlueJeepVsBoss2State getBlueJeepVsBoss2State() {
        return blueJeepVsBoss2State;
    }

    public RedJeepVsBoss2State getRedJeepVsBoss2State() {
        return redJeepVsBoss2State;
    }

    public GreenJeepVsBoss2State getGreenJeepVsBoss2State() {
        return greenJeepVsBoss2State;
    }

    // ── Level 3 Boss State Getters ────────────────────────────
    public BlueJeepVsBoss3State getBlueJeepVsBoss3State() {
        return blueJeepVsBoss3State;
    }

    public RedJeepVsBoss3State getRedJeepVsBoss3State() {
        return redJeepVsBoss3State;
    }

    public GreenJeepVsBoss3State getGreenJeepVsBoss3State() {
        return greenJeepVsBoss3State;
    }

    public void setCurrentGameLevel(int level) {
        currentBossLevel = level;
    }

    public void advanceToNextLevel() {
        if (currentBossLevel < 3) {
            currentBossLevel++;
            System.out.println("[Game] Advanced to level " + currentBossLevel);
        }
    }
}

