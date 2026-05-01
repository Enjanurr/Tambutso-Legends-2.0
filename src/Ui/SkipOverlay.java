package Ui;

import gameStates.GameStates;
import gameStates.Playing;
import main.Game;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Debug skip overlay for testing.
 * Press F1 to open menu with skip options.
 * Toggle enabled with Ctrl+Shift+S.
 */
public class SkipOverlay {

    // -------------------------------------------------------
    // SETTINGS
    // -------------------------------------------------------
    private static final int OVERLAY_W = 300;
    private static final int OVERLAY_H = 330;
    private static final int BUTTON_H = 40;
    private static final int BUTTON_GAP = 10;
    private static final Color BG_COLOR = new Color(0, 0, 0, 200);
    private static final Color BUTTON_COLOR = new Color(60, 60, 60, 220);
    private static final Color BUTTON_HOVER_COLOR = new Color(80, 80, 80, 220);
    private static final Color TEXT_COLOR = Color.WHITE;
    // -------------------------------------------------------

    private final Game game;
    private final Playing playing;

    private boolean enabled = false;  // Toggle with Ctrl+Shift+S
    private boolean visible = false;

    private int overlayX, overlayY;
    private Rectangle completeLevelBtn;
    private Rectangle skipToBossBtn;
    private Rectangle nextLevelBtn;
    private Rectangle nextBossBtn;
    private Rectangle closeBtn;

    public SkipOverlay(Game game, Playing playing) {
        this.game = game;
        this.playing = playing;
        calculatePositions();
    }

    private void calculatePositions() {
        overlayX = (Game.GAME_WIDTH - OVERLAY_W) / 2;
        overlayY = (Game.GAME_HEIGHT - OVERLAY_H) / 2;

        int btnX = overlayX + 20;
        int startY = overlayY + 60;

        completeLevelBtn = new Rectangle(btnX, startY, OVERLAY_W - 40, BUTTON_H);
        skipToBossBtn    = new Rectangle(btnX, startY + BUTTON_H + BUTTON_GAP, OVERLAY_W - 40, BUTTON_H);
        nextLevelBtn     = new Rectangle(btnX, startY + 2 * (BUTTON_H + BUTTON_GAP), OVERLAY_W - 40, BUTTON_H);
        nextBossBtn      = new Rectangle(btnX, startY + 3 * (BUTTON_H + BUTTON_GAP), OVERLAY_W - 40, BUTTON_H);
        closeBtn         = new Rectangle(btnX, startY + 4 * (BUTTON_H + BUTTON_GAP), OVERLAY_W - 40, BUTTON_H);
    }

    // ─────────────────────────────────────────────────────────
    // TOGGLE
    // ─────────────────────────────────────────────────────────
    public void toggleEnabled() {
        enabled = !enabled;
        System.out.println("[SkipOverlay] Cheat mode " + (enabled ? "ENABLED" : "DISABLED"));
        if (!enabled) visible = false;
    }

    public boolean isEnabled() { return enabled; }
    public boolean isVisible() { return visible && enabled; }

    public void show() {
        if (enabled) visible = true;
    }

    public void hide() {
        visible = false;
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (!visible || !enabled) return;

        Graphics2D g2 = (Graphics2D) g;

        // Background
        g2.setColor(BG_COLOR);
        g2.fillRoundRect(overlayX, overlayY, OVERLAY_W, OVERLAY_H, 10, 10);

        // Title
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.setColor(TEXT_COLOR);
        String title = "DEBUG SKIP MENU (F1)";
        int titleW = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, overlayX + (OVERLAY_W - titleW) / 2, overlayY + 30);

        // Buttons
        drawButton(g2, completeLevelBtn, "Complete Current Level");
        drawButton(g2, skipToBossBtn, "Skip to Boss");
        drawButton(g2, nextLevelBtn, "Next Level");
        drawButton(g2, nextBossBtn, "Next Boss");
        drawButton(g2, closeBtn, "Close");
    }

    private void drawButton(Graphics2D g2, Rectangle rect, String text) {
        // Check hover
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        boolean hover = rect.contains(mousePos.x, mousePos.y);

        g2.setColor(hover ? BUTTON_HOVER_COLOR : BUTTON_COLOR);
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 5, 5);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.setColor(TEXT_COLOR);
        int textW = g2.getFontMetrics().stringWidth(text);
        int textX = rect.x + (rect.width - textW) / 2;
        int textY = rect.y + rect.height / 2 + 5;
        g2.drawString(text, textX, textY);
    }

    // ─────────────────────────────────────────────────────────
    // INPUT
    // ─────────────────────────────────────────────────────────
    public void keyPressed(KeyEvent e) {
        // F1 to open/close menu
        if (e.getKeyCode() == KeyEvent.VK_F1) {
            if (visible) hide();
            else show();
            return;
        }

        // Quick skips when enabled (even without menu open)
        if (!enabled) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_N: // Next Level
                skipToNextLevel();
                break;
            case KeyEvent.VK_B: // Skip to Boss
                skipToBoss();
                break;
            case KeyEvent.VK_C: // Complete Level
                completeCurrentLevel();
                break;
        }
    }

    public void mousePressed(MouseEvent e) {
        if (!visible || !enabled) return;

        int x = e.getX();
        int y = e.getY();

        if (completeLevelBtn.contains(x, y)) {
            completeCurrentLevel();
        } else if (skipToBossBtn.contains(x, y)) {
            skipToBoss();
        } else if (nextLevelBtn.contains(x, y)) {
            skipToNextLevel();
        } else if (nextBossBtn.contains(x, y)) {
            skipToNextBoss();
        } else if (closeBtn.contains(x, y)) {
            hide();
        }
    }

    // ─────────────────────────────────────────────────────────
    // SKIP ACTIONS
    // ─────────────────────────────────────────────────────────
    private void completeCurrentLevel() {
        System.out.println("[SkipOverlay] Completing current level");
        // Force progress bar to max column for current level
        int maxLoops = playing.getLevelManager().getMaxWorldLoops();
        playing.getProgressBar().setProgress(maxLoops);
        playing.completeLevelForDebug();
        hide();
    }

    private void skipToBoss() {
        System.out.println("[SkipOverlay] Skipping to boss");

        // Check if driver is selected before proceeding
        if (game.getSelectedDriver() == null) {
            System.err.println("[SkipOverlay] Cannot start boss fight - no driver selected!");
            // Try to recover from Playing
            var playingDriver = game.getPlaying().getCurrentDriver();
            if (playingDriver != null) {
                System.out.println("[SkipOverlay] Recovered driver: " + playingDriver.displayName);
            } else {
                System.err.println("[SkipOverlay] No driver to recover - must complete character selection first");
                hide();
                return;
            }
        }

        int currentLevel = game.getPlaying().getLevelManager().getCurrentLevelId();
        game.startBossFightWithLevel(currentLevel);
        hide();
    }

    private void skipToNextLevel() {
        System.out.println("[SkipOverlay] Skipping to next level");

        int currentLevel = game.getPlaying().getLevelManager().getCurrentLevelId();

        if (currentLevel >= 3) {
            System.out.println("[SkipOverlay] Already at max level!");
            hide();
            return;
        }

        // Advance level in Playing's LevelManager
        game.getPlaying().getLevelManager().advanceToNextLevel();

        // Get the new level ID after advancement
        int newLevelId = game.getPlaying().getLevelManager().getCurrentLevelId();

        // Create new ProgressBar instance for the new level
        ProgressBar newProgressBar = new ProgressBar(newLevelId);
        newProgressBar.setProgress(0);

        // Replace the old progress bar with the new one
        game.getPlaying().setProgressBar(newProgressBar);

        System.out.println("[SkipOverlay] Advanced to Level " + newLevelId);

        // DO NOT call game.resetGameState() - that clears selectedDriver!
        // Just show the mission screen for the new level
        game.getPlaying().showMissionForCurrentLevel();
        hide();
    }

    private void skipToNextBoss() {
        int currentLevel = game.getPlaying().getLevelManager().getCurrentLevelId();
        int nextLevel = currentLevel + 1;

        System.out.println("[SkipOverlay] Skipping to next boss - Level " + currentLevel + " -> " + nextLevel);

        // Ensure driver is set
        if (game.getSelectedDriver() == null) {
            var playingDriver = game.getPlaying().getCurrentDriver();
            if (playingDriver != null) {
                game.setSelectedDriver(playingDriver);
                System.out.println("[SkipOverlay] Restored driver: " + playingDriver.displayName);
            } else {
                System.err.println("[SkipOverlay] ERROR: No driver available!");
                hide();
                return;
            }
        }

        if (nextLevel <= 3) {
            // FIRST: Advance Playing's level manager
            game.getPlaying().getLevelManager().advanceToNextLevel();

            // SECOND: Create new progress bar for the new level
            ProgressBar newProgressBar = new ProgressBar(nextLevel);
            newProgressBar.setProgress(0);
            game.getPlaying().setProgressBar(newProgressBar);

            // THIRD: Update Game's boss level
            game.setCurrentGameLevel(nextLevel);

            // FOURTH: Start boss fight
            game.startBossFightWithLevel(nextLevel);
        } else {
            System.out.println("[SkipOverlay] Already at max level (3), cannot skip further");
        }
        hide();
    }
}