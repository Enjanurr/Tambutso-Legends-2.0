package gameStates;

import Ui.OptionsOverlay;
import main.Game;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class Options extends State implements StateMethods {

    private final OptionsOverlay optionsOverlay;

    public Options(Game game) {
        super(game);
        optionsOverlay = new OptionsOverlay(this);
    }

    @Override
    public void update() {
        optionsOverlay.update();
    }

    @Override
    public void draw(Graphics g) {
        game.getMenu().draw(g);
        optionsOverlay.draw(g);
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        optionsOverlay.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        optionsOverlay.mouseReleased(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        optionsOverlay.mouseMoved(e);
    }

    public void mouseDragged(MouseEvent e) {
        optionsOverlay.mouseDragged(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
            GameStates.state = GameStates.MENU;
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}
