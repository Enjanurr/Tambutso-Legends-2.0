package entities;

import gameStates.Playing;
import main.Game;
import objects.Powerup;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Random;

public class PowerupManager {

    private final Playing playing;
    private final Random  rng = new Random();

    private Powerup activePowerup = null;
    private int     spawnCooldown = 0;

    // ── Spawn settings ────────────────────────────────────────
    private static final float SPAWN_CHANCE    = 0.20f;
    private static final int   MAX_WORLD_LOOPS = 15;

    // ── Lane Y positions ──────────────────────────────────────
    private static final float[] LANE_Y_POSITIONS = {
            Powerup.LANE_1_Y * Game.SCALE,
            Powerup.LANE_2_Y * Game.SCALE,
            Powerup.LANE_3_Y * Game.SCALE
    };

    // ── Speed boost settings ──────────────────────────────────
    // -------------------------------------------------------
    // SPEED BOOST SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float SPEED_BOOST_MULTIPLIER = 1.5f; // +50%
    private static final int   SPEED_BOOST_DURATION   = 10 * 200; // 10 s at 200 UPS
    // -------------------------------------------------------

    private int     speedBoostTimer  = 0;
    private boolean speedBoostActive = false;
    private float   originalMaxSpeed;
    private Player  player;

    public PowerupManager(Playing playing) {
        this.playing       = playing;
        this.player        = playing.getPlayer();
        this.originalMaxSpeed = 1.5f; // matches X_MAX_SPEED in Player
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update() {
        // Speed boost countdown
        if (speedBoostActive) {
            speedBoostTimer--;
            if (speedBoostTimer <= 0)
                deactivateSpeedBoost();
        }

        // Only spawn while scrolling
        if (playing.isScrolling()) {
            if (spawnCooldown > 0) spawnCooldown--;
            if (activePowerup == null && spawnCooldown == 0)
                trySpawnPowerup();
        }

        // Update active powerup
        if (activePowerup != null) {
            activePowerup.update(playing.isScrolling(), playing.getScrollSpeed());

            if (activePowerup.isActive() && checkCollisionWithPlayer()) {
                collectPowerup(activePowerup);
                activePowerup = null;
            } else if (activePowerup != null && !activePowerup.isActive()) {
                activePowerup = null;
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // SPAWN
    // ─────────────────────────────────────────────────────────
    private void trySpawnPowerup() {
        if (rng.nextFloat() >= SPAWN_CHANCE) {
            spawnCooldown = getSpawnCooldown();
            return;
        }

        int   laneIndex = rng.nextInt(LANE_Y_POSITIONS.length);
        float spawnY    = LANE_Y_POSITIONS[laneIndex];
        float spawnX    = Game.GAME_WIDTH;
        int   typeValue = rng.nextInt(3) + 1;

        activePowerup = new Powerup(spawnX, spawnY, Powerup.PowerupType.fromValue(typeValue));
        spawnCooldown = getSpawnCooldown();
    }

    private int getSpawnCooldown() {
        return MAX_WORLD_LOOPS * 2;
    }

    // ─────────────────────────────────────────────────────────
    // COLLECT
    // ─────────────────────────────────────────────────────────
    private void collectPowerup(Powerup powerup) {
        switch (powerup.getType()) {
            case SPEED_BOOST:
                activateSpeedBoost();
                break;
            case HEAL:
                playing.onPlayerHeal();     // ← NOW ACTIVE: restores 1 health bar
                break;
            case DAMAGE_AMPLIFIER:
                // Reserved for future implementation
                break;
        }
        powerup.setActive(false);
    }

    // ─────────────────────────────────────────────────────────
    // SPEED BOOST
    // ─────────────────────────────────────────────────────────
    private void activateSpeedBoost() {
        if (speedBoostActive) {
            speedBoostTimer = SPEED_BOOST_DURATION; // refresh timer
        } else {
            speedBoostActive = true;
            speedBoostTimer  = SPEED_BOOST_DURATION;
            player.setMaxSpeed(originalMaxSpeed * SPEED_BOOST_MULTIPLIER);
        }
    }

    private void deactivateSpeedBoost() {
        speedBoostActive = false;
        player.setMaxSpeed(originalMaxSpeed);
    }

    // ─────────────────────────────────────────────────────────
    // COLLISION
    // ─────────────────────────────────────────────────────────
    private boolean checkCollisionWithPlayer() {
        if (activePowerup == null || !activePowerup.isActive()) return false;
        Rectangle        pBox = activePowerup.getHitbox();
        Rectangle2D.Float qBox = player.getHitBox();
        return pBox.intersects(new Rectangle(
                (int)qBox.x, (int)qBox.y, (int)qBox.width, (int)qBox.height));
    }

    // ─────────────────────────────────────────────────────────
    // RENDER / RESET
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        if (activePowerup != null && activePowerup.isActive())
            activePowerup.render(g);
    }

    public void resetAll() {
        activePowerup = null;
        spawnCooldown = 0;
        if (speedBoostActive) deactivateSpeedBoost();
        speedBoostTimer = 0;
    }


}