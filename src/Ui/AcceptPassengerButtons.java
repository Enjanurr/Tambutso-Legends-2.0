package Ui;

import utils.LoadSave;
import static utils.Constants.UI.Buttons.*;

import java.awt.*;
import java.awt.image.BufferedImage;

public class AcceptPassengerButtons {

    private int xPos, yPos, rowIndex, index;
    private int xOffsetCenter = B_WIDTH / 2;    // identical to MenuButton

    private boolean mouseOver, mousePressed;
    private BufferedImage[] imgs;
    private Rectangle bounds;

    public AcceptPassengerButtons(int xPos, int yPos, int rowIndex) {
        this.xPos     = xPos;
        this.yPos     = yPos;
        this.rowIndex = rowIndex;
        loadImages();
        initBounds();
    }

    private void initBounds() {
        bounds = new Rectangle(xPos - xOffsetCenter, yPos, B_WIDTH, B_HEIGHT);
    }

    // Mirrors MenuButton.loadImages() — just swaps atlas path
    private void loadImages() {
        imgs = new BufferedImage[3];                        // 3 states: normal, hover, pressed
        BufferedImage temp = LoadSave.getSpriteAtlas(LoadSave.MENU_BUTTONS);


        for (int i = 0; i < imgs.length; i++) {
            imgs[i] = temp.getSubimage(
                    i * B_WIDTH_DEFAULT,
                    rowIndex * B_HEIGHT_DEFAULT,
                    B_WIDTH_DEFAULT,
                    B_HEIGHT_DEFAULT);
        }
    }

    // Identical to MenuButton.draw()
    public void draw(Graphics g) {
        g.drawImage(imgs[index], xPos - xOffsetCenter, yPos, B_WIDTH, B_HEIGHT, null);
    }

    // Identical to MenuButton.update()
    public void update() {
        index = 0;
        if (mouseOver)    index = 1;
        if (mousePressed) index = 2;
    }

    public boolean   isMouseOver()              { return mouseOver; }
    public void      setMouseOver(boolean v)    { mouseOver = v; }
    public boolean   isMousePressed()           { return mousePressed; }
    public void      setMousePressed(boolean v) { mousePressed = v; }
    public void      resetBools()               { mouseOver = false; mousePressed = false; }
    public Rectangle getBounds()                { return bounds; }
}