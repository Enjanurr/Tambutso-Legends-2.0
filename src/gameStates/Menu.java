package gameStates;

import Ui.MenuButton;
import Ui.AboutButton;
import Ui.AboutOverlay;
import Ui.CreditsOverlay;
import main.Game;
import utils.LoadSave;
import utils.Constants.AboutButtons;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class Menu extends State implements StateMethods {

    // ── Visual adjustments ─────────────────────────────────────
    private static final float BACKGROUND_RENDER_SCALE = 0.9f;  // Make background 70% of original size
    private static final int MENU_BG_WIDTH_DEFAULT = 282;
    private static final int MENU_BG_HEIGHT_DEFAULT = 400;

    // Background Y position offset (positive = move down, negative = move up)
    private static final int BACKGROUND_Y_OFFSET = -40;  // Move background 20px up (adjust as needed)

    // Button Y positions (allows easy adjustment)
    private static final int PLAY_BUTTON_Y = 115;      // PLAY button Y position
    private static final int OPTIONS_BUTTON_Y = 175;   // OPTIONS button Y position
    private static final int QUIT_BUTTON_Y = 235;      // QUIT button Y position
    private static final int ABOUT_BUTTON_Y = 295;     // ABOUT GAME button Y position

    private MenuButton[] buttons = new MenuButton[3];
    private AboutButton aboutGameButton;
    private Rectangle aboutButtonBounds;

    private BufferedImage backgroundImg;
    private int menuX, menuY, menuWidth, menuHeight;

    private BufferedImage backgroundImgPink;

    // Overlays
    private AboutOverlay aboutOverlay;
    private CreditsOverlay creditsOverlay;
    private boolean aboutOpen = false;
    private boolean creditsOpen = false;

    public Menu(Game game) {
        super(game);
        loadButtons();
        loadBackground();
        backgroundImgPink = LoadSave.getSpriteAtlas(LoadSave.MENU_BACKGROUND_IMG);

        // Initialize overlays
        aboutOverlay = new AboutOverlay(
                () -> { aboutOpen = false; },
                () -> {
                    aboutOpen = false;
                    openCreditsOverlay();
                }
        );
        creditsOverlay = new CreditsOverlay(() -> { creditsOpen = false; });

        // Initialize About Game button
        initAboutButton();
    }

    private void initAboutButton() {
        int aboutButtonYPos = (int)(ABOUT_BUTTON_Y * Game.SCALE);
        int aboutButtonXPos = Game.GAME_WIDTH / 2 - AboutButtons.BUTTON_WIDTH / 2;

        aboutGameButton = new AboutButton(aboutButtonXPos, aboutButtonYPos, AboutButtons.ABOUT_GAME_ROW);
        aboutButtonBounds = aboutGameButton.getBounds();

        System.out.println("[Menu] About Game button created at Y=" + aboutButtonYPos +
                " using row " + AboutButtons.ABOUT_GAME_ROW);
    }

    private void openCreditsOverlay() {
        creditsOpen = creditsOverlay.open();
        if (creditsOpen) {
            System.out.println("[Menu] Credits overlay opened");
        }
    }

    private void loadBackground() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.MENU_BACKGROUNDS);

        // Apply render scale to make background smaller/larger
        menuWidth = (int)(MENU_BG_WIDTH_DEFAULT * Game.SCALE * BACKGROUND_RENDER_SCALE);
        menuHeight = (int)(MENU_BG_HEIGHT_DEFAULT * Game.SCALE * BACKGROUND_RENDER_SCALE);
        menuX = Game.GAME_WIDTH / 2 - menuWidth / 2;
        menuY = (int)(45 * Game.SCALE) + BACKGROUND_Y_OFFSET;

        System.out.println("[Menu] Background loaded: " + menuWidth + "x" + menuHeight +
                " at Y=" + menuY + " (renderScale=" + BACKGROUND_RENDER_SCALE + ")");
    }

    private void loadButtons() {
        // Button positions now use the constants for easy adjustment
        buttons[0] = new MenuButton(Game.GAME_WIDTH / 2, (int)(PLAY_BUTTON_Y * Game.SCALE), 0, GameStates.PLAYING);
        buttons[1] = new MenuButton(Game.GAME_WIDTH / 2, (int)(OPTIONS_BUTTON_Y * Game.SCALE), 1, GameStates.OPTIONS);
        buttons[2] = new MenuButton(Game.GAME_WIDTH / 2, (int)(QUIT_BUTTON_Y * Game.SCALE), 2, GameStates.QUIT);

        System.out.println("[Menu] Buttons loaded at Y positions: " +
                PLAY_BUTTON_Y + ", " + OPTIONS_BUTTON_Y + ", " + QUIT_BUTTON_Y);
    }

    @Override
    public void update() {
        if (creditsOpen) {
            creditsOverlay.update();
        } else if (aboutOpen) {
            aboutOverlay.update();
        } else {
            for (MenuButton mb : buttons) mb.update();
            if (aboutGameButton != null) aboutGameButton.update();
        }
    }

    @Override
    public void draw(Graphics g) {
        if (backgroundImgPink != null)
            g.drawImage(backgroundImgPink, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);

        // Draw menu background and buttons ONLY if no overlays are open
        if (!aboutOpen && !creditsOpen) {
            g.drawImage(backgroundImg, menuX, menuY, menuWidth, menuHeight, null);
            for (MenuButton mb : buttons) mb.draw(g);
            if (aboutGameButton != null) aboutGameButton.draw(g);
        }

        // Draw overlays on top
        if (aboutOpen) aboutOverlay.draw(g);
        if (creditsOpen) creditsOverlay.draw(g);
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if (creditsOpen) {
            creditsOverlay.mousePressed(e);
        } else if (aboutOpen) {
            aboutOverlay.mousePressed(e);
        } else {
            for (MenuButton mb : buttons) {
                if (isIn(e, mb)) { mb.setMousePressed(true); break; }
            }
            if (aboutGameButton != null && aboutButtonBounds.contains(e.getX(), e.getY())) {
                aboutGameButton.setMousePressed(true);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (creditsOpen) {
            creditsOverlay.mouseReleased(e);
            return;
        }

        if (aboutOpen) {
            aboutOverlay.mouseReleased(e);
            return;
        }

        for (int i = 0; i < buttons.length; i++) {
            MenuButton mb = buttons[i];
            if (isIn(e, mb) && mb.isMousePressed()) {
                if (i == 0) {
                    game.startOrResumeGame();
                } else {
                    mb.applyGameState();
                }
                break;
            }
        }

        if (aboutGameButton != null && aboutButtonBounds.contains(e.getX(), e.getY()) && aboutGameButton.isMousePressed()) {
            aboutOpen = aboutOverlay.open();
            System.out.println("[Menu] About overlay opened: " + aboutOpen);
        }

        resetButtons();
    }

    private void resetButtons() {
        for (MenuButton mb : buttons) mb.resetBools();
        if (aboutGameButton != null) aboutGameButton.resetBools();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (creditsOpen) {
            creditsOverlay.mouseMoved(e);
            return;
        }

        if (aboutOpen) {
            aboutOverlay.mouseMoved(e);
            return;
        }

        for (MenuButton mb : buttons) mb.setMouseOver(false);
        if (aboutGameButton != null) aboutGameButton.setMouseOver(false);

        for (MenuButton mb : buttons) {
            if (isIn(e, mb)) { mb.setMouseOver(true); break; }
        }

        if (aboutGameButton != null && aboutButtonBounds.contains(e.getX(), e.getY())) {
            aboutGameButton.setMouseOver(true);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (creditsOpen) {
                creditsOverlay.handleEsc();
            } else if (aboutOpen) {
                aboutOverlay.handleEsc();
            }
            return;
        }

        if (!aboutOpen && !creditsOpen && e.getKeyCode() == KeyEvent.VK_ENTER) {
            game.startIntroOverlay();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    public boolean isAboutOpen() { return aboutOpen; }
    public boolean isCreditsOpen() { return creditsOpen; }
}