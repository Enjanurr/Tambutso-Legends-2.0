package entities;

import utils.LoadSave;
import main.Game;
import java.awt.*;
import java.awt.image.BufferedImage;

import static utils.Constants.PersonConstants.*;

public class Person extends Entity {

    public enum PersonType { WALKER, PASSENGER }

    private final PersonType type;
    private BufferedImage[][] frames;

    private int aniTick  = 0;
    private int aniIndex = 0;
    private boolean interactable = false;


    private boolean active = true;


    private boolean movingLeft = true;
    // ── Passenger trip data ───────────────────────────────────
    private String destinationStop = "";
    private int    fare            = 0;

    public Person(float x, float y, PersonType type, String atlasPath) {
        super(x, y, PERSON_WIDTH, PERSON_HEIGHT);
        this.type = type;

        // Initialise hitbox — adjust inset values to taste
        initHitbox(x , y,
                PERSON_WIDTH  * Game.SCALE - 80 * Game.SCALE,
                PERSON_HEIGHT * Game.SCALE - 40 * Game.SCALE);

        loadFrames(atlasPath);
    }

    private void loadFrames(String atlasPath) {
        BufferedImage sheet = LoadSave.getSpriteAtlas(atlasPath);
        frames = new BufferedImage[2][PERSON_FRAME_COUNT];
        for (int row = 0; row < 2; row++)
            for (int col = 0; col < PERSON_FRAME_COUNT; col++)
                frames[row][col] = sheet.getSubimage(
                        col * PERSON_WIDTH_DEFAULT,
                        row * PERSON_HEIGHT_DEFAULT,
                        PERSON_WIDTH_DEFAULT,
                        PERSON_HEIGHT_DEFAULT);
    }
    public java.awt.geom.Rectangle2D.Float getHitBox() { return hitBox; }
    /** Call from render() to see the hitbox — remove when done debugging. */
    private void drawDebugHitbox(Graphics g) {
        if (hitBox == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(255, 105, 180, 180));   // hot-pink, semi-transparent
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect((int) hitBox.x, (int) hitBox.y,
                (int) hitBox.width, (int) hitBox.height);
    }

    public void update(boolean worldScrolling, float scrollSpeed) {



        if (type == PersonType.WALKER) {
            // ── Direction: LEFT when jeep moving, RIGHT when stopped ──
            movingLeft = worldScrolling;

            if (movingLeft) {
                // Jeep moving — walker moves left at own speed
                x -= WALK_SPEED;
                // World scroll pushes walker further left (jeep overtaking)
                x -= scrollSpeed;
            } else {
                // Jeep stopped — walker moves right at own speed
                x += WALK_SPEED;
            }

            if (movingLeft  && x + width < 0)          active = false; // exited left
            if (!movingLeft && x         > Game.GAME_WIDTH) active = false; // exited right

        } else {
            // ── PASSENGER: only moves when world scrolls ──────
            if (worldScrolling)
                x -= scrollSpeed;

            if (x + width < 0) active = false;

        }

        // hitbox
        if (hitBox != null) {
            hitBox.x = x;
            hitBox.y = y;
        }

        // ── Animation ────────────────────────────────────────
        int aniSpeed = (type == PersonType.PASSENGER) ? PASSENGER_ANI_SPEED : WALKER_ANI_SPEED;
        aniTick++;
        if (aniTick >= aniSpeed) {
            aniTick  = 0;
            aniIndex = (aniIndex + 1) % PERSON_FRAME_COUNT;
        }

    }

    public void render(Graphics g) {
        if (!active) return;
        int row = (type == PersonType.PASSENGER) ? ROW_IDLE : ROW_WALK;
        g.drawImage(frames[row][aniIndex], (int) x, (int) y, width, height, null);
        drawDebugHitbox(g);
    }

    public boolean    isActive()     { return active; }
    public void    setActive(boolean v)    { active = v; }   // ← add this line
    public PersonType getType()      { return type; }
    public boolean isInteractable()           { return interactable; }
    public void    setInteractable(boolean v) { interactable = v; }

    public String getDestinationStop()             { return destinationStop; }
    public void   setDestinationStop(String stop)  { destinationStop = stop; }
    public int    getFare()                        { return fare; }
    public void   setFare(int fare)                { this.fare = fare; }
    public float getY() { return hitBox != null ? hitBox.y : y; }

}