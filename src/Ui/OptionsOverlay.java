package Ui;

import gameStates.GameStates;
import gameStates.Options;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static utils.Constants.UI.PauseButtons.SOUND_SIZE;
import static utils.Constants.UI.URMButtons.URM_SIZE;
import static utils.Constants.UI.VolumeButtons.SLIDER_WIDTH;
import static utils.Constants.UI.VolumeButtons.VOLUME_HEIGHT;

public class OptionsOverlay {

    private static final Color BACKDROP_COLOR = new Color(0, 0, 0, 120);
    private static final int SOUND_X = 450;
    private static final int MUSIC_Y = 60;
    private static final int SFX_Y = 106;
    private static final int VOLUME_X = 293;
    private static final int VOLUME_Y = 198;
    private static final int REPLAY_X = 374;
    private static final int REPLAY_Y = 245;

    private final Options options;
    private BufferedImage backgroundImg;
    private int bgX, bgY, bgW, bgH;

    private AudioControlsPanel audioControls;
    private UrmButton replayButton;

    public OptionsOverlay(Options options) {
        this.options = options;
        loadBackground();
        createAudioControls();
        createReplayButton();
    }

    private void loadBackground() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.OPTIONS_BACKGROUND);
        bgW = (int) (backgroundImg.getWidth() * Game.SCALE);
        bgH = (int) (backgroundImg.getHeight() * Game.SCALE);
        bgX = Game.GAME_WIDTH / 2 - bgW / 2;
        bgY = (int) (25 * Game.SCALE);
    }

    private void createAudioControls() {
        int soundX = (int) (SOUND_X * Game.SCALE);
        int musicY = (int) (MUSIC_Y * Game.SCALE);
        int sfxY = (int) (SFX_Y * Game.SCALE);
        SoundButton musicButton = new SoundButton(soundX, musicY, SOUND_SIZE, SOUND_SIZE);
        SoundButton sfxButton = new SoundButton(soundX, sfxY, SOUND_SIZE, SOUND_SIZE);
        int volumeX = (int) (VOLUME_X * Game.SCALE);
        int volumeY = (int) (VOLUME_Y * Game.SCALE);
        VolumeButton volumeButton = new VolumeButton(volumeX, volumeY, SLIDER_WIDTH, VOLUME_HEIGHT);
        audioControls = new AudioControlsPanel(getAudioPlayer(), musicButton, sfxButton, volumeButton);
    }

    private void createReplayButton() {
        int replayX = (int) (REPLAY_X * Game.SCALE);
        int buttonY = (int) (REPLAY_Y * Game.SCALE);
        replayButton = new UrmButton(replayX, buttonY, URM_SIZE, URM_SIZE, 1);
    }

    public void update() {
        audioControls.update();
        replayButton.update();
    }

    public void draw(Graphics g) {
        g.setColor(BACKDROP_COLOR);
        g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
        g.drawImage(backgroundImg, bgX, bgY, bgW, bgH, null);
        audioControls.draw(g);
        replayButton.draw(g);
    }

    public void mouseDragged(MouseEvent e) {
        audioControls.mouseDragged(e);
    }

    public void mousePressed(MouseEvent e) {
        audioControls.mousePressed(e);
        if (isIn(e, replayButton)) replayButton.setMousePressed(true);
    }

    public void mouseReleased(MouseEvent e) {
        boolean handledByAudio = audioControls.mouseReleased(e);
        if (!handledByAudio && isIn(e, replayButton)) {
            if (replayButton.isMousePressed())
                GameStates.state = GameStates.MENU;
        }

        audioControls.resetBools();
        replayButton.resetBools();
    }

    public void mouseMoved(MouseEvent e) {
        audioControls.mouseMoved(e);
        replayButton.setMouseOver(false);

        if (isIn(e, replayButton)) replayButton.setMouseOver(true);
    }

    private boolean isIn(MouseEvent e, PauseButton button) {
        return button.getBounds().contains(e.getX(), e.getY());
    }

    private utils.AudioPlayer getAudioPlayer() {
        return options.getGame().getAudioPlayer();
    }
}
