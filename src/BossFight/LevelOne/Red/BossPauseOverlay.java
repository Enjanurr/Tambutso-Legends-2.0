package BossFight.LevelOne.Red;

import Ui.PauseButton;
import Ui.SoundButton;
import Ui.UrmButton;
import Ui.VolumeButton;
import gameStates.GameStates;
import main.Game;
import utils.AudioPlayer;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static utils.Constants.UI.PauseButtons.*;
import static utils.Constants.UI.URMButtons.*;
import static utils.Constants.UI.VolumeButtons.*;

/**
 * Pause overlay for the boss fight.
 * Reuses the same sprite assets as PauseOverlay but routes button
 * callbacks into BossFightState instead of Playing.
 *
 * Button layout (mirrors PauseOverlay):
 *   Resume  — 295 × SCALE, 325 × SCALE
 *   Restart — 374 × SCALE, 325 × SCALE
 *   Home    — 451 × SCALE, 325 × SCALE
 */
public class BossPauseOverlay {

    private final RedJeepVsBoss1State redJeepVsBoss1State;

    private BufferedImage backgroundImg;
    private int bgX, bgY, bgW, bgH;

    private SoundButton  musicButton, sfxButton;
    private UrmButton    resumeBtn, restartBtn, menuBtn;
    private VolumeButton volumeButton;

    public BossPauseOverlay(RedJeepVsBoss1State blueJeepVsBoss1State) {
        this.redJeepVsBoss1State = blueJeepVsBoss1State;
        loadBackground();
        createSoundButtons();
        createUrmButtons();
        createVolumeButton();
    }

    // ── Asset loading ─────────────────────────────────────────
    private void loadBackground() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PAUSE_BACKGROUNDS);
        bgW = (int)(backgroundImg.getWidth()  * Game.SCALE);
        bgH = (int)(backgroundImg.getHeight() * Game.SCALE);
        bgX = Game.GAME_WIDTH / 2 - bgW / 2;
        bgY = (int)(25 * Game.SCALE);
    }

    private void createSoundButtons() {
        int soundX = (int)(450 * Game.SCALE);
        int musicY = (int)(140 * Game.SCALE);
        int sfxY   = (int)(186 * Game.SCALE);
        musicButton = new SoundButton(soundX, musicY, SOUND_SIZE, SOUND_SIZE);
        sfxButton   = new SoundButton(soundX, sfxY,   SOUND_SIZE, SOUND_SIZE);
        musicButton.setMuted(getAudioPlayer().isMuted());
    }

    private void createUrmButtons() {
        int resumeX  = (int)(295 * Game.SCALE);
        int restartX = (int)(374 * Game.SCALE);
        int homeX    = (int)(451 * Game.SCALE);
        int bY       = (int)(325 * Game.SCALE);

        resumeBtn  = new UrmButton(resumeX,  bY, URM_SIZE, URM_SIZE, 0); // row 0 = resume
        restartBtn = new UrmButton(restartX, bY, URM_SIZE, URM_SIZE, 1); // row 1 = restart
        menuBtn    = new UrmButton(homeX,    bY, URM_SIZE, URM_SIZE, 2); // row 2 = home
    }

    private void createVolumeButton() {
        int vX = (int)(293 * Game.SCALE);
        int vY = (int)(278 * Game.SCALE);
        volumeButton = new VolumeButton(vX, vY, SLIDER_WIDTH, VOLUME_HEIGHT);
        volumeButton.setValue(getAudioPlayer().getVolume());
    }

    // ── Update / Draw ─────────────────────────────────────────
    public void update() {
        musicButton.update();
        sfxButton.update();
        resumeBtn.update();
        restartBtn.update();
        menuBtn.update();
        volumeButton.update();
    }

    public void draw(Graphics g) {
        g.drawImage(backgroundImg, bgX, bgY, bgW, bgH, null);
        musicButton.draw(g);
        sfxButton.draw(g);
        resumeBtn.draw(g);
        restartBtn.draw(g);
        menuBtn.draw(g);
        volumeButton.draw(g);
    }

    // ── Mouse input ───────────────────────────────────────────
    public void mouseDragged(MouseEvent e) {
        if (volumeButton.isMousePressed()) {
            volumeButton.changeX(e.getX());
            getAudioPlayer().setVolume(volumeButton.getValue());
            if (musicButton.isMuted()) {
                musicButton.setMuted(false);
                getAudioPlayer().setMuted(false);
            }
        }
    }

    public void mousePressed(MouseEvent e) {
        if      (isIn(e, musicButton))  musicButton.setMousePressed(true);
        else if (isIn(e, sfxButton))    sfxButton.setMousePressed(true);
        else if (isIn(e, resumeBtn))    resumeBtn.setMousePressed(true);
        else if (isIn(e, restartBtn))   restartBtn.setMousePressed(true);
        else if (isIn(e, menuBtn))      menuBtn.setMousePressed(true);
        else if (isIn(e, volumeButton)) volumeButton.setMousePressed(true);
    }

    public void mouseReleased(MouseEvent e) {
        if (isIn(e, musicButton)) {
            if (musicButton.isMousePressed()) {
                boolean muted = !musicButton.isMuted();
                musicButton.setMuted(muted);
                getAudioPlayer().setMuted(muted);
            }

        } else if (isIn(e, sfxButton)) {
            if (sfxButton.isMousePressed())
                sfxButton.setMuted(!sfxButton.isMuted());

        } else if (isIn(e, resumeBtn)) {
            if (resumeBtn.isMousePressed())
                redJeepVsBoss1State.unpause();          // resume

        } else if (isIn(e, restartBtn)) {
            if (restartBtn.isMousePressed())
                redJeepVsBoss1State.fullReset();        // full boss fight restart

        } else if (isIn(e, menuBtn)) {
            if (menuBtn.isMousePressed()) {
                redJeepVsBoss1State.unpause();
                GameStates.state = GameStates.MENU; // back to main menu
            }
        }

        musicButton.resetBools();
        sfxButton.resetBools();
        resumeBtn.resetBools();
        restartBtn.resetBools();
        menuBtn.resetBools();
        volumeButton.resetBools();
    }

    public void mouseMoved(MouseEvent e) {
        musicButton.setMouseOver(false);
        sfxButton.setMouseOver(false);
        resumeBtn.setMouseOver(false);
        restartBtn.setMouseOver(false);
        menuBtn.setMouseOver(false);
        volumeButton.setMouseOver(false);

        if      (isIn(e, musicButton))  musicButton.setMouseOver(true);
        else if (isIn(e, sfxButton))    sfxButton.setMouseOver(true);
        else if (isIn(e, resumeBtn))    resumeBtn.setMouseOver(true);
        else if (isIn(e, restartBtn))   restartBtn.setMouseOver(true);
        else if (isIn(e, menuBtn))      menuBtn.setMouseOver(true);
        else if (isIn(e, volumeButton)) volumeButton.setMouseOver(true);
    }

    private boolean isIn(MouseEvent e, PauseButton b) {
        return b.getBounds().contains(e.getX(), e.getY());
    }

    private AudioPlayer getAudioPlayer() {
        return redJeepVsBoss1State.getGame().getAudioPlayer();
    }
}
