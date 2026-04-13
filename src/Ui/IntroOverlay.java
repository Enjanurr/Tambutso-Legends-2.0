package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static utils.Constants.UI.URMButtons.*;

/**
 * Tutorial / mission intro overlay.
 *
 * Displayed OVER the already-running (but paused) PLAYING state.
 * Playing owns this object and controls when it opens / closes.
 *
 * Show-once behaviour:
 *   • First call to open() shows the overlay and marks it as shown.
 *   • Subsequent calls to open() are ignored (hasBeenShown == true).
 *   • Call resetShown() only if you explicitly want to re-show it
 *     (e.g. for testing). Normal gameplay never calls resetShown().
 */
public class IntroOverlay {

    // ── Screens ───────────────────────────────────────────────
    private static final int TOTAL_STEPS = 2;
    private final BufferedImage[] screens = new BufferedImage[TOTAL_STEPS];
    private int currentStep = 0;

    // ── Image layout ──────────────────────────────────────────
    private static final float IMG_SIZE_PX     = 500f;
    private static final float IMG_RENDER_SCALE = 0.5f;
    private final int imgW, imgH, imgX, imgY;

    // ── NEXT button ───────────────────────────────────────────
    // -------------------------------------------------------
    // BUTTON POSITION  ← ADJUST
    // -------------------------------------------------------
    private static final int BTN_X = (int)(374 * Game.SCALE);
    private static final int BTN_Y = (int)(325 * Game.SCALE);
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
    /** true while the overlay is on-screen (between open() and the last NEXT click). */
    private boolean open         = false;
    /** true once the overlay has been fully shown at least once this session. */
    private boolean hasBeenShown = false;

    // ── Callback ─────────────────────────────────────────────
    /** Called by the overlay when the player clicks through the last screen. */
    private Runnable onComplete;

    // ─────────────────────────────────────────────────────────
    public IntroOverlay(Runnable onComplete) {
        this.onComplete = onComplete;

        imgW = (int)(IMG_SIZE_PX * Game.SCALE * IMG_RENDER_SCALE);
        imgH = (int)(IMG_SIZE_PX * Game.SCALE * IMG_RENDER_SCALE);
        imgX = (Game.GAME_WIDTH  - imgW) / 2;
        imgY = (Game.GAME_HEIGHT - imgH) / 2;

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

    // ─────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────

    /**
     * Attempt to open the overlay.
     * Does nothing if it has already been shown this session.
     * @return true  if the overlay was opened (game should pause)
     *         false if skipped because it has already been shown
     */
    public boolean open() {
        if (hasBeenShown) return false;

        hasBeenShown  = true;
        open          = true;
        currentStep   = 0;
        fadeState     = FadeState.FADE_IN;
        fadeAlpha     = 0f;
        clickCooldown = 0;
        nextBtn.resetBools();
        return true;
    }

    /** Whether the overlay is currently visible. */
    public boolean isOpen() { return open; }

    /** Whether the overlay has already been shown at least once. */
    public boolean hasBeenShown() { return hasBeenShown; }

    /**
     * Resets the "has been shown" flag so open() will work again.
     * Only needed for testing; normal gameplay does not call this.
     */
    public void resetShown() { hasBeenShown = false; }

    // ─────────────────────────────────────────────────────────
    // UPDATE  (call every tick while isOpen())
    // ─────────────────────────────────────────────────────────
    public void update() {
        if (!open) return;

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
                    if (currentStep >= TOTAL_STEPS) {
                        // All screens shown — close and notify Playing
                        open = false;
                        if (onComplete != null) onComplete.run();
                    } else {
                        fadeState = FadeState.FADE_IN;
                    }
                }
                break;

            case VISIBLE:
            default:
                break;
        }
    }

    // ─────────────────────────────────────────────────────────
    // RENDER  (call every frame while isOpen())
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (!open) return;

        // Guard: step out of range means we're in the closing tick
        if (currentStep < 0 || currentStep >= TOTAL_STEPS) return;

        Graphics2D g2d = (Graphics2D) g;

        // 1 — semi-transparent dark overlay (game world shows through behind it)
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // 2 — tutorial / mission image
        if (screens[currentStep] != null) {
            float imgAlpha = Math.min(fadeAlpha / MAX_OVERLAY, 1f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, imgAlpha));
            g2d.drawImage(screens[currentStep], imgX, imgY, imgW, imgH, null);
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
        if (!open || fadeState != FadeState.VISIBLE || clickCooldown > 0) return;
        if (nextBtn.getBounds().contains(e.getX(), e.getY()))
            nextBtn.setMousePressed(true);
    }

    public void mouseReleased(MouseEvent e) {
        if (!open || fadeState != FadeState.VISIBLE || clickCooldown > 0) {
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