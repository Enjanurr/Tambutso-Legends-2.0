package utils;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AudioPlayer {

    private static final String MENU_THEME = "/audio/music/theme_menu.wav";
    private static final String MAIN_THEME = "/audio/music/theme_playing.wav";

    private Clip musicClip;
    private String currentTrack;
    private float musicVolume = 0.5f;
    private float sfxMasterVolume = 1.0f;
    private boolean musicMuted;
    private boolean sfxMuted;
    private final Map<String, Integer> savedFramePositions = new HashMap<>();
    private final Map<SoundEffect, Float> sfxVolumes = new EnumMap<>(SoundEffect.class);
    private final List<Clip> activeSfxClips = new ArrayList<>();
    private final Map<Clip, SoundEffect> activeSfxTypes = new HashMap<>();

    public AudioPlayer() {
        for (SoundEffect soundEffect : SoundEffect.values()) {
            sfxVolumes.put(soundEffect, clamp(soundEffect.getDefaultVolume()));
        }
    }

    public void playMenuTheme() {
        playLoop(MENU_THEME);
    }

    public void playMainTheme() {
        playLoop(MAIN_THEME);
    }

    public void playIntroExplosionSfx() {
        playSfx(SoundEffect.INTRO_EXPLOSION);
    }

    public void playSfx(SoundEffect soundEffect) {
        cleanupFinishedSfxClips();

        Clip newClip = null;
        try (InputStream rawStream = AudioPlayer.class.getResourceAsStream(soundEffect.getResourcePath())) {
            if (rawStream == null) {
                System.err.println("[AudioPlayer] Missing audio resource: " + soundEffect.getResourcePath());
                return;
            }

            try (BufferedInputStream bufferedStream = new BufferedInputStream(rawStream);
                 AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedStream)) {
                newClip = AudioSystem.getClip();
                newClip.open(audioStream);
                applySfxPlaybackSettings(newClip, soundEffect);
                registerSfxLifecycle(newClip);
                synchronized (activeSfxClips) {
                    activeSfxClips.add(newClip);
                    activeSfxTypes.put(newClip, soundEffect);
                }
                newClip.setFramePosition(0);
                newClip.start();
            }
        } catch (UnsupportedAudioFileException e) {
            System.err.println("[AudioPlayer] Unsupported audio format: " + soundEffect.getResourcePath());
        } catch (IOException | LineUnavailableException e) {
            if (newClip != null) {
                newClip.close();
            }
            e.printStackTrace();
        }
    }

    public void stop() {
        rememberCurrentTrackPosition();
        closeMusicClip();
        stopAllSfx();
        currentTrack = null;
    }

    private void playLoop(String resourcePath) {
        if (resourcePath.equals(currentTrack) && musicClip != null && musicClip.isOpen()) {
            if (!musicClip.isRunning()) {
                musicClip.setFramePosition(getSavedFramePosition(resourcePath, musicClip));
                applyMusicPlaybackSettings(musicClip);
                musicClip.loop(Clip.LOOP_CONTINUOUSLY);
            }
            return;
        }

        if (musicClip != null) {
            rememberCurrentTrackPosition();
            closeMusicClip();
        }
        currentTrack = null;

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
                applyMusicPlaybackSettings(newClip);
                newClip.loop(Clip.LOOP_CONTINUOUSLY);
                musicClip = newClip;
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

    public void setMusicVolume(float volume) {
        this.musicVolume = clamp(volume);
        applyMusicPlaybackSettings(musicClip);
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public void setSfxMasterVolume(float volume) {
        this.sfxMasterVolume = clamp(volume);
        refreshActiveSfxPlaybackSettings();
    }

    public float getSfxMasterVolume() {
        return sfxMasterVolume;
    }

    public void setSfxVolume(SoundEffect soundEffect, float volume) {
        sfxVolumes.put(soundEffect, clamp(volume));
        refreshActiveSfxPlaybackSettings();
    }

    public float getSfxVolume(SoundEffect soundEffect) {
        return sfxVolumes.getOrDefault(soundEffect, soundEffect.getDefaultVolume());
    }

    public void setMusicMuted(boolean muted) {
        if (this.musicMuted == muted)
            return;

        this.musicMuted = muted;

        if (muted) {
            rememberCurrentTrackPosition();
            if (musicClip != null) {
                musicClip.stop();
                musicClip.flush();
                applyMusicPlaybackSettings(musicClip);
            }
            return;
        }

        if (musicClip != null) {
            musicClip.setFramePosition(getSavedFramePosition(currentTrack, musicClip));
            applyMusicPlaybackSettings(musicClip);
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public boolean isMusicMuted() {
        return musicMuted;
    }

    public void setSfxMuted(boolean muted) {
        if (this.sfxMuted == muted)
            return;

        this.sfxMuted = muted;
        refreshActiveSfxPlaybackSettings();
    }

    public boolean isSfxMuted() {
        return sfxMuted;
    }

    public float getVolume() {
        return getMusicVolume();
    }

    public void setVolume(float volume) {
        setMusicVolume(volume);
    }

    public void setMuted(boolean muted) {
        setMusicMuted(muted);
    }

    public boolean isMuted() {
        return isMusicMuted();
    }

    private void applyMusicPlaybackSettings(Clip targetClip) {
        applyPlaybackSettings(targetClip, musicVolume, musicMuted);
    }

    private void applySfxPlaybackSettings(Clip targetClip, SoundEffect soundEffect) {
        applyPlaybackSettings(targetClip, sfxMasterVolume * getSfxVolume(soundEffect), sfxMuted);
    }

    private void applyPlaybackSettings(Clip targetClip, float volume, boolean muted) {
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
        if (musicClip == null || currentTrack == null)
            return;

        savedFramePositions.put(currentTrack, musicClip.getFramePosition());
    }

    private int getSavedFramePosition(String resourcePath, Clip targetClip) {
        int saved = savedFramePositions.getOrDefault(resourcePath, 0);
        int maxFrame = Math.max(0, targetClip.getFrameLength() - 1);
        return Math.min(saved, maxFrame);
    }

    private void registerSfxLifecycle(Clip clip) {
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP && clip.getFramePosition() >= clip.getFrameLength()) {
                synchronized (activeSfxClips) {
                    activeSfxClips.remove(clip);
                    activeSfxTypes.remove(clip);
                }
                clip.close();
            } else if (event.getType() == LineEvent.Type.CLOSE) {
                synchronized (activeSfxClips) {
                    activeSfxClips.remove(clip);
                    activeSfxTypes.remove(clip);
                }
            }
        });
    }

    private void refreshActiveSfxPlaybackSettings() {
        cleanupFinishedSfxClips();

        synchronized (activeSfxClips) {
            for (Clip clip : activeSfxClips) {
                SoundEffect soundEffect = activeSfxTypes.get(clip);
                if (soundEffect != null) {
                    applySfxPlaybackSettings(clip, soundEffect);
                }
            }
        }
    }

    private void cleanupFinishedSfxClips() {
        List<Clip> clipsToClose = new ArrayList<>();

        synchronized (activeSfxClips) {
            Iterator<Clip> iterator = activeSfxClips.iterator();
            while (iterator.hasNext()) {
                Clip clip = iterator.next();
                if (!clip.isOpen()) {
                    iterator.remove();
                    activeSfxTypes.remove(clip);
                    continue;
                }

                if (!clip.isRunning() && clip.getFramePosition() >= clip.getFrameLength()) {
                    iterator.remove();
                    activeSfxTypes.remove(clip);
                    clipsToClose.add(clip);
                }
            }
        }

        for (Clip clip : clipsToClose) {
            if (clip.isOpen()) {
                clip.close();
            }
        }
    }

    private void stopAllSfx() {
        List<Clip> clipsToClose;

        synchronized (activeSfxClips) {
            clipsToClose = new ArrayList<>(activeSfxClips);
            activeSfxClips.clear();
            activeSfxTypes.clear();
        }

        for (Clip clip : clipsToClose) {
            if (clip.isRunning()) {
                clip.stop();
            }
            if (clip.isOpen()) {
                clip.close();
            }
        }
    }

    private void closeMusicClip() {
        if (musicClip == null) {
            return;
        }

        musicClip.stop();
        musicClip.close();
        musicClip = null;
    }
}
