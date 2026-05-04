package main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

import inputs.KeyboardInputs;
import inputs.MouseInputs;

public class GamePanel extends JPanel {

    private MouseInputs mouseInputs;
    private Game game;

    // -------------------------------------------------------
    // SCREEN FADE SETTINGS
    // -------------------------------------------------------
    private static final long FADE_OUT_MS = 100; // ← ADJUST: fade-out duration in ms
    private static final long FADE_IN_MS  = 100; // ← ADJUST: fade-in duration in ms
    // -------------------------------------------------------

    // Fade state — do not touch
    public enum FadeState { NONE, FADING_OUT, FADING_IN }
    private FadeState fadeState = FadeState.NONE;
    private long      fadeStart = 0;
    private float     fadeAlpha = 0f;


    private Runnable warpCallback = null;

    public GamePanel(Game game) {
        mouseInputs = new MouseInputs(this);
        this.game   = game;
        setPanelSize();
        setFocusable(true);
        setRequestFocusEnabled(true);
        addKeyListener(new KeyboardInputs(this));
        addMouseListener(mouseInputs);
        addMouseMotionListener(mouseInputs);
    }

    private void setPanelSize() {
        Dimension size = new Dimension(Game.GAME_WIDTH, Game.GAME_HEIGHT);
        setPreferredSize(size);
    }

    // Called by Player when it hits the border wall
    public void triggerScreenFade(Runnable onMidpoint) {
        if (fadeState != FadeState.NONE) return; // already fading, ignore
        this.warpCallback = onMidpoint;
        fadeState = FadeState.FADING_OUT;
        fadeStart = System.currentTimeMillis();
        fadeAlpha = 0f;
    }


    public void updateFade() {
        if (fadeState == FadeState.NONE) return;

        long elapsed = System.currentTimeMillis() - fadeStart;

        if (fadeState == FadeState.FADING_OUT) {
            fadeAlpha = Math.min((float) elapsed / FADE_OUT_MS, 1f); // 0 → 1

            if (elapsed >= FADE_OUT_MS) {
                // Screen is fully black — fire the warp now
                if (warpCallback != null) {
                    warpCallback.run();
                    warpCallback = null;
                }
                fadeState = FadeState.FADING_IN;
                fadeStart = System.currentTimeMillis();
                fadeAlpha = 1f;
            }

        } else if (fadeState == FadeState.FADING_IN) {
            fadeAlpha = 1f - Math.min((float) elapsed / FADE_IN_MS, 1f); // 1 → 0

            if (elapsed >= FADE_IN_MS) {
                fadeState = FadeState.NONE;
                fadeAlpha = 0f;
            }
        }
    }

    public boolean isFading() {
        return fadeState != FadeState.NONE;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw game world
        game.render(g);

        if (fadeAlpha > 0f) {
            Graphics2D g2d = (Graphics2D) g;
            Composite original = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
            g2d.setComposite(original);
        }
    }

    public Game getGame() { return game; }

    public void reclaimFocus() {
        if (!hasFocus()) {
            requestFocusInWindow();
        }
    }
}
