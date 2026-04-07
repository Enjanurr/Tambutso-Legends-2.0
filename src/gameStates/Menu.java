package gameStates;

import Ui.MenuButton;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class Menu extends State implements StateMethods {

    private MenuButton[] buttons = new MenuButton[3];

    private BufferedImage backgroundImg;
    private int menuX, menuY, menuWidth, menuHeight;

    private BufferedImage backgroundImgPink;

    public Menu(Game game) {
        super(game);
        loadButtons();
        loadBackground();
        backgroundImgPink = LoadSave.getSpriteAtlas(LoadSave.MENU_BACKGROUND_IMG);
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
        if (backgroundImgPink != null)
            g.drawImage(backgroundImgPink, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        g.drawImage(backgroundImg, menuX, menuY, menuWidth, menuHeight, null);
        for (MenuButton mb : buttons) mb.draw(g);
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        for (MenuButton mb : buttons) {
            if (isIn(e, mb)) { mb.setMousePressed(true); break; }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        for (int i = 0; i < buttons.length; i++) {
            MenuButton mb = buttons[i];
            if (isIn(e, mb) && mb.isMousePressed()) {
                if (i == 0)
                    game.startPlayingOrIntro(); // PLAY → handles intro internally
                else
                    mb.applyGameState();        // OPTIONS / QUIT work normally
                break;
            }
        }
        resetButtons();
    }

    private void resetButtons() {
        for (MenuButton mb : buttons) mb.resetBools();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        for (MenuButton mb : buttons) mb.setMouseOver(false);
        for (MenuButton mb : buttons) {
            if (isIn(e, mb)) { mb.setMouseOver(true); break; }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER)
            game.startPlayingOrIntro();
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}