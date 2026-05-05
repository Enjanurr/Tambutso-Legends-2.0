package LeaderBoards;

import java.io.Serializable;

public class PlayerStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private String playerName;
    private long bestTimeMs;
    private int gamesPlayed;

    public PlayerStats(String playerName) {
        this.playerName = playerName;
        this.bestTimeMs = 0;
        this.gamesPlayed = 0;
    }

    public String getPlayerName() { return playerName; }
    public long getBestTimeMs() { return bestTimeMs; }
    public int getGamesPlayed() { return gamesPlayed; }

    public void setBestTimeMs(long time) { this.bestTimeMs = time; }
    public void setGamesPlayed(int played) { this.gamesPlayed = played; }
    public void incrementGamesPlayed() { this.gamesPlayed++; }

    public String getFormattedBestTime() {
        if (bestTimeMs == 0) return "--:--";
        long seconds = bestTimeMs / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }
}




