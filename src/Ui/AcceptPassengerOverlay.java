package Ui;

import entities.Person;
import gameStates.GameStates;
import gameStates.Playing;
import main.Game;
import utils.LoadSave;

import static utils.Constants.UI.Buttons.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class AcceptPassengerOverlay {

    private final Playing playing;

    // ── Modal background ──────────────────────────────────────
    private BufferedImage backgroundImg;
    private int bgX, bgY, bgW, bgH;

    // ── Buttons — loaded exactly like MenuButton ──────────────
    private AcceptPassengerButtons yesButton;
    private AcceptPassengerButtons noButton;
    private final PassengerCounter passengerCounter;



    // Counter draw size — mirrors HealthBar's BAR_SCALE approach
    private static final float COUNTER_SCALE = 0.6f;
    private int counterDrawW, counterDrawH;

    // ── Modal state ───────────────────────────────────────────
    private boolean open         = false;
    private Person  activePerson = null;
    private int passengerCount ;

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

    // ── Buttons — positioned like Menu.loadButtons() ──────────
   // buttons[0] = new MenuButton(Game.GAME_WIDTH / 2, (int)(150 * Game.SCALE), 0, GameStates.PLAYING);
    private void createButtons() {
        yesButton = new AcceptPassengerButtons(Game.GAME_WIDTH / 2 - 150, 500, 0);
        noButton  = new AcceptPassengerButtons(Game.GAME_WIDTH / 2 + 150, 500, 1);
    }

    // ── Counter — loaded exactly like HealthBar.loadFrames() ──


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

    // ── Update — mirrors PauseOverlay.update() ────────────────
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

        // Counter — upper-right of modal, drawn before buttons
       // drawCounter(g2);

        // Buttons
        yesButton.draw(g);
        noButton.draw(g);
    }

    // ── Counter — mirrors HealthBar.render() ──────────────────


    // ── Input — mirrors PauseOverlay mouse methods ────────────
    public void mousePressed(MouseEvent e) {
        if (!open) return;
        if      (isIn(e, yesButton)) yesButton.setMousePressed(true);
        else if (isIn(e, noButton))  noButton.setMousePressed(true);
    }

    public void mouseReleased(MouseEvent e) {
        if (!open) return;

        if (isIn(e, yesButton) && yesButton.isMousePressed()) {
            System.out.println("Passenger accepted");
            if (activePerson != null) activePerson.setActive(false);
            passengerCounter.increment();
            System.out.println("Passengers: " + passengerCounter.getCount() + "/" + PassengerCounter.MAX_PASSENGERS);
            close();
            playing.resumeFromInteraction();
        } else if (isIn(e, noButton) && noButton.isMousePressed()) {
            System.out.println("Passenger denied");
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
}