package Ui;

import entities.PassengerManager;
import entities.Person;
import gameStates.Playing;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

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
    private static final int acceptOverlayHeight = 320;

    // =========================================================
    // STOP NAMES - Retrieved dynamically from LevelConfig
    // via playing.getLevelManager().getCurrentLevelConfig()
    // =========================================================

    // =========================================================
    // STOP NAME TEXT  ← ADJUST
    // =========================================================
    private static final int   stopNameX        = 90;
    private static final int   stopNameY        = 135;
    private static final Color stopNameColor    = new Color(255, 255, 255);
    private static final int   stopNameFontSize = 14;
    private static final int   stopNameMaxWidth = 240;  // Max width before wrapping

    // =========================================================
    // STOP NUMBER TEXT  ← ADJUST
    // =========================================================
    private static final int   stopNumberX      = 125;
    private static final int   stopNumberY      = 170;
    private static final Color stopNumberColor  = new Color(255, 220, 50);
    private static final int   stopNumberFontSize = 18;

    // =========================================================
    // FARE TEXT  ← ADJUST
    // =========================================================
    private static final int   fareTextX        = 175;
    private static final int   fareTextY        = 198;
    private static final Color fareTextColor    = new Color(100, 220, 100);
    private static final int   fareFontSize     = 20;

    // =========================================================
    // YES BUTTON  ← ADJUST
    // =========================================================
    private static final int yesButtonX      = -50;
    private static final int yesButtonY      = 230;
    private static final int yesButtonWidth  = 280;
    private static final int yesButtonHeight = 100;

    // =========================================================
    // NO BUTTON  ← ADJUST
    // =========================================================
    private static final int noButtonX      = 70;
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
    private int     ignoreInputTimer = 0;
    private int     openingTimestamp = 0;  // Frame counter when opened

    /**
     * The real stop and fare generated at open() time.
     * These exact values are shown to the player AND stored in RidingPassenger
     * on YES — no re-calculation ever happens between display and storage.
     */
    private int generatedStop = -1;
    private int generatedFare =  0;

    // Cache for wrapped stop name text lines
    private List<String> wrappedStopNameLines = new ArrayList<>();

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
        yesButton = new AcceptPassengerButtons(ybX + (int)(yesButtonWidth * Game.SCALE) / 2, ybY, 0);

        int nbX = acceptOverlayX + (int)(noButtonX * Game.SCALE);
        int nbY = acceptOverlayY + (int)(noButtonY * Game.SCALE);
        noButton = new AcceptPassengerButtons(nbX + (int)(noButtonWidth * Game.SCALE) / 2, nbY, 1);
    }

    // ─────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────

    /**
     * Opens the overlay for the given person.
     * Generates the real random stop and its exact fare HERE — once — so
     * what is displayed is identical to what will be stored on YES.
     * @return true if overlay opened successfully, false otherwise
     */
    public boolean open(Person person) {
        if (passengerManager == null) {
            System.out.println("[AcceptOverlay] open() failed - passengerManager is null");
            return false;
        }

        int currentLoop = playing.getWorldLoopCount();
        int maxLoop = playing.getLevelManager().getMaxWorldLoops();

        // Check if any future stops exist
        if (currentLoop >= maxLoop) {
            System.out.println("[AcceptOverlay] open() failed - no future stops available");
            return false;
        }

        // ── NEW: Check for cached data ──────────────────────────────
        if (person.getCachedStop() != null && person.getCachedFare() != null) {
            // Use cached values (same as before)
            generatedStop = person.getCachedStop();
            generatedFare = person.getCachedFare();
            System.out.println("[AcceptOverlay] Using cached: Stop " + generatedStop + ", Fare ₱" + generatedFare);
        } else {
            // Generate new random stop and fare
            int stop = passengerManager.drawRandomStop(currentLoop, maxLoop);
            if (stop < 0) {
                System.out.println("[AcceptOverlay] open() failed - drawRandomStop returned -1");
                return false;
            }
            int fare = PassengerManager.computeFare(currentLoop, stop);

            generatedStop = stop;
            generatedFare = fare;

            // Cache the values for this passenger
            person.setCachedStop(stop);
            person.setCachedFare(fare);
            System.out.println("[AcceptOverlay] Generated new: Stop " + stop + ", Fare ₱" + fare);
        }

        activePerson = person;
        open = true;
        ignoreInputTimer = 30;  // Ignore mouse for ~30 frames (150ms at 200 UPS)
        openingTimestamp = 30;  // Mark as recently opened (same duration as ignoreInputTimer)

        // Pre-wrap the stop name for rendering
        wrapStopName();

        resetBools();
        System.out.println("[AcceptOverlay] open() SUCCESS - overlay opened");
        return true;
    }

    /**
     * Gets the stop name from LevelConfig for the current level.
     */
    private String getStopNameFromConfig(int stopNumber) {
        if (playing == null || playing.getLevelManager() == null) {
            return "Stop " + stopNumber;
        }
        return playing.getLevelManager().getStopName(stopNumber);
    }

    /**
     * Wraps the stop name text to fit within max width.
     */
    private void wrapStopName() {
        wrappedStopNameLines.clear();

        String stopName = getStopNameFromConfig(generatedStop);
        if (stopName.startsWith("Unknown") || stopName.startsWith("Stop ")) {
            wrappedStopNameLines.add(stopName);
            return;
        }

        // Calculate font metrics for wrapping
        FontMetrics metrics = getFontMetrics(stopNameFontSize);
        if (metrics == null) {
            // Fallback: just add the whole string
            wrappedStopNameLines.add(stopName);
            return;
        }

        String[] words = stopName.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            int stringWidth = metrics.stringWidth(testLine);

            if (stringWidth < stopNameMaxWidth * Game.SCALE) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    wrappedStopNameLines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            wrappedStopNameLines.add(currentLine.toString());
        }

        // If no wrapping occurred, just add the original
        if (wrappedStopNameLines.isEmpty()) {
            wrappedStopNameLines.add(stopName);
        }
    }

    /**
     * Helper to get FontMetrics for a given font size.
     */
    private FontMetrics getFontMetrics(int fontSize) {
        // This is a bit hacky - we need a Graphics context for accurate metrics
        // The actual metrics will be computed during render
        return null;
    }

    /**
     * Closes the overlay (acts like NO — nothing is accepted).
     * Also called by ESC in Playing.keyPressed().
     */
    public void close() {
        System.out.println("[AcceptOverlay] close() called - open was " + open);
        open             = false;
        activePerson     = null;
        generatedStop    = -1;
        generatedFare    =  0;
        ignoreInputTimer = 0;   // Clear input lock so next open() starts clean
        // NOTE: openingTimestamp is intentionally NOT zeroed here.
        // It ticks down naturally in update(), keeping isRecentlyOpened() == true
        // for the remainder of the 30-frame grace period even after close() is called.
        // This prevents the force-reset in Playing.update() from firing before the
        // AWT event queue has fully drained, which would cause the ghost-overlay bug.
        wrappedStopNameLines.clear();
        resetBools();
        System.out.println("[AcceptOverlay] close() completed - open=" + open);
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
        // Always tick timers — even when closed — so they never freeze mid-countdown.
        if (ignoreInputTimer > 0) ignoreInputTimer--;
        if (openingTimestamp > 0) openingTimestamp--;
        if (!open) return;
        yesButton.update();
        noButton.update();
    }

    /**
     * Returns true if overlay was opened within the last 30 frames.
     * Used by Playing to prevent premature force-reset of interactionPaused.
     */
    public boolean isRecentlyOpened() {
        return openingTimestamp > 0;
    }

    /**
     * Returns number of frames since overlay was opened.
     * 0 = just opened, 30+ = safe to force reset if closed.
     */
    public int getFramesSinceOpen() {
        return openingTimestamp > 0 ? (30 - openingTimestamp) : 999;
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

        // ── Stop Name (wrapped, centered, displayed first) ─────────
        if (generatedStop >= 1) {
            Font nameFont = new Font("SansSerif", Font.BOLD, (int)(stopNameFontSize * Game.SCALE));
            g2.setFont(nameFont);
            g2.setColor(stopNameColor);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int lineHeight = (int)((stopNameFontSize + 4) * Game.SCALE);
            int startY = acceptOverlayY + (int)(stopNameY * Game.SCALE);
            int overlayCenterX = acceptOverlayX + (bgW / 2);

            for (int i = 0; i < wrappedStopNameLines.size(); i++) {
                int yPos = startY + (i * lineHeight);
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(wrappedStopNameLines.get(i));
                int centeredX = overlayCenterX - (textWidth / 2);
                g2.drawString(wrappedStopNameLines.get(i), centeredX, yPos);
            }
        }else {
            // Fallback for invalid stop number
            Font nameFont = new Font("SansSerif", Font.BOLD, (int)(stopNameFontSize * Game.SCALE));
            g2.setFont(nameFont);
            g2.setColor(stopNameColor);
            g2.drawString("Unknown Stop",
                    acceptOverlayX + (int)(stopNameX * Game.SCALE),
                    acceptOverlayY + (int)(stopNameY * Game.SCALE));
        }

        // ── Stop Number (displayed second) ─────────────────────
        Font stopFont = new Font("SansSerif", Font.BOLD, (int)(stopNumberFontSize * Game.SCALE));
        g2.setFont(stopFont);
        g2.setColor(stopNumberColor);

        // Calculate Y position based on wrapped lines
        int lineCount = Math.max(1, wrappedStopNameLines.size());
        int lineHeight = (int)((stopNameFontSize + 4) * Game.SCALE);
        int stopNumberActualY = acceptOverlayY + (int)(stopNumberY * Game.SCALE);

        String stopStr = (generatedStop > 0)
                ? "" + generatedStop
                : "";
        g2.drawString(stopStr,
                acceptOverlayX + (int)(stopNumberX * Game.SCALE),
                stopNumberActualY);

        // ── Fare (generated at open time, displayed third) ─────────────────────
        Font fareFont = new Font("SansSerif", Font.BOLD, (int)(fareFontSize * Game.SCALE));
        g2.setFont(fareFont);
        g2.setColor(fareTextColor);
        String fareStr = (generatedFare > 0)
                ? "" + generatedFare
                : "";
        g2.drawString(fareStr,
                acceptOverlayX + (int)(fareTextX * Game.SCALE),
                acceptOverlayY + (int)(fareTextY * Game.SCALE));
    }

    // ─────────────────────────────────────────────────────────
    // INPUT — only YES and NO are wired; all other clicks are silently swallowed
    // ─────────────────────────────────────────────────────────
    public void mousePressed(MouseEvent e) {
        if (!open || ignoreInputTimer > 0) {
            if (ignoreInputTimer > 0) System.out.println("[AcceptOverlay] mousePressed - ignoring during input lock");
            return;
        }
        System.out.println("[AcceptOverlay] mousePressed at (" + e.getX() + ", " + e.getY() + ")");
        System.out.println("[AcceptOverlay] yesButton bounds: " + yesButton.getBounds());
        System.out.println("[AcceptOverlay] noButton bounds: " + noButton.getBounds());

        if (isIn(e, yesButton)) {
            System.out.println("[AcceptOverlay] YES button pressed");
            yesButton.setMousePressed(true);
        } else if (isIn(e, noButton)) {
            System.out.println("[AcceptOverlay] NO button pressed");
            noButton.setMousePressed(true);
        } else {
            System.out.println("[AcceptOverlay] Click outside buttons - ignored");
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (!open || ignoreInputTimer > 0) return;

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
        System.out.println("[AcceptOverlay] handleYes() STARTED - open=" + open);
        if (activePerson == null || passengerManager == null) {
            System.out.println("[AcceptOverlay] handleYes() - null person/manager");
            return;
        }
        if (passengerManager.isFull()) {
            System.out.println("[AcceptOverlay] Jeepney full");
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
            activePerson.clearCache();
            // Get stop name for console output
            String stopName = getStopNameFromConfig(generatedStop);
            System.out.println("[AcceptOverlay] Accepted → " + stopName + " (Stop " + generatedStop
                    + ")  fare ₱" + generatedFare);
        } else {
            System.out.println("[AcceptOverlay] Accept failed (no slot?)");
        }

        System.out.println("[AcceptOverlay] handleYes() - closing overlay");
        close();
        playing.resumeFromInteraction();
        System.out.println("[AcceptOverlay] handleYes() COMPLETED - open=" + open);
    }

    private void handleNo() {
        System.out.println("[AcceptOverlay] handleNo() called - closing");
        close();
        playing.resumeFromInteraction();
        System.out.println("[AcceptOverlay] handleNo() COMPLETED - open=" + open);
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

    // Stop names now retrieved dynamically from LevelConfig via getStopNameFromConfig()
}