// ═══════════════════════════════════════════════════════════════════════════════
// FIXED: NameEntryOverlay - startGame() now calls game.startOrResumeGame()
//        so that a full reset (resetToLevel1, intro reset, etc.) is triggered
//        when returning from game completion, before going to char select.
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

    // Colors
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

        // Name input field
        int fieldW = (int)(overlayW * 0.6);
        int fieldH = (int)(45 * Game.SCALE);
        int fieldX = overlayX + (overlayW - fieldW) / 2;
        int fieldY = overlayY + (int)(overlayH * 0.4);
        nameField = new Rectangle(fieldX, fieldY, fieldW, fieldH);

        // Start button using new ENTER button (scaled)
        int btnWidth = (int)(ENTER_BTN_WIDTH * Game.SCALE);
        int btnHeight = (int)(ENTER_BTN_HEIGHT * Game.SCALE);
        int btnX = overlayX + (overlayW - btnWidth) / 2;
        int btnY = overlayY + (int)(overlayH * 0.65);
        startBtn = new Rectangle(btnX, btnY, btnWidth, btnHeight);

        System.out.println("[NameEntryOverlay] Button bounds: " + startBtn);
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

        // Name input field border
        g2d.setColor(isTyping ? VALUE_COLOR : Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(nameField.x, nameField.y, nameField.width, nameField.height);

        // Name input field text
        g2d.setFont(NAME_FONT);

        if (playerName.isEmpty()) {
            g2d.setColor(Color.WHITE);
            String placeholder = "Enter your name...";
            FontMetrics pfm = g2d.getFontMetrics();
            int textX = nameField.x + (nameField.width - pfm.stringWidth(placeholder)) / 2;
            int textY = nameField.y + (nameField.height + pfm.getAscent() - pfm.getDescent()) / 2;
            g2d.drawString(placeholder, textX, textY);
        } else {
            g2d.setColor(INPUT_COLOR);
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

        // Debug: Print current state
        System.out.println("[NameEntryOverlay] drawStartButton - startBtnPressed=" + startBtnPressed +
                ", startBtnHover=" + startBtnHover);
        System.out.println("[NameEntryOverlay] Button images - Normal=" + (enterButtonNormal != null) +
                ", Hover=" + (enterButtonHover != null) +
                ", Pressed=" + (enterButtonPressed != null));

        // Priority: Pressed > Hover > Normal
        if (startBtnPressed && enterButtonPressed != null) {
            btnImage = enterButtonPressed;
            System.out.println("[NameEntryOverlay] Using PRESSED button");
        } else if (startBtnHover && enterButtonHover != null) {
            btnImage = enterButtonHover;
            System.out.println("[NameEntryOverlay] Using HOVER button");
        } else if (enterButtonNormal != null) {
            btnImage = enterButtonNormal;
            System.out.println("[NameEntryOverlay] Using NORMAL button");
        } else {
            System.out.println("[NameEntryOverlay] All button images are NULL!");
        }

        if (btnImage != null) {
            g2d.drawImage(btnImage, startBtn.x, startBtn.y, startBtn.width, startBtn.height, null);
        } else {
            drawFallbackStartButton(g2d);
        }
    }

    private void drawFallbackStartButton(Graphics2D g2d) {
        // ── FIXED: Correct fallback states ──
        if (startBtnPressed) {
            g2d.setColor(new Color(0, 50, 0));  // Darkest when pressed
        } else if (startBtnHover) {
            g2d.setColor(new Color(0, 100, 0)); // Dark when hovering
        } else {
            g2d.setColor(new Color(0, 150, 0)); // Bright when normal
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

        // Always print when mouse moves (not just on change)
        System.out.println("[NameEntryOverlay] mouseMoved called - mouse at: (" + e.getX() + ", " + e.getY() + ")");
        System.out.println("[NameEntryOverlay] Button bounds: " + startBtn);
        System.out.println("[NameEntryOverlay] Button contains: " + startBtn.contains(e.getX(), e.getY()));

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

        // Check if clicked on name field
        if (nameField.contains(e.getX(), e.getY())) {
            isTyping = true;
            startBtnPressed = false;
        }
        // Check if clicked on start button
        else if (startBtn.contains(e.getX(), e.getY())) {
            startBtnPressed = true;
            isTyping = false;
            System.out.println("[NameEntryOverlay] Start button PRESSED");
        }
        // Clicked outside - close the overlay
        else {
            isTyping = false;
            startBtnPressed = false;
            hide();  // ← This closes the overlay when clicking outside
            System.out.println("[NameEntryOverlay] Clicked outside modal - hiding");
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (!visible) return;

        // ── FIXED: Check if release is WITHIN button bounds ──
        if (startBtnPressed && startBtn.contains(e.getX(), e.getY())) {
            System.out.println("[NameEntryOverlay] Start button CLICKED");
            startGame();
        }

        // Always reset pressed state on release
        startBtnPressed = false;
    }

    public void mouseClicked(MouseEvent e) {}

    // ─────────────────────────────────────────────────────────────────────────
    // CRITICAL FIX: startGame() now calls game.startOrResumeGame() instead of
    // game.startCharSelect() directly.
    //
    // Why this matters:
    //   • After game completion, Boss3 calls game.markNeedsFullReset().
    //   • startOrResumeGame() checks needsFullReset and calls playing.resetToLevel1()
    //     which resets the level manager, progress bar, clock, and introOverlay.
    //   • Then, since hasActiveGame is false after the reset, it calls startCharSelect().
    //   • If needsFullReset is NOT set (normal new-game flow), startOrResumeGame()
    //     just calls startCharSelect() as before — no behaviour change.
    //
    // OLD (broken):
    //   game.startCharSelect();   ← skips reset entirely, game resumes from Level 3
    //
    // NEW (correct):
    //   game.startOrResumeGame(); ← applies reset if flagged, then goes to char select
    // ─────────────────────────────────────────────────────────────────────────
    private void startGame() {
        if (playerName.trim().isEmpty()) {
            errorMessage = "Please enter your name!";
            return;
        }

        String cleanName = playerName.trim();
        System.out.println("[NameEntryOverlay] Starting game with name: " + cleanName);

        // CRITICAL: Register this player (creates new record or loads existing one)
        leaderboardManager.setCurrentPlayer(cleanName);

        // NOTE: Timer (startSession) is started in Playing.onIntroDone() once the
        // intro overlay finishes — do NOT start it here to avoid double-starting.

        hide();

        // CRITICAL: Use startOrResumeGame() so that needsFullReset is respected.
        // This resets Playing to Level 1, resets introOverlay, then goes to char select.
        game.startOrResumeGame(); // ← CRITICAL CHANGE (was: game.startCharSelect())
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