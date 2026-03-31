package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static utils.Constants.UI.URMButtons.*;


public class IntroOverlay {

    // ── Screens ───────────────────────────────────────────────
    private static final int TOTAL_STEPS = 2;
    private final BufferedImage[] screens = new BufferedImage[TOTAL_STEPS];
    private int currentStep = 0;

    // ── Image layout — identical to DeathOverlay ──────────────
    private static final float IMG_SIZE_PX = 500f;   // source image size
    private static final float IMG_RENDER_SCALE = 0.5f; // ← ADJUST same as death screen
    private final int imgW, imgH, imgX, imgY;

    // ── NEXT button — same position as DeathOverlay replay btn ─
    // -------------------------------------------------------
    // BUTTON POSITION  ← ADJUST (mirrors death screen values)
    // -------------------------------------------------------
    private static final int BTN_X = (int)(374 * Game.SCALE);
    private static final int BTN_Y = (int)(325 * Game.SCALE);
    // -------------------------------------------------------

    private final UrmButton nextBtn;


    private static final int CLICK_COOLDOWN_TICKS = 20;
    private int clickCooldown = 0;


    // -------------------------------------------------------
    // FADE SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float FADE_SPEED    = 0.03f;  // alpha per tick (200 UPS)
    private static final float MAX_OVERLAY   = 0.85f;  // peak darkness
    // -------------------------------------------------------
    private enum FadeState { FADE_IN, VISIBLE, FADE_OUT }
    private FadeState fadeState = FadeState.FADE_IN;
    private float     fadeAlpha = 0f;

    public IntroOverlay() {
        // ── Image size & position (same math as DeathOverlay) ─
        imgW = (int)(IMG_SIZE_PX * Game.SCALE * IMG_RENDER_SCALE);
        imgH = (int)(IMG_SIZE_PX * Game.SCALE * IMG_RENDER_SCALE);
        imgX = (Game.GAME_WIDTH  - imgW) / 2;
        imgY = (Game.GAME_HEIGHT - imgH) / 2;

        // ── Button ────────────────────────────────────────────
        nextBtn = new UrmButton(BTN_X, BTN_Y, URM_SIZE, URM_SIZE, 0);

        loadScreens();
    }

    private void loadScreens() {
        screens[0] = LoadSave.getSpriteAtlas(LoadSave.TUTORIAL_IMG);
        screens[1] = LoadSave.getSpriteAtlas(LoadSave.MISSION_MAP1_IMG);
        for (int i = 0; i < screens.length; i++)
            if (screens[i] == null)
                System.err.println("[IntroOverlay] Missing image for step " + i);
    }


    public void reset() {
        currentStep   = 0;
        fadeState     = FadeState.FADE_IN;
        fadeAlpha     = 0f;
        clickCooldown = 0;
        nextBtn.resetBools();
    }


    public boolean update() {
        if (clickCooldown > 0) clickCooldown--;
        if (fadeState == FadeState.VISIBLE) nextBtn.update();

        switch (fadeState) {
            case FADE_IN:
                fadeAlpha = Math.min(fadeAlpha + FADE_SPEED, MAX_OVERLAY);
                if (fadeAlpha >= MAX_OVERLAY) fadeState = FadeState.VISIBLE;
                break;

            case FADE_OUT:
                fadeAlpha = Math.max(fadeAlpha - FADE_SPEED, 0f);
                if (fadeAlpha <= 0f) {
                    currentStep++;
                    if (currentStep >= TOTAL_STEPS) return true; // all done
                    fadeState = FadeState.FADE_IN;               // next screen
                }
                break;

            case VISIBLE:
            default:
                break;
        }
        return false;
    }

    public void render(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // 1 — semi-transparent black overlay
        g2d.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, fadeAlpha));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // 2 — tutorial / mission image (fades in with overlay)
        if (screens[currentStep] != null) {
            float imgAlpha = Math.min(fadeAlpha / MAX_OVERLAY, 1f);
            g2d.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, imgAlpha));
            g2d.drawImage(screens[currentStep], imgX, imgY, imgW, imgH, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // 3 — NEXT button (only after fully faded in)
        if (fadeState == FadeState.VISIBLE) nextBtn.draw(g);
    }

    // ─────────────────────────────────────────────────────────
    // MOUSE INPUT
    // ─────────────────────────────────────────────────────────
    public void mouseMoved(MouseEvent e) {
        if (fadeState != FadeState.VISIBLE) return;
        nextBtn.setMouseOver(nextBtn.getBounds().contains(e.getX(), e.getY()));
    }

    public void mousePressed(MouseEvent e) {
        if (fadeState != FadeState.VISIBLE || clickCooldown > 0) return;
        if (nextBtn.getBounds().contains(e.getX(), e.getY()))
            nextBtn.setMousePressed(true);
    }

    public void mouseReleased(MouseEvent e) {
        if (fadeState != FadeState.VISIBLE || clickCooldown > 0) {
            nextBtn.resetBools();
            return;
        }
        if (nextBtn.isMousePressed() &&
                nextBtn.getBounds().contains(e.getX(), e.getY())) {
            fadeState     = FadeState.FADE_OUT;
            clickCooldown = CLICK_COOLDOWN_TICKS;
        }
        nextBtn.resetBools();
    }
}