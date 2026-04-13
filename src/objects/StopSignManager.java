package objects;

import gameStates.Playing;
import main.Game;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class StopSignManager {

    private final Playing playing;
    private final WorldObjectManager worldObjectManager;
    private final List<StopSign> signs = new ArrayList<>();

    // Tracks which completed world loop last triggered a roadside spawn.
    private int lastSpawnedLoop = -1;
    public StopSignManager(Playing playing, WorldObjectManager worldObjectManager) {
        this.playing = playing;
        this.worldObjectManager = worldObjectManager;
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
                && currentLoop <= Playing.MAX_WORLD_LOOPS
                && currentLoop > 0) {
            spawnSign();
            lastSpawnedLoop = currentLoop;
        }
    }

    private void spawnSign() {
        float spawnX = Game.GAME_WIDTH; // spawn just outside the right edge
        int stopIndex = playing.getWorldLoopCount();
        signs.add(new StopSign(spawnX));
        // Decorative roadside props are scheduled off the stop count.
        worldObjectManager.onStopSignSpawned(stopIndex);
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
