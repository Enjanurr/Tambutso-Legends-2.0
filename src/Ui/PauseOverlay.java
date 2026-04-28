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

    // =========================================================
    // OVERLAY SCALE & POSITION  ← ADJUST
    // =========================================================
    private static final float OVERLAY_SCALE = 0.95f;      // 0.5 = half, 1.5 = larger
    private static final int   OVERLAY_Y_OFFSET = 25;      // Pixels from top (× Game.SCALE)

    // =========================================================
    // BUTTON POSITIONS (relative to background)  ← ADJUST
    // =========================================================
    // URM Buttons (Resume, Restart, Home)
    private static final int   BTN_MENU_X = 295;           // Home button X (original)
    private static final int   BTN_REPLAY_X = 374;         // Restart button X
    private static final int   BTN_UNPAUSE_X = 451;        // Resume button X
    private static final int   BTN_Y = 318;                // All buttons Y

    // =========================================================
    // AUDIO CONTROLS POSITIONS  ← ADJUST
    // =========================================================
    // Sound buttons (Music, SFX)
    private static final int   SOUND_X = 450;              // Sound buttons X
    private static final int   MUSIC_Y = 136;              // Music button Y
    private static final int   SFX_Y = 180;                // SFX button Y

    // Volume slider
    private static final int   VOLUME_X = 293;             // Volume slider X
    private static final int   VOLUME_Y = 263;             // Volume slider Y
    // =========================================================

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
        bgW = (int)(backgroundImg.getWidth() * Game.SCALE * OVERLAY_SCALE);
        bgH = (int)(backgroundImg.getHeight() * Game.SCALE * OVERLAY_SCALE);
        bgX = Game.GAME_WIDTH / 2 - bgW / 2;
        bgY = (int)(OVERLAY_Y_OFFSET * Game.SCALE);
    }

    private void createAudioControls() {
        int soundX = (int)(SOUND_X * Game.SCALE);
        int musicY = (int)(MUSIC_Y * Game.SCALE);
        int sfxY   = (int)(SFX_Y * Game.SCALE);
        SoundButton musicButton = new SoundButton(soundX, musicY, SOUND_SIZE, SOUND_SIZE);
        SoundButton sfxButton   = new SoundButton(soundX, sfxY,   SOUND_SIZE, SOUND_SIZE);

        int vX = (int)(VOLUME_X * Game.SCALE);
        int vY = (int)(VOLUME_Y * Game.SCALE);
        VolumeButton volumeButton = new VolumeButton(vX, vY, SLIDER_WIDTH, VOLUME_HEIGHT);
        audioControls = new AudioControlsPanel(getAudioPlayer(), musicButton, sfxButton, volumeButton);
    }

    private void createUrmButtons() {
        int menuX = (int)(BTN_MENU_X * Game.SCALE);
        int replayX  = (int)(BTN_REPLAY_X * Game.SCALE);
        int unPauseX = (int)(BTN_UNPAUSE_X * Game.SCALE);
        int bY       = (int)(BTN_Y * Game.SCALE);

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
            // Returns to menu WITHOUT resetting hasActiveGame — player can resume later
            if (menuB.isMousePressed()) {
                playing.getGame().setLastActiveGameState(GameStates.PLAYING);
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
