package utils;

public class RouteConstants {

    // ── Ordered route — index = stop number (0 = first stop) ─
    public static final String[] STOPS = {
            "Naga City Jeepney Terminal",
            "KEPCO",
            "Inoburan Stop",
            "Tinaan Crossing",
            "Langtad Stop",
            "Cantao-an Junction",
            "Tunghaan Stop (Boundary Area)",
            "Calajo-an Stop",
            "Tulic Stop",
            "Minglanilla Public Market / Town Proper"
    };

    // ── Fare range ─────────────────────────────────────────
    public static final int FARE_MIN = 10;
    public static final int FARE_MAX = 50;

    /** Returns a random stop index strictly ahead of currentStopIndex. */
    public static int randomForwardStopIndex(int currentStopIndex, java.util.Random rng) {
        int firstValid = currentStopIndex + 1;
        int lastValid  = STOPS.length - 1;
        if (firstValid > lastValid) return lastValid; // already at end
        return firstValid + rng.nextInt(lastValid - firstValid + 1);
    }

    /** Returns a random fare between FARE_MIN and FARE_MAX inclusive. */
    public static int randomFare(java.util.Random rng) {
        return FARE_MIN + rng.nextInt(FARE_MAX - FARE_MIN + 1);
    }
}