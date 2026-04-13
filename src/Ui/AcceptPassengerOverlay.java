package Ui;

import entities.PassengerManager;
import entities.Person;
import gameStates.Playing;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Modal shown when the player clicks a walking passenger.
 *
 * Data-consistency guarantee (Part 1):
 *   When open() is called the overlay immediately draws a real random stop
 *   (via PassengerManager.drawRandomStop()) and computes the fare with the
 *   canonical formula (via PassengerManager.computeFare()).  These two values
 *   are stored in generatedStop / generatedFare and DISPLAYED to the player.
 *
 *   On YES the same values are forwarded verbatim to
 *   PassengerManager.acceptPassenger(…, generatedStop, generatedFare).
 *   RidingPassenger stores them unchanged, so PassengerListOverlay always
 *   shows data that matches what the player saw in this overlay — no
 *   discrepancy is possible.
 *
 * Input blocking (Part 2):
 *   mousePressed / mouseReleased only act on YES and NO.
 *   Playing.mousePressed() already routes to this overlay exclusively when
 *   interactionPaused == true, so outside clicks never reach here.
 *   ESC closes the overlay (acts like NO) — handled in Playing.keyPressed().
 */
public class AcceptPassengerOverlay {

    // =========================================================
    // OVERLAY SIZE & POSITION  ← ADJUST
    // =========================================================
    private static final int acceptOverlayWidth  = 300;
    private static final int acceptOverlayHeight = 300;

    // =========================================================
    // FARE TEXT  ← ADJUST
    // =========================================================
    private static final int   fareTextX     = 100;
    private static final int   fareTextY     = 170;
    private static final Color fareTextColor = new Color(100, 220, 100);
    private static final int   fareFontSize  = 20;

    // =========================================================
    // STOP TEXT  ← ADJUST
    // =========================================================
    private static final int   stopTextX     = 100;
    private static final int   stopTextY     = 140;
    private static final Color stopTextColor = new Color(255, 220, 50);
    private static final int   stopFontSize  = 20;

    // =========================================================
    // YES BUTTON  ← ADJUST
    // =========================================================
    private static final int yesButtonX      = 20;
    private static final int yesButtonY      = 230;
    private static final int yesButtonWidth  = 280;
    private static final int yesButtonHeight = 100;

    // =========================================================
    // NO BUTTON  ← ADJUST
    // =========================================================
    private static final int noButtonX      = 140;
    private static final int noButtonY      = 230;
    private static final int noButtonWidth  = 280;
    private static final int noButtonHeight = 100;

    // =========================================================
    // INTERNAL
    // =========================================================
    private final Playing          playing;
    private final PassengerCounter passengerCounter;
    private       PassengerManager passengerManager;

    private BufferedImage backgroundImg;
    private int acceptOverlayX, acceptOverlayY, bgW, bgH;

    private AcceptPassengerButtons yesButton;
    private AcceptPassengerButtons noButton;

    private boolean open         = false;
    private Person  activePerson = null;

    /**
     * The real stop and fare generated at open() time.
     * These exact values are shown to the player AND stored in RidingPassenger
     * on YES — no re-calculation ever happens between display and storage.
     */
    private int generatedStop = -1;
    private int generatedFare =  0;

    // ─────────────────────────────────────────────────────────
    public AcceptPassengerOverlay(Playing playing, PassengerCounter passengerCounter) {
        this.playing          = playing;
        this.passengerCounter = passengerCounter;
        loadBackground();
        createButtons();
    }

    public void setPassengerManager(PassengerManager pm) {
        this.passengerManager = pm;
    }

    // ─────────────────────────────────────────────────────────
    // LAYOUT
    // ─────────────────────────────────────────────────────────
    private void loadBackground() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.ACCEPT_PASSENGER_BACKGROUND);
        bgW = (int)(acceptOverlayWidth  * Game.SCALE);
        bgH = (int)(acceptOverlayHeight * Game.SCALE);
        acceptOverlayX = (Game.GAME_WIDTH  - bgW) / 2;
        acceptOverlayY = (Game.GAME_HEIGHT - bgH) / 2;
    }

    private void createButtons() {
        int ybX = acceptOverlayX + (int)(yesButtonX * Game.SCALE);
        int ybY = acceptOverlayY + (int)(yesButtonY * Game.SCALE);
        yesButton = new AcceptPassengerButtons(ybX + yesButtonWidth / 2, ybY, 0);

        int nbX = acceptOverlayX + (int)(noButtonX * Game.SCALE);
        int nbY = acceptOverlayY + (int)(noButtonY * Game.SCALE);
        noButton = new AcceptPassengerButtons(nbX + noButtonWidth / 2, nbY, 1);
    }

    // ─────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────

    /**
     * Opens the overlay for the given person.
     * Generates the real random stop and its exact fare HERE — once — so
     * what is displayed is identical to what will be stored on YES.
     */
    public void open(Person person) {
        if (passengerManager == null) return;

        int currentLoop = playing.getWorldLoopCount();
        int maxLoop     = playing.getMaxWorldLoops();

        // Draw the real stop and compute the real fare right now
        int stop = passengerManager.drawRandomStop(currentLoop, maxLoop);
        if (stop < 0) {
            // No valid stop exists — cannot open overlay
            return;
        }
        int fare = PassengerManager.computeFare(currentLoop, stop);

        generatedStop = stop;
        generatedFare = fare;
        activePerson  = person;
        open          = true;
        resetBools();
    }

    /**
     * Closes the overlay (acts like NO — nothing is accepted).
     * Also called by ESC in Playing.keyPressed().
     */
    public void close() {
        open          = false;
        activePerson  = null;
        generatedStop = -1;
        generatedFare =  0;
        resetBools();
    }

    public boolean isOpen()          { return open; }
    public Person  getActivePerson() { return activePerson; }

    // Legacy stubs kept for API compatibility
    public void resetPassengerCount() {}
    public void resetEarnings()       {}
    public int  getTotalEarnings()    {
        return (passengerManager != null) ? passengerManager.getTotalFareEarned() : 0;
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update() {
        if (!open) return;
        yesButton.update();
        noButton.update();
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (!open) return;

        Graphics2D g2 = (Graphics2D) g;

        // Dim world — blocks visual access to game behind overlay
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);

        if (backgroundImg != null)
            g2.drawImage(backgroundImg, acceptOverlayX, acceptOverlayY, bgW, bgH, null);

        drawContent(g2);
        yesButton.draw(g);
        noButton.draw(g);
    }

    private void drawContent(Graphics2D g2) {
        if (activePerson == null) return;

        // ── Fare (generated at open time) ─────────────────────
        Font fareFont = new Font("SansSerif", Font.BOLD, (int)(fareFontSize * Game.SCALE));
        g2.setFont(fareFont);
        g2.setColor(fareTextColor);
        String fareStr = (generatedFare > 0)
                ? "Earn: \u20B1" + generatedFare
                : "Earn: --";
        g2.drawString(fareStr,
                acceptOverlayX + (int)(fareTextX * Game.SCALE),
                acceptOverlayY + (int)(fareTextY * Game.SCALE));

        // ── Stop (generated at open time) ─────────────────────
        Font stopFont = new Font("SansSerif", Font.BOLD, (int)(stopFontSize * Game.SCALE));
        g2.setFont(stopFont);
        g2.setColor(stopTextColor);
        String stopStr = (generatedStop > 0)
                ? "Stop: " + generatedStop
                : "Stop: --";
        g2.drawString(stopStr,
                acceptOverlayX + (int)(stopTextX * Game.SCALE),
                acceptOverlayY + (int)(stopTextY * Game.SCALE));
    }

    // ─────────────────────────────────────────────────────────
    // INPUT — only YES and NO are wired; all other clicks are silently swallowed
    // ─────────────────────────────────────────────────────────
    public void mousePressed(MouseEvent e) {
        if (!open) return;
        if      (isIn(e, yesButton)) yesButton.setMousePressed(true);
        else if (isIn(e, noButton))  noButton.setMousePressed(true);
        // clicks anywhere else are intentionally ignored
    }

    public void mouseReleased(MouseEvent e) {
        if (!open) return;

        if (isIn(e, yesButton) && yesButton.isMousePressed()) {
            handleYes();
        } else if (isIn(e, noButton) && noButton.isMousePressed()) {
            handleNo();
        }
        // clicks anywhere else are intentionally ignored
        resetBools();
    }

    /** Called by Playing.keyPressed() when ESC is pressed while this overlay is open. */
    public void handleEsc() {
        if (!open) return;
        handleNo();
    }

    private void handleYes() {
        if (activePerson == null || passengerManager == null) { close(); playing.resumeFromInteraction(); return; }
        if (passengerManager.isFull()) {
            System.out.println("[AcceptOverlay] Jeepney full");
            close();
            playing.resumeFromInteraction();
            return;
        }

        int currentLoop = playing.getWorldLoopCount();

        // Pass the EXACT values already shown to the player — no re-randomisation
        boolean accepted = passengerManager.acceptPassenger(
                activePerson.getPersonId(),
                currentLoop,
                activePerson.getSpawnLaneY(),
                activePerson.getAtlasPath(),
                activePerson.getPersonId(),
                generatedStop,
                generatedFare);

        if (accepted) {
            activePerson.setActive(false);
            passengerCounter.increment();
            System.out.println("[AcceptOverlay] Accepted → stop " + generatedStop
                    + "  fare \u20B1" + generatedFare);
        } else {
            System.out.println("[AcceptOverlay] Accept failed (no slot?)");
        }

        close();
        playing.resumeFromInteraction();
    }

    private void handleNo() {
        close();
        playing.resumeFromInteraction();
    }

    public void mouseMoved(MouseEvent e) {
        if (!open) return;
        yesButton.setMouseOver(false);
        noButton.setMouseOver(false);
        if      (isIn(e, yesButton)) yesButton.setMouseOver(true);
        else if (isIn(e, noButton))  noButton.setMouseOver(true);
    }

    public void mouseDragged(MouseEvent e) { mouseMoved(e); }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────
    private boolean isIn(MouseEvent e, AcceptPassengerButtons b) {
        return b.getBounds().contains(e.getX(), e.getY());
    }

    private void resetBools() {
        yesButton.resetBools();
        noButton.resetBools();
    }
}