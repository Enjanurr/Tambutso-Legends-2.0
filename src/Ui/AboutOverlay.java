package Ui;

import main.Game;
import utils.LoadSave;
import utils.Constants.AboutButtons;  // Add this import

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * About overlay with multi-page content (11 pages, 1000×500 each).
 * Shows transparent overlay over menu background.
 */
public class AboutOverlay {
    private static final int TOTAL_PAGES = 11;  // 0-10
    private static final int PAGE_WIDTH = 1000;
    private static final int PAGE_HEIGHT = 500;

    private BufferedImage[] pages;
    private int currentPage = 0;
    private boolean isOpen = false;

    // Buttons
    private AboutButton exitButton;
    private AboutButton backButton;
    private AboutButton nextButton;
    private AboutButton creditsButton;  // Shown on last page instead of Next

    // Callbacks
    private Runnable onClose;
    private Runnable onOpenCredits;

    // Image position (centered)
    private int imageX, imageY;

    public AboutOverlay(Runnable onClose, Runnable onOpenCredits) {
        this.onClose = onClose;
        this.onOpenCredits = onOpenCredits;
        loadPages();
    }

    private void loadPages() {
        pages = new BufferedImage[TOTAL_PAGES];
        BufferedImage fullAtlas = LoadSave.getSpriteAtlas(LoadSave.ABOUT_GAME_IMG);

        if (fullAtlas == null) {
            System.err.println("[AboutOverlay] Failed to load about_game.png");
            return;
        }

        // Extract each page (1000×500) from the 11000×500 sprite sheet
        for (int i = 0; i < TOTAL_PAGES; i++) {
            pages[i] = fullAtlas.getSubimage(i * PAGE_WIDTH, 0, PAGE_WIDTH, PAGE_HEIGHT);
        }
    }

    private void initButtonPositions() {
        // Exit button: top-right corner
        int exitX = Game.GAME_WIDTH - AboutButtons.BUTTON_WIDTH - 40;
        int exitY = 20;
        exitButton = new AboutButton(exitX, exitY, AboutButtons.EXIT_ROW);

        // Back button: bottom-left corner
        int backX = 20;
        int backY = Game.GAME_HEIGHT - AboutButtons.BUTTON_HEIGHT - 30;
        backButton = new AboutButton(backX, backY, AboutButtons.BACK_ROW);

        // Next button: bottom-right corner
        int nextX = Game.GAME_WIDTH - AboutButtons.BUTTON_WIDTH - 40;
        int nextY = Game.GAME_HEIGHT - AboutButtons.BUTTON_HEIGHT - 30;
        nextButton = new AboutButton(nextX, nextY, AboutButtons.NEXT_ROW);

        // Credits button: same position as Next (shown on last page)
        creditsButton = new AboutButton(nextX, nextY, AboutButtons.CREDITS_ROW);

        // Center the page image
        imageX = (Game.GAME_WIDTH - PAGE_WIDTH) / 2;
        imageY = (Game.GAME_HEIGHT - PAGE_HEIGHT) / 2;
    }

    public boolean open() {
        if (isOpen) return false;
        initButtonPositions();
        currentPage = 0;
        isOpen = true;
        resetButtons();
        System.out.println("[AboutOverlay] Opened - page 0/" + (TOTAL_PAGES - 1));
        return true;
    }

    public void close() {
        isOpen = false;
        currentPage = 0;
        if (onClose != null) onClose.run();
        System.out.println("[AboutOverlay] Closed");
    }

    private void resetButtons() {
        if (exitButton != null) exitButton.resetBools();
        if (backButton != null) backButton.resetBools();
        if (nextButton != null) nextButton.resetBools();
        if (creditsButton != null) creditsButton.resetBools();
    }

    public void update() {
        if (!isOpen) return;

        // Update buttons
        exitButton.update();

        // Show Next or Credits based on current page
        if (currentPage >= TOTAL_PAGES - 1) {
            creditsButton.update();
        } else {
            nextButton.update();
        }

        // Back button is only active on page > 0
        if (currentPage > 0) {
            backButton.update();
        }
    }

    public void draw(Graphics g) {
        if (!isOpen) return;

        // Draw semi-transparent background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);

        // Draw current page
        if (pages != null && pages[currentPage] != null) {
            g.drawImage(pages[currentPage], imageX, imageY, PAGE_WIDTH, PAGE_HEIGHT, null);
        } else {
            // Fallback: draw placeholder
            g.setColor(Color.DARK_GRAY);
            g.fillRect(imageX, imageY, PAGE_WIDTH, PAGE_HEIGHT);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            String text = "About Game - Page " + (currentPage + 1) + "/" + TOTAL_PAGES;
            FontMetrics fm = g.getFontMetrics();
            int textX = imageX + (PAGE_WIDTH - fm.stringWidth(text)) / 2;
            int textY = imageY + PAGE_HEIGHT / 2;
            g.drawString(text, textX, textY);
        }

        // Draw buttons
        exitButton.draw(g);

        if (currentPage > 0) {
            backButton.draw(g);
        }

        if (currentPage >= TOTAL_PAGES - 1) {
            creditsButton.draw(g);
        } else {
            nextButton.draw(g);
        }
    }

    public void mousePressed(MouseEvent e) {
        if (!isOpen) return;

        if (exitButton.getBounds().contains(e.getX(), e.getY())) {
            exitButton.setMousePressed(true);
        } else if (currentPage > 0 && backButton.getBounds().contains(e.getX(), e.getY())) {
            backButton.setMousePressed(true);
        } else if (currentPage >= TOTAL_PAGES - 1 && creditsButton.getBounds().contains(e.getX(), e.getY())) {
            creditsButton.setMousePressed(true);
        } else if (currentPage < TOTAL_PAGES - 1 && nextButton.getBounds().contains(e.getX(), e.getY())) {
            nextButton.setMousePressed(true);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (!isOpen) return;

        // Exit button
        if (exitButton.getBounds().contains(e.getX(), e.getY()) && exitButton.isMousePressed()) {
            close();
            exitButton.setMousePressed(false);
            return;
        }

        // Back button
        if (currentPage > 0 && backButton.getBounds().contains(e.getX(), e.getY()) && backButton.isMousePressed()) {
            currentPage--;
            System.out.println("[AboutOverlay] Page: " + currentPage);
            backButton.setMousePressed(false);
            return;
        }

        // Credits button (last page)
        if (currentPage >= TOTAL_PAGES - 1 && creditsButton.getBounds().contains(e.getX(), e.getY()) && creditsButton.isMousePressed()) {
            System.out.println("[AboutOverlay] Credits clicked - opening Credits overlay");
            creditsButton.setMousePressed(false);
            close();
            if (onOpenCredits != null) onOpenCredits.run();
            return;
        }

        // Next button
        if (currentPage < TOTAL_PAGES - 1 && nextButton.getBounds().contains(e.getX(), e.getY()) && nextButton.isMousePressed()) {
            currentPage++;
            System.out.println("[AboutOverlay] Page: " + currentPage);
            nextButton.setMousePressed(false);
        }

        resetButtonPressedStates();
    }

    private void resetButtonPressedStates() {
        exitButton.setMousePressed(false);
        backButton.setMousePressed(false);
        nextButton.setMousePressed(false);
        creditsButton.setMousePressed(false);
    }

    public void mouseMoved(MouseEvent e) {
        if (!isOpen) return;

        // Reset hover states
        exitButton.setMouseOver(false);
        backButton.setMouseOver(false);
        nextButton.setMouseOver(false);
        creditsButton.setMouseOver(false);

        // Set hover for exit button
        if (exitButton.getBounds().contains(e.getX(), e.getY())) {
            exitButton.setMouseOver(true);
        }

        // Set hover for back button (if enabled)
        if (currentPage > 0 && backButton.getBounds().contains(e.getX(), e.getY())) {
            backButton.setMouseOver(true);
        }

        // Set hover for next/credits button
        if (currentPage >= TOTAL_PAGES - 1) {
            if (creditsButton.getBounds().contains(e.getX(), e.getY())) {
                creditsButton.setMouseOver(true);
            }
        } else {
            if (nextButton.getBounds().contains(e.getX(), e.getY())) {
                nextButton.setMouseOver(true);
            }
        }
    }

    public void handleEsc() {
        if (isOpen) {
            close();
        }
    }

    public boolean isOpen() { return isOpen; }
}