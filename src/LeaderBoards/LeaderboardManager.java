package LeaderBoards;

import java.io.*;
import java.util.*;

public class LeaderboardManager {
    private static final String SAVE_DIR = "saves/";
    private static final String CURRENT_PLAYER_FILE = SAVE_DIR + "current_player.dat";
    private static final String LEADERBOARD_FILE = SAVE_DIR + "leaderboard.dat";
    private static final int MAX_ENTRIES = 10;

    private List<PlayerStats> leaderboard;
    private PlayerStats currentPlayer;
    private long sessionStartTime;

    public LeaderboardManager() {
        leaderboard = new ArrayList<>();
        createSaveDirectory();
        loadLeaderboard();
        loadCurrentPlayer();
    }

    private void createSaveDirectory() {
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("[Leaderboard] Created save directory: " + SAVE_DIR);
        }
    }

    // Load all leaderboard entries
    @SuppressWarnings("unchecked")
    private void loadLeaderboard() {
        File file = new File(LEADERBOARD_FILE);
        if (!file.exists()) {
            System.out.println("[Leaderboard] No leaderboard file found.");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            leaderboard = (ArrayList<PlayerStats>) ois.readObject();
            System.out.println("[Leaderboard] Loaded " + leaderboard.size() + " entries from file.");
            printLeaderboard();
        } catch (Exception e) {
            System.err.println("[Leaderboard] Failed to load leaderboard: " + e.getMessage());
            leaderboard = new ArrayList<>();
        }
    }

    // Save all leaderboard entries
    private void saveLeaderboard() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LEADERBOARD_FILE))) {
            oos.writeObject(leaderboard);
            System.out.println("[Leaderboard] Saved " + leaderboard.size() + " entries to file.");
        } catch (IOException e) {
            System.err.println("[Leaderboard] Failed to save leaderboard: " + e.getMessage());
        }
    }

    // Save current player to file
    private void saveCurrentPlayer() {
        if (currentPlayer == null) return;

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CURRENT_PLAYER_FILE))) {
            oos.writeObject(currentPlayer);
            System.out.println("[Leaderboard] Saved current player: " + currentPlayer.getPlayerName());
        } catch (IOException e) {
            System.err.println("[Leaderboard] Failed to save current player: " + e.getMessage());
        }
    }

    // Load current player from file
    private void loadCurrentPlayer() {
        File file = new File(CURRENT_PLAYER_FILE);
        if (!file.exists()) {
            System.out.println("[Leaderboard] No current player file found.");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            currentPlayer = (PlayerStats) ois.readObject();
            System.out.println("[Leaderboard] Loaded current player: " + currentPlayer.getPlayerName());
        } catch (Exception e) {
            System.err.println("[Leaderboard] Failed to load current player: " + e.getMessage());
        }
    }

    // Start the timer when gameplay begins
    public void startSession() {
        sessionStartTime = System.currentTimeMillis();
        System.out.println("[Leaderboard] Timer started!");
    }

    // Complete the game and save time when Boss 3 is defeated
    public void completeGame() {
        if (currentPlayer == null) {
            System.out.println("[Leaderboard] No current player!");
            return;
        }

        long completionTime = System.currentTimeMillis() - sessionStartTime;
        System.out.println("[Leaderboard] Completion time: " + formatTime(completionTime));

        // Check if player already exists in leaderboard
        int playerIndex = -1;
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getPlayerName().equals(currentPlayer.getPlayerName())) {
                playerIndex = i;
                break;
            }
        }

        if (playerIndex != -1) {
            // Player exists - check if new time is better (smaller)
            PlayerStats existingPlayer = leaderboard.get(playerIndex);
            long existingBestTime = existingPlayer.getBestTimeMs();

            System.out.println("[Leaderboard] Existing best time: " + formatTime(existingBestTime));
            System.out.println("[Leaderboard] New time: " + formatTime(completionTime));

            if (existingBestTime == 0 || completionTime < existingBestTime) {
                // New time is FASTER - update
                currentPlayer.setBestTimeMs(completionTime);
                currentPlayer.setGamesPlayed(existingPlayer.getGamesPlayed() + 1);
                leaderboard.set(playerIndex, currentPlayer);
                System.out.println("[Leaderboard] ✅ UPDATED! New best time: " + formatTime(completionTime));
            } else {
                // New time is SLOWER - keep existing
                currentPlayer.setBestTimeMs(existingBestTime);
                currentPlayer.setGamesPlayed(existingPlayer.getGamesPlayed() + 1);
                System.out.println("[Leaderboard] ⏩ KEPT existing best: " + formatTime(existingBestTime) +
                        " (new was slower: " + formatTime(completionTime) + ")");
            }
            saveCurrentPlayer();
        } else {
            // New player - just add
            currentPlayer.setBestTimeMs(completionTime);
            currentPlayer.setGamesPlayed(1);
            leaderboard.add(currentPlayer);
            System.out.println("[Leaderboard] ➕ Added new player: " + currentPlayer.getPlayerName() +
                    " | Time: " + formatTime(completionTime));
        }

        // Sort by best time (fastest/smallest first)
        leaderboard.sort(Comparator.comparingLong(PlayerStats::getBestTimeMs));

        // Keep top 10
        while (leaderboard.size() > MAX_ENTRIES) {
            leaderboard.remove(leaderboard.size() - 1);
        }

        // Save to file
        saveLeaderboard();

        System.out.println("[Leaderboard] Final best time for " + currentPlayer.getPlayerName() +
                ": " + currentPlayer.getFormattedBestTime());
        printLeaderboard();
    }

    // Set current player (called from name entry)
    public void setCurrentPlayer(String playerName) {
        // Check if player exists in leaderboard
        for (PlayerStats stats : leaderboard) {
            if (stats.getPlayerName().equals(playerName)) {
                // Create a NEW PlayerStats object with the existing stats
                // This prevents modification of the original object
                currentPlayer = new PlayerStats(playerName);
                currentPlayer.setBestTimeMs(stats.getBestTimeMs());
                currentPlayer.setGamesPlayed(stats.getGamesPlayed());
                System.out.println("[Leaderboard] Welcome back, " + playerName +
                        "! Best time: " + currentPlayer.getFormattedBestTime());
                saveCurrentPlayer();
                return;
            }
        }

        // Create new player
        currentPlayer = new PlayerStats(playerName);
        System.out.println("[Leaderboard] New player created: " + playerName);
        saveCurrentPlayer();
    }

    // Helper method to format time for display
    private String formatTime(long timeMs) {
        if (timeMs == 0) return "--:--";
        long seconds = timeMs / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    // Print leaderboard to console for debugging
    public void printLeaderboard() {
        System.out.println("\n=== LEADERBOARD ===");
        List<PlayerStats> sorted = getLeaderboard();
        for (int i = 0; i < sorted.size(); i++) {
            PlayerStats p = sorted.get(i);
            System.out.printf("%d. %s - %s (%d games)%n",
                    i+1, p.getPlayerName(), p.getFormattedBestTime(), p.getGamesPlayed());
        }
        System.out.println("==================\n");
    }

    public List<PlayerStats> getLeaderboard() {
        List<PlayerStats> sorted = new ArrayList<>(leaderboard);
        sorted.sort(Comparator.comparingLong(PlayerStats::getBestTimeMs));
        return sorted;
    }

    public PlayerStats getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean hasCurrentPlayer() {
        return currentPlayer != null;
    }

    // Delete a specific player
    public boolean deletePlayer(String playerName) {
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getPlayerName().equals(playerName)) {
                leaderboard.remove(i);
                saveLeaderboard();

                if (currentPlayer != null && currentPlayer.getPlayerName().equals(playerName)) {
                    currentPlayer = null;
                    File file = new File(CURRENT_PLAYER_FILE);
                    if (file.exists()) {
                        file.delete();
                    }
                }
                System.out.println("[Leaderboard] Deleted player: " + playerName);
                return true;
            }
        }
        System.out.println("[Leaderboard] Player not found: " + playerName);
        return false;
    }

    // Clear all leaderboard
    public void clearAllLeaderboard() {
        leaderboard.clear();
        saveLeaderboard();
        System.out.println("[Leaderboard] Cleared all leaderboard entries");
    }
}