package utils;

public enum SoundEffect {
    INTRO_EXPLOSION("/audio/sfx/explosion.wav", 0.5f);

    private final String resourcePath;
    private final float defaultVolume;

    SoundEffect(String resourcePath, float defaultVolume) {
        this.resourcePath = resourcePath;
        this.defaultVolume = defaultVolume;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public float getDefaultVolume() {
        return defaultVolume;
    }
}
