package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * Jeep skill buttons for boss fights.
 * Displays Skill 1 (E key) and Skill 2 (Q key) buttons based on jeep color.
 * Cooldown state is read from the boss fight state (not tracked internally).
 *
 * Sprite: jeeps_skill_button.png (168 × 336, 6 rows × 3 cols)
 *   Rows 0-1: Blue Jeep (Skill 1, Skill 2)
 *   Rows 2-3: Green Jeep (Skill 1, Skill 2)
 *   Rows 4-5: Red Jeep (Skill 1, Skill 2)
 *   Col 0: Ready, Col 1: Pressed, Col 2: Cooldown
 */
public class JeepSkillButtons {

    // -------------------------------------------------------
    // POSITION SETTINGS  ← ADJUST PER BUTTON
    // -------------------------------------------------------
    // Skill 1 position (from left edge, from bottom edge)
    private static final float SKILL1_X_OFFSET = 350f;
    private static final float SKILL1_Y_OFFSET = 5f;

    // Skill 2 position (from left edge, from bottom edge)
    private static final float SKILL2_X_OFFSET = 430f;
    private static final float SKILL2_Y_OFFSET = 5f;

    private static final float BUTTON_SCALE = 1.0f;
    // -------------------------------------------------------

    // -------------------------------------------------------
    // COOLDOWN TEXT SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final Font COOLDOWN_FONT = new Font("SansSerif", Font.BOLD, 25);
    private static final Color COOLDOWN_TEXT_COLOR = Color.WHITE;
    // -------------------------------------------------------

    private static final int BUTTON_W = 56;  // 168 / 3 columns
    private static final int BUTTON_H = 56;  // 336 / 6 rows

    private final BufferedImage[][] buttonFrames; // [row][col]
    private final int skill1Row;
    private final int skill2Row;

    // Button positions
    private final int skill1X, skill1Y;
    private final int skill2X, skill2Y;
    private final int drawW, drawH;

    // Cooldown tracking - supplied by boss state
    private final BooleanSupplier skill1ReadyCheck;
    private final BooleanSupplier skill2ReadyCheck;

    // Cooldown remaining time suppliers (for timer display)
    private final IntSupplier skill1CooldownRemaining;
    private final IntSupplier skill2CooldownRemaining;

    // Button states
    private boolean skill1Pressed = false;
    private boolean skill2Pressed = false;
    private boolean skill1Over = false;
    private boolean skill2Over = false;

    // Callbacks
    private final Runnable onSkill1;
    private final Runnable onSkill2;

    /**
     * Creates skill buttons for jeep boss fights.
     *
     * @param jeepColor               "blue", "green", or "red"
     * @param skill1Ready             Supplier returning true if skill 1 is ready
     * @param onSkill1                Callback when skill 1 is activated
     * @param skill1CooldownRemaining Supplier returning remaining cooldown seconds (for display)
     * @param skill2Ready             Supplier returning true if skill 2 is ready
     * @param onSkill2                Callback when skill 2 is activated
     * @param skill2CooldownRemaining Supplier returning remaining cooldown seconds (for display)
     */
    public JeepSkillButtons(String jeepColor,
                            BooleanSupplier skill1Ready, Runnable onSkill1, IntSupplier skill1CooldownRemaining,
                            BooleanSupplier skill2Ready, Runnable onSkill2, IntSupplier skill2CooldownRemaining) {
        this.skill1ReadyCheck = skill1Ready;
        this.skill2ReadyCheck = skill2Ready;
        this.skill1CooldownRemaining = skill1CooldownRemaining;
        this.skill2CooldownRemaining = skill2CooldownRemaining;
        this.onSkill1 = onSkill1;
        this.onSkill2 = onSkill2;

        // Determine rows based on jeep color
        switch (jeepColor.toLowerCase()) {
            case "blue":
                skill1Row = 0;
                skill2Row = 1;
                break;
            case "green":
                skill1Row = 2;
                skill2Row = 3;
                break;
            case "red":
                skill1Row = 4;
                skill2Row = 5;
                break;
            default:
                skill1Row = 0;
                skill2Row = 1;
        }

        // Calculate positions (bottom left reference)
        drawW = (int)(BUTTON_W * Game.SCALE * BUTTON_SCALE);
        drawH = (int)(BUTTON_H * Game.SCALE * BUTTON_SCALE);

        // Individual button positions
        skill1X = (int)(SKILL1_X_OFFSET * Game.SCALE);
        skill1Y = Game.GAME_HEIGHT - (int)(SKILL1_Y_OFFSET * Game.SCALE) - drawH;

        skill2X = (int)(SKILL2_X_OFFSET * Game.SCALE);
        skill2Y = Game.GAME_HEIGHT - (int)(SKILL2_Y_OFFSET * Game.SCALE) - drawH;

        // Load sprite sheet
        buttonFrames = loadFrames();
    }

    private BufferedImage[][] loadFrames() {
        BufferedImage[][] frames = new BufferedImage[6][3];
        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.JEEP_SKILL_BUTTONS);

        if (sheet == null) {
            System.err.println("[JeepSkillButtons] Failed to load: " + LoadSave.JEEP_SKILL_BUTTONS);
            return frames;
        }

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 3; col++) {
                frames[row][col] = sheet.getSubimage(
                        col * BUTTON_W,
                        row * BUTTON_H,
                        BUTTON_W,
                        BUTTON_H);
            }
        }
        return frames;
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update() {
        // Cooldowns are checked via the supplier methods
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (buttonFrames == null) return;

        Graphics2D g2d = (Graphics2D) g;

        // Draw Skill 1 button
        boolean skill1Ready = isSkill1Ready();
        int skill1Col = getColumnForState(skill1Ready, skill1Pressed);
        BufferedImage skill1Frame = buttonFrames[skill1Row][skill1Col];
        if (skill1Frame != null) {
            g2d.drawImage(skill1Frame, skill1X, skill1Y, drawW, drawH, null);
        }

        // Draw cooldown timer if in cooldown
        if (!skill1Ready && skill1Col == 2) {
            drawCooldownTimer(g2d, skill1X, skill1Y, drawW, drawH, skill1CooldownRemaining.getAsInt());
        }

        // Draw Skill 2 button
        boolean skill2Ready = isSkill2Ready();
        int skill2Col = getColumnForState(skill2Ready, skill2Pressed);
        BufferedImage skill2Frame = buttonFrames[skill2Row][skill2Col];
        if (skill2Frame != null) {
            g2d.drawImage(skill2Frame, skill2X, skill2Y, drawW, drawH, null);
        }

        // Draw cooldown timer if in cooldown
        if (!skill2Ready && skill2Col == 2) {
            drawCooldownTimer(g2d, skill2X, skill2Y, drawW, drawH, skill2CooldownRemaining.getAsInt());
        }
    }

    private int getColumnForState(boolean ready, boolean pressed) {
        if (!ready) return 2;  // Cooldown (column 2)
        if (pressed) return 1; // Pressed (column 1)
        return 0;              // Ready (column 0)
    }

    private void drawCooldownTimer(Graphics2D g2d, int x, int y, int w, int h, int secondsRemaining) {
        if (secondsRemaining <= 0) return;

        g2d.setFont(COOLDOWN_FONT);
        g2d.setColor(COOLDOWN_TEXT_COLOR);

        String text = String.valueOf(secondsRemaining);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        int textX = x + (w - textWidth) / 2;
        int textY = y + (h + textHeight) / 2 - 2;

        g2d.drawString(text, textX, textY);
    }

    // ─────────────────────────────────────────────────────────
    // SKILL ACTIVATION
    // ─────────────────────────────────────────────────────────

    public void activateSkill1() {
        if (!isSkill1Ready()) return;
        if (onSkill1 != null) onSkill1.run();
    }

    public void activateSkill2() {
        if (!isSkill2Ready()) return;
        if (onSkill2 != null) onSkill2.run();
    }

    public boolean isSkill1Ready() {
        return skill1ReadyCheck != null && skill1ReadyCheck.getAsBoolean();
    }

    public boolean isSkill2Ready() {
        return skill2ReadyCheck != null && skill2ReadyCheck.getAsBoolean();
    }

    // ─────────────────────────────────────────────────────────
    // INPUT HANDLING
    // ─────────────────────────────────────────────────────────
    public void mousePressed(MouseEvent e) {
        if (isInSkill1(e)) {
            skill1Pressed = true;
            activateSkill1();
        } else if (isInSkill2(e)) {
            skill2Pressed = true;
            activateSkill2();
        }
    }

    public void mouseReleased(MouseEvent e) {
        skill1Pressed = false;
        skill2Pressed = false;
    }

    public void mouseMoved(MouseEvent e) {
        skill1Over = isInSkill1(e);
        skill2Over = isInSkill2(e);
    }

    private boolean isInSkill1(MouseEvent e) {
        return e.getX() >= skill1X && e.getX() <= skill1X + drawW &&
               e.getY() >= skill1Y && e.getY() <= skill1Y + drawH;
    }

    private boolean isInSkill2(MouseEvent e) {
        return e.getX() >= skill2X && e.getX() <= skill2X + drawW &&
               e.getY() >= skill2Y && e.getY() <= skill2Y + drawH;
    }

    // ─────────────────────────────────────────────────────────
    // GETTERS
    // ─────────────────────────────────────────────────────────
    public boolean isSkill1Over() { return skill1Over; }
    public boolean isSkill2Over() { return skill2Over; }
}
