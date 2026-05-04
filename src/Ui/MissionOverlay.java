package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static utils.Constants.UI.URMButtons.*;

/**
 * Single-screen mission overlay for Level 2 and Level 3.
 * Shows mission objectives before starting gameplay.
 *
 * Usage:
 *   - Create with level ID (1, 2, or 3)
 *   - Call open() to show
 *   - Player clicks NEXT → onClose callback fires
 *   - Callback should start gameplay
 */
public class MissionOverlay {

    // ── Screen ───────────────────────────────────────────────
    private BufferedImage missionScreen;
    private int imgW, imgH, imgX, imgY;

    // ── NEXT button ───────────────────────────────────────────
    // -------------------------------------------------------
    // BUTTON POSITION  ← ADJUST
    // -------------------------------------------------------
    private static final int BTN_X = (int)(374 * Game.SCALE);
    private static final int BTN_Y = (int)(335 * Game.SCALE);
    // -------------------------------------------------------
    private final UrmButton nextBtn;

    private static final int CLICK_COOLDOWN_TICKS = 20;
    private int clickCooldown = 0;

    // ── Fade ──────────────────────────────────────────────────
    // -------------------------------------------------------
    // FADE SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float FADE_SPEED  = 0.03f;
    private static final float MAX_OVERLAY = 0.85f;
    // -------------------------------------------------------
    private enum FadeState { FADE_IN, VISIBLE, FADE_OUT }
    private FadeState fadeState = FadeState.FADE_IN;
    private float     fadeAlpha = 0f;

    // ── State ─────────────────────────────────────────────────
    private boolean open = false;

    // ── Callback ─────────────────────────────────────────────
    private final Runnable onClose;

    // ─────────────────────────────────────────────────────────
    public MissionOverlay(int levelId, Runnable onClose) {
        this.onClose = onClose;

        imgW = (int)(500 * Game.SCALE * 0.5f);
        imgH = (int)(538 * Game.SCALE * 0.5f);
        imgX = (Game.GAME_WIDTH  - imgW) / 2;
        imgY = (Game.GAME_HEIGHT - imgH) / 2;

        nextBtn = new UrmButton(BTN_X, BTN_Y, URM_SIZE, URM_SIZE, 0);

        loadMissionScreen(levelId);
    }

    private void loadMissionScreen(int levelId) {
        String atlasPath;
        switch (levelId) {
            case 2:  atlasPath = LoadSave.MISSION_MAP2_IMG; break;
            case 3:  atlasPath = LoadSave.MISSION_MAP3_IMG; break;
            default:
                System.err.println("[MissionOverlay] Mission overlay only for Level 2 & 3, got Level " + levelId);
                missionScreen = null;
                return;
        }
        missionScreen = LoadSave.getSpriteAtlas(atlasPath);
        if (missionScreen == null)
            System.err.println("[MissionOverlay] Missing image for Level " + levelId + ": " + atlasPath);
    }

    // ─────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────

    public void open() {
        open          = true;
        fadeState     = FadeState.FADE_IN;
        fadeAlpha     = 0f;
        clickCooldown = 0;
        nextBtn.resetBools();
    }

    public boolean isOpen() { return open; }

    // ─────────────────────────────────────────────────────────
    // UPDATE  (call every tick while isOpen())
    // ─────────────────────────────────────────────────────────
    public void update() {
        if (!open) return;

        if (clickCooldown > 0) clickCooldown--;

        switch (fadeState) {
            case FADE_IN:
                fadeAlpha = Math.min(fadeAlpha + FADE_SPEED, MAX_OVERLAY);
                if (fadeAlpha >= MAX_OVERLAY) {
                    fadeState = FadeState.VISIBLE;
                    System.out.println("[MissionOverlay] Now VISIBLE - button should appear");
                }
                break;

            case FADE_OUT:
                fadeAlpha = Math.max(fadeAlpha - FADE_SPEED, 0f);
                if (fadeAlpha <= 0f) {
                    open = false;
                    System.out.println("[MissionOverlay] FADE_OUT complete - calling onClose");
                    if (onClose != null) onClose.run();
                }
                break;

            case VISIBLE:
                nextBtn.update();
                break;

            default:
                break;
        }
    }

    // ─────────────────────────────────────────────────────────
    // RENDER  (call every frame while isOpen())
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (!open) return;

        Graphics2D g2d = (Graphics2D) g;

        // 1 — semi-transparent dark overlay
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // 2 — mission image
        if (missionScreen != null) {
            float imgAlpha = Math.min(fadeAlpha / MAX_OVERLAY, 1f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, imgAlpha));
            g2d.drawImage(missionScreen, imgX, imgY, imgW, imgH, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // 3 — NEXT button (only when fully visible)
        if (fadeState == FadeState.VISIBLE) nextBtn.draw(g);
    }

    // ─────────────────────────────────────────────────────────
    // MOUSE INPUT
    // ─────────────────────────────────────────────────────────
    public void mouseMoved(MouseEvent e) {
        if (!open || fadeState != FadeState.VISIBLE) return;
        nextBtn.setMouseOver(nextBtn.getBounds().contains(e.getX(), e.getY()));
    }

    public void mousePressed(MouseEvent e) {
        if (!open || fadeState == FadeState.FADE_OUT || clickCooldown > 0) return;

        if (nextBtn.getBounds().contains(e.getX(), e.getY())) {
            nextBtn.setMousePressed(true);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (!open || fadeState == FadeState.FADE_OUT || clickCooldown > 0) {
            nextBtn.resetBools();
            return;
        }

        if (nextBtn.isMousePressed() && nextBtn.getBounds().contains(e.getX(), e.getY())) {
            fadeState = FadeState.FADE_OUT;
            clickCooldown = CLICK_COOLDOWN_TICKS;
        }
        nextBtn.resetBools();
    }

}
