package main;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import inputs.MouseInputs;
import inputs.KeyboardInputs;

import static utils.Constants.PlayerConstants;
import static utils.Constants.Directions.*;

public class GamePanel extends JPanel{

    private MouseInputs mouseInputs;
    private Game game;

    public GamePanel(Game game){
         mouseInputs = new MouseInputs(this);
         this.game = game;

        setPanelSize();
        addKeyListener(new KeyboardInputs(this));
        addMouseListener(mouseInputs);
        addMouseMotionListener(mouseInputs);

    }

    private void setPanelSize() {
        Dimension size = new Dimension(Game.GAME_WIDTH, Game.GAME_HEIGHT); // 1280,800
        setPreferredSize(size);
    }

    public void updateGame(){

    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.white);

        for (int i = 0; i < 100; i++)
            for (int j = 0; j < 80; j++)
                g.fillRect(i * 20, j * 20, 20, 20);

        game.render(g);

    }

    public Game getGame() {
        return game;
    }


}
