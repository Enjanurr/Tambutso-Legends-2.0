package Ui;

import utils.AudioPlayer;

import java.awt.*;
import java.awt.event.MouseEvent;

public class AudioControlsPanel {

    private final AudioPlayer audioPlayer;
    private final SoundButton musicButton;
    private final SoundButton sfxButton;
    private final VolumeButton volumeButton;

    public AudioControlsPanel(AudioPlayer audioPlayer,
                              SoundButton musicButton,
                              SoundButton sfxButton,
                              VolumeButton volumeButton) {
        this.audioPlayer = audioPlayer;
        this.musicButton = musicButton;
        this.sfxButton = sfxButton;
        this.volumeButton = volumeButton;
        this.musicButton.setMuted(audioPlayer.isMusicMuted());
        this.sfxButton.setMuted(audioPlayer.isSfxMuted());
        this.volumeButton.setValue(audioPlayer.getMusicVolume());
    }

    public void update() {
        musicButton.update();
        sfxButton.update();
        volumeButton.update();
    }

    public void draw(Graphics g) {
        musicButton.draw(g);
        sfxButton.draw(g);
        volumeButton.draw(g);
    }

    public void mouseDragged(MouseEvent e) {
        if (!volumeButton.isMousePressed()) return;

        volumeButton.changeX(e.getX());
        audioPlayer.setMusicVolume(volumeButton.getValue());
        if (musicButton.isMuted()) {
            musicButton.setMuted(false);
            audioPlayer.setMusicMuted(false);
        }
    }

    public void mousePressed(MouseEvent e) {
        if (isIn(e, musicButton)) {
            musicButton.setMousePressed(true);
            boolean muted = !musicButton.isMuted();
            musicButton.setMuted(muted);
            audioPlayer.setMusicMuted(muted);
        }
        else if (isIn(e, sfxButton)) sfxButton.setMousePressed(true);
        else if (isIn(e, volumeButton)) volumeButton.setMousePressed(true);
    }

    public boolean mouseReleased(MouseEvent e) {
        if (isIn(e, musicButton)) {
            return true;
        }

        if (isIn(e, sfxButton)) {
            if (sfxButton.isMousePressed()) {
                boolean muted = !sfxButton.isMuted();
                sfxButton.setMuted(muted);
                audioPlayer.setSfxMuted(muted);
            }
            return true;
        }

        return isIn(e, volumeButton);
    }

    public void mouseMoved(MouseEvent e) {
        musicButton.setMouseOver(false);
        sfxButton.setMouseOver(false);
        volumeButton.setMouseOver(false);

        if (isIn(e, musicButton)) musicButton.setMouseOver(true);
        else if (isIn(e, sfxButton)) sfxButton.setMouseOver(true);
        else if (isIn(e, volumeButton)) volumeButton.setMouseOver(true);
    }

    public void resetBools() {
        musicButton.resetBools();
        sfxButton.resetBools();
        volumeButton.resetBools();
    }

    private boolean isIn(MouseEvent e, PauseButton button) {
        return button.getBounds().contains(e.getX(), e.getY());
    }
}
