package Ui;

import entities.RidingPassenger;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Passenger List Popup — shows the jeepney's top-down seat view.
 *
 * Input blocking (Part 2):
 *   When the popup is open, mousePressed() and mouseReleased() only act on
 *   Drop button, Exit button, and seat slots.  All other screen areas are
 *   silently swallowed.  Playing.mousePressed() already routes exclusively to
 *   this overlay when listPopupPaused == true, so the Open button behind the
 *   popup cannot be clicked accidentally.
 *   ESC closes the popup — handled via handleEsc() called from Playing.keyPressed().
 *
 * Drop condition:
 *   Drop button is ONLY enabled when worldLoopCount >= selectedPassenger.assignedStop.
 *   Drawn at 35 % opacity when disabled.
 *   Late drops show penalty info in colour-coded text.
 */
public class PassengerListOverlay {

    // =========================================================
    // PASSENGER LIST BACKGROUND  ← ADJUST
    // =========================================================
    private static final int   BG_SRC_W        = 500;
    private static final int   BG_SRC_H        = 299;
    private static final float BG_RENDER_SCALE = 1f;
    private static final int   BG_Y_OFFSET     = -20;

    private int bgX, bgY, bgW, bgH;

    // =========================================================
    // SEAT SLOT POSITIONS (pre-scale pixels relative to bgX/bgY)  ← ADJUST
    // =========================================================
    private static final int seat0_0_X = 45,  seat0_0_Y = 74;
    private static final int seat0_1_X = 105, seat0_1_Y = 74;
    private static final int seat0_2_X = 165, seat0_2_Y = 74;
    private static final int seat0_3_X = 225, seat0_3_Y = 74;
    private static final int seat0_4_X = 307, seat0_4_Y = 65;
    private static final int seat1_0_X = 45,  seat1_0_Y = 137;
    private static final int seat1_1_X = 105, seat1_1_Y = 137;
    private static final int seat1_2_X = 165, seat1_2_Y = 137;
    private static final int seat1_3_X = 225, seat1_3_Y = 137;

    private static final int[][] SLOT_RAW = {
            { seat0_0_X, seat0_0_Y },
            { seat0_1_X, seat0_1_Y },
            { seat0_2_X, seat0_2_Y },
            { seat0_3_X, seat0_3_Y },
            { seat0_4_X, seat0_4_Y },
            { seat1_0_X, seat1_0_Y },
            { seat1_1_X, seat1_1_Y },
            { seat1_2_X, seat1_2_Y },
            { seat1_3_X, seat1_3_Y },
    };

    // =========================================================
    // STOP INDICATOR  ← ADJUST
    // =========================================================
    private static final int IND_SRC_W = 41;
    private static final int IND_SRC_H = 12;
    private static final int stopIndicatorWidth   = (int)(IND_SRC_W * Game.SCALE);
    private static final int stopIndicatorHeight  = (int)(IND_SRC_H * Game.SCALE);
    private static final int stopIndicatorOffsetX = 3;
    private static final int stopIndicatorOffsetY = -2;

    // =========================================================
    // TOTAL FARE DISPLAY  ← ADJUST
    // =========================================================
    private static final int   fareDisplayX  = 380;
    private static final int   fareDisplayY  = -50;
    private static final Color fareTextColor = new Color(100, 220, 100);
    private static final int   fareFontSize  = 15;

    // =========================================================
    // SELECTED PASSENGER FARE STATUS LINE  ← ADJUST
    // =========================================================
    private static final int   selectedFareX    = 120;
    private static final int   selectedFareY    = -25;
    private static final Color selectedFareOk   = new Color(100, 220, 100);
    private static final Color selectedFareLate = new Color(255, 160,  50);
    private static final Color selectedFareWait = new Color(180, 180, 180);
    private static final int   selectedFontSize = 13;

    // =========================================================
    // PASSENGER_BUTTON SPRITE  (420 × 168, 3 rows × 3 cols)
    // =========================================================
    private static final int BTN_SRC_W = 140;
    private static final int BTN_SRC_H = 56;

    private static final int dropButtonWidth  = (int)(BTN_SRC_W * Game.SCALE * 0.8f);
    private static final int dropButtonHeight = (int)(BTN_SRC_H * Game.SCALE * 0.8f);
    private static final int dropButtonX      = -45;
    private static final int dropButtonY      = -85;

    private static final int exitButtonWidth  = (int)(BTN_SRC_W * Game.SCALE * 0.8f);
    private static final int exitButtonHeight = (int)(BTN_SRC_H * Game.SCALE * 0.8f);
    private static final int exitButtonX      = 40;
    private static final int exitButtonY      = -85;

    private static final int openButtonWidth  = (int)(BTN_SRC_W * Game.SCALE * 0.8f);
    private static final int openButtonHeight = (int)(BTN_SRC_H * Game.SCALE * 0.8f);
    private static final int OPEN_BTN_BOTTOM_MARGIN = (int)(10 * Game.SCALE);

    // =========================================================
    // CONSTANTS
    // =========================================================
    public static final int MAX_SEATS = 9;
    private static final int OPEN_ROW = 2;
    private static final int DROP_ROW = 0;
    private static final int EXIT_ROW = 1;

    /** Alpha for the Drop button when it is disabled. */
    private static final float DROP_DISABLED_ALPHA = 0.35f;

    // =========================================================
    // IMAGES
    // =========================================================
    private BufferedImage   bgImg;
    private BufferedImage[][] indicatorFrames;
    private BufferedImage[][] btnFrames;

    // =========================================================
    // STATE
    // =========================================================
    private boolean popupOpen    = false;
    private int     selectedSlot = -1;

    private boolean dropPressed = false, dropOver = false;
    private boolean exitPressed = false, exitOver = false;
    private boolean openPressed = false, openOver = false;

    private Rectangle dropBounds, exitBounds, openBounds;

    // =========================================================
    // CALLBACKS
    // =========================================================
    private final Runnable onDrop;
    private final Runnable onClose;
    private final Runnable onOpen;
    private final Runnable onOpenPayment;  // Callback to open PaymentOverlay

    // =========================================================
    // FARE TOTAL
    // =========================================================
    private int totalFareEarned = 0;

    // ─────────────────────────────────────────────────────────
    public PassengerListOverlay(Runnable onDrop, Runnable onClose, Runnable onOpen, Runnable onOpenPayment) {
        this.onDrop  = onDrop;
        this.onClose = onClose;
        this.onOpen  = onOpen;
        this.onOpenPayment = onOpenPayment;
        loadAssets();
        buildLayout();
    }

    // ─────────────────────────────────────────────────────────
    // ASSET LOADING
    // ─────────────────────────────────────────────────────────
    private void loadAssets() {
        bgImg = LoadSave.getSpriteAtlas(LoadSave.PASSENGER_LIST);

        BufferedImage indSheet = LoadSave.getSpriteAtlas(LoadSave.STOP_INDICATOR);
        if (indSheet != null) {
            indicatorFrames = new BufferedImage[10][3];
            for (int r = 0; r < 10; r++)
                for (int c = 0; c < 3; c++)
                    indicatorFrames[r][c] = indSheet.getSubimage(
                            c * IND_SRC_W, r * IND_SRC_H, IND_SRC_W, IND_SRC_H);
        }

        BufferedImage btnSheet = LoadSave.getSpriteAtlas(LoadSave.PASSENGER_BUTTON);
        if (btnSheet != null) {
            btnFrames = new BufferedImage[3][3];
            for (int r = 0; r < 3; r++)
                for (int c = 0; c < 3; c++)
                    btnFrames[r][c] = btnSheet.getSubimage(
                            c * BTN_SRC_W, r * BTN_SRC_H, BTN_SRC_W, BTN_SRC_H);
        }
    }

    private void buildLayout() {
        bgW = (int)(BG_SRC_W * Game.SCALE * BG_RENDER_SCALE);
        bgH = (int)(BG_SRC_H * Game.SCALE * BG_RENDER_SCALE);
        bgX = (Game.GAME_WIDTH  - bgW) / 2;
        bgY = (Game.GAME_HEIGHT - bgH) / 2 + (int)(BG_Y_OFFSET * Game.SCALE);

        int centreX      = bgX + bgW / 2;
        int dropBtnBaseY = bgY + bgH + (int)(dropButtonY * Game.SCALE);
        int exitBtnBaseY = bgY + bgH + (int)(exitButtonY * Game.SCALE);
        int dropBtnBaseX = centreX + (int)(dropButtonX * Game.SCALE);
        int exitBtnBaseX = centreX + (int)(exitButtonX * Game.SCALE);

        dropBounds = new Rectangle(
                dropBtnBaseX - dropButtonWidth / 2, dropBtnBaseY,
                dropButtonWidth, dropButtonHeight);
        exitBounds = new Rectangle(
                exitBtnBaseX - exitButtonWidth / 2, exitBtnBaseY,
                exitButtonWidth, exitButtonHeight);
        openBounds = new Rectangle(
                (Game.GAME_WIDTH - openButtonWidth) / 2,
                Game.GAME_HEIGHT - openButtonHeight - OPEN_BTN_BOTTOM_MARGIN,
                openButtonWidth, openButtonHeight);
    }

    // ─────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────
    public void openPopup()      { popupOpen = true;  selectedSlot = -1; }
    public void closePopup()     { popupOpen = false; selectedSlot = -1; }
    public boolean isPopupOpen() { return popupOpen; }

    public int  getSelectedSlot() { return selectedSlot; }
    public void clearSelection()  { selectedSlot = -1; }

    public void addFare(int fare)    { totalFareEarned += fare; }
    public int  getTotalFareEarned() { return totalFareEarned; }
    public void resetFare()          { totalFareEarned = 0; }

    public void update() {}

    /**
     * Called by Playing.keyPressed() when ESC is pressed while the popup is open.
     * Closes the popup and fires the onClose callback.
     */
    public void handleEsc() {
        if (!popupOpen) return;
        closePopup();
        if (onClose != null) onClose.run();
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g, List<RidingPassenger> passengers, int currentLoop) {

        // Open button — visible when popup is CLOSED (not needed when popup is open)
        if (!popupOpen)
            drawBtn(g, OPEN_ROW, openBounds, openOver, openPressed);

        if (!popupOpen) return;

        Graphics2D g2 = (Graphics2D) g;

        // 1 — dim overlay (blocks interaction with game world behind it)
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);

        // 2 — popup background
        if (bgImg != null)
            g2.drawImage(bgImg, bgX, bgY, bgW, bgH, null);

        // 3 — passenger slots
        for (int slot = 0; slot < MAX_SEATS; slot++) {
            if (slot >= passengers.size() || passengers.get(slot) == null) continue;
            RidingPassenger rp = passengers.get(slot);
            int[] pos  = slotScreenPos(slot);
            boolean isSelected = (slot == selectedSlot);
            rp.setSelected(isSelected);
            rp.renderInSlot(g2, pos[0], pos[1]);
            if (isSelected && !rp.isGettingOut())
                drawStopIndicator(g2, rp.getAssignedStop(), pos[0], pos[1]);
        }

        // 4 — total fare earned
        drawFare(g2);

        // 5 — selected passenger fare status line
        if (selectedSlot >= 0 && selectedSlot < passengers.size()
                && passengers.get(selectedSlot) != null)
            drawSelectedFareInfo(g2, passengers.get(selectedSlot), currentLoop);

        // 6 — Drop & Exit buttons
        boolean canDrop = canDropSelected(passengers, currentLoop);
        drawDropButton(g2, canDrop);
        drawBtn(g2, EXIT_ROW, exitBounds, exitOver, exitPressed);
    }

    // ─────────────────────────────────────────────────────────
    // DROP BUTTON ENABLE LOGIC
    // ─────────────────────────────────────────────────────────
    private boolean canDropSelected(List<RidingPassenger> passengers, int currentLoop) {
        if (selectedSlot < 0 || selectedSlot >= passengers.size()) return false;
        RidingPassenger rp = passengers.get(selectedSlot);
        if (rp == null) return false;
        return rp.isReadyToDrop(currentLoop);
    }

    private void drawDropButton(Graphics2D g2, boolean enabled) {
        if (!enabled) {
            Composite saved = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, DROP_DISABLED_ALPHA));
            drawBtn(g2, DROP_ROW, dropBounds, false, false);
            g2.setComposite(saved);
        } else {
            drawBtn(g2, DROP_ROW, dropBounds, dropOver, dropPressed);
        }
    }

    // ─────────────────────────────────────────────────────────
    // SELECTED PASSENGER FARE STATUS
    // ─────────────────────────────────────────────────────────
    private void drawSelectedFareInfo(Graphics2D g2, RidingPassenger rp, int currentLoop) {
        Font font = new Font("SansSerif", Font.BOLD, (int)(selectedFontSize * Game.SCALE));
        g2.setFont(font);
        int tx = bgX + (int)(selectedFareX * Game.SCALE);
        int ty = bgY + bgH + (int)(selectedFareY * Game.SCALE);

        if (currentLoop < rp.getAssignedStop()) {
            g2.setColor(selectedFareWait);
            int stopsLeft = rp.getAssignedStop() - currentLoop;
            g2.drawString("Fare: \u20B1" + rp.getAssignedFare()
                            + "  (" + stopsLeft + " stop" + (stopsLeft == 1 ? "" : "s") + " away)",
                    tx, ty);
        } else if (currentLoop == rp.getAssignedStop()) {
            g2.setColor(selectedFareOk);
            g2.drawString("Fare: \u20B1" + rp.calculateFare(currentLoop) + "  (Drop now!)", tx, ty);
        } else {
            int missed = currentLoop - rp.getAssignedStop();
            int fare   = rp.calculateFare(currentLoop);
            g2.setColor(selectedFareLate);
            g2.drawString("Fare: \u20B1" + fare
                            + "  (-\u20B1" + (missed * RidingPassenger.MISSED_STOP_PENALTY) + " late penalty)",
                    tx, ty);
        }
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────
    private int[] slotScreenPos(int slot) {
        float sx = (float) bgW / BG_SRC_W;
        float sy = (float) bgH / BG_SRC_H;
        int[] raw = SLOT_RAW[slot];
        return new int[]{ bgX + (int)(raw[0] * sx), bgY + (int)(raw[1] * sy) };
    }

    public Rectangle getSlotBounds(int slot) {
        int[] pos = slotScreenPos(slot);
        return new Rectangle(pos[0], pos[1],
                RidingPassenger.RENDER_W, RidingPassenger.RENDER_H);
    }

    private void drawStopIndicator(Graphics2D g2, int stopNumber, int slotX, int slotY) {
        if (indicatorFrames == null || stopNumber < 1 || stopNumber > 30) return;
        int idx = stopNumber - 1;
        int row = idx / 3;
        int col = idx % 3;
        BufferedImage frame = indicatorFrames[row][col];
        if (frame == null) return;
        g2.drawImage(frame,
                slotX + stopIndicatorOffsetX, slotY + stopIndicatorOffsetY,
                stopIndicatorWidth, stopIndicatorHeight, null);
    }

    private void drawFare(Graphics2D g2) {
        Font font = new Font("SansSerif", Font.BOLD, (int)(fareFontSize * Game.SCALE));
        g2.setFont(font);
        g2.setColor(fareTextColor);
        int tx = bgX + (int)(fareDisplayX * Game.SCALE);
        int ty = bgY + bgH + (int)(fareDisplayY * Game.SCALE);
        g2.drawString("FARE: \u20B1" + totalFareEarned, tx, ty);
    }

    private void drawBtn(Graphics g, int row, Rectangle bounds, boolean over, boolean pressed) {
        if (btnFrames == null) return;
        int col = pressed ? 2 : over ? 1 : 0;
        BufferedImage frame = btnFrames[row][col];
        if (frame != null)
            g.drawImage(frame, bounds.x, bounds.y, bounds.width, bounds.height, null);
    }

    // ─────────────────────────────────────────────────────────
    // INPUT
    // Only the active overlay's own interactive areas receive events.
    // When popup is CLOSED  → only the Open button is live.
    // When popup is OPEN    → Drop, Exit, and slot clicks are live;
    //                         the Open button and game world are blocked.
    // ─────────────────────────────────────────────────────────
    public void mousePressed(MouseEvent e, List<RidingPassenger> passengers) {
        int mx = e.getX(), my = e.getY();

        if (!popupOpen) {
            // Only Open button is clickable
            if (openBounds.contains(mx, my)) openPressed = true;
            return;
        }

        // Popup is open — only Drop, Exit, and slot clicks are valid
        if (dropBounds.contains(mx, my))      { dropPressed = true; return; }
        if (exitBounds.contains(mx, my))      { exitPressed = true; return; }

        for (int slot = 0; slot < MAX_SEATS; slot++) {
            if (slot >= passengers.size() || passengers.get(slot) == null) continue;
            if (getSlotBounds(slot).contains(mx, my)) {
                selectedSlot = (selectedSlot == slot) ? -1 : slot;
                return;
            }
        }
        // Click anywhere else inside the popup → silently swallowed (input blocked)
    }

    public void mouseReleased(MouseEvent e) {
        int mx = e.getX(), my = e.getY();

        if (!popupOpen) {
            if (openPressed && openBounds.contains(mx, my)) {
                openPopup();
                if (onOpen != null) onOpen.run();
            }
            openPressed = false;
            return;
        }

        // Popup open — only Drop and Exit fire actions
        if (dropPressed && dropBounds.contains(mx, my)) {
            System.out.println("[PassengerListOverlay] DROP button released - selectedSlot=" + selectedSlot + ", onOpenPayment=" + (onOpenPayment != null));
            // Open PaymentOverlay instead of immediate drop
            if (onOpenPayment != null && selectedSlot >= 0) {
                System.out.println("[PassengerListOverlay] Calling onOpenPayment.run()");
                onOpenPayment.run();
            } else {
                System.out.println("[PassengerListOverlay] DROP ignored - onOpenPayment=" + (onOpenPayment == null) + ", selectedSlot=" + selectedSlot);
            }
        }

        if (exitPressed && exitBounds.contains(mx, my)) {
            closePopup();
            if (onClose != null) onClose.run();
        }

        dropPressed = false;
        exitPressed = false;
        // openPressed is irrelevant while popup is open
    }

    public void mouseMoved(MouseEvent e) {
        int mx = e.getX(), my = e.getY();
        openOver = !popupOpen && openBounds.contains(mx, my);
        dropOver =  popupOpen && dropBounds.contains(mx, my);
        exitOver =  popupOpen && exitBounds.contains(mx, my);
    }

    public Rectangle getOpenBounds()  { return openBounds; }
    public Rectangle getDropBounds()  { return dropBounds; }
    public Rectangle getExitBounds()  { return exitBounds; }
}