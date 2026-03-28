package gameStates;

import Ui.PauseOverlay;
import entities.Player;
import levels.LevelManager;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

import static utils.Constants.Environment.*;

public class Playing extends State implements StateMethods{

    private Player player;
    private LevelManager levelManager;
    private PauseOverlay pauseOverlay;
    private boolean paused = false;
    private int xLvlOffset;
    private int leftBorder = (int)(0.2 * Game.GAME_WIDTH);
    private int rightBorder = (int) (0.5 * Game.GAME_WIDTH);
    private int levelTilesWide = LoadSave.GetLevelData()[0].length;
    private int maxTilesOffset = levelTilesWide - Game.TILES_IN_WIDTH;

    private int maxLevelOffSetX = maxTilesOffset * Game.TILES_SIZE;

    private BufferedImage backgroundImg,bigClouds,smallClouds, busStop;
    private int[] smallCloudsPos;
    private Random rnd = new Random();

    public Playing(Game game) {
        super(game);
        initClasses();
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PLAYING_BACKGROUND_IMG);
        bigClouds = LoadSave.getSpriteAtlas(LoadSave.BIG_CLOUDS);
        smallClouds = LoadSave.getSpriteAtlas(LoadSave.SMALL_CLOUDS);
        busStop = LoadSave.getSpriteAtlas(LoadSave.BUS_STOP);

        smallCloudsPos = new int[8];
        for(int i = 0 ; i < smallCloudsPos.length;i++){
            smallCloudsPos[i] = (int)(20 * Game.SCALE) + rnd.nextInt((int)(100 * Game.SCALE));
        }
    }
    private void initClasses() {
        levelManager = new LevelManager(game);
        player = new Player(200, 520, (int) (110 * Game.SCALE), (int) (40 * Game.SCALE));
        player.loadLvlData(levelManager.getCurrentLevel().getLevelData());

        pauseOverlay = new PauseOverlay(this);//
    }




    @Override
    public void update() {
        if(!paused){
            levelManager.update();
            player.update();
            checkCloseToBorder();
        }else{
            pauseOverlay.update();
        }
    }

    private void checkCloseToBorder() {
      int playerX = (int) player.getHitBox().x;
      int diff = playerX - xLvlOffset;

      if(diff > rightBorder){
           xLvlOffset += diff - rightBorder;
      }else if(diff < leftBorder){
          xLvlOffset  += diff - leftBorder;
      }

      if(xLvlOffset > maxLevelOffSetX){
          xLvlOffset = maxLevelOffSetX;
      }else if(xLvlOffset < 0 ){
            xLvlOffset = 0;
      }
    }

    @Override
    public void draw(Graphics g) {
        g.drawImage(backgroundImg,0,0,Game.GAME_WIDTH,Game.GAME_HEIGHT,null);
        drawClouds(g);
        levelManager.draw(g,xLvlOffset);
        player.render(g, xLvlOffset);

        g.drawImage(busStop, 50, 225,  BUS_STOP_WIDTH, BUS_STOP_HEIGHT, null);
        if(paused){
            g.setColor(new Color(0,0,0,150));
            g.fillRect(0,0, Game.GAME_WIDTH,Game.GAME_HEIGHT);
            pauseOverlay.draw(g);
        }

    }

    private void drawClouds(Graphics g) {
        for(int i = 0 ; i  < 3 ;i++){
            g.drawImage(bigClouds,i * BIG_CLOUD_WIDTH - (int)(xLvlOffset * 0.3),(int)(40 * Game.SCALE),BIG_CLOUD_WIDTH,BIG_CLOUD_HEIGHT,null);
        }

        for(int i = 0 ; i < smallCloudsPos.length;i++){
            g.drawImage(smallClouds,SMALL_CLOUD_WIDTH * 4 * i - (int)(xLvlOffset * 0.7  ),smallCloudsPos[i],SMALL_CLOUD_WIDTH,SMALL_CLOUD_HEIGHT,null);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getButton() == MouseEvent.BUTTON1){
            player.setAttacking(true);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if(paused){
          pauseOverlay.mousePressed(e);

      }
    }

    public void mouseDragged(MouseEvent e){
         if(paused){
             pauseOverlay.mouseDragged(e);
         }
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        if(paused){
            pauseOverlay.mouseReleased(e);

        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if(paused){
            pauseOverlay.mouseMoved(e);

        }
    }

    public void unPauseGame(){
        paused = false;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
               player.setUp(true);
                break;
            case KeyEvent.VK_S:
                player.setDown(true);
                break;
            case KeyEvent.VK_A:
                player.setLeft(true);
                break;
            case KeyEvent.VK_D:
                player.setRight(true);
                break;
            case KeyEvent.VK_ESCAPE:
                paused = !paused;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W:
                player.setUp(false);
                break;
            case KeyEvent.VK_S:
                player.setDown(false);
                break;
            case KeyEvent.VK_A:
                player.setLeft(false);
                break;
            case KeyEvent.VK_D:
                player.setRight(false);
                break;
        }
    }



    public void windowFocusLost(){
        player.resetDirBooleans();

    }
    public Player getPlayer(){
        return player;
    }
}
