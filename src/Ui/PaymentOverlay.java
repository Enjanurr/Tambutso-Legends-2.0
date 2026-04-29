package Ui;

import entities.PassengerManager;
import entities.RidingPassenger;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Payment overlay for passenger drop flow.
 *
 * Flow:
 * 1. Click Drop in PassengerListOverlay → Open PaymentOverlay (passenger still seated)
 * 2. Passenger paid amount is shown (expected fare + random surplus 1-30 pesos)
 * 3. Player enters CHANGE amount to give back to passenger
 * 4. Input < surplus → "Lack!" error (3 sec), DROP disabled
 * 5. Input > surplus → "Too much!" error (3 sec), DROP disabled
 * 6. Input = surplus → DROP enabled
 * 7. Click DROP → finalize drop, close overlay, remove passenger
 */
public class PaymentOverlay {

    // =========================================================
    // OVERLAY SETTINGS ← ADJUST
    // =========================================================
    private static final int OVERLAY_W = 235;
    private static final int OVERLAY_H = 250;  // Increased height for new section
    private static final float OVERLAY_SCALE = 1f;

    // =========================================================
    // TEXT SETTINGS ← ADJUST
    // =========================================================
    private static final Font VALUE_FONT = new Font("SansSerif", Font.BOLD, 30);
    private static final Font COMPUTATION_FONT = new Font("SansSerif", Font.PLAIN, 20);
    private static final Font ERROR_FONT = new Font("SansSerif", Font.BOLD, 25);
    private static final Font INPUT_FONT = new Font("SansSerif", Font.PLAIN, 30);

    private static final Color VALUE_COLOR = new Color(100, 220, 100);
    private static final Color COMPUTATION_COLOR = Color.WHITE;
    private static final Color ERROR_COLOR = new Color(212, 8, 8, 255);
    private static final Color INPUT_COLOR = Color.YELLOW;

    // =========================================================
    // PASSENGER PAID SECTION SETTINGS ← ADJUST
    // =========================================================
    private static final Font PAID_VALUE_FONT = new Font("SansSerif", Font.BOLD, 30);
    private static final Color PAID_VALUE_COLOR = new Color(255, 200, 100);  // Orange-gold

    // =========================================================
    // POSITION SETTINGS ← ADJUST (pixels relative to overlay)
    // =========================================================
    // Section 1: To Be Earned (value only)
    private static final int EARNED_VALUE_X = 135;
    private static final int EARNED_VALUE_Y = 100;

    // Section 2: Computation
    private static final int COMP_VALUE_X = 40;
    private static final int COMP_VALUE_Y = 165;

    // Section 3: Passenger Paid
    private static final int PAID_VALUE_X = 115;
    private static final int PAID_VALUE_Y = 190;

    // Section 4: Change to Give input
    private static final int CHANGE_INPUT_X = 95;
    private static final int CHANGE_INPUT_Y = 200;
    private static final int CHANGE_INPUT_W = 50;
    private static final int CHANGE_INPUT_H = 17;

    // Error message
    private static final int ERROR_X = 155;
    private static final int ERROR_Y = 215;

    // DROP button (centered horizontally)
    private static final int DROP_BTN_W = 100;
    private static final int DROP_BTN_H = 50;
    private static final int DROP_BTN_X = (OVERLAY_W - DROP_BTN_W) / 2;
    private static final int DROP_BTN_Y = 265;

    // =========================================================
    // BUTTON SPRITE SETTINGS
    // =========================================================
    private static final int BTN_SRC_W = 140;
    private static final int BTN_SRC_H = 56;
    private static final int DROP_ROW = 0;  // Row for DROP button

    // =========================================================
    // IMAGES
    // =========================================================
    private BufferedImage overlayImg;
    private BufferedImage[][] btnFrames;  // [row][col] col: 0=normal, 1=hover, 2=pressed

    // =========================================================
    // STATE
    // =========================================================
    private boolean isOpen = false;

    // Payment state
    private int expectedFare = 0;
    private int surplusAmount = 0;  // Amount passenger paid above expected fare (1-30)
    private int passengerPaid = 0;   // Total paid by passenger (expected + surplus)
    private int changeInputAmount = 0;  // What player inputs as change to give back
    private String changeInput = "";
    private boolean canDrop = false;

    // Error state
    private boolean showError = false;
    private int errorTimer = 0;
    private String errorMessage = "";
    private static final int ERROR_DURATION = 3 * 200;  // 3 seconds at 200 UPS

    // Button states
    private boolean dropOver = false, dropPressed = false;

    // Bounds
    private Rectangle overlayBounds;
    private Rectangle dropBounds;
    private Rectangle changeInputBounds;

    // =========================================================
    // CALLBACKS
    // =========================================================
    private final Runnable onConfirmDrop;  // Called when DROP finalized
    private final Runnable onClose;         // Called when overlay closes

    // =========================================================
    // CONSTRUCTOR
    // =========================================================
    public PaymentOverlay(Runnable onConfirmDrop, Runnable onClose) {
        this.onConfirmDrop = onConfirmDrop;
        this.onClose = onClose;
        loadAssets();
        buildLayout();
    }

    // =========================================================
    // ASSET LOADING
    // =========================================================
    private void loadAssets() {
        // Load overlay background
        overlayImg = LoadSave.getSpriteAtlas(LoadSave.PAYMENT_OVERLAY);

        // Load button frames (passenger_button.png for DROP button)
        BufferedImage dropBtnSheet = LoadSave.getSpriteAtlas(LoadSave.PASSENGER_BUTTON);

        btnFrames = new BufferedImage[1][3];  // Row 0: DROP only

        if (dropBtnSheet != null) {
            for (int col = 0; col < 3; col++) {
                btnFrames[0][col] = dropBtnSheet.getSubimage(
                        col * BTN_SRC_W, DROP_ROW * BTN_SRC_H, BTN_SRC_W, BTN_SRC_H);
            }
        }
    }

    private void buildLayout() {
        int overlayRenderW = (int)(OVERLAY_W * Game.SCALE * OVERLAY_SCALE);
        int overlayRenderH = (int)(OVERLAY_H * Game.SCALE * OVERLAY_SCALE);
        int overlayX = (Game.GAME_WIDTH - overlayRenderW) / 2;
        int overlayY = (Game.GAME_HEIGHT - overlayRenderH) / 2;

        overlayBounds = new Rectangle(overlayX, overlayY, overlayRenderW, overlayRenderH);

        // Scale button positions
        float scaleX = (float)overlayRenderW / OVERLAY_W;
        float scaleY = (float)overlayRenderH / OVERLAY_H;

        dropBounds = new Rectangle(
                overlayX + (int)(DROP_BTN_X * scaleX),
                overlayY + (int)(DROP_BTN_Y * scaleY),
                (int)(DROP_BTN_W * scaleX),
                (int)(DROP_BTN_H * scaleY));

        changeInputBounds = new Rectangle(
                overlayX + (int)(CHANGE_INPUT_X * scaleX),
                overlayY + (int)(CHANGE_INPUT_Y * scaleY),
                (int)(CHANGE_INPUT_W * scaleX),
                (int)(CHANGE_INPUT_H * scaleY));
    }

    // =========================================================
    // PUBLIC API
    // =========================================================

    /**
     * Opens the payment overlay with the expected fare.
     * Generates a random surplus amount (1-30) that passenger paid above expected fare.
     * If surplus already cached in RidingPassenger, reuse it for consistency.
     * @param expectedFare The fare amount expected from passenger
     * @param rp The RidingPassenger object (for cached surplus)
     */
    public void open(int expectedFare, RidingPassenger rp) {
        System.out.println("[PaymentOverlay] open() STARTED - expectedFare=" + expectedFare + ", isOpen was " + isOpen);
        this.expectedFare = expectedFare;

        // Zero fare special case - auto-enable drop
        if (expectedFare <= 0) {
            this.expectedFare = 0;
            this.surplusAmount = 0;
            this.passengerPaid = 0;
            this.changeInput = "0";
            this.changeInputAmount = 0;
            this.canDrop = true;
            this.showError = false;
            this.errorTimer = 0;
            this.errorMessage = "";
            this.isOpen = true;
            System.out.println("[PaymentOverlay] Zero fare - auto-enabled drop");
            return;
        }

        // Check for cached surplus in RidingPassenger
        Integer cachedSurplus = rp.getCachedSurplus();
        if (cachedSurplus != null) {
            this.surplusAmount = cachedSurplus;
            System.out.println("[PaymentOverlay] Using cached surplus: " + surplusAmount);
        } else {
            // Generate new random surplus between 1 and 30 pesos
            this.surplusAmount = 1 + new java.util.Random().nextInt(30);
            rp.setCachedSurplus(surplusAmount);
            System.out.println("[PaymentOverlay] Generated new surplus: " + surplusAmount);
        }

        this.passengerPaid = expectedFare + surplusAmount;
        this.changeInputAmount = 0;
        this.changeInput = "";
        this.canDrop = false;
        this.showError = false;
        this.errorTimer = 0;
        this.errorMessage = "";
        this.isOpen = true;
        System.out.println("[PaymentOverlay] open() COMPLETED - isOpen=" + isOpen + ", passengerPaid=" + passengerPaid + ", surplus=" + surplusAmount);
    }

    public void close() {
        isOpen = false;
        if (onClose != null) onClose.run();
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void update() {
        // Update error timer
        if (showError) {
            errorTimer--;
            if (errorTimer <= 0) {
                showError = false;
            }
        }
    }

    /**
     * Handles CHANGE amount input (called from Playing.keyPressed).
     * @param digit The digit key pressed (0-9)
     */
    public void inputDigit(int digit) {
        if (changeInput.length() < 3) {  // Max 3 digits for change
            changeInput += digit;
            updatePaymentState();
        }
    }

    /**
     * Handles backspace for CHANGE input.
     */
    public void backspace() {
        if (changeInput.length() > 0) {
            changeInput = changeInput.substring(0, changeInput.length() - 1);
            updatePaymentState();
        }
    }


    private void updatePaymentState() {
        // Zero fare - always can drop
        if (expectedFare <= 0) {
            canDrop = true;
            showError = false;
            errorMessage = "";
            return;
        }

        try {
            changeInputAmount = changeInput.isEmpty() ? 0 : Integer.parseInt(changeInput);

            if (changeInputAmount < surplusAmount) {
                // Not enough change given
                canDrop = false;
                showError = true;
                errorTimer = ERROR_DURATION;
                errorMessage = "Kulang!";
            } else if (changeInputAmount > surplusAmount) {
                // Too much change given
                canDrop = false;
                showError = true;
                errorTimer = ERROR_DURATION;
                errorMessage = "Sobra!";
            } else {
                // Correct change amount
                canDrop = true;
                showError = false;
                errorMessage = "";
            }
        } catch (NumberFormatException e) {
            changeInputAmount = 0;
            canDrop = false;
        }
    }

    /**
     * Confirms the drop (DROP button action).
     */
    public void confirmDrop() {
        if (canDrop && onConfirmDrop != null) {
            onConfirmDrop.run();
            close();
        }
    }

    // =========================================================
    // RENDER
    // =========================================================
    public void render(Graphics g) {
        if (!isOpen) return;

        Graphics2D g2 = (Graphics2D) g;

        // Draw overlay background
        if (overlayImg != null) {
            g2.drawImage(overlayImg, overlayBounds.x, overlayBounds.y,
                    overlayBounds.width, overlayBounds.height, null);
        } else {
            // Fallback: draw semi-transparent background
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillRoundRect(overlayBounds.x, overlayBounds.y,
                    overlayBounds.width, overlayBounds.height, 10, 10);
        }

        // Scale factors for text positioning
        float scaleX = (float)overlayBounds.width / OVERLAY_W;
        float scaleY = (float)overlayBounds.height / OVERLAY_H;

        // Section 1: To Be Earned
        g2.setFont(VALUE_FONT);
        g2.setColor(VALUE_COLOR);
        g2.drawString("" + expectedFare,
                overlayBounds.x + (int)(EARNED_VALUE_X * scaleX),
                overlayBounds.y + (int)(EARNED_VALUE_Y * scaleY));

        // Section 2: Computation
        g2.setFont(COMPUTATION_FONT);
        g2.setColor(COMPUTATION_COLOR);
        String computation = computeFormula(expectedFare);
        g2.drawString(computation,
                overlayBounds.x + (int)(COMP_VALUE_X * scaleX),
                overlayBounds.y + (int)(COMP_VALUE_Y * scaleY));

        // Section 3: Passenger Paid
        g2.setFont(PAID_VALUE_FONT);
        g2.setColor(PAID_VALUE_COLOR);
        g2.drawString("" + passengerPaid,
                overlayBounds.x + (int)(PAID_VALUE_X * scaleX),
                overlayBounds.y + (int)(PAID_VALUE_Y * scaleY));

        // Section 4: Change to Give input
        g2.setFont(VALUE_FONT);
        g2.setColor(VALUE_COLOR);
        g2.drawString("",
                overlayBounds.x + (int)(CHANGE_INPUT_X * scaleX) - 70,
                overlayBounds.y + (int)(CHANGE_INPUT_Y * scaleY) + 12);

        // Draw input field
        g2.setColor(Color.WHITE);
        g2.drawRect(changeInputBounds.x, changeInputBounds.y,
                changeInputBounds.width, changeInputBounds.height);
        g2.setFont(INPUT_FONT);
        g2.setColor(INPUT_COLOR);
        g2.drawString(changeInput.isEmpty() ? "0" : changeInput,
                changeInputBounds.x + 5,
                changeInputBounds.y + changeInputBounds.height - 5);

        // Error message
        if (showError) {
            g2.setFont(ERROR_FONT);
            g2.setColor(ERROR_COLOR);
            g2.drawString(errorMessage,
                    overlayBounds.x + (int)(ERROR_X * scaleX),
                    overlayBounds.y + (int)(ERROR_Y * scaleY));
        }

        // Draw DROP button (only button, no label)
        drawButton(g2, 0, dropBounds, dropOver, dropPressed, canDrop);
    }

    /**
     * Computes the fare formula string for display.
     */
    private String computeFormula(int fare) {
        int base = PassengerManager.BASE_FARE;
        int perStop = PassengerManager.PER_STOP_FARE;
        int stops = (fare - base) / perStop;
        return base + " + (" + stops + " × " + perStop + ") = " + base + " + " + (stops * perStop) + " = ₱" + fare;
    }

    private void drawButton(Graphics2D g2, int row, Rectangle bounds,
                            boolean over, boolean pressed, boolean enabled) {
        if (btnFrames == null || btnFrames[row] == null) return;

        if (!enabled) {
            // Draw disabled button with reduced alpha
            Composite saved = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            int col = pressed ? 2 : over ? 1 : 0;
            BufferedImage frame = btnFrames[row][col];
            if (frame != null) {
                g2.drawImage(frame, bounds.x, bounds.y, bounds.width, bounds.height, null);
            }
            g2.setComposite(saved);
        } else {
            int col = pressed ? 2 : over ? 1 : 0;
            BufferedImage frame = btnFrames[row][col];
            if (frame != null) {
                g2.drawImage(frame, bounds.x, bounds.y, bounds.width, bounds.height, null);
            }
        }
        // No button labels - images only
    }

    // =========================================================
    // INPUT HANDLING
    // =========================================================
    public void mousePressed(MouseEvent e) {
        if (!isOpen) return;

        int mx = e.getX();
        int my = e.getY();

        if (dropBounds.contains(mx, my)) {
            dropPressed = true;
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (!isOpen) return;

        int mx = e.getX();
        int my = e.getY();

        if (dropPressed && dropBounds.contains(mx, my)) {
            if (canDrop) {
                confirmDrop();
            }
        }

        dropPressed = false;
    }

    public void mouseMoved(MouseEvent e) {
        if (!isOpen) return;

        int mx = e.getX();
        int my = e.getY();

        dropOver = dropBounds.contains(mx, my);
    }

    // =========================================================
    // GETTERS
    // =========================================================
    public boolean isCanDrop() {
        return canDrop;
    }

    public int getSurplusAmount() {
        return surplusAmount;
    }

    public int getPassengerPaid() {
        return passengerPaid;
    }
}