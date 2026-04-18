package utils;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AudioPlayer {

    private static final String MENU_THEME = "/audio/music/theme_menu.wav";
    private static final String MAIN_THEME = "/audio/music/theme_playing.wav";

    private Clip clip;
    private String currentTrack;
    private float volume = 0.5f;
    private boolean muted;
    private final Map<String, Integer> savedFramePositions = new HashMap<>();

    public void playMenuTheme() {
        playLoop(MENU_THEME);
    }

    public void playMainTheme() {
        playLoop(MAIN_THEME);
    }

    public void stop() {
        rememberCurrentTrackPosition();
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
        currentTrack = null;
    }

    private void playLoop(String resourcePath) {
        if (resourcePath.equals(currentTrack) && clip != null && clip.isOpen()) {
            if (!clip.isRunning()) {
                clip.setFramePosition(getSavedFramePosition(resourcePath, clip));
                applyPlaybackSettings(clip);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
            return;
        }

        stop();

        Clip newClip = null;
        try (InputStream rawStream = AudioPlayer.class.getResourceAsStream(resourcePath)) {
            if (rawStream == null) {
                System.err.println("[AudioPlayer] Missing audio resource: " + resourcePath);
                return;
            }

            try (BufferedInputStream bufferedStream = new BufferedInputStream(rawStream);
                 AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedStream)) {
                newClip = AudioSystem.getClip();
                newClip.open(audioStream);
                newClip.setFramePosition(getSavedFramePosition(resourcePath, newClip));
                applyPlaybackSettings(newClip);
                newClip.loop(Clip.LOOP_CONTINUOUSLY);
                clip = newClip;
                currentTrack = resourcePath;
            }
        } catch (UnsupportedAudioFileException e) {
            System.err.println("[AudioPlayer] Unsupported audio format: " + resourcePath);
            System.err.println("[AudioPlayer] If MP3 playback fails in this runtime, convert the file to WAV or add an MP3 decoder library.");
        } catch (IOException | LineUnavailableException e) {
            if (newClip != null) {
                newClip.close();
            }
            e.printStackTrace();
        }
    }

    public void setVolume(float volume) {
        this.volume = clamp(volume);
        applyPlaybackSettings(clip);
    }

    public float getVolume() {
        return volume;
    }

    public void setMuted(boolean muted) {
        if (this.muted == muted)
            return;

        this.muted = muted;

        if (clip == null)
            return;

        if (muted) {
            rememberCurrentTrackPosition();
            clip.stop();
            clip.flush();
            applyPlaybackSettings(clip);
            return;
        }

        clip.setFramePosition(getSavedFramePosition(currentTrack, clip));
        applyPlaybackSettings(clip);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public boolean isMuted() {
        return muted;
    }

    private void applyPlaybackSettings(Clip targetClip) {
        if (targetClip == null)
            return;

        if (targetClip.isControlSupported(BooleanControl.Type.MUTE)) {
            BooleanControl muteControl = (BooleanControl) targetClip.getControl(BooleanControl.Type.MUTE);
            muteControl.setValue(muted);
        }

        if (targetClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) targetClip.getControl(FloatControl.Type.MASTER_GAIN);
            float min = gainControl.getMinimum();
            float max = gainControl.getMaximum();

            if (muted || volume <= 0f) {
                gainControl.setValue(min);
                return;
            }

            float gain = (float) (20f * Math.log10(Math.max(volume, 0.0001f)));
            gain = Math.min(gain, max);
            gainControl.setValue(Math.max(min, Math.min(max, gain)));
        }
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private void rememberCurrentTrackPosition() {
        if (clip == null || currentTrack == null)
            return;

        savedFramePositions.put(currentTrack, clip.getFramePosition());
    }

    private int getSavedFramePosition(String resourcePath, Clip targetClip) {
        int saved = savedFramePositions.getOrDefault(resourcePath, 0);
        int maxFrame = Math.max(0, targetClip.getFrameLength() - 1);
        return Math.min(saved, maxFrame);
    }
}
