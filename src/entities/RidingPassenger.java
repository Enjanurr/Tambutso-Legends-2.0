package entities;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A passenger currently seated in the jeepney.
 *
 * Drop condition:
 *   A passenger can ONLY be dropped when worldLoopCount >= assignedStop.
 *   If worldLoopCount > assignedStop the fare is penalised -MISSED_STOP_PENALTY
 *   per extra loop (minimum ₱0).
 *
 * assignedFare is set once at construction time (computed and passed in by
 * AcceptPassengerOverlay / PassengerManager) so both overlays always show
 * the same canonical value.
 */
public class RidingPassenger {

    // ── Sprite sheet layout ───────────────────────────────────
    private static final int SHEET_COLS = 3;
    private static final int SHEET_ROWS = 6;
    private static final int CELL_W     = 45;
    private static final int CELL_H     = 45;

    public static final int RENDER_W = (int)(CELL_W * Game.SCALE);
    public static final int RENDER_H = (int)(CELL_H * Game.SCALE);

    private static final int GET_OUT_ANI_SPEED = 12;

    // ── Penalty per missed stop ── ← ADJUST ──────────────────
    public static final int MISSED_STOP_PENALTY = 30;

    // ── Person-ID → sprite-row mapping ───────────────────────
    public static int getRowForPersonId(int personId) {
        switch (personId) {
            case 2: return 0;
            case 1: return 1;
            case 3: return 2;
            case 4: return 3;
            case 5: return 4;
            case 6: return 5;
            default: return 0;
        }
    }

    // ── Instance fields ───────────────────────────────────────
    private final int    personId;
    private final int    spriteRow;
    private final int    assignedStop;
    private final int    acceptedAtLoop;
    /**
     * Fare pre-calculated at acceptance time.
     * Single source of truth shown in both AcceptPassengerOverlay and
     * PassengerListOverlay.
     */
    private final int    assignedFare;
    private final float  laneY;
    private final String atlasPath;
    private final int    personTypeId;

    /**
     * Surplus amount passenger paid (random 1-30 pesos above fare).
     * Cached to remain consistent if PaymentOverlay closed/reopened.
     */
    private Integer cachedSurplus = null;

    private boolean gettingOut = false;
    private boolean selected   = false;

    private int aniTick  = 0;
    private int aniIndex = 1;

    private static BufferedImage[][] ridingFrames;
    private static boolean           framesLoaded = false;

    // ─────────────────────────────────────────────────────────
    // CONSTRUCTORS
    // ─────────────────────────────────────────────────────────

    /** Primary constructor — always supply the pre-calculated assignedFare. */
    public RidingPassenger(int personId, int assignedStop, int acceptedAtLoop,
                           float laneY, String atlasPath, int personTypeId,
                           int assignedFare) {
        this.personId       = personId;
        this.spriteRow      = getRowForPersonId(personId);
        this.assignedStop   = assignedStop;
        this.acceptedAtLoop = acceptedAtLoop;
        this.assignedFare   = assignedFare;
        this.laneY          = laneY;
        this.atlasPath      = atlasPath;
        this.personTypeId   = personTypeId;
        ensureFramesLoaded();
    }

    /** Legacy constructor — assignedFare defaults to 0. */
    public RidingPassenger(int personId, int assignedStop, int acceptedAtLoop,
                           float laneY, String atlasPath, int personTypeId) {
        this(personId, assignedStop, acceptedAtLoop, laneY, atlasPath, personTypeId, 0);
    }

    private static synchronized void ensureFramesLoaded() {
        if (framesLoaded) return;
        framesLoaded = true;
        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.RIDING_PASSENGER);
        if (sheet == null) {
            System.err.println("[RidingPassenger] Cannot load " + LoadSave.RIDING_PASSENGER);
            ridingFrames = new BufferedImage[SHEET_ROWS][SHEET_COLS];
            return;
        }
        ridingFrames = new BufferedImage[SHEET_ROWS][SHEET_COLS];
        for (int r = 0; r < SHEET_ROWS; r++)
            for (int c = 0; c < SHEET_COLS; c++)
                ridingFrames[r][c] = sheet.getSubimage(
                        c * CELL_W, r * CELL_H, CELL_W, CELL_H);
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update(int currentLoop) {
        if (!gettingOut && currentLoop >= assignedStop) {
            gettingOut = true;
            aniIndex   = 1;
            aniTick    = 0;
        }
        if (gettingOut) {
            aniTick++;
            if (aniTick >= GET_OUT_ANI_SPEED) {
                aniTick  = 0;
                aniIndex = (aniIndex == 1) ? 2 : 1;
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void renderInSlot(Graphics g, int slotX, int slotY) {
        if (ridingFrames == null) return;
        int col = gettingOut ? aniIndex : 0;
        BufferedImage frame = ridingFrames[spriteRow][col];
        if (frame == null) return;
        g.drawImage(frame, slotX, slotY, RENDER_W, RENDER_H, null);
        if (selected) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(255, 220, 50, 200));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(slotX, slotY, RENDER_W, RENDER_H);
        }
    }

    // ─────────────────────────────────────────────────────────
    // FARE / DROP CONDITION
    // ─────────────────────────────────────────────────────────

    /**
     * Returns true when the passenger may be dropped.
     * Condition: currentLoop >= assignedStop.
     */
    public boolean isReadyToDrop(int currentLoop) {
        return currentLoop >= assignedStop;
    }

    /**
     * Fare owed at drop time.
     *   On time  (currentLoop == assignedStop) → assignedFare
     *   Late     (currentLoop >  assignedStop) → assignedFare - missedStops × MISSED_STOP_PENALTY
     *   Minimum  → ₱0
     */
    public int calculateFare(int currentLoop) {
        int missedStops = Math.max(0, currentLoop - assignedStop);
        return Math.max(0, assignedFare - missedStops * MISSED_STOP_PENALTY);
    }

    // ─────────────────────────────────────────────────────────
    // GETTERS / SETTERS
    // ─────────────────────────────────────────────────────────
    public int     getPersonId()      { return personId; }
    public int     getAssignedStop()  { return assignedStop; }
    /** The pre-calculated fare stored at acceptance time. */
    public int     getAssignedFare()  { return assignedFare; }
    public boolean isGettingOut()     { return gettingOut; }

    /** @deprecated Use isReadyToDrop(int currentLoop) instead. */
    @Deprecated
    public boolean isReadyToDrop()    { return false; }

    public boolean isSelected()              { return selected; }
    public void    setSelected(boolean v)    { selected = v; }

    public float  getLaneY()         { return laneY; }
    public String getAtlasPath()     { return atlasPath; }
    public int    getPersonTypeId()  { return personTypeId; }
    public int    getAcceptedAtLoop(){ return acceptedAtLoop; }

    // ─────────────────────────────────────────────────────────
    // CACHED SURPLUS (for PaymentOverlay consistency)
    // ─────────────────────────────────────────────────────────
    public Integer getCachedSurplus() { return cachedSurplus; }
    public void    setCachedSurplus(int surplus) { this.cachedSurplus = surplus; }
}