package LeaderBoards;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static utils.Constants.UI.Buttons.*;

public class NameEntryOverlay {
    private boolean startBtnHover = false;
    private boolean startBtnPressed = false;
    private Game game;
    private boolean visible = false;
    private String playerName = "";
    private String errorMessage = "";
    private boolean isTyping = true;

    // Overlay dimensions
    private BufferedImage backgroundImg;
    private int overlayX, overlayY, overlayW, overlayH;

    // UI Components
    private Rectangle nameField;
    private Rectangle startBtn;
    private LeaderboardManager leaderboardManager;

    // Colors from PaymentOverlay reference
    private static final Color INPUT_COLOR = Color.YELLOW;
    private static final Color ERROR_COLOR = new Color(212, 8, 8, 255);
    private static final Color VALUE_COLOR = new Color(100, 220, 100);
    private static final Font NAME_FONT = new Font("SansSerif", Font.PLAIN, 20);
    private static final Font ERROR_FONT = new Font("SansSerif", Font.BOLD, 18);

    public NameEntryOverlay(Game game, LeaderboardManager leaderboardManager) {
        this.game = game;
        this.leaderboardManager = leaderboardManager;
        loadAssets();
        calculatePositions();
    }

    private void loadAssets() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PLAYERNAME);
        if (backgroundImg == null) {
            System.err.println("[NameEntryOverlay] Could not load PlayerName.png");
        }
    }

    private void calculatePositions() {
        float scaleFactor = 0.65f;

        if (backgroundImg != null) {
            overlayW = (int)(backgroundImg.getWidth() * scaleFactor);
            overlayH = (int)(backgroundImg.getHeight() * scaleFactor);
        } else {
            overlayW = (int)(450 * Game.SCALE);
            overlayH = (int)(350 * Game.SCALE);
        }

        overlayX = (Game.GAME_WIDTH - overlayW) / 2;
        overlayY = (Game.GAME_HEIGHT - overlayH) / 2;

        // Name input field - MOVED UP (was 0.5, now 0.4)
        int fieldW = (int)(overlayW * 0.6);
        int fieldH = (int)(45 * Game.SCALE);
        int fieldX = overlayX + (overlayW - fieldW) / 2;
        int fieldY = overlayY + (int)(overlayH * 0.4);  // MOVED UP from 0.5 to 0.4
        nameField = new Rectangle(fieldX, fieldY, fieldW, fieldH);

        // Start button - moved up slightly to follow
        int btnX = overlayX + overlayW / 2;
        int btnY = overlayY + (int)(overlayH * 0.65);  // MOVED UP from 0.7 to 0.65
        startBtn = new Rectangle(btnX - (B_WIDTH / 2), btnY, B_WIDTH, B_HEIGHT);
    }

    public void render(Graphics g) {
        if (!visible) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw menu background covering the whole screen
        BufferedImage menuBg = LoadSave.getSpriteAtlas(LoadSave.MENU_BACKGROUND_IMG);
        if (menuBg != null) {
            g.drawImage(menuBg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        }

        // Darken background for contrast
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);

        // Draw PLAYERNAME overlay on top
        if (backgroundImg != null) {
            g2d.drawImage(backgroundImg, overlayX, overlayY, overlayW, overlayH, null);
        } else {
            g2d.setColor(new Color(30, 30, 50, 240));
            g2d.fillRoundRect(overlayX, overlayY, overlayW, overlayH, 20, 20);
        }

        // Name input field border (using highlight color)
        g2d.setColor(isTyping ? VALUE_COLOR : Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(nameField.x, nameField.y, nameField.width, nameField.height);

        // Name input field text (using INPUT_COLOR from reference)
        g2d.setFont(NAME_FONT);

        if (playerName.isEmpty()) {
            g2d.setColor(Color.WHITE);
            String placeholder = "Enter your name...";
            FontMetrics pfm = g2d.getFontMetrics();
            int textX = nameField.x + (nameField.width - pfm.stringWidth(placeholder)) / 2;
            int textY = nameField.y + (nameField.height + pfm.getAscent() - pfm.getDescent()) / 2;
            g2d.drawString(placeholder, textX, textY);
        } else {
            g2d.setColor(INPUT_COLOR);  // Yellow like PaymentOverlay
            FontMetrics pfm = g2d.getFontMetrics();

            String displayName = playerName;
            while (pfm.stringWidth(displayName) > nameField.width - 20 && displayName.length() > 0) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }

            int textX = nameField.x + (nameField.width - pfm.stringWidth(displayName)) / 2;
            int textY = nameField.y + (nameField.height + pfm.getAscent() - pfm.getDescent()) / 2;
            g2d.drawString(displayName, textX, textY);

            // Blinking cursor
            if (isTyping && (System.currentTimeMillis() / 500 % 2 == 0)) {
                int cursorX = textX + pfm.stringWidth(displayName);
                g2d.drawString("_", cursorX, textY);
            }
        }

        // Draw START button
        drawStartButton(g2d);

        // Error message - MOVED UP (was Game.GAME_HEIGHT - 50, now closer to button)
        if (!errorMessage.isEmpty()) {
            g2d.setFont(ERROR_FONT);
            g2d.setColor(ERROR_COLOR);
            FontMetrics fm = g2d.getFontMetrics();
            int errW = fm.stringWidth(errorMessage);
            int errX = (Game.GAME_WIDTH - errW) / 2;
            int errY = startBtn.y + startBtn.height + 30;  // MOVED UP - just below start button
            g2d.drawString(errorMessage, errX, errY);
        }
    }

    private void drawStartButton(Graphics2D g2d) {
        BufferedImage btnImg = LoadSave.getSpriteAtlas(LoadSave.MENU_BUTTONS);
        if (btnImg != null) {
            int btnState = 0;
            if (startBtnPressed) btnState = 2;
            else if (startBtnHover) btnState = 1;

            BufferedImage currentBtn = btnImg.getSubimage(
                    btnState * B_WIDTH_DEFAULT,
                    0 * B_HEIGHT_DEFAULT,
                    B_WIDTH_DEFAULT,
                    B_HEIGHT_DEFAULT
            );
            g2d.drawImage(currentBtn, startBtn.x, startBtn.y, B_WIDTH, B_HEIGHT, null);
        } else {
            // Fallback
            g2d.setColor(new Color(0, 150, 0));
            g2d.fillRoundRect(startBtn.x, startBtn.y, startBtn.width, startBtn.height, 15, 15);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, (int)(18 * Game.SCALE)));
            String startText = "START";
            FontMetrics sfm = g2d.getFontMetrics();
            int startX = startBtn.x + (startBtn.width - sfm.stringWidth(startText)) / 2;
            int startY = startBtn.y + (startBtn.height + sfm.getAscent() - sfm.getDescent()) / 2;
            g2d.drawString(startText, startX, startY);
        }
    }

    public void keyPressed(KeyEvent e) {
        if (!visible) return;

        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_ENTER) {
            startGame();
            return;
        }

        if (keyCode == KeyEvent.VK_BACK_SPACE) {
            if (playerName.length() > 0) {
                playerName = playerName.substring(0, playerName.length() - 1);
                errorMessage = "";
            }
            return;
        }

        if (keyCode == KeyEvent.VK_ESCAPE) {
            hide();
            return;
        }

        char keyChar = e.getKeyChar();
        if (Character.isLetterOrDigit(keyChar) || keyChar == '_' || keyChar == ' ') {
            if (playerName.length() < 20) {
                playerName += keyChar;
                errorMessage = "";
            }
        }
    }

    public void mouseMoved(MouseEvent e) {
        if (!visible) return;
        startBtnHover = startBtn.contains(e.getX(), e.getY());
    }

    public void mousePressed(MouseEvent e) {
        if (!visible) return;

        if (nameField.contains(e.getX(), e.getY())) {
            isTyping = true;
            startBtnPressed = false;
        } else if (startBtn.contains(e.getX(), e.getY())) {
            startBtnPressed = true;
            isTyping = false;
        } else {
            isTyping = false;
            startBtnPressed = false;
            hide();
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (!visible) return;

        if (startBtnPressed && startBtn.contains(e.getX(), e.getY())) {
            startGame();
        }
        startBtnPressed = false;
    }

    public void mouseClicked(MouseEvent e) {
        // Not used but needed for compatibility
    }

    private void startGame() {
        if (playerName.trim().isEmpty()) {
            errorMessage = "Please enter your name!";
            return;
        }

        String cleanName = playerName.trim();
        System.out.println("[NameEntryOverlay] Starting game with name: " + cleanName);

        leaderboardManager.setCurrentPlayer(cleanName);

        hide();
        game.startCharSelect();
    }

    public void show() {
        visible = true;
        playerName = "";
        errorMessage = "";
        isTyping = true;
        System.out.println("[NameEntryOverlay] Showing");
    }

    public void hide() {
        visible = false;
        startBtnPressed = false;
        startBtnHover = false;
        System.out.println("[NameEntryOverlay] Hiding");
    }

    public boolean isVisible() {
        return visible;
    }
}