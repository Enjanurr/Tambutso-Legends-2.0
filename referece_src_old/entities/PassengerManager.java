package entities;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import gameStates.Playing;

/**
 * Manages all passengers currently seated in the jeepney.
 *
 * Data-consistency contract (revised):
 *   AcceptPassengerOverlay generates destinationStop and calculatedFare at
 *   open() time using drawRandomStop() + computeFare(), displays them to the
 *   player, then passes those EXACT values to acceptPassenger(…, stop, fare)
 *   on YES.  RidingPassenger stores them verbatim — PassengerListOverlay reads
 *   identical data with no re-calculation.
 *
 * Drop rule:
 *   dropPassenger() only succeeds when worldLoopCount >= assignedStop.
 *   Returns -1 (refused) if called too early.
 *   Late drops deduct MISSED_STOP_PENALTY per extra loop (min ₱0).
 */
public class PassengerManager {

    public static final int MAX_SEATS = 9;

    // ── Fare constants ── ← ADJUST ───────────────────────────
    public static final int BASE_FARE     = 50;  // ₱ base
    public static final int PER_STOP_FARE = 30;  // ₱ per stop ridden
    // ─────────────────────────────────────────────────────────

    private final RidingPassenger[] seats = new RidingPassenger[MAX_SEATS];
    private final List<DropAnimation> dropAnimations = new ArrayList<>();

    private int totalFareEarned = 0;

    private final Playing playing;
    private final Random  rng = new Random();

    public PassengerManager(Playing playing) {
        this.playing = playing;
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update(int currentLoop, boolean worldScrolling, float scrollSpeed) {
        for (RidingPassenger rp : seats)
            if (rp != null) rp.update(currentLoop);

        dropAnimations.removeIf(da -> {
            da.update(worldScrolling, scrollSpeed);
            return !da.isActive();
        });
    }

    // ─────────────────────────────────────────────────────────
    // FARE UTILITIES  (used by AcceptPassengerOverlay)
    // ─────────────────────────────────────────────────────────

    /**
     * Canonical fare formula.
     * AcceptPassengerOverlay calls this at open() so the value it shows
     * is identical to what will be stored in RidingPassenger.
     */
    public static int computeFare(int currentLoop, int targetStop) {
        int stopsToRide = Math.max(0, targetStop - currentLoop);
        return BASE_FARE + stopsToRide * PER_STOP_FARE;
    }

    /**
     * Draws a random stop in [currentLoop+1 … maxLoop].
     * Called ONCE by AcceptPassengerOverlay.open() so the real stop is shown
     * to the player before they decide, not recalculated on YES.
     *
     * @return chosen stop, or -1 if no valid stop exists
     */
    public int drawRandomStop(int currentLoop, int maxLoop) {
        int min = currentLoop + 1;
        int max = maxLoop;
        if (min > max) return -1;
        return min + rng.nextInt(max - min + 1);
    }

    // ─────────────────────────────────────────────────────────
    // ACCEPT
    // ─────────────────────────────────────────────────────────

    /**
     * PRIMARY path — called by AcceptPassengerOverlay on YES.
     *
     * Accepts a passenger with the EXACT stop and fare already displayed in
     * the overlay.  Nothing is re-randomised or re-calculated here.
     *
     * @param assignedStop  stop drawn at overlay open() time
     * @param assignedFare  fare computed at overlay open() time
     * @return true if seated successfully; false if jeepney full / no slot
     */
    public boolean acceptPassenger(int personId, int currentLoop,
                                   float laneY, String atlasPath, int personTypeId,
                                   int assignedStop, int assignedFare) {
        if (isFull()) return false;
        int slot = firstEmptySlot();
        if (slot < 0) return false;

        seats[slot] = new RidingPassenger(
                personId, assignedStop, currentLoop,
                laneY, atlasPath, personTypeId,
                assignedFare);
        return true;
    }

    /**
     * LEGACY overload — kept so nothing outside the overlay system breaks.
     * Randomises its own stop; prefer the 7-arg overload for new callers.
     *
     * @return assigned stop number (≥1), or -1 if jeepney full / no future stops
     */
    public int acceptPassenger(int personId, int currentLoop, int maxLoop,
                               float laneY, String atlasPath, int personTypeId) {
        if (isFull())               return -1;
        if (currentLoop >= maxLoop) return -1;

        int stop = drawRandomStop(currentLoop, maxLoop);
        if (stop < 0) return -1;

        int slot = firstEmptySlot();
        if (slot < 0) return -1;

        int fare = computeFare(currentLoop, stop);
        seats[slot] = new RidingPassenger(
                personId, stop, currentLoop,
                laneY, atlasPath, personTypeId, fare);
        return stop;
    }

    // ─────────────────────────────────────────────────────────
    // DROP
    // ─────────────────────────────────────────────────────────

    /**
     * Drops the passenger in the given slot.
     *
     * Condition: worldLoopCount >= assignedStop.
     * Returns -1 if the slot is empty OR the drop is not yet allowed.
     * Late drops deduct MISSED_STOP_PENALTY per extra loop, minimum ₱0.
     */
    public int dropPassenger(int slot, int currentLoop, float jeepX, float jeepY) {
        if (slot < 0 || slot >= MAX_SEATS) return -1;
        RidingPassenger rp = seats[slot];
        if (rp == null) return -1;
        if (!rp.isReadyToDrop(currentLoop)) return -1;

        int fare = rp.calculateFare(currentLoop);
        totalFareEarned += fare;

        dropAnimations.add(new DropAnimation(
                jeepX, rp.getLaneY(),
                rp.getAtlasPath(), rp.getPersonTypeId()));

        seats[slot] = null;
        return fare;
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void renderDropAnimations(Graphics g) {
        for (DropAnimation da : dropAnimations) da.render(g);
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS / GETTERS
    // ─────────────────────────────────────────────────────────
    public boolean isFull() {
        for (RidingPassenger rp : seats) if (rp == null) return false;
        return true;
    }

    public int occupiedCount() {
        int n = 0;
        for (RidingPassenger rp : seats) if (rp != null) n++;
        return n;
    }

    private int firstEmptySlot() {
        for (int i = 0; i < MAX_SEATS; i++) if (seats[i] == null) return i;
        return -1;
    }


    /** Returns a list of size MAX_SEATS preserving nulls for empty slots. */
    // This method already exists in PassengerManager.java
    public List<RidingPassenger> getSeatList() {
        List<RidingPassenger> list = new ArrayList<>(MAX_SEATS);
        for (RidingPassenger rp : seats) list.add(rp);
        return list;
    }
    public RidingPassenger getSeat(int slot) {
        if (slot < 0 || slot >= MAX_SEATS) return null;
        return seats[slot];
    }

    public int getTotalFareEarned() { return totalFareEarned; }

    public void resetAll() {
        for (int i = 0; i < MAX_SEATS; i++) seats[i] = null;
        dropAnimations.clear();
        totalFareEarned = 0;
    }
}