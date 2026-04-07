package Ui;

import gameStates.Playing;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static utils.Constants.UI.URMButtons.*;


public class DeathOverlay {

    private final Playing playing;

    // ── Death screen image ────────────────────────────────────
    private BufferedImage deathImg;
    private int imgW, imgH, imgX, imgY;


    // -------------------------------------------------------
    // REPLAY BUTTON POSITION  ← ADJUST
    // -------------------------------------------------------
    private static final int REPLAY_X = (int)(374 * Game.SCALE);
    private static final int REPLAY_Y = (int)(325 * Game.SCALE);
    // -------------------------------------------------------
    private UrmButton replayB;

    // ── Fade-in ───────────────────────────────────────────────
    // -------------------------------------------------------
    // FADE SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float FADE_SPEED = 0.03f; // alpha added per tick (200 UPS)
    // -------------------------------------------------------
    private float   overlayAlpha = 0f;
    private boolean fadeComplete = false;

    public DeathOverlay(Playing playing) {
        this.playing = playing;
        loadImage();
        buildLayout();
    }

    private void loadImage() {
        deathImg = LoadSave.getSpriteAtlas(LoadSave.DEATH_SCREEN);
        if (deathImg == null)
            System.err.println("[DeathOverlay] Could not load " + LoadSave.DEATH_SCREEN);
    }

    private void buildLayout() {
        // ── Image centred on screen ───────────────────────────
        imgW = (int)(500 * Game.SCALE * 0.5f); // ← ADJUST 0.5f to resize
        imgH = (int)(500 * Game.SCALE * 0.5f);
        imgX = (Game.GAME_WIDTH  - imgW) / 2;
        imgY = (Game.GAME_HEIGHT - imgH) / 2;

        // ── URM replay button (row 1 = restart icon) ──────────
        replayB = new UrmButton(REPLAY_X, REPLAY_Y, URM_SIZE, URM_SIZE, 1);
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update() {
        if (!fadeComplete) {
            overlayAlpha = Math.min(overlayAlpha + FADE_SPEED, 0.85f);
            if (overlayAlpha >= 0.85f) fadeComplete = true;
        }
        if (fadeComplete) replayB.update();
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // 1 — semi-transparent black overlay
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // 2 — death screen image (fades in together with overlay)
        if (deathImg != null) {
            float imgAlpha = Math.min(overlayAlpha / 0.85f, 1f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, imgAlpha));
            g2d.drawImage(deathImg, imgX, imgY, imgW, imgH, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // 3 — replay button appears only after fade completes
        if (fadeComplete) replayB.draw(g);
    }


    public void reset() {
        overlayAlpha = 0f;
        fadeComplete = false;
        replayB.resetBools();
    }


    public void mouseMoved(MouseEvent e) {
        if (!fadeComplete) return;
        replayB.setMouseOver(replayB.getBounds().contains(e.getX(), e.getY()));
    }

    public void mousePressed(MouseEvent e) {
        if (!fadeComplete) return;
        if (replayB.getBounds().contains(e.getX(), e.getY()))
            replayB.setMousePressed(true);
    }

    public void mouseReleased(MouseEvent e) {
        if (!fadeComplete) return;
        if (replayB.isMousePressed() && replayB.getBounds().contains(e.getX(), e.getY()))
            playing.restartGame();      // full reset + closes death screen
        replayB.resetBools();
    }
}