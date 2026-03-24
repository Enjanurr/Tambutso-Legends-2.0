package Ui;

import gameStates.GameStates;
import gameStates.Playing;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import static utils.Constants.UI.PauseButtons.*;
import static utils.Constants.UI.URMButtons.*;
import static utils.Constants.UI.VolumeButtons.*;
public class PauseOverlay   {
    private Playing playing;
    private BufferedImage backgroundImg;
    private int bgX,bgY,bgW,bgH;
    private SoundButton musicButton , sfxButton;
    private UrmButton menuB,replayB,unPausedB;

    private VolumeButton volumeButton;

    public PauseOverlay(Playing playing){
        this.playing = playing;
      loadBackground();
      createSoundButtons();
      createUrmButtons();
      createVolumeButton();
    }

    private void createVolumeButton() {
        int vX = (int)(293 * Game.SCALE);
        int vY = (int)(278 * Game.SCALE);
        volumeButton = new VolumeButton(vX,vY,SLIDER_WIDTH,VOLUME_HEIGHT);
    }

    // you can adjust the alignment here
    private void createUrmButtons() {
        int menuX = (int)(295 * Game.SCALE);
        int replayX = (int)(374 * Game.SCALE);
        int unPauseX = (int)(451 * Game.SCALE);
        int bY = (int)(325 * Game.SCALE);

        menuB = new UrmButton(menuX, bY, URM_SIZE, URM_SIZE,2);
        replayB = new UrmButton(replayX, bY, URM_SIZE, URM_SIZE,1);
        unPausedB = new UrmButton(unPauseX, bY, URM_SIZE, URM_SIZE,0);
    }

    // you can adjust the scaling here
    private void createSoundButtons() {
        int soundX =    (int) (450 * Game.SCALE);
        int musicY = (int) (140 * Game.SCALE);
        int sfxY = (int) (186 * Game.SCALE);
        musicButton = new SoundButton(soundX,musicY,SOUND_SIZE,SOUND_SIZE);
        sfxButton = new SoundButton(soundX,sfxY,SOUND_SIZE,SOUND_SIZE);

    }

    private void loadBackground() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PAUSE_BACKGROUNDS);
        bgW = (int) (backgroundImg.getWidth() * Game.SCALE);
        bgH = (int) (backgroundImg.getHeight() * Game.SCALE);
        bgX = Game.GAME_WIDTH / 2 - bgW / 2;
        bgY = (int)(25 * Game.SCALE);
    }

    public void update(){
      musicButton.update();
      sfxButton.update();

      menuB.update();
      replayB.update();
      unPausedB.update();

      volumeButton.update();
    }

    public void draw(Graphics g){
        g.drawImage(backgroundImg,bgX,bgY,bgW,bgH,null);
        musicButton.draw(g);
        sfxButton.draw(g);

        menuB.draw(g);
        replayB.draw(g);
        unPausedB.draw(g);

        volumeButton.draw(g);
    }

    public void mouseDragged(MouseEvent e){
       if(volumeButton.isMousePressed()){
           volumeButton.changeX(e.getX());
       }
    }

    public void mousePressed(MouseEvent e) {
       if(isIn(e,musicButton)){
           musicButton.setMousePressed(true);
       }else if(isIn(e,sfxButton)){
           sfxButton.setMousePressed(true);
       } else if(isIn(e, menuB)){
           menuB.setMousePressed(true);
       }else if(isIn(e, replayB)){
           replayB.setMousePressed(true);
       }
       else if(isIn(e, unPausedB)){
           unPausedB.setMousePressed(true);
       } else if(isIn(e, volumeButton)){
           volumeButton.setMousePressed(true);
       }

    }


    public void mouseReleased(MouseEvent e) {
        if(isIn(e,musicButton)){
          if(musicButton.isMousePressed()){
              musicButton.setMuted(!musicButton.isMuted());
          }
        }
        else if(isIn(e,sfxButton)){
            if(sfxButton.isMousePressed()){
                sfxButton.setMuted(!sfxButton.isMuted());
            }
        }

        else if(isIn(e,menuB)){
            if(menuB.isMousePressed()){
                GameStates.state = GameStates.MENU;
                playing.unPauseGame();
            }
        }

        else if(isIn(e,replayB)){
            if(replayB.isMousePressed()){
                System.out.println("Replay level");
            }
        }

        else if(isIn(e,unPausedB)){
            if(unPausedB.isMousePressed()){
                playing.unPauseGame();
            }
        }

        musicButton.resetBools();
        sfxButton.resetBools();
        menuB.resetBools();
        replayB.resetBools();
        unPausedB.resetBools();
        volumeButton.resetBools();
    }


    public void mouseMoved(MouseEvent e) {
        musicButton.setMouseOver(false);
        sfxButton.setMouseOver(false);
        menuB.setMouseOver(false);
        replayB.setMouseOver(false);
        unPausedB.setMouseOver(false);
        volumeButton.setMouseOver(false);

        if(isIn(e,musicButton)){
            musicButton.setMouseOver(true);
        }else if(isIn(e,sfxButton)){
            sfxButton.setMouseOver(true);
        }
        else if(isIn(e,menuB)){
            menuB.setMouseOver(true);
        }
        else if(isIn(e,replayB)){
            replayB.setMouseOver(true);
        }
        else if(isIn(e,unPausedB)){
            unPausedB.setMouseOver(true);
        }
        else if(isIn(e,volumeButton)){
            volumeButton.setMouseOver(true);
        }

    }

    private boolean isIn(MouseEvent e, PauseButton b){
      return (b.getBounds().contains(e.getX(),e.getY()));
    }




}
