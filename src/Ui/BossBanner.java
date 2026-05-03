package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BossBanner {

    // ── Banner settings ───────────────────────────────────────
    private static final float RENDER_SCALE = 0.10f;  // 35% of original size
    private static final float POS_Y_OFFSET = 10f;     // Pixels from top

    private BufferedImage bannerImage;
    private int drawW, drawH, drawX, drawY;
    private int bossLevel;

    // Original dimensions (adjust based on your banner sizes)
    private static final int ORIGINAL_WIDTH = 800;   // Adjust to your banner width
    private static final int ORIGINAL_HEIGHT = 200;  // Adjust to your banner height

    public BossBanner(int bossLevel) {
        this.bossLevel = bossLevel;
        loadBanner(bossLevel);

        // Calculate scaled dimensions based on original size
        if (bannerImage != null) {
            drawW = (int)(bannerImage.getWidth() * Game.SCALE * RENDER_SCALE);
            drawH = (int)(bannerImage.getHeight() * Game.SCALE * RENDER_SCALE);
        } else {
            drawW = (int)(ORIGINAL_WIDTH * Game.SCALE * RENDER_SCALE);
            drawH = (int)(ORIGINAL_HEIGHT * Game.SCALE * RENDER_SCALE);
        }
        drawX = (Game.GAME_WIDTH - drawW) / 2;
    }

    private void loadBanner(int bossLevel) {
        String atlasPath;
        switch (bossLevel) {
            case 2:
                atlasPath = LoadSave.BOSS2BANNER;
                break;
            case 3:
                atlasPath = LoadSave.BOSS3BANNER;
                break;
            default:
                atlasPath = LoadSave.BOSS1BANNER;
                break;
        }
        bannerImage = LoadSave.getSpriteAtlas(atlasPath);
        if (bannerImage == null) {
            System.err.println("[BossBanner] Could not load " + atlasPath);
        }
    }

    public void updatePosition(int yOffset) {
        drawY = yOffset + (int)(POS_Y_OFFSET * Game.SCALE);
    }

    public void render(Graphics g) {
        if (bannerImage != null) {
            // Draw scaled banner
            g.drawImage(bannerImage, drawX, drawY, drawW, drawH, null);
        } else {
            // Fallback text
            g.setColor(new Color(255, 215, 0));
            g.setFont(new Font("Arial", Font.BOLD, (int)(24 * Game.SCALE)));
            String bossText = "BOSS " + bossLevel;
            FontMetrics fm = g.getFontMetrics();
            int textX = (Game.GAME_WIDTH - fm.stringWidth(bossText)) / 2;
            int textY = drawY + drawH / 2 + fm.getAscent() / 2;
            g.drawString(bossText, textX, textY);
        }
    }

    public int getHeight() {
        return drawH;
    }
}