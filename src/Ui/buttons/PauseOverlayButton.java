package Ui.buttons;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class PauseOverlayButton {

    private int x, y;
    private int width, height;
    private int btnWidthDefault, btnHeightDefault;
    private int index;
    private boolean mouseOver = false;
    private boolean mousePressed = false;
    private BufferedImage[] imgs;
    private Rectangle bounds;
    private Runnable onClick;

    public PauseOverlayButton(int x, int y, float scale, Runnable onClick) {
        this.x = x;
        this.y = y;
        this.onClick = onClick;
        loadImages();
        this.width = (int)(btnWidthDefault * Game.SCALE * scale);
        this.height = (int)(btnHeightDefault * Game.SCALE * scale);
        this.bounds = new Rectangle(x, y, width, height);
    }

    private void loadImages() {
        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.PAUSE_OVERLAY_BUTTON);
        if (sheet == null) {
            System.err.println("[PauseOverlayButton] Failed to load " + LoadSave.PAUSE_OVERLAY_BUTTON);
            btnWidthDefault = 126;
            btnHeightDefault = 42;
            return;
        }

        int frameWidth = sheet.getWidth() / 3;
        int frameHeight = sheet.getHeight();
        btnWidthDefault = frameWidth;
        btnHeightDefault = frameHeight;

        System.out.println("[PauseOverlayButton] Loaded: " + sheet.getWidth() + "x" + sheet.getHeight()
                + " -> button " + frameWidth + "x" + frameHeight);

        imgs = new BufferedImage[3];
        for (int i = 0; i < 3; i++) {
            imgs[i] = sheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
        }
    }

    public void update() {
        index = 0;
        if (mouseOver) index = 1;
        if (mousePressed) index = 2;
    }

    public void draw(Graphics g) {
        if (imgs != null && imgs[index] != null) {
            g.drawImage(imgs[index], x, y, width, height, null);
        } else {
            // Fallback
            g.setColor(new Color(50, 50, 50, 180));
            g.fillRoundRect(x, y, width, height, 8, 8);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, (int)(10 * Game.SCALE)));
            g.drawString("PAUSE", x + width/2 - 15, y + height/2 + 5);
        }
    }

    public void mousePressed(MouseEvent e) {
        if (bounds.contains(e.getX(), e.getY())) {
            mousePressed = true;
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (mousePressed && bounds.contains(e.getX(), e.getY())) {
            if (onClick != null) onClick.run();
        }
        mousePressed = false;
    }

    public void mouseMoved(MouseEvent e) {
        mouseOver = bounds.contains(e.getX(), e.getY());
    }

    public void resetBools() {
        mouseOver = false;
        mousePressed = false;
    }

    public Rectangle getBounds() { return bounds; }
}





