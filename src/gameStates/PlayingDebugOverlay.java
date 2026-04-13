package gameStates;

import main.Game;
import objects.WorldObjectManager;
import utils.RouteMap;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class PlayingDebugOverlay {
    private static final int GRID_MINOR_SPACING = 20;
    private static final int GRID_MAJOR_SPACING = 100;

    private boolean showLandmarkDebug = false;
    private boolean showAlignmentGrid = false;

    public void toggleLandmarkDebug() {
        showLandmarkDebug = !showLandmarkDebug;
    }

    public void toggleAlignmentGrid() {
        showAlignmentGrid = !showAlignmentGrid;
    }

    public boolean shouldHandleLandmarkDebug() {
        return showLandmarkDebug;
    }

    public boolean shouldHandleAlignmentGrid() {
        return showAlignmentGrid;
    }

    public void draw(Graphics g, WorldObjectManager worldObjectManager, RouteMap currentMap, int worldLoopCount) {
        if (showAlignmentGrid) {
            drawAlignmentGrid(g);
        }
        if (showLandmarkDebug) {
            drawLandmarkDebugOverlay(g, worldObjectManager, currentMap, worldLoopCount);
        }
    }

    private void drawLandmarkDebugOverlay(Graphics g, WorldObjectManager worldObjectManager,
                                          RouteMap currentMap, int worldLoopCount) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setFont(new Font("Consolas", Font.PLAIN, 14));

        int panelX = 12;
        int panelY = 12;
        int lineHeight = 18;
        int lineCount = worldObjectManager.getActiveLandmarkDebugEntries().size() + 2;
        int panelHeight = 16 + (lineCount * lineHeight);

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(panelX, panelY, 460, panelHeight, 12, 12);
        g2.setColor(Color.WHITE);
        g2.drawString("Landmark Debug [F3]", panelX + 12, panelY + 20);
        g2.drawString("Map: " + currentMap + " | Stop: " + worldLoopCount, panelX + 12, panelY + 38);

        int textY = panelY + 56;
        for (WorldObjectManager.LandmarkDebugEntry entry : worldObjectManager.getActiveLandmarkDebugEntries()) {
            String line = String.format(
                    "%s x=%.0f drawY=%d baseY=%d w=%d h=%d scale=%.2f offset=%.0f",
                    shortLabel(entry.label()), entry.x(), entry.y(), entry.anchorY(),
                    entry.width(), entry.height(), entry.scale(), entry.xOffset()
            );
            g2.drawString(line, panelX + 12, textY);
            g2.setColor(new Color(255, 220, 90, 180));
            g2.drawRect(Math.round(entry.x()), entry.y(), entry.width(), entry.height());
            g2.drawLine(Math.round(entry.x()), entry.anchorY(), Math.round(entry.x()) + entry.width(), entry.anchorY());
            g2.setColor(Color.WHITE);
            textY += lineHeight;
        }

        g2.dispose();
    }

    private void drawAlignmentGrid(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setFont(new Font("Consolas", Font.PLAIN, 12));

        for (int x = 0; x <= Game.GAME_WIDTH; x += GRID_MINOR_SPACING) {
            boolean major = x % GRID_MAJOR_SPACING == 0;
            boolean center = x == Game.GAME_WIDTH / 2;

            if (center) {
                g2.setColor(new Color(255, 120, 120, 220));
            } else if (major) {
                g2.setColor(new Color(255, 255, 255, 120));
            } else {
                g2.setColor(new Color(255, 255, 255, 45));
            }

            g2.drawLine(x, 0, x, Game.GAME_HEIGHT);

            if (major) {
                g2.setColor(new Color(255, 255, 255, 210));
                g2.drawString(Integer.toString(x), x + 2, 14);
            }
        }

        for (int y = 0; y <= Game.GAME_HEIGHT; y += GRID_MINOR_SPACING) {
            boolean major = y % GRID_MAJOR_SPACING == 0;
            boolean center = y == Game.GAME_HEIGHT / 2;

            if (center) {
                g2.setColor(new Color(120, 220, 255, 220));
            } else if (major) {
                g2.setColor(new Color(255, 255, 255, 120));
            } else {
                g2.setColor(new Color(255, 255, 255, 45));
            }

            g2.drawLine(0, y, Game.GAME_WIDTH, y);

            if (major && y > 0) {
                g2.setColor(new Color(255, 255, 255, 210));
                g2.drawString(Integer.toString(y), 4, y - 4);
            }
        }

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(12, Game.GAME_HEIGHT - 42, 260, 28, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawString("Alignment Grid [F4] minor=20px major=100px", 20, Game.GAME_HEIGHT - 23);
        g2.dispose();
    }

    private String shortLabel(String fullPath) {
        int slash = fullPath.lastIndexOf('/');
        return slash >= 0 ? fullPath.substring(slash + 1) : fullPath;
    }
}
