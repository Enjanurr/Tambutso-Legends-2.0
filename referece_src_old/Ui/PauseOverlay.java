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

public class PauseOverlay {

    private Playing playing;
    private BufferedImage backgroundImg;
    private int bgX, bgY, bgW, bgH;
    private AudioControlsPanel audioControls;

    private UrmButton unPausedB;
    private UrmButton replayB;
    private UrmButton menuB;

    public PauseOverlay(Playing playing) {
        this.playing = playing;
        loadBackground();
        createUrmButtons();
        createAudioControls();
    }

    private void loadBackground() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PAUSE_BACKGROUNDS);
        bgW = (int)(backgroundImg.getWidth()  * Game.SCALE);
        bgH = (int)(backgroundImg.getHeight() * Game.SCALE);
        bgX = Game.GAME_WIDTH / 2 - bgW / 2;
        bgY = (int)(25 * Game.SCALE);
    }

    private void createAudioControls() {
        int soundX = (int)(450 * Game.SCALE);
        int musicY = (int)(140 * Game.SCALE);
        int sfxY   = (int)(186 * Game.SCALE);
        SoundButton musicButton = new SoundButton(soundX, musicY, SOUND_SIZE, SOUND_SIZE);
        SoundButton sfxButton   = new SoundButton(soundX, sfxY,   SOUND_SIZE, SOUND_SIZE);

        int vX = (int)(293 * Game.SCALE);
        int vY = (int)(278 * Game.SCALE);
        VolumeButton volumeButton = new VolumeButton(vX, vY, SLIDER_WIDTH, VOLUME_HEIGHT);
        audioControls = new AudioControlsPanel(getAudioPlayer(), musicButton, sfxButton, volumeButton);
    }

    private void createUrmButtons() {
        int menuX = (int)(295 * Game.SCALE);
        int replayX  = (int)(374 * Game.SCALE);
        int unPauseX    = (int)(451 * Game.SCALE);
        int bY       = (int)(325 * Game.SCALE);

        unPausedB = new UrmButton(unPauseX, bY, URM_SIZE, URM_SIZE, 0);
        replayB   = new UrmButton(replayX,  bY, URM_SIZE, URM_SIZE, 1);
        menuB     = new UrmButton(menuX,    bY, URM_SIZE, URM_SIZE, 2);
    }

    public void update() {
        audioControls.update();
        unPausedB.update();
        replayB.update();
        menuB.update();
    }

    public void draw(Graphics g) {
        g.drawImage(backgroundImg, bgX, bgY, bgW, bgH, null);
        audioControls.draw(g);
        unPausedB.draw(g);
        replayB.draw(g);
        menuB.draw(g);
    }

    public void mouseDragged(MouseEvent e) {
        audioControls.mouseDragged(e);
    }

    public void mousePressed(MouseEvent e) {
        audioControls.mousePressed(e);
        if      (isIn(e, unPausedB))    unPausedB.setMousePressed(true);
        else if (isIn(e, replayB))      replayB.setMousePressed(true);
        else if (isIn(e, menuB))        menuB.setMousePressed(true);
    }

    public void mouseReleased(MouseEvent e) {
        boolean handledByAudio = audioControls.mouseReleased(e);
        if (!handledByAudio && isIn(e, unPausedB)) {
            // ── RESUME ──────────────────────────────────────
            if (unPausedB.isMousePressed())
                playing.unPauseGame();

        } else if (!handledByAudio && isIn(e, replayB)) {
            // ── RESTART ─────────────────────────────────────
            if (replayB.isMousePressed())
                playing.restartGame();

        } else if (!handledByAudio && isIn(e, menuB)) {
            // ── HOME ────────────────────────────────────────
            if (menuB.isMousePressed()) {
                GameStates.state = GameStates.MENU;
                playing.unPauseGame();
            }
        }

        audioControls.resetBools();
        unPausedB.resetBools();
        replayB.resetBools();
        menuB.resetBools();
    }

    public void mouseMoved(MouseEvent e) {
        audioControls.mouseMoved(e);
        unPausedB.setMouseOver(false);
        replayB.setMouseOver(false);
        menuB.setMouseOver(false);

        if      (isIn(e, unPausedB))    unPausedB.setMouseOver(true);
        else if (isIn(e, replayB))      replayB.setMouseOver(true);
        else if (isIn(e, menuB))        menuB.setMouseOver(true);
    }

    private boolean isIn(MouseEvent e, PauseButton b) {
        return b.getBounds().contains(e.getX(), e.getY());
    }
    private utils.AudioPlayer getAudioPlayer() {
        return playing.getGame().getAudioPlayer();
    }
}
