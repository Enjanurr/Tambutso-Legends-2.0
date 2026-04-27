package Ui;

import main.Game;

import java.awt.*;
import java.io.*;
import java.nio.file.*;

/**
 * Game clock that tracks time per level.
 * Starts after IntroOverlay completes, stops on boss defeat.
 * Saves level completion times to records.txt.
 *
 * Display format: MM:SS (e.g., 03:45)
 * Position: Below progress bar (center of screen, adjustable)
 */
public class GameClock {

    // -------------------------------------------------------
    // POSITION SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float CLOCK_Y_OFFSET = 30f;  // Below progress bar
    private static final int   FONT_SIZE      = 16;
    private static final Color CLOCK_COLOR    = new Color(255, 255, 255);
    // -------------------------------------------------------

    // Records file location (project root)
    private static final String RECORDS_FILE = "records.txt";

    private final int drawX, drawY;
    private int elapsedSeconds = 0;
    private long lastTickTime = 0;
    private boolean running = false;

    // Current level being tracked
    private int currentLevel = 1;

    public GameClock() {
        // Center of screen
        drawY = (int)(CLOCK_Y_OFFSET * Game.SCALE);
        drawX = Game.GAME_WIDTH / 2;
    }

    // ─────────────────────────────────────────────────────────
    // CONTROL
    // ─────────────────────────────────────────────────────────

    /** Starts the clock from 00:00 */
    public void start() {
        running = true;
        elapsedSeconds = 0;
        lastTickTime = System.currentTimeMillis();
    }

    /** Stops the clock. Call before saving record. */
    public void stop() {
        running = false;
    }

    /** Resets to 00:00 and stops. Call when advancing to next level. */
    public void reset() {
        running = false;
        elapsedSeconds = 0;
        lastTickTime = 0;
    }

    /** Sets the current level being tracked. */
    public void setCurrentLevel(int level) {
        this.currentLevel = level;
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update() {
        if (!running) return;

        long now = System.currentTimeMillis();
        if (now - lastTickTime >= 1000) {
            elapsedSeconds++;
            lastTickTime = now;
        }
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        String timeStr = formatTime(elapsedSeconds);

        g.setFont(new Font("SansSerif", Font.BOLD, (int)(FONT_SIZE * Game.SCALE)));
        g.setColor(CLOCK_COLOR);

        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(timeStr);

        g.drawString(timeStr, drawX - textWidth / 2, drawY);
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // ─────────────────────────────────────────────────────────
    // RECORDS FILE
    // ─────────────────────────────────────────────────────────

    /**
     * Saves the current time for the completed level to records.txt.
     * Overwrites previous time for this level.
     */
    public void saveLevelRecord() {
        int[] records = loadRecords();
        records[currentLevel - 1] = elapsedSeconds;
        saveRecords(records);
    }

    /**
     * Loads existing records from records.txt.
     * Returns int[3] with times for levels 1-3 (0 if not completed).
     */
    public int[] loadRecords() {
        int[] records = new int[3]; // Level 1, 2, 3

        File file = new File(RECORDS_FILE);
        if (!file.exists()) {
            return records;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Level 1:")) {
                    records[0] = parseTime(line.substring(8).trim());
                } else if (line.startsWith("Level 2:")) {
                    records[1] = parseTime(line.substring(8).trim());
                } else if (line.startsWith("Level 3:")) {
                    records[2] = parseTime(line.substring(8).trim());
                }
            }
        } catch (IOException e) {
            System.err.println("[GameClock] Error loading records: " + e.getMessage());
        }

        return records;
    }

    private int parseTime(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return minutes * 60 + seconds;
        } catch (Exception e) {
            return 0;
        }
    }

    private void saveRecords(int[] records) {
        int total = records[0] + records[1] + records[2];

        try (PrintWriter writer = new PrintWriter(new FileWriter(RECORDS_FILE))) {
            writer.println("Level 1: " + formatTime(records[0]));
            writer.println("Level 2: " + formatTime(records[1]));
            writer.println("Level 3: " + formatTime(records[2]));
            writer.println("Total: " + formatTime(total));
            System.out.println("[GameClock] Records saved to " + RECORDS_FILE);
        } catch (IOException e) {
            System.err.println("[GameClock] Error saving records: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────
    // GETTERS
    // ─────────────────────────────────────────────────────────
    public boolean isRunning() { return running; }
    public int getElapsedSeconds() { return elapsedSeconds; }
    public String getFormattedTime() { return formatTime(elapsedSeconds); }
}
