package gameStates;

import Ui.AcceptPassengerOverlay;
import Ui.PassengerCounter;
import entities.Person;
import entities.PersonManager;
import entities.Player;
import utils.RouteConstants;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Random;

public class PassengerInteractionController {
    private final Playing playing;
    private final AcceptPassengerOverlay overlay;
    private final PassengerCounter passengerCounter;
    private final Random rng = new Random();

    private boolean interactionPaused = false;
    private int currentStopIndex = 0;

    public PassengerInteractionController(Playing playing, PassengerCounter passengerCounter) {
        this.playing = playing;
        this.passengerCounter = passengerCounter;
        this.overlay = new AcceptPassengerOverlay(playing, passengerCounter);
    }

    public void updateOverlay() {
        overlay.update();
    }

    public void renderOverlay(Graphics g) {
        overlay.render(g);
    }

    public boolean isInteractionPaused() {
        return interactionPaused;
    }

    public void resumeInteraction() {
        interactionPaused = false;
        overlay.close();
    }

    public void reset() {
        interactionPaused = false;
        currentStopIndex = 0;
        overlay.close();
        overlay.resetPassengerCount();
        overlay.resetEarnings();
    }

    public void checkPassengerInteractions(Player player, PersonManager personManager) {
        if (playing.getPassengerManager().isFull()) return;

        Rectangle2D.Float jeepHB = player.getHitBox();
        if (jeepHB == null) return;

        for (Person p : personManager.getPersons()) {
            if (p.getType() != Person.PersonType.PASSENGER) continue;
            if (!p.isActive()) continue;

            Rectangle2D.Float pHB = p.getHitBox();
            if (pHB == null) continue;

            boolean overlapping = jeepHB.intersects(pHB);
            if (overlapping && !p.isInteractable()) {
                System.out.println("Passenger ready for interaction");
            }

            p.setInteractable(overlapping);
        }
    }

    public boolean handleMouseClick(MouseEvent e, PersonManager personManager) {
        if (interactionPaused || e.getButton() != MouseEvent.BUTTON1) {
            return false;
        }

        for (Person p : personManager.getPersons()) {
            if (!p.isInteractable()) continue;

            Rectangle2D.Float pHB = p.getHitBox();
            if (pHB != null && pHB.contains(e.getX(), e.getY())) {
                System.out.println("Passenger clicked");
                System.out.println("Game paused");

                int destIndex = RouteConstants.randomForwardStopIndex(currentStopIndex, rng);
                p.setDestinationStop(RouteConstants.STOPS[destIndex]);
                p.setFare(RouteConstants.randomFare(rng));

                System.out.println("Destination: " + p.getDestinationStop());
                System.out.println("Fare: ₱" + p.getFare());

                interactionPaused = true;
                overlay.open(p);
                return true;
            }
        }

        return false;
    }

    public void mousePressed(MouseEvent e) {
        if (interactionPaused) {
            overlay.mousePressed(e);
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (interactionPaused) {
            overlay.mouseDragged(e);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (interactionPaused) {
            overlay.mouseReleased(e);
        }
    }

    public void mouseMoved(MouseEvent e) {
        overlay.mouseMoved(e);
    }

    public boolean closeIfOverlayClosed() {
        if (interactionPaused && !overlay.isOpen()) {
            interactionPaused = false;
            return true;
        }
        return false;
    }
}
