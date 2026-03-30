package objects;

import gameStates.Playing;
import main.Game;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class StopSignManager {

    private final Playing playing;
    private final List<StopSign> signs = new ArrayList<>();


    // -------------------------------------------------------
    // STOP SIGN SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final int MAX_WORLD_LOOPS = 15;
    // -------------------------------------------------------

    // Tracks which loop we last spawned on so we spawn exactly once per loop
    private int lastSpawnedLoop = -1;

    public StopSignManager(Playing playing) {
        this.playing = playing;
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update() {
        boolean scrolling = playing.isScrolling();
        float   speed     = playing.getScrollSpeed();


        List<StopSign> snapshot = new ArrayList<>(signs);
        for (StopSign s : snapshot) {
            s.update(scrolling, speed);
            if (!s.isActive()) signs.remove(s);
        }


        int currentLoop = playing.getWorldLoopCount();
        if (currentLoop > lastSpawnedLoop
                && currentLoop <= MAX_WORLD_LOOPS
                && currentLoop > 0) {
            spawnSign();
            lastSpawnedLoop = currentLoop;
        }
    }

    private void spawnSign() {
        float spawnX = Game.GAME_WIDTH; // right border
        signs.add(new StopSign(spawnX));
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        List<StopSign> snapshot = new ArrayList<>(signs);
        for (StopSign s : snapshot)
            s.render(g);
    }

    // ─────────────────────────────────────────────────────────
    // RESET
    // ─────────────────────────────────────────────────────────
    public void resetAll() {
        signs.clear();
        lastSpawnedLoop = -1;
    }
}