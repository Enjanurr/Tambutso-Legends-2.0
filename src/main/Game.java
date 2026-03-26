package main;

import java.awt.Graphics;
import gameStates.GameStates;
import gameStates.Menu;
import gameStates.Playing;

public class Game implements Runnable {
    private GameWindow gameWindow;
    private GamePanel  gamePanel;
    private Thread     gameThread;
    private final int FPS_SET = 120;
    private final int UPS_SET = 200;

    private Playing playing;
    private Menu    menu;

    public final static int   TILES_DEFAULT_SIZE = 20;
    public final static float SCALE              = 2f;
    public final static int   TILES_SIZE         = (int)(TILES_DEFAULT_SIZE * SCALE);
    public final static int   TILES_IN_WIDTH     = 40;
    public final static int   TILES_IN_HEIGHT    = 20;
    public final static int   GAME_WIDTH         = TILES_SIZE * TILES_IN_WIDTH;
    public final static int   GAME_HEIGHT        = TILES_SIZE * TILES_IN_HEIGHT;

    public Game() {
        // GamePanel MUST be created before initClasses()
        // because Playing needs game.getGamePanel() for the Player constructor
        gamePanel = new GamePanel(this);
        initClasses();
        gameWindow = new GameWindow(gamePanel);
        gamePanel.requestFocus();
        startGameLoop();
    }

    private void initClasses() {
        menu    = new Menu(this);
        playing = new Playing(this);
    }

    private void startGameLoop() {
        gameThread = new Thread(this);
        gameThread.start();
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
            case OPTIONS:
                break;
            case QUIT:
            default:
                System.exit(0);
                break;
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
            default:
                break;
        }
    }

    // Called by Player warp callback when jeep crosses right border
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
    public GamePanel getGamePanel() { return gamePanel; }
    public Menu      getMenu()      { return menu; }
    public Playing   getPlaying()   { return playing; }
}