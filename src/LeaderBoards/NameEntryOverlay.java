// ═══════════════════════════════════════════════════════════════════════════════
// UPDATED: NameEntryOverlay - No input box border, text color #33323D
// Overlay size: 282 x 275
// ═══════════════════════════════════════════════════════════════════════════════

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

    // Button images for ENTER button (420x56 total, 3 frames of 140x56)
    private BufferedImage enterButtonNormal;
    private BufferedImage enterButtonHover;
    private BufferedImage enterButtonPressed;
    private static final int ENTER_BTN_WIDTH = 140;   // 420 / 3 = 140
    private static final int ENTER_BTN_HEIGHT = 56;

    // =========================================================
    // ADJUSTABLE POSITIONS - MODIFY THESE VALUES ↓
    // =========================================================
    // Name input field Y position (as percentage of overlay height)
    private static final float NAME_FIELD_Y_OFFSET = 0.45f;  // 45% from top of overlay
    private static final float NAME_FIELD_HEIGHT = 0.10f;    // 10% of overlay height

    // START button Y position (as percentage of overlay height)
    private static final float START_BTN_Y_OFFSET = 0.70f;   // 70% from top of overlay
    // =========================================================

    // Colors
    private static final Color INPUT_COLOR = new Color(51, 50, 61);  // #33323D
    private static final Color ERROR_COLOR = new Color(212, 8, 8, 255);
    private static final Color CURSOR_COLOR = new Color(51, 50, 61);  // #33323D for cursor
    private static final Font NAME_FONT = new Font("SansSerif", Font.BOLD, 24);
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

        // Load the ENTER button images (1 row, 3 cols: 420x56 total)
        BufferedImage enterButtonSheet = LoadSave.getSpriteAtlas(LoadSave.ENTERNAME_BUTTON);
        if (enterButtonSheet != null) {
            int frameWidth = enterButtonSheet.getWidth() / 3;  // 420 / 3 = 140
            int frameHeight = enterButtonSheet.getHeight();     // 56

            // Col 0 = Normal (bright)
            enterButtonNormal = enterButtonSheet.getSubimage(0, 0, frameWidth, frameHeight);
            // Col 1 = Hover (darkened)
            enterButtonHover = enterButtonSheet.getSubimage(frameWidth, 0, frameWidth, frameHeight);
            // Col 2 = Pressed
            enterButtonPressed = enterButtonSheet.getSubimage(frameWidth * 2, 0, frameWidth, frameHeight);

            System.out.println("[NameEntryOverlay] Loaded ENTER button - Frame width: " + frameWidth + ", height: " + frameHeight);
        } else {
            System.err.println("[NameEntryOverlay] Failed to load ENTER button sheet");
        }
    }

    private void calculatePositions() {
        // Fixed overlay size: 282 x 275
        overlayW = (int)(282 * Game.SCALE);
        overlayH = (int)(275 * Game.SCALE);

        overlayX = (Game.GAME_WIDTH - overlayW) / 2;
        overlayY = (Game.GAME_HEIGHT - overlayH) / 2;

        // Name input field (position only, NO border drawn)
        int fieldW = (int)(overlayW * 0.6);  // 60% of overlay width
        int fieldH = (int)(overlayH * NAME_FIELD_HEIGHT);  // Adjustable height
        int fieldX = overlayX + (overlayW - fieldW) / 2;
        int fieldY = overlayY + (int)(overlayH * NAME_FIELD_Y_OFFSET);
        nameField = new Rectangle(fieldX, fieldY, fieldW, fieldH);  // Used only for click detection

        // Start button using new ENTER button (scaled)
        int btnWidth = (int)(ENTER_BTN_WIDTH * Game.SCALE * 0.8f);
        int btnHeight = (int)(ENTER_BTN_HEIGHT * Game.SCALE * 0.8f);
        int btnX = overlayX + (overlayW - btnWidth) / 2;
        int btnY = overlayY + (int)(overlayH * START_BTN_Y_OFFSET);
        startBtn = new Rectangle(btnX, btnY, btnWidth, btnHeight);

        System.out.println("[NameEntryOverlay] Overlay: " + overlayW + "x" + overlayH + " at (" + overlayX + "," + overlayY + ")");
        System.out.println("[NameEntryOverlay] NameField Y: " + fieldY + ", Button Y: " + btnY);
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

        // Draw PLAYERNAME overlay
        if (backgroundImg != null) {
            g2d.drawImage(backgroundImg, overlayX, overlayY, overlayW, overlayH, null);
        } else {
            g2d.setColor(new Color(30, 30, 50, 240));
            g2d.fillRoundRect(overlayX, overlayY, overlayW, overlayH, 20, 20);
        }

        // ─────────────────────────────────────────────────────────────────
        // NAME INPUT FIELD - NO BORDER DRAWN (only text + cursor)
        // ─────────────────────────────────────────────────────────────────
        g2d.setFont(NAME_FONT);

        if (playerName.isEmpty()) {
            // Placeholder text
            g2d.setColor(new Color(100, 100, 120));  // Light gray placeholder
            String placeholder = "Enter your name...";
            FontMetrics pfm = g2d.getFontMetrics();
            int textX = nameField.x + (nameField.width - pfm.stringWidth(placeholder)) / 2;
            int textY = nameField.y + (nameField.height + pfm.getAscent() - pfm.getDescent()) / 2;
            g2d.drawString(placeholder, textX, textY);
        } else {
            // Player name text in #33323D color
            g2d.setColor(INPUT_COLOR);  // #33323D
            FontMetrics pfm = g2d.getFontMetrics();

            String displayName = playerName;
            while (pfm.stringWidth(displayName) > nameField.width - 20 && displayName.length() > 0) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }

            int textX = nameField.x + (nameField.width - pfm.stringWidth(displayName)) / 2;
            int textY = nameField.y + (nameField.height + pfm.getAscent() - pfm.getDescent()) / 2;
            g2d.drawString(displayName, textX, textY);

            // Blinking cursor (also #33323D color)
            if (isTyping && (System.currentTimeMillis() / 500 % 2 == 0)) {
                g2d.setColor(CURSOR_COLOR);
                int cursorX = textX + pfm.stringWidth(displayName);
                int cursorY = textY - pfm.getAscent() + 2;
                g2d.drawLine(cursorX, cursorY, cursorX, cursorY + pfm.getHeight() - 4);
            }
        }

        // Draw START button
        drawStartButton(g2d);

        // Error message
        if (!errorMessage.isEmpty()) {
            g2d.setFont(ERROR_FONT);
            g2d.setColor(ERROR_COLOR);
            FontMetrics fm = g2d.getFontMetrics();
            int errW = fm.stringWidth(errorMessage);
            int errX = (Game.GAME_WIDTH - errW) / 2;
            int errY = startBtn.y + startBtn.height + 30;
            g2d.drawString(errorMessage, errX, errY);
        }
    }

    private void drawStartButton(Graphics2D g2d) {
        BufferedImage btnImage = null;

        // Priority: Pressed > Hover > Normal
        if (startBtnPressed && enterButtonPressed != null) {
            btnImage = enterButtonPressed;
        } else if (startBtnHover && enterButtonHover != null) {
            btnImage = enterButtonHover;
        } else if (enterButtonNormal != null) {
            btnImage = enterButtonNormal;
        }

        if (btnImage != null) {
            g2d.drawImage(btnImage, startBtn.x, startBtn.y, startBtn.width, startBtn.height, null);
        } else {
            drawFallbackStartButton(g2d);
        }
    }

    private void drawFallbackStartButton(Graphics2D g2d) {
        if (startBtnPressed) {
            g2d.setColor(new Color(0, 50, 0));
        } else if (startBtnHover) {
            g2d.setColor(new Color(0, 100, 0));
        } else {
            g2d.setColor(new Color(0, 150, 0));
        }

        g2d.fillRoundRect(startBtn.x, startBtn.y, startBtn.width, startBtn.height, 15, 15);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, (int)(18 * Game.SCALE)));
        String startText = "START";
        FontMetrics sfm = g2d.getFontMetrics();
        int startX = startBtn.x + (startBtn.width - sfm.stringWidth(startText)) / 2;
        int startY = startBtn.y + (startBtn.height + sfm.getAscent() - sfm.getDescent()) / 2;
        g2d.drawString(startText, startX, startY);
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

        boolean wasHover = startBtnHover;
        startBtnHover = startBtn.contains(e.getX(), e.getY());

        if (wasHover != startBtnHover) {
            System.out.println("[NameEntryOverlay] Hover state changed: " + startBtnHover);
        }

        if (startBtnHover) {
            game.getGamePanel().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            game.getGamePanel().setCursor(Cursor.getDefaultCursor());
        }
    }

    public void mousePressed(MouseEvent e) {
        if (!visible) return;

        if (nameField.contains(e.getX(), e.getY())) {
            isTyping = true;
            startBtnPressed = false;
        } else if (startBtn.contains(e.getX(), e.getY())) {
            startBtnPressed = true;
            isTyping = false;
            System.out.println("[NameEntryOverlay] Start button PRESSED");
        } else {
            isTyping = false;
            startBtnPressed = false;
            hide();
            System.out.println("[NameEntryOverlay] Clicked outside modal - hiding");
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (!visible) return;

        if (startBtnPressed && startBtn.contains(e.getX(), e.getY())) {
            System.out.println("[NameEntryOverlay] Start button CLICKED");
            startGame();
        }

        startBtnPressed = false;
    }

    public void mouseClicked(MouseEvent e) {}

    private void startGame() {
        if (playerName.trim().isEmpty()) {
            errorMessage = "Please enter your name!";
            return;
        }

        String cleanName = playerName.trim();
        System.out.println("[NameEntryOverlay] Starting game with name: " + cleanName);

        leaderboardManager.setCurrentPlayer(cleanName);

        hide();

        game.startOrResumeGame();
    }

    public void show() {
        visible = true;
        playerName = "";
        errorMessage = "";
        isTyping = true;
        startBtnHover = false;
        startBtnPressed = false;
        System.out.println("[NameEntryOverlay] Showing");
    }

    public void hide() {
        visible = false;
        startBtnPressed = false;
        startBtnHover = false;
        game.getGamePanel().setCursor(Cursor.getDefaultCursor());
        System.out.println("[NameEntryOverlay] Hiding");
    }

    public void handleEsc() {
        if (visible) {
            hide();
            System.out.println("[NameEntryOverlay] ESC pressed - hiding");
        }
    }

    public boolean isVisible() {
        return visible;
    }
}