package gameStates;

import LeaderBoards.LeaderboardDisplay;
import LeaderBoards.NameEntryOverlay;
import Ui.MenuButton;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class Menu extends State implements StateMethods {

    private MenuButton[] buttons = new MenuButton[3];
    private LeaderboardDisplay leaderboardDisplay;
    private NameEntryOverlay nameEntryOverlay;

    // Leaderboard button (126x42 total, 3 frames of 42x42)
    private Rectangle leaderboardButton;
    private BufferedImage leaderboardButtonNormal;
    private BufferedImage leaderboardButtonHover;
    private BufferedImage leaderboardButtonPressed;
    private boolean leaderboardBtnHover = false;
    private boolean leaderboardBtnPressed = false;
    private static final int LEADERBOARD_BTN_FRAME_W = 42;   // 126 / 3 = 42
    private static final int LEADERBOARD_BTN_FRAME_H = 42;

    private BufferedImage backgroundImg;
    private int menuX, menuY, menuWidth, menuHeight;

    private BufferedImage backgroundImgPink;

    public Menu(Game game) {
        super(game);
        loadButtons();
        loadBackground();
        loadLeaderboardButton();
        backgroundImgPink = LoadSave.getSpriteAtlas(LoadSave.MENU_BACKGROUND_IMG);
        nameEntryOverlay = new NameEntryOverlay(game, game.getLeaderboardManager());
        leaderboardDisplay = new LeaderboardDisplay(game, game.getLeaderboardManager());

        // Create leaderboard button bounds in upper right corner
        int btnWidth = (int)(LEADERBOARD_BTN_FRAME_W * Game.SCALE);
        int btnHeight = (int)(LEADERBOARD_BTN_FRAME_H * Game.SCALE);
        int btnX = Game.GAME_WIDTH - btnWidth - (int)(20 * Game.SCALE);
        int btnY = (int)(20 * Game.SCALE);
        leaderboardButton = new Rectangle(btnX, btnY, btnWidth, btnHeight);
    }

    private void loadLeaderboardButton() {
        BufferedImage leaderboardSheet = LoadSave.getSpriteAtlas(LoadSave.LEADERBOADS_BUTTON);
        if (leaderboardSheet != null) {
            int frameWidth = leaderboardSheet.getWidth() / 3;  // 126 / 3 = 42
            int frameHeight = leaderboardSheet.getHeight() / 2; // 84 / 2 = 42 (if 2 rows total)

            // Row 0, Col 0 = Normal (bright)
            leaderboardButtonNormal = leaderboardSheet.getSubimage(frameWidth, 0, frameWidth, frameHeight);
            // Row 0, Col 1 = Hover (darkened)
            leaderboardButtonHover = leaderboardSheet.getSubimage(0, 0, frameWidth, frameHeight);
            // Row 0, Col 2 = Pressed
            leaderboardButtonPressed = leaderboardSheet.getSubimage(frameWidth * 2, 0, frameWidth, frameHeight);

            System.out.println("[Menu] Loaded LEADERBOARD button - Normal, Hover (darkened), Pressed states");
        } else {
            System.err.println("[Menu] Failed to load leaderboard button sheet");
        }
    }

    private void drawLeaderboardButton(Graphics g) {
        BufferedImage btnImage = null;

        // Priority: Pressed > Hover > Normal
        if (leaderboardBtnPressed && leaderboardButtonPressed != null) {
            btnImage = leaderboardButtonPressed;
        }
        // Hover state (col 1 - darkened)
        else if (leaderboardBtnHover && leaderboardButtonHover != null) {
            btnImage = leaderboardButtonHover;
        }
        // Normal state (col 0 - bright)
        else if (leaderboardButtonNormal != null) {
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
        // Normal state (bright)
        if (!leaderboardBtnHover && !leaderboardBtnPressed) {
            g.setColor(new Color(70, 50, 150, 200));
        }
        // Hover state (darkened)
        else if (leaderboardBtnHover && !leaderboardBtnPressed) {
            g.setColor(new Color(50, 35, 110, 200));
        }
        // Pressed state (darker)
        else {
            g.setColor(new Color(30, 20, 70, 200));
        }

        g.fillRoundRect(leaderboardButton.x, leaderboardButton.y,
                leaderboardButton.width, leaderboardButton.height, 10, 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, (int)(12 * Game.SCALE)));
        g.drawString("🏆", leaderboardButton.x + leaderboardButton.width / 3,
                leaderboardButton.y + leaderboardButton.height - 12);
    }
    private void loadBackground() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.MENU_BACKGROUNDS);
        menuWidth  = (int)(backgroundImg.getWidth()  * Game.SCALE);
        menuHeight = (int)(backgroundImg.getHeight() * Game.SCALE);
        menuX = Game.GAME_WIDTH  / 2 - menuWidth  / 2;
        menuY = (int)(45 * Game.SCALE);
    }

    private void loadButtons() {
        buttons[0] = new MenuButton(Game.GAME_WIDTH / 2, (int)(150 * Game.SCALE), 0, GameStates.PLAYING);
        buttons[1] = new MenuButton(Game.GAME_WIDTH / 2, (int)(220 * Game.SCALE), 1, GameStates.OPTIONS);
        buttons[2] = new MenuButton(Game.GAME_WIDTH / 2, (int)(290 * Game.SCALE), 2, GameStates.QUIT);
    }

    @Override
    public void update() {
        for (MenuButton mb : buttons) mb.update();
    }

    @Override
    public void draw(Graphics g) {
        // Draw menu background
        if (backgroundImgPink != null)
            g.drawImage(backgroundImgPink, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        g.drawImage(backgroundImg, menuX, menuY, menuWidth, menuHeight, null);
        for (MenuButton mb : buttons) mb.draw(g);

        // Draw leaderboard button
        drawLeaderboardButton(g);

        // Draw name overlay on top if visible
        if (nameEntryOverlay.isVisible()) {
            nameEntryOverlay.render(g);
        }

        // Draw leaderboard display on top if visible
        if (leaderboardDisplay != null && leaderboardDisplay.isVisible()) {
            leaderboardDisplay.render(g);
        }
    }



    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        // If leaderboard is visible, send input to it first
        if (leaderboardDisplay != null && leaderboardDisplay.isVisible()) {
            leaderboardDisplay.mousePressed(e);
            return;
        }

        // If name overlay is visible, send input to it
        if (nameEntryOverlay.isVisible()) {
            nameEntryOverlay.mousePressed(e);
            return;
        }

        // Otherwise, handle menu buttons
        for (MenuButton mb : buttons) {
            if (isIn(e, mb)) {
                mb.setMousePressed(true);
                break;
            }
        }

        // Check leaderboard button
        if (leaderboardButton != null && leaderboardButton.contains(e.getX(), e.getY())) {
            leaderboardBtnPressed = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // If leaderboard is visible, send mouseReleased to it
        if (leaderboardDisplay != null && leaderboardDisplay.isVisible()) {
            leaderboardDisplay.mouseReleased(e);
            return;
        }

        // If name overlay is visible, send mouseReleased to it
        if (nameEntryOverlay.isVisible()) {
            nameEntryOverlay.mouseReleased(e);
            return;
        }

        // Otherwise, handle menu buttons clicks
        for (int i = 0; i < buttons.length; i++) {
            MenuButton mb = buttons[i];
            if (isIn(e, mb) && mb.isMousePressed()) {
                if (i == 0) {
                    // PLAY button clicked - show name entry overlay
                    System.out.println("[Menu] Play button clicked - showing name entry");
                    nameEntryOverlay.show();
                } else {
                    mb.applyGameState(); // OPTIONS / QUIT work normally
                }
                break;
            }
        }

        // Handle leaderboard button click
        if (leaderboardBtnPressed && leaderboardButton != null && leaderboardButton.contains(e.getX(), e.getY())) {
            System.out.println("[Menu] Leaderboard button clicked - showing leaderboard");
            leaderboardDisplay.show();
        }

        resetButtons();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        System.out.println("[Menu] mouseMoved - leaderboard visible: " + (leaderboardDisplay != null && leaderboardDisplay.isVisible()) +
                ", name overlay visible: " + nameEntryOverlay.isVisible());

        if (leaderboardDisplay != null && leaderboardDisplay.isVisible()) {
            leaderboardDisplay.mouseMoved(e);
            return;
        }

        if (nameEntryOverlay.isVisible()) {
            System.out.println("[Menu] Forwarding mouseMoved to NameEntryOverlay");
            nameEntryOverlay.mouseMoved(e);
            return;
        }

        // Handle menu button hover (NOT clicks)
        for (MenuButton mb : buttons) {
            mb.setMouseOver(false);
        }
        for (MenuButton mb : buttons) {
            if (isIn(e, mb)) {
                mb.setMouseOver(true);
                break;
            }
        }

        // Track leaderboard button hover
        if (leaderboardButton != null) {
            leaderboardBtnHover = leaderboardButton.contains(e.getX(), e.getY());
        }
    }

    private void resetButtons() {
        for (MenuButton mb : buttons) mb.resetBools();
        leaderboardBtnPressed = false;
        leaderboardBtnHover = false;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // If leaderboard is visible, send input to it
        if (leaderboardDisplay != null && leaderboardDisplay.isVisible()) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                leaderboardDisplay.hide();
            }
            return;
        }

        // If name overlay is visible, send input to it
        if (nameEntryOverlay.isVisible()) {
            nameEntryOverlay.keyPressed(e);
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            nameEntryOverlay.show();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}