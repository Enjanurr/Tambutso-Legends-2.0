package gameStates;

import LeaderBoards.LeaderboardDisplay;
import LeaderBoards.NameEntryOverlay;
import Ui.MenuButton;
import Ui.SoundButton;
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
    private SoundButton leaderboardButton;

    private BufferedImage backgroundImg;
    private int menuX, menuY, menuWidth, menuHeight;

    private BufferedImage backgroundImgPink;

    public Menu(Game game) {
        super(game);
        loadButtons();
        loadBackground();
        backgroundImgPink = LoadSave.getSpriteAtlas(LoadSave.MENU_BACKGROUND_IMG);
        nameEntryOverlay = new NameEntryOverlay(game, game.getLeaderboardManager());
        leaderboardDisplay = new LeaderboardDisplay(game, game.getLeaderboardManager());

        // Create square leaderboard button (42x42) in upper right corner
        int btnSize = (int)(42 * Game.SCALE);
        int leaderboardBtnX = Game.GAME_WIDTH - btnSize - (int)(20 * Game.SCALE);
        int leaderboardBtnY = (int)(20 * Game.SCALE);
        leaderboardButton = new SoundButton(leaderboardBtnX, leaderboardBtnY, btnSize, btnSize);
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
        if (leaderboardButton != null) leaderboardButton.update();
    }

    @Override
    public void draw(Graphics g) {
        // Draw menu background
        if (backgroundImgPink != null)
            g.drawImage(backgroundImgPink, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        g.drawImage(backgroundImg, menuX, menuY, menuWidth, menuHeight, null);
        for (MenuButton mb : buttons) mb.draw(g);

        // Draw leaderboard button
        if (leaderboardButton != null) leaderboardButton.draw(g);

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
        if (leaderboardButton != null && leaderboardButton.getBounds().contains(e.getX(), e.getY())) {
            leaderboardButton.setMousePressed(true);
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

        // Otherwise, handle menu buttons
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
        if (leaderboardButton != null && leaderboardButton.isMousePressed() &&
                leaderboardButton.getBounds().contains(e.getX(), e.getY())) {
            System.out.println("[Menu] Leaderboard button clicked - showing leaderboard");
            leaderboardDisplay.show();
        }

        resetButtons();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // If leaderboard is visible, send mouseMoved to it
        if (leaderboardDisplay != null && leaderboardDisplay.isVisible()) {
            leaderboardDisplay.mouseMoved(e);
            return;
        }

        // If name overlay is visible, don't process menu hover
        if (nameEntryOverlay.isVisible()) {
            return;
        }

        // Otherwise, handle menu button hover
        for (MenuButton mb : buttons) mb.setMouseOver(false);
        for (MenuButton mb : buttons) {
            if (isIn(e, mb)) {
                mb.setMouseOver(true);
                break;
            }
        }

        // Check leaderboard button hover
        if (leaderboardButton != null) {
            leaderboardButton.setMouseOver(leaderboardButton.getBounds().contains(e.getX(), e.getY()));
        }
    }

    private void resetButtons() {
        for (MenuButton mb : buttons) mb.resetBools();
        if (leaderboardButton != null) leaderboardButton.resetBools();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // If leaderboard is visible, send input to it
        if (leaderboardDisplay != null && leaderboardDisplay.isVisible()) {
            // Leaderboard can handle ESC to close
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
            // When ENTER is pressed on menu, show name entry
            nameEntryOverlay.show();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}