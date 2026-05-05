package gameStates.menu;
import gameStates.core.*;

import LeaderBoards.LeaderboardDisplay;
import LeaderBoards.NameEntryOverlay;
import Ui.buttons.MenuButton;
import Ui.buttons.AboutButton;
import Ui.overlays.AboutOverlay;
import Ui.overlays.CreditsOverlay;
import main.Game;
import utils.LoadSave;
import utils.Constants.AboutButtons;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class Menu extends State implements StateMethods {

    // ── Visual adjustments ─────────────────────────────────────
    private static final float BACKGROUND_RENDER_SCALE = 0.9f;
    private static final int MENU_BG_WIDTH_DEFAULT = 282;
    private static final int MENU_BG_HEIGHT_DEFAULT = 400;

    private static final int BACKGROUND_Y_OFFSET = -40;

    private static final int PLAY_BUTTON_Y    = 115;
    private static final int OPTIONS_BUTTON_Y = 175;
    private static final int QUIT_BUTTON_Y    = 235;
    private static final int ABOUT_BUTTON_Y   = 295;

    private MenuButton[] buttons = new MenuButton[3];
    private AboutButton aboutGameButton;
    private Rectangle aboutButtonBounds;

    private BufferedImage backgroundImg;
    private int menuX, menuY, menuWidth, menuHeight;

    private BufferedImage backgroundImgPink;

    // Overlays
    private AboutOverlay aboutOverlay;
    private CreditsOverlay creditsOverlay;
    private boolean aboutOpen   = false;
    private boolean creditsOpen = false;

    // Leaderboard integration
    private LeaderboardDisplay leaderboardDisplay;
    private NameEntryOverlay nameEntryOverlay;

    // Leaderboard button (126x42 total, 3 frames of 42x42)
    private Rectangle leaderboardButton;
    private BufferedImage leaderboardButtonNormal;
    private BufferedImage leaderboardButtonHover;
    private BufferedImage leaderboardButtonPressed;
    private boolean leaderboardBtnHover   = false;
    private boolean leaderboardBtnPressed = false;
    private static final int LEADERBOARD_BTN_FRAME_W = 42;
    private static final int LEADERBOARD_BTN_FRAME_H = 42;

    public Menu(Game game) {
        super(game);
        loadButtons();
        loadBackground();
        backgroundImgPink = LoadSave.getSpriteAtlas(LoadSave.MENU_BACKGROUND_IMG);

        aboutOverlay = new AboutOverlay(
                () -> { aboutOpen = false; },
                () -> {
                    aboutOpen = false;
                    openCreditsOverlay();
                }
        );
        creditsOverlay = new CreditsOverlay(() -> { creditsOpen = false; });

        initAboutButton();

        nameEntryOverlay  = new NameEntryOverlay(game, game.getLeaderboardManager());
        leaderboardDisplay = new LeaderboardDisplay(game, game.getLeaderboardManager());
        loadLeaderboardButton();

        int btnWidth  = (int)(LEADERBOARD_BTN_FRAME_W * Game.SCALE);
        int btnHeight = (int)(LEADERBOARD_BTN_FRAME_H * Game.SCALE);
        int btnX      = Game.GAME_WIDTH - btnWidth - (int)(20 * Game.SCALE);
        int btnY      = (int)(20 * Game.SCALE);
        leaderboardButton = new Rectangle(btnX, btnY, btnWidth, btnHeight);
    }

    private void initAboutButton() {
        int aboutButtonYPos = (int)(ABOUT_BUTTON_Y * Game.SCALE);
        int aboutButtonXPos = Game.GAME_WIDTH / 2 - AboutButtons.BUTTON_WIDTH / 2;

        aboutGameButton  = new AboutButton(aboutButtonXPos, aboutButtonYPos, AboutButtons.ABOUT_GAME_ROW);
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

    private void loadLeaderboardButton() {
        BufferedImage leaderboardSheet = LoadSave.getSpriteAtlas(LoadSave.LEADERBOADS_BUTTON);
        if (leaderboardSheet != null) {
            int frameWidth  = leaderboardSheet.getWidth()  / 3;
            int frameHeight = leaderboardSheet.getHeight() / 2;

            leaderboardButtonNormal  = leaderboardSheet.getSubimage(frameWidth,     0, frameWidth, frameHeight);
            leaderboardButtonHover   = leaderboardSheet.getSubimage(0,              0, frameWidth, frameHeight);
            leaderboardButtonPressed = leaderboardSheet.getSubimage(frameWidth * 2, 0, frameWidth, frameHeight);

            System.out.println("[Menu] Loaded LEADERBOARD button - Normal, Hover (darkened), Pressed states");
        } else {
            System.err.println("[Menu] Failed to load leaderboard button sheet");
        }
    }

    private void drawLeaderboardButton(Graphics g) {
        BufferedImage btnImage = null;

        if (leaderboardBtnPressed && leaderboardButtonPressed != null) {
            btnImage = leaderboardButtonPressed;
        } else if (leaderboardBtnHover && leaderboardButtonHover != null) {
            btnImage = leaderboardButtonHover;
        } else if (leaderboardButtonNormal != null) {
            btnImage = leaderboardButtonNormal;
        }

        if (btnImage != null) {
            g.drawImage(btnImage, leaderboardButton.x, leaderboardButton.y,
                    leaderboardButton.width, leaderboardButton.height, null);
        } else {
            drawFallbackLeaderboardButton(g);
        }
    }

    private void drawFallbackLeaderboardButton(Graphics g) {
        if (!leaderboardBtnHover && !leaderboardBtnPressed) {
            g.setColor(new Color(70, 50, 150, 200));
        } else if (leaderboardBtnHover && !leaderboardBtnPressed) {
            g.setColor(new Color(50, 35, 110, 200));
        } else {
            g.setColor(new Color(30, 20, 70, 200));
        }

        g.fillRoundRect(leaderboardButton.x, leaderboardButton.y,
                leaderboardButton.width, leaderboardButton.height, 10, 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, (int)(12 * Game.SCALE)));
        g.drawString("\uD83C\uDFC6", leaderboardButton.x + leaderboardButton.width / 3,
                leaderboardButton.y + leaderboardButton.height - 12);
    }

    private void loadBackground() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.MENU_BACKGROUNDS);

        menuWidth  = (int)(MENU_BG_WIDTH_DEFAULT  * Game.SCALE * BACKGROUND_RENDER_SCALE);
        menuHeight = (int)(MENU_BG_HEIGHT_DEFAULT * Game.SCALE * BACKGROUND_RENDER_SCALE);
        menuX      = Game.GAME_WIDTH / 2 - menuWidth / 2;
        menuY      = (int)(45 * Game.SCALE) + BACKGROUND_Y_OFFSET;

        System.out.println("[Menu] Background loaded: " + menuWidth + "x" + menuHeight +
                " at Y=" + menuY + " (renderScale=" + BACKGROUND_RENDER_SCALE + ")");
    }

    private void loadButtons() {
        buttons[0] = new MenuButton(Game.GAME_WIDTH / 2, (int)(PLAY_BUTTON_Y    * Game.SCALE), 0, GameStates.PLAYING);
        buttons[1] = new MenuButton(Game.GAME_WIDTH / 2, (int)(OPTIONS_BUTTON_Y * Game.SCALE), 1, GameStates.OPTIONS);
        buttons[2] = new MenuButton(Game.GAME_WIDTH / 2, (int)(QUIT_BUTTON_Y    * Game.SCALE), 2, GameStates.QUIT);

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

        if (!aboutOpen && !creditsOpen) {
            g.drawImage(backgroundImg, menuX, menuY, menuWidth, menuHeight, null);
            for (MenuButton mb : buttons) mb.draw(g);
            if (aboutGameButton != null) aboutGameButton.draw(g);
        }

        drawLeaderboardButton(g);

        if (nameEntryOverlay != null && nameEntryOverlay.isVisible()) {
            nameEntryOverlay.render(g);
        }

        if (leaderboardDisplay != null && leaderboardDisplay.isVisible()) {
            leaderboardDisplay.render(g);
        }

        if (aboutOpen)   aboutOverlay.draw(g);
        if (creditsOpen) creditsOverlay.draw(g);
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if (leaderboardDisplay != null && leaderboardDisplay.isVisible()) {
            leaderboardDisplay.mousePressed(e);
            return;
        }

        if (nameEntryOverlay != null && nameEntryOverlay.isVisible()) {
            nameEntryOverlay.mousePressed(e);
            return;
        }

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

            if (leaderboardButton != null && leaderboardButton.contains(e.getX(), e.getY())) {
                leaderboardBtnPressed = true;
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

        if (leaderboardDisplay != null && leaderboardDisplay.isVisible()) {
            leaderboardDisplay.mouseReleased(e);
            return;
        }

        if (nameEntryOverlay != null && nameEntryOverlay.isVisible()) {
            nameEntryOverlay.mouseReleased(e);
            return;
        }

        for (int i = 0; i < buttons.length; i++) {
            MenuButton mb = buttons[i];
            if (isIn(e, mb) && mb.isMousePressed()) {
                if (i == 0) {
                    handlePlayButtonClicked(); // ← CRITICAL: extracted to its own method
                } else {
                    mb.applyGameState();
                }
                break;
            }
        }

        if (leaderboardBtnPressed && leaderboardButton != null && leaderboardButton.contains(e.getX(), e.getY())) {
            System.out.println("[Menu] Leaderboard button clicked - showing leaderboard");
            if (leaderboardDisplay != null) leaderboardDisplay.show();
        }

        if (aboutGameButton != null && aboutButtonBounds.contains(e.getX(), e.getY()) && aboutGameButton.isMousePressed()) {
            aboutOpen = aboutOverlay.open();
            System.out.println("[Menu] About overlay opened: " + aboutOpen);
        }

        resetButtons();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRITICAL FIX: PLAY button routing logic
    //
    // Three scenarios when PLAY is pressed:
    //
    //   1. needsFullReset == true  (game was just completed)
    //      → Show NameEntryOverlay so player enters a new name.
    //        NameEntryOverlay.startGame() then calls game.startOrResumeGame()
    //        which detects needsFullReset, resets to Level 1, resets intro,
    //        and proceeds to char select.
    //
    //   2. hasActiveGame == true  (mid-game, player went to menu via pause)
    //      → Resume immediately via game.startOrResumeGame().
    //        No name entry needed — it's the same player continuing.
    //
    //   3. Neither flag set  (brand-new session, no game ever started)
    //      → Show NameEntryOverlay so player enters their name first.
    //        NameEntryOverlay.startGame() calls game.startOrResumeGame()
    //        which goes straight to char select (no reset needed).
    //
    // ─────────────────────────────────────────────────────────────────────────
    private void handlePlayButtonClicked() {
        System.out.println("[Menu] Play button clicked — needsFullReset=" + game.needsFullReset() +
                ", hasActiveGame=" + game.hasActiveGame());

        if (game.hasActiveGame() && !game.needsFullReset()) {
            // SCENARIO 2: Resume an in-progress game — skip name entry entirely.
            System.out.println("[Menu] Resuming active game — skipping name entry");
            game.startOrResumeGame();
        } else {
            // SCENARIO 1 (needsFullReset) or SCENARIO 3 (fresh start):
            // Show name entry so the player identifies themselves.
            // NameEntryOverlay.startGame() will call game.startOrResumeGame()
            // which handles the reset (if flagged) before going to char select.
            System.out.println("[Menu] Showing name entry overlay");
            if (nameEntryOverlay != null) nameEntryOverlay.show();
        }
    }

    private void resetButtons() {
        for (MenuButton mb : buttons) mb.resetBools();
        if (aboutGameButton != null) aboutGameButton.resetBools();
        leaderboardBtnPressed = false;
        leaderboardBtnHover   = false;
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

        if (leaderboardDisplay != null && leaderboardDisplay.isVisible()) {
            leaderboardDisplay.mouseMoved(e);
            return;
        }

        if (nameEntryOverlay != null && nameEntryOverlay.isVisible()) {
            nameEntryOverlay.mouseMoved(e);
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

        if (leaderboardButton != null) {
            leaderboardBtnHover = leaderboardButton.contains(e.getX(), e.getY());
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (nameEntryOverlay != null && nameEntryOverlay.isVisible()) {
            nameEntryOverlay.keyPressed(e);
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (creditsOpen) {
                creditsOverlay.handleEsc();
            } else if (aboutOpen) {
                aboutOverlay.handleEsc();
            } else if (leaderboardDisplay != null && leaderboardDisplay.isVisible()) {
                leaderboardDisplay.hide();
            }
            return;
        }

        if (!aboutOpen && !creditsOpen && !leaderboardDisplay.isVisible() && e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (nameEntryOverlay != null) {
                nameEntryOverlay.show();
            } else {
                game.startIntroOverlay();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    public boolean isAboutOpen()   { return aboutOpen; }
    public boolean isCreditsOpen() { return creditsOpen; }
}






