package entities;

import utils.LoadSave;
import main.Game;
import java.awt.*;
import java.awt.image.BufferedImage;

import static utils.Constants.PersonConstants.*;

public class Person extends Entity {

    public enum PersonType { WALKER, PASSENGER }

    private final PersonType type;
    private BufferedImage[][] frames;   // [row][col], 3 rows now

    private final int frameCount;
    private final int cellWidthDefault;

    // ── Person identity (for PassengerManager handoff) ────────
    private final int    personId;      // 1-6 matching filename
    private final String atlasPath;     // sprite sheet path
    private final float  spawnLaneY;    // Y position of the lane this person spawned in

    private int aniTick  = 0;
    private int aniIndex = 0;
    private boolean interactable = false;
    private boolean active       = true;
    private boolean movingLeft   = true;

    // Add these fields to Person class
    private Integer cachedStop = null;
    private Integer cachedFare = null;

    public Integer getCachedStop() { return cachedStop; }
    public Integer getCachedFare() { return cachedFare; }
    public void setCachedStop(int stop) { this.cachedStop = stop; }
    public void setCachedFare(int fare) { this.cachedFare = fare; }
    public void clearCache() { cachedStop = null; cachedFare = null; }
    // ── Passenger trip data (pre-acceptance) ──────────────────
    // After acceptance, the riding passenger carries the real stop/fare.
    // These fields are kept for the AcceptPassengerOverlay preview.
    private String destinationStop = "";
    private int    fare            = 0;

    // ─────────────────────────────────────────────────────────
    public Person(float x, float y, PersonType type, String atlasPath, int personTypeId) {
        super(x, y, PERSON_WIDTH, PERSON_HEIGHT);
        this.type          = type;
        this.personId      = personTypeId;
        this.atlasPath     = atlasPath;
        this.spawnLaneY    = y;   // record spawn Y for drop animation lane

        this.frameCount       = getFrameCountForPerson(personTypeId);
        this.cellWidthDefault = getWidthDefaultForPerson(personTypeId);

        initHitbox(x, y,
                PERSON_WIDTH  * Game.SCALE - 80 * Game.SCALE,
                PERSON_HEIGHT * Game.SCALE - 40 * Game.SCALE);

        loadFrames(atlasPath);
    }

    /** Backward-compatible constructor (defaults to personId=1). */
    public Person(float x, float y, PersonType type, String atlasPath) {
        this(x, y, type, atlasPath, 1);
    }

    private void loadFrames(String path) {
        BufferedImage sheet = LoadSave.getSpriteAtlas(path);
        if (sheet == null) {
            System.err.println("[Person] Failed to load: " + path);
            frames = new BufferedImage[3][frameCount];
            return;
        }

        // Sprite sheets now have 3 rows: 0=idle, 1=walk, 2=drop
        int rowCount = Math.min(3, sheet.getHeight() / PERSON_HEIGHT_DEFAULT);
        frames = new BufferedImage[3][frameCount];
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < frameCount; col++) {
                frames[row][col] = sheet.getSubimage(
                        col * cellWidthDefault,
                        row * PERSON_HEIGHT_DEFAULT,
                        cellWidthDefault,
                        PERSON_HEIGHT_DEFAULT);
            }
        }
    }

    public java.awt.geom.Rectangle2D.Float getHitBox() { return hitBox; }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update(boolean worldScrolling, float scrollSpeed) {
        if (type == PersonType.WALKER) {
            movingLeft = worldScrolling;
            if (movingLeft) {
                x -= WALK_SPEED;
                x -= scrollSpeed;
            } else {
                x += WALK_SPEED;
            }
            if ( movingLeft && x + width < 0)         active = false;
            if (!movingLeft && x > Game.GAME_WIDTH)   active = false;
        } else {
            // PASSENGER: moves with world scroll only
            if (worldScrolling) x -= scrollSpeed;
            if (x + width < 0)  active = false;
        }

        if (hitBox != null) { hitBox.x = x; hitBox.y = y; }

        int aniSpeed = (type == PersonType.PASSENGER) ? PASSENGER_ANI_SPEED : WALKER_ANI_SPEED;
        aniTick++;
        if (aniTick >= aniSpeed) {
            aniTick  = 0;
            aniIndex = (aniIndex + 1) % frameCount;
        }
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (!active) return;
        int row = (type == PersonType.PASSENGER) ? ROW_IDLE : ROW_WALK;
        if (frames[row] == null || frames[row][aniIndex] == null) return;
        g.drawImage(frames[row][aniIndex], (int) x, (int) y, width, height, null);
        // drawDebugHitbox(g);   // uncomment to debug
    }

    private void drawDebugHitbox(Graphics g) {
        if (hitBox == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(255, 105, 180, 180));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect((int) hitBox.x, (int) hitBox.y,
                (int) hitBox.width, (int) hitBox.height);
    }

    // ─────────────────────────────────────────────────────────
    // GETTERS / SETTERS
    // ─────────────────────────────────────────────────────────
    public boolean    isActive()                   { return active; }
    public void       setActive(boolean v)         { active = v; }
    public PersonType getType()                    { return type; }
    public boolean    isInteractable()             { return interactable; }
    public void       setInteractable(boolean v)   { interactable = v; }

    public String getDestinationStop()             { return destinationStop; }
    public void   setDestinationStop(String stop)  { destinationStop = stop; }
    public int    getFare()                        { return fare; }
    public void   setFare(int fare)                { this.fare = fare; }

    public int    getPersonId()    { return personId; }
    public String getAtlasPath()   { return atlasPath; }
    public float  getSpawnLaneY()  { return spawnLaneY; }

    public float  getY() { return hitBox != null ? hitBox.y : y; }
}