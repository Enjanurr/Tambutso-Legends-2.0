package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

public class LevelBanner {

    // ── Banner settings ───────────────────────────────────────
    private static final float RENDER_SCALE = 0.10f;  // Reduced from 0.6f to 0.35f (35% of original)
    private static final float POS_Y_OFFSET = 10f;     // Pixels below progress bar

    private BufferedImage bannerImage;
    private int drawW, drawH, drawX, drawY;
    private int currentLevel;

    // Original dimensions (1701 x 608)
    private static final int ORIGINAL_WIDTH = 1701;
    private static final int ORIGINAL_HEIGHT = 608;

    public LevelBanner(int levelId) {
        this.currentLevel = levelId;
        loadBanner(levelId);

        // Calculate scaled dimensions based on original size
        if (bannerImage != null) {
            // Scale using the original dimensions
            drawW = (int)(ORIGINAL_WIDTH * Game.SCALE * RENDER_SCALE);
            drawH = (int)(ORIGINAL_HEIGHT * Game.SCALE * RENDER_SCALE);
        } else {
            drawW = (int)(400 * Game.SCALE * RENDER_SCALE);
            drawH = (int)(60 * Game.SCALE * RENDER_SCALE);
        }
        drawX = (Game.GAME_WIDTH - drawW) / 2;
    }

    private void loadBanner(int levelId) {
        String atlasPath;
        switch (levelId) {
            case 2:
                atlasPath = LoadSave.LEVEL_BANNER2;
                break;
            case 3:
                atlasPath = LoadSave.LEVEL_BANNER3;
                break;
            default:
                atlasPath = LoadSave.LEVEL_BANNER1;
                break;
        }
        bannerImage = LoadSave.getSpriteAtlas(atlasPath);
        if (bannerImage == null) {
            System.err.println("[LevelBanner] Could not load " + atlasPath);
        }
    }

    public void updatePosition(int progressBarY, int progressBarHeight) {
        drawY = progressBarY + progressBarHeight + (int)(POS_Y_OFFSET * Game.SCALE);
    }

    public void render(Graphics g) {
        if (bannerImage != null) {
            // Draw scaled banner
            g.drawImage(bannerImage, drawX, drawY, drawW, drawH, null);
        } else {
            // Fallback text
            g.setColor(new Color(255, 215, 0));
            g.setFont(new Font("Arial", Font.BOLD, (int)(20 * Game.SCALE)));
            String levelText = "LEVEL " + currentLevel;
            FontMetrics fm = g.getFontMetrics();
            int textX = (Game.GAME_WIDTH - fm.stringWidth(levelText)) / 2;
            int textY = drawY + drawH / 2 + fm.getAscent() / 2;
            g.drawString(levelText, textX, textY);
        }
    }

    public int getHeight() {
        return drawH;
    }
}