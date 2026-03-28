package entities;

import gameStates.Playing;
import main.Game;
import objects.Powerup;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Random;

public class PowerupManager {
    private final Playing playing;
    private final Random rng = new Random();

    private Powerup activePowerup = null;
    private int spawnCooldown = 0;

    // Spawn settings
    private static final float SPAWN_CHANCE = 0.20f; // 20% chance
    private static final int MAX_WORLD_LOOPS = 15; // Same as in Playing class

    // Lane Y positions
    private static final float[] LANE_Y_POSITIONS = {
            Powerup.LANE_1_Y * Game.SCALE,
            Powerup.LANE_2_Y * Game.SCALE,
            Powerup.LANE_3_Y * Game.SCALE
    };

    // Speed boost settings
    private static final float SPEED_BOOST_MULTIPLIER = 1.5f; // +50%
    private static final int SPEED_BOOST_DURATION = 10 * 60; // 5 seconds at 60 FPS

    private int speedBoostTimer = 0;
    private boolean speedBoostActive = false;
    private float originalMaxSpeed;
    private Player player;

    public PowerupManager(Playing playing) {
        this.playing = playing;
        this.player = playing.getPlayer();
        this.originalMaxSpeed = getPlayerMaxSpeed();
    }

    private float getPlayerMaxSpeed() {
        // Access Player's MAX_SPEED constant
        return 1.5f; //
    }

    public void update() {
        // Update speed boost timer
        if (speedBoostActive) {
            speedBoostTimer--;
            if (speedBoostTimer <= 0) {
                deactivateSpeedBoost();
            }
        }

        // Only spawn powerups when world is scrolling
        if (playing.isScrolling()) {
            // Update spawn cooldown
            if (spawnCooldown > 0) {
                spawnCooldown--;
            }

            // Try to spawn new powerup if none active and cooldown is 0
            if (activePowerup == null && spawnCooldown == 0) {
                trySpawnPowerup();
            }
        }

        // Update active powerup
        if (activePowerup != null) {
            boolean scrolling = playing.isScrolling();
            float scrollSpeed = playing.getScrollSpeed();
            activePowerup.update(scrolling, scrollSpeed);

            // Check collision with player
            if (activePowerup.isActive() && checkCollisionWithPlayer()) {
                collectPowerup(activePowerup);
                activePowerup = null;
            }

            // Remove if inactive
            if (activePowerup != null && !activePowerup.isActive()) {
                activePowerup = null;
            }
        }
    }

    private void trySpawnPowerup() {
        // Check spawn chance
        if (rng.nextFloat() >= SPAWN_CHANCE) {
            spawnCooldown = getSpawnCooldown();
            return;
        }

        // Random lane selection
        int laneIndex = rng.nextInt(LANE_Y_POSITIONS.length);
        float spawnY = LANE_Y_POSITIONS[laneIndex];

        // Spawn at right border
        float spawnX = Game.GAME_WIDTH;

        // Random powerup type (1-3)
        int typeValue = rng.nextInt(3) + 1;
        Powerup.PowerupType type = Powerup.PowerupType.fromValue(typeValue);

        activePowerup = new Powerup(spawnX, spawnY, type);
        spawnCooldown = getSpawnCooldown();
    }

    private int getSpawnCooldown() {
        // Cooldown based on world loops - prevents immediate respawn
        return MAX_WORLD_LOOPS * 2;
    }

    private boolean checkCollisionWithPlayer() {
        if (activePowerup == null || !activePowerup.isActive()) return false;

        Rectangle powerupHitbox = activePowerup.getHitbox();
        Rectangle2D.Float playerHitbox = player.getHitBox();

        // Convert Rectangle2D.Float to Rectangle for intersection check
        Rectangle playerRect = new Rectangle(
                (int)playerHitbox.x,
                (int)playerHitbox.y,
                (int)playerHitbox.width,
                (int)playerHitbox.height
        );

        return powerupHitbox.intersects(playerRect);
    }

    private void collectPowerup(Powerup powerup) {
        switch (powerup.getType()) {
            case SPEED_BOOST:
                activateSpeedBoost();
                System.out.println("SPEED BOOST");
                break;
            case HEAL:
                // Reserved for future implementation
                System.out.println("Heal powerup collected (reserved)");
                break;
            case DAMAGE_AMPLIFIER:
                // Reserved for future implementation
                System.out.println("Damage Amplifier powerup collected (reserved)");
                break;
        }
        powerup.setActive(false);
    }

    private void activateSpeedBoost() {
        // If boost already active, reset timer
        if (speedBoostActive) {
            speedBoostTimer = SPEED_BOOST_DURATION;
        } else {
            // Activate new boost
            speedBoostActive = true;
            speedBoostTimer = SPEED_BOOST_DURATION;
            // Apply speed boost
            player.setMaxSpeed(originalMaxSpeed * SPEED_BOOST_MULTIPLIER);
        }
    }

    private void deactivateSpeedBoost() {
        speedBoostActive = false;
        // Return to normal speed
        player.setMaxSpeed(originalMaxSpeed);
    }

    public void render(Graphics g) {
        if (activePowerup != null && activePowerup.isActive()) {
            activePowerup.render(g);
        }
    }

    public void resetAll() {
        activePowerup = null;
        spawnCooldown = 0;
        if (speedBoostActive) {
            deactivateSpeedBoost();
        }
        speedBoostTimer = 0;
    }

}