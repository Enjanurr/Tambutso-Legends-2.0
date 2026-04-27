package entities;

import java.util.List;
import java.util.Collections;

/**
 * Immutable data record for a selectable jeepney driver.
 *
 * To add a new driver: append one entry to ALL[].
 * To add a new stat/ability type: add a field + constructor param here —
 * no changes needed in CharSelectState or Player.
 */
public class DriverProfile {

    public final String       id;
    public final String       displayName;
    public final String       atlasPath;
    public final float        maxSpeed;
    public final String       description;
    public final List<String> abilities;   // e.g. ["Fast Brake","Honk Boost"]
    public  float width ;
    public float height;


    // Sprite sheet layout — shared by all drivers
    public static final int FRAME_W      = 110;
    public static final int FRAME_H      = 40;
    public static final int ROW_IDLE     = 0;   // row 1 = IDLE   (4 frames)
    public static final int ROW_RUNNING  = 0;   // row 0 = RUNNING(4 frames)
    public static final int FRAMES_IDLE  = 4;

    public DriverProfile(String id, String displayName,
                         String atlasPath, float maxSpeed,
                         String description, List<String> abilities, float width,float height) {
        this.id          = id;
        this.displayName = displayName;
        this.atlasPath   = atlasPath;
        this.maxSpeed    = maxSpeed;
        this.description = description;
        this.width = width;
        this.height = height;

        this.abilities   = Collections.unmodifiableList(abilities);
    }

    // ── Built-in roster ───────────────────────────────────────
    public static final DriverProfile[] ALL = {
            new DriverProfile(
                    "driver_1", "Kuya Ben",
                    utils.LoadSave.PLAYER_ATLAS_1,
                    1.5f, "Steady. Balanced speed.",
                    List.of("Smooth Brake", "Steady Wheel"),
                    110,40
            ),
            new DriverProfile(
                    "driver_2", "Manong Ricky",
                    utils.LoadSave.PLAYER_ATLAS_2,
                    1.8f, "Fast but risky.",
                    List.of("Turbo Boost", "Near Miss"),
                    110,40
            ),
            new DriverProfile(
                    "driver_3", "Ate Gloria",
                    utils.LoadSave.PLAYER_ATLAS_3,
                    1.2f, "Careful. Easy handling.",
                    List.of("Safe Stop", "Fare Sense"),
                    110,40
            ),
    };
}