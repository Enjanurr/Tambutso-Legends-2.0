package Ui;

import entities.Person;
import gameStates.Playing;
import main.Game;
import utils.LoadSave;
import utils.RouteConstants;

import static utils.Constants.UI.Buttons.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class AcceptPassengerOverlay {

    private final Playing          playing;
    private final PassengerCounter passengerCounter;

    // ── Modal background ──────────────────────────────────────
    private BufferedImage backgroundImg;
    private int bgX, bgY, bgW, bgH;

    // ── Buttons ───────────────────────────────────────────────
    private AcceptPassengerButtons yesButton;
    private AcceptPassengerButtons noButton;

    // ── Earnings ──────────────────────────────────────────────
    private int totalEarnings = 0;
    private int passengerCount = 0;

    // ── Modal state ───────────────────────────────────────────
    private boolean open         = false;
    private Person  activePerson = null;

    // ─────────────────────────────────────────────────────────
    public AcceptPassengerOverlay(Playing playing, PassengerCounter passengerCounter) {
        this.playing          = playing;
        this.passengerCounter = passengerCounter;
        loadBackground();
        createButtons();
    }

    // ── Background ────────────────────────────────────────────
    private void loadBackground() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.ACCEPT_PASSENGER_BACKGROUND);

        float modalScale = Math.min(
                (float) Game.GAME_WIDTH  / backgroundImg.getWidth(),
                (float) Game.GAME_HEIGHT / backgroundImg.getHeight()) * 1f;

        bgW = (int)(backgroundImg.getWidth()  * modalScale);
        bgH = (int)(backgroundImg.getHeight() * modalScale);
        bgX = Game.GAME_WIDTH  / 2 - bgW / 2;
        bgY = Game.GAME_HEIGHT / 2 - bgH / 2;
    }

    // ── Buttons ───────────────────────────────────────────────
    private void createButtons() {
        yesButton = new AcceptPassengerButtons(Game.GAME_WIDTH / 2 - 120, 600, 0);
        noButton  = new AcceptPassengerButtons(Game.GAME_WIDTH / 2 + 120, 600, 1);
    }

    // ── API ───────────────────────────────────────────────────
    public void open(Person person) {
        activePerson = person;
        open         = true;
        resetBools();
        System.out.println("Modal opened");
    }

    public void close() {
        open         = false;
        activePerson = null;
        resetBools();
    }

    public boolean isOpen()              { return open; }
    public Person  getActivePerson()     { return activePerson; }
    public int     getPassengerCount()   { return passengerCount; }
    public void    resetPassengerCount() { passengerCount = 0; }
    public int     getTotalEarnings()    { return totalEarnings; }
    public void    resetEarnings()       { totalEarnings = 0; }

    // ── Update ────────────────────────────────────────────────
    public void update() {
        if (!open) return;
        yesButton.update();
        noButton.update();
    }

    // ── Render ────────────────────────────────────────────────
    public void render(Graphics g) {
        if (!open) return;

        Graphics2D g2 = (Graphics2D) g;

        // Dim frozen world
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);

        // Modal background
        g2.drawImage(backgroundImg, bgX, bgY, bgW, bgH, null);

        // Trip info — stop name and fare
        drawTripInfo(g2);

        // Buttons
        yesButton.draw(g);
        noButton.draw(g);
    }

    // ── Trip info ─────────────────────────────────────────────
    private void drawTripInfo(Graphics2D g2) {
        if (activePerson == null) return;

        String stop = activePerson.getDestinationStop();
        int    fare = activePerson.getFare();

        if (stop == null || stop.isEmpty()) return;

        Font labelFont = new Font("SansSerif", Font.BOLD, (int)(12 * Game.SCALE));
        Font valueFont = new Font("SansSerif", Font.BOLD, (int)(14 * Game.SCALE));
        Font totalFont = new Font("SansSerif", Font.BOLD, (int)(16 * Game.SCALE));
        // ── "Total Fare:" line — visually distinct ────────────────
        g2.setFont(totalFont);
        g2.setColor(new Color(100, 220, 100));
        drawCentered(g2, "Total Fare: \u20B1" + totalEarnings,
                bgX, bgY + (int)(bgH * 0.40f), bgW);

        // ── "Going to:" line ──────────────────────────────────────
        g2.setFont(labelFont);
        g2.setColor(new Color(255, 220, 50));
        drawCentered(g2, "Going to: " + stop,
                bgX, bgY + (int)(bgH * 0.60f), bgW);

        // ── "Fare:" line ──────────────────────────────────────────
        g2.setFont(valueFont);
        g2.setColor(new Color(255, 220, 50));
        drawCentered(g2, "Fare: \u20B1" + fare,
                bgX, bgY + (int)(bgH * 0.50f), bgW);


    }
    // ── Input ─────────────────────────────────────────────────
    public void mousePressed(MouseEvent e) {
        // incase
        if(passengerCounter.isFull()) return;
        if (!open) return;
        if      (isIn(e, yesButton)) yesButton.setMousePressed(true);
        else if (isIn(e, noButton))  noButton.setMousePressed(true);
    }

    public void mouseReleased(MouseEvent e) {
        if (!open) return;



        if (isIn(e, yesButton) && yesButton.isMousePressed()) {
            System.out.println("Passenger accepted");

            if (activePerson != null) {
                totalEarnings += activePerson.getFare();
                System.out.println("Fare collected: \u20B1" + activePerson.getFare());
                System.out.println("Total earnings: \u20B1" + totalEarnings);
                activePerson.setActive(false);   // remove from world
            }

            passengerCounter.increment();
            passengerCount++;
            System.out.println("Passengers: "
                    + passengerCounter.getCount() + "/"
                    + PassengerCounter.MAX_PASSENGERS);

            close();
            playing.resumeFromInteraction();

        } else if (isIn(e, noButton) && noButton.isMousePressed()) {
            System.out.println("Passenger denied");
            // Passenger stays — activePerson untouched
            close();
            playing.resumeFromInteraction();
        }

        resetBools();
    }

    public void mouseMoved(MouseEvent e) {
        if (!open) return;
        yesButton.setMouseOver(false);
        noButton.setMouseOver(false);
        if      (isIn(e, yesButton)) yesButton.setMouseOver(true);
        else if (isIn(e, noButton))  noButton.setMouseOver(true);
    }

    public void mouseDragged(MouseEvent e) { mouseMoved(e); }

    // ── Helpers ───────────────────────────────────────────────
    private boolean isIn(MouseEvent e, AcceptPassengerButtons b) {
        return b.getBounds().contains(e.getX(), e.getY());
    }

    private void resetBools() {
        yesButton.resetBools();
        noButton.resetBools();
    }

    private void drawCentered(Graphics2D g2, String text,
                              int containerX, int y, int containerW) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text,
                containerX + (containerW - fm.stringWidth(text)) / 2, y);
    }
}