package LeaderBoards;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

public class LeaderboardDisplay {

    private Game game;
    private LeaderboardManager leaderboardManager;
    private boolean visible = false;

    // Overlay dimensions
    private BufferedImage backgroundImg;
    private int overlayX, overlayY, overlayW, overlayH;

    // UI Components
    private Rectangle closeBtn;

    // Scroll position
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;
    private static final int ENTRIES_PER_PAGE = 8;
    private static final int ROW_HEIGHT = 45;

    public LeaderboardDisplay(Game game, LeaderboardManager leaderboardManager) {
        this.game = game;
        this.leaderboardManager = leaderboardManager;
        loadAssets();
        calculatePositions();
    }

    private void loadAssets() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.MENU_BACKGROUND_IMG);
        if (backgroundImg == null) {
            System.err.println("[LeaderboardDisplay] Could not load background");
        }
    }

    private void calculatePositions() {
        // Set overlay size (800x600)
        overlayW = (int)(800 * Game.SCALE);
        overlayH = (int)(600 * Game.SCALE);
        overlayX = (Game.GAME_WIDTH - overlayW) / 2;
        overlayY = (Game.GAME_HEIGHT - overlayH) / 2;

        // Close button (X) - top right corner of overlay
        int closeSize = (int)(40 * Game.SCALE);
        closeBtn = new Rectangle(
                overlayX + overlayW - closeSize - (int)(15 * Game.SCALE),
                overlayY + (int)(15 * Game.SCALE),
                closeSize,
                closeSize
        );
    }
    public void handleEsc() {
        if (visible) {
            hide();
            System.out.println("[LeaderboardDisplay] ESC pressed - hiding");
        }
    }

    public void render(Graphics g) {
        if (!visible) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw menu background
        BufferedImage menuBg = LoadSave.getSpriteAtlas(LoadSave.MENU_BACKGROUND_IMG);
        if (menuBg != null) {
            g.drawImage(menuBg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        }

        // Darken background
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);

        // Draw overlay background
        g2d.setColor(new Color(30, 30, 50, 240));
        g2d.fillRoundRect(overlayX, overlayY, overlayW, overlayH, 20, 20);

        // Draw border
        g2d.setColor(new Color(255, 215, 0));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(overlayX, overlayY, overlayW, overlayH, 20, 20);

        // Draw content
        drawContent(g2d);

        // Draw close button
        drawCloseButton(g2d);
    }

    private void drawContent(Graphics2D g2d) {
        // Title - removed trophy icon
        g2d.setFont(new Font("Arial", Font.BOLD, (int)(32 * Game.SCALE)));
        g2d.setColor(new Color(255, 215, 0));
        String title = "LEADERBOARD";
        FontMetrics fm = g2d.getFontMetrics();
        int titleX = overlayX + (overlayW - fm.stringWidth(title)) / 2;
        g2d.drawString(title, titleX, overlayY + (int)(60 * Game.SCALE));

        // Table Headers
        int startX = overlayX + (int)(80 * Game.SCALE);
        int headerY = overlayY + (int)(120 * Game.SCALE);
        g2d.setFont(new Font("Arial", Font.BOLD, (int)(18 * Game.SCALE)));
        g2d.setColor(Color.YELLOW);
        g2d.drawString("RANK", startX, headerY);
        g2d.drawString("PLAYER NAME", startX + (int)(80 * Game.SCALE), headerY);
        g2d.drawString("BEST TIME", startX + (int)(400 * Game.SCALE), headerY);
        g2d.drawString("PLAYS", startX + (int)(580 * Game.SCALE), headerY);

        // Separator line
        g2d.setColor(new Color(255, 215, 0, 100));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(overlayX + 40, headerY + 10, overlayX + overlayW - 40, headerY + 10);

        // Get leaderboard entries
        List<PlayerStats> leaderboard = leaderboardManager.getLeaderboard();

        // Calculate max scroll offset
        maxScrollOffset = Math.max(0, leaderboard.size() - ENTRIES_PER_PAGE);
        if (scrollOffset > maxScrollOffset) scrollOffset = maxScrollOffset;

        // Draw entries
        g2d.setFont(new Font("Arial", Font.PLAIN, (int)(16 * Game.SCALE)));
        int y = overlayY + (int)(160 * Game.SCALE);

        for (int i = scrollOffset; i < Math.min(leaderboard.size(), scrollOffset + ENTRIES_PER_PAGE); i++) {
            PlayerStats stats = leaderboard.get(i);
            int rank = i + 1;
            int currentY = y + ((i - scrollOffset) * (int)(ROW_HEIGHT * Game.SCALE));

            // Highlight current player - moved UP to cover the name properly
            if (leaderboardManager.hasCurrentPlayer() &&
                    stats.getPlayerName().equals(leaderboardManager.getCurrentPlayer().getPlayerName())) {
                g2d.setColor(new Color(0, 150, 0, 100));
                int highlightY = currentY - 50;  // Moved up from -20 to -28
                g2d.fillRoundRect(overlayX + 30, highlightY, overlayW - 60, (int)(38 * Game.SCALE), 10, 10);
            }

            // Rank color (no emojis, just numbers)
            if (rank == 1) {
                g2d.setColor(new Color(255, 215, 0)); // Gold
                g2d.setFont(new Font("Arial", Font.BOLD, (int)(20 * Game.SCALE)));
            } else if (rank == 2) {
                g2d.setColor(new Color(192, 192, 192)); // Silver
                g2d.setFont(new Font("Arial", Font.BOLD, (int)(18 * Game.SCALE)));
            } else if (rank == 3) {
                g2d.setColor(new Color(205, 127, 50)); // Bronze
                g2d.setFont(new Font("Arial", Font.BOLD, (int)(18 * Game.SCALE)));
            } else {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.PLAIN, (int)(16 * Game.SCALE)));
            }

            // Draw rank (just number, no emoji)
            String rankText = String.valueOf(rank);
            g2d.drawString(rankText, startX, currentY);

            // Draw name (truncate if too long)
            String playerName = stats.getPlayerName();
            if (playerName.length() > 15) {
                playerName = playerName.substring(0, 12) + "...";
            }
            g2d.drawString(playerName, startX + (int)(80 * Game.SCALE), currentY);

            // Draw time
            g2d.drawString(stats.getFormattedBestTime(), startX + (int)(400 * Game.SCALE), currentY);

            // Draw games played
            g2d.drawString(String.valueOf(stats.getGamesPlayed()), startX + (int)(585 * Game.SCALE), currentY);
        }

        // No entries message
        if (leaderboard.isEmpty()) {
            g2d.setFont(new Font("Arial", Font.ITALIC, (int)(20 * Game.SCALE)));
            g2d.setColor(Color.GRAY);
            String noData = "No records yet! Complete the game to appear on leaderboard.";
            int noDataX = overlayX + (overlayW - g2d.getFontMetrics().stringWidth(noData)) / 2;
            g2d.drawString(noData, noDataX, overlayY + overlayH / 2);
        }

        // Scroll indicators - simplified (no arrows, just text)
        if (maxScrollOffset > 0) {
            g2d.setFont(new Font("Arial", Font.PLAIN, (int)(14 * Game.SCALE)));
            g2d.setColor(new Color(200, 200, 200, 150));
            if (scrollOffset > 0) {
                g2d.drawString("▲", overlayX + overlayW - 40, overlayY + 200);
            }
            if (scrollOffset < maxScrollOffset) {
                g2d.drawString("▼", overlayX + overlayW - 40, overlayY + 240);
            }
        }

        // Current player info at bottom - removed icon
        if (leaderboardManager.hasCurrentPlayer()) {
            PlayerStats current = leaderboardManager.getCurrentPlayer();
            g2d.setFont(new Font("Arial", Font.BOLD, (int)(14 * Game.SCALE)));
            g2d.setColor(Color.CYAN);

            String playerInfo = "CURRENT: " + current.getPlayerName() +
                    "  |  BEST: " + current.getFormattedBestTime() +
                    "  |  GAMES: " + current.getGamesPlayed();

            int infoWidth = g2d.getFontMetrics().stringWidth(playerInfo);
            int infoX = overlayX + (overlayW - infoWidth) / 2;
            int infoY = overlayY + overlayH - (int)(40 * Game.SCALE);

            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRoundRect(infoX - 10, infoY - 20, infoWidth + 20, (int)(30 * Game.SCALE), 10, 10);
            g2d.setColor(Color.CYAN);
            g2d.drawString(playerInfo, infoX, infoY);
        }
    }

    private void drawCloseButton(Graphics2D g2d) {
        // Close button background
        g2d.setColor(new Color(150, 50, 50, 200));
        g2d.fillRoundRect(closeBtn.x, closeBtn.y, closeBtn.width, closeBtn.height, 10, 10);

        // Close button border
        g2d.setColor(new Color(255, 100, 100));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(closeBtn.x, closeBtn.y, closeBtn.width, closeBtn.height, 10, 10);

        // Close button text
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, (int)(20 * Game.SCALE)));
        String closeText = "X";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = closeBtn.x + (closeBtn.width - fm.stringWidth(closeText)) / 2;
        int textY = closeBtn.y + (closeBtn.height + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(closeText, textX, textY);
    }

    public void mousePressed(MouseEvent e) {
        if (!visible) return;

        // Check close button
        if (closeBtn.contains(e.getX(), e.getY())) {
            hide();
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (!visible) return;
    }

    public void mouseMoved(MouseEvent e) {
        if (!visible) return;
    }

    public void mouseWheelMoved(int rotation) {
        if (!visible) return;
        if (rotation < 0) {
            scrollUp();
        } else {
            scrollDown();
        }
    }

    private void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--;
        }
    }

    private void scrollDown() {
        if (scrollOffset < maxScrollOffset) {
            scrollOffset++;
        }
    }

    public void show() {
        visible = true;
        scrollOffset = 0;
        System.out.println("[LeaderboardDisplay] Showing");
    }

    public void hide() {
        visible = false;
        System.out.println("[LeaderboardDisplay] Hiding");
    }

    public boolean isVisible() {
        return visible;
    }
}