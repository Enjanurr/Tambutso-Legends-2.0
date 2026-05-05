package Ui.buttons;

import utils.LoadSave;
import utils.Constants.AboutButtons;  // Changed: UI.AboutButtons -> AboutButtons

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Custom button for about_buttons.png sprite sheet.
 * Layout: 5 rows × 3 columns, each button 140×56 pixels.
 */
public class AboutButton {
    private int x, y;
    private int rowIndex;
    private int index;  // 0=normal, 1=hover, 2=pressed
    private boolean mouseOver, mousePressed;
    private BufferedImage[] imgs;
    private Rectangle bounds;

    public AboutButton(int x, int y, int rowIndex) {
        this.x = x;
        this.y = y;
        this.rowIndex = rowIndex;
        loadImages();
        initBounds();
    }

    private void loadImages() {
        BufferedImage temp = LoadSave.getSpriteAtlas(LoadSave.ABOUT_BUTTONS);
        if (temp == null) {
            System.err.println("[AboutButton] Failed to load about_buttons.png");
            return;
        }
        imgs = new BufferedImage[3];
        for (int i = 0; i < imgs.length; i++) {
            imgs[i] = temp.getSubimage(
                    i * AboutButtons.BUTTON_WIDTH_DEFAULT,
                    rowIndex * AboutButtons.BUTTON_HEIGHT_DEFAULT,
                    AboutButtons.BUTTON_WIDTH_DEFAULT,
                    AboutButtons.BUTTON_HEIGHT_DEFAULT
            );
        }
    }

    private void initBounds() {
        bounds = new Rectangle(x, y, AboutButtons.BUTTON_WIDTH, AboutButtons.BUTTON_HEIGHT);
    }

    public void update() {
        index = 0;
        if (mouseOver) index = 1;
        if (mousePressed) index = 2;
    }

    public void draw(Graphics g) {
        if (imgs != null && imgs[index] != null) {
            g.drawImage(imgs[index], x, y, AboutButtons.BUTTON_WIDTH, AboutButtons.BUTTON_HEIGHT, null);
        } else {
            // Fallback: draw a colored rectangle
            g.setColor(mouseOver ? Color.GRAY : Color.DARK_GRAY);
            g.fillRect(x, y, AboutButtons.BUTTON_WIDTH, AboutButtons.BUTTON_HEIGHT);
            g.setColor(Color.WHITE);
            g.drawRect(x, y, AboutButtons.BUTTON_WIDTH, AboutButtons.BUTTON_HEIGHT);
        }
    }

    public void resetBools() {
        mouseOver = false;
        mousePressed = false;
    }

    public boolean isMouseOver() { return mouseOver; }
    public void setMouseOver(boolean mouseOver) { this.mouseOver = mouseOver; }
    public boolean isMousePressed() { return mousePressed; }
    public void setMousePressed(boolean mousePressed) { this.mousePressed = mousePressed; }
    public Rectangle getBounds() { return bounds; }
    public int getRowIndex() { return rowIndex; }
}





