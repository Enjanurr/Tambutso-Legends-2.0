package gameStates;

import BossFight.LevelOne.Blue.BlueJeepVsBoss1State;
import BossFight.LevelOne.Red.RedJeepVsBoss1State;
import BossFight.LevelOne.Green.GreenJeepVsBoss1State;
import BossFight.LevelTwo.Blue.BlueJeepVsBoss2State;

import BossFight.LevelTwo.Green.GreenJeepVsBoss2State;
import BossFight.LevelTwo.Red.RedJeepVsBoss2State;
import entities.DriverProfile;
import entities.Player;
import main.Game;
import Ui.HealthBar;

/**
 * Routes the selected driver + level combination to the correct boss fight state.
 *
 * NO ECS — just a simple factory that maps driver IDs to boss fight classes.
 *
 * To add a new driver:
 *   1. Add case in getLevel1State()
 *   2. Create corresponding XxxJeepVsBoss1State class
 *
 * To add a new level (Boss2, Boss3):
 *   1. Add getLevel2State(), getLevel3State() methods
 *   2. Create corresponding XxxVsBoss2State classes
 */
public class BossFightMatchmaker {

    private final Game game;
    private final Player player;
    private final HealthBar healthBar;

    public BossFightMatchmaker(Game game, Player player, HealthBar healthBar) {
        this.game = game;
        this.player = player;
        this.healthBar = healthBar;
    }

    /**
     * Returns the appropriate boss fight state for the given driver and level.
     *
     * @param driver The selected driver profile
     * @param levelIndex Level number (1 = Boss1, 2 = Boss2, etc.)
     * @return The boss fight state, or null if invalid combination
     */
    public State getBossFightState(DriverProfile driver, int levelIndex) {
        if (driver == null) {
            System.err.println("❌ [Matchmaker] No driver selected!");
            return null;
        }

        switch (levelIndex) {
            case 1:
                return getLevel1State(driver);
            case 2:
                return getLevel2State(driver);
            case 3:
                return getLevel3State(driver);
            default:
                System.err.println("❌ [Matchmaker] Invalid level: " + levelIndex);
                return null;
        }
    }

    // ─────────────────────────────────────────────────────────
    // LEVEL 1 (Boss1) ROUTING
    // ─────────────────────────────────────────────────────────
    private State getLevel1State(DriverProfile driver) {
        System.out.println("🎮 [Matchmaker] Level 1 - Driver: " + driver.displayName);

        switch (driver.id) {
            case "driver_1": // Manong Ricky (Red Jeep)
                return createRedJeepVsBoss1();

            case "driver_2": // Ate Gloria (Green Jeep)
                return createGreenJeepVsBoss1();

            case "driver_3": // Kuya Ben (Blue Jeep)
                return createBlueJeepVsBoss1();

            default:
                System.err.println("❌ [Matchmaker] Unknown driver ID: " + driver.id);
                return null;
        }
    }

    // ─────────────────────────────────────────────────────────
    // LEVEL 2 (Boss2) ROUTING
    // ─────────────────────────────────────────────────────────
    private State getLevel2State(DriverProfile driver) {
        System.out.println("🎮 [Matchmaker] Level 2 - Driver: " + driver.displayName);

        switch (driver.id) {
            case "driver_1": // Manong Ricky (Red Jeep)
                return createRedJeepVsBoss2();

            case "driver_2": // Ate Gloria (Green Jeep)
                return createGreenJeepVsBoss2();

            case "driver_3": // Kuya Ben (Blue Jeep)
                return createBlueJeepVsBoss2();

            default:
                System.err.println("❌ [Matchmaker] Unknown driver ID: " + driver.id);
                return null;
        }
    }

    // ─────────────────────────────────────────────────────────
    // LEVEL 3 (Boss3) ROUTING - TODO: Implement when Boss3 exists
    // ─────────────────────────────────────────────────────────
    private State getLevel3State(DriverProfile driver) {
        System.err.println("⚠️ [Matchmaker] Level 3 not implemented yet!");
        // TODO: Add Boss3 state routing here
        return null;
    }

    // ─────────────────────────────────────────────────────────
    // LEVEL 1 STATE FACTORY METHODS
    // ─────────────────────────────────────────────────────────

    private BlueJeepVsBoss1State createBlueJeepVsBoss1() {
        BlueJeepVsBoss1State state = new BlueJeepVsBoss1State(game, player, healthBar);
        state.applyDriverAssets(game.getSelectedDriver());
        System.out.println("✓ Created: BlueJeepVsBoss1State");
        return state;
    }

    private RedJeepVsBoss1State createRedJeepVsBoss1() {
        RedJeepVsBoss1State state = new RedJeepVsBoss1State(game, player, healthBar);
        state.applyDriverAssets(game.getSelectedDriver());
        System.out.println("✓ Created: RedJeepVsBoss1State");
        return state;
    }

    private GreenJeepVsBoss1State createGreenJeepVsBoss1() {
        GreenJeepVsBoss1State state = new GreenJeepVsBoss1State(game, player, healthBar);
        state.applyDriverAssets(game.getSelectedDriver());
        System.out.println("✓ Created: GreenJeepVsBoss1State");
        return state;
    }

    // ─────────────────────────────────────────────────────────
    // LEVEL 2 STATE FACTORY METHODS
    // ─────────────────────────────────────────────────────────

    private BlueJeepVsBoss2State createBlueJeepVsBoss2() {
        BlueJeepVsBoss2State state = new BlueJeepVsBoss2State(game, player, healthBar);
        state.applyDriverAssets(game.getSelectedDriver());
        System.out.println("✓ Created: BlueJeepVsBoss2State");
        return state;
    }

    private RedJeepVsBoss2State createRedJeepVsBoss2() {
        RedJeepVsBoss2State state = new RedJeepVsBoss2State(game, player, healthBar);
        state.applyDriverAssets(game.getSelectedDriver());
        System.out.println("✓ Created: RedJeepVsBoss2State");
        return state;
    }

    private GreenJeepVsBoss2State createGreenJeepVsBoss2() {
        GreenJeepVsBoss2State state = new GreenJeepVsBoss2State(game, player, healthBar);
        state.applyDriverAssets(game.getSelectedDriver());
        System.out.println("✓ Created: GreenJeepVsBoss2State");
        return state;
    }

    // ─────────────────────────────────────────────────────────
    // UTILITY METHODS
    // ─────────────────────────────────────────────────────────

    /**
     * Quick access for Level 1 boss fight (most common case).
     */
    public State getLevel1BossFight(DriverProfile driver) {
        return getBossFightState(driver, 1);
    }

    /**
     * Validates if a driver + level combination is implemented.
     */
    public boolean isImplemented(DriverProfile driver, int levelIndex) {
        if (driver == null) return false;

        // All 3 drivers implemented for Levels 1 and 2
        if (levelIndex == 1 || levelIndex == 2) {
            return driver.id.equals("driver_1")
                    || driver.id.equals("driver_2")
                    || driver.id.equals("driver_3");
        }

        return false; // Level 3 not implemented yet
    }
}