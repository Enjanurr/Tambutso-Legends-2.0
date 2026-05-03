package BossFight.LevelThree.Green;

import BossFight.BossObstacleManager;
import BossFight.BossWalkerManager;
import BossFight.CloudRenderer;
import BossFight.LevelThree.GravySauce;
import Ui.*;
import entities.EnemyCar;
import entities.Player;
import gameStates.GameStates;
import gameStates.State;
import gameStates.StateMethods;
import main.Game;
import utils.LoadSave;


import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


import static utils.Constants.UI.URMButtons.*;

public class GreenJeepVsBoss3State extends State implements StateMethods {

    // -------------------------------------------------------
    // BOSS FIGHT SETTINGS
    // -------------------------------------------------------
    private BossBanner bossBanner;
    private static final float SCROLL_SPEED = BossFight.LevelThree.Green.Boss3.BOSS_SCROLL_SPEED;
    private static final float LEFT_BORDER_PUSH = 0.3f;
    private static final float PLAYER_RIGHT_LIMIT_FRACTION = 0.50f;

    // Shoot settings
    private static final int MAX_BULLETS_PER_USE = 5;
    private static final int SHOOT_FULL_COOLDOWN = 4 * 200;

    // Death overlay fade
    private static final float DEATH_FADE_SPEED = 0.03f;
    private static final float DEATH_FADE_MAX = 0.85f;

    // Heal (Skill 2) settings
    private static final int HEAL_COOLDOWN = 10000; // 10 seconds in milliseconds
    private static final int HEAL_ANI_SPEED = 20;   // ticks per frame

    private final Player player;
    private final HealthBar healthBar;
    private BossHealthBar bossBar;
    private BossFight.LevelThree.Green.Boss3 boss;

    private BossWalkerManager walkerManager;

    // Shoot state
    private final List<GreenJeepProjectile> playerBullets = new CopyOnWriteArrayList<>();
    private BufferedImage[] shootFrames;
    private int shootCooldown = 0;
    private int bulletsRemaining = 0;
    private boolean canShoot = true;

    // Heal (Skill 2) tracking
    private long healLastUsed = 0;
    private BufferedImage[] healAnimationFrames;  // 12 frames for heal
    private boolean isHealing = false;
    private int healAnimTick = 0;
    private int healAnimIndex = 0;

    // World scroll
    private float worldOffset = 0;
    private final int levelPixelWidth;

    // Background

    private final float playerRightLimit;

    // Pause
    private boolean paused = false;
    private BossPauseOverlay pauseOverlay;

    // Jeep death overlay
    private boolean playerDead = false;
    private float deathAlpha = 0f;
    private boolean deathFadeDone = false;
    private UrmButton deathRestartBtn;
    private BufferedImage deathScreenImg;
    private int deathImgW, deathImgH, deathImgX, deathImgY;

    // Boss defeat overlay
    private boolean bossDefeated = false;
    private BossDefeatOverlay defeatOverlay;

    // Skill buttons
    private JeepSkillButtons skillButtons;
    private CloudRenderer cloudRenderer;
    private BossObstacleManager obstacleManager;
    // -------------------------------------------------------
    public GreenJeepVsBoss3State(Game game, Player player, HealthBar healthBar) {
        super(game);
        this.player = player;
        player.setBossMode(true);
        this.healthBar = healthBar;
        cloudRenderer = new CloudRenderer();
        obstacleManager = new BossObstacleManager();
        this.levelPixelWidth = LoadSave.GetLevelData()[0].length * Game.TILES_SIZE;
        this.playerRightLimit = Game.GAME_WIDTH * PLAYER_RIGHT_LIMIT_FRACTION - player.getHitBox().width;

        pauseOverlay = new BossPauseOverlay(this);
        buildDeathOverlay();
        buildDefeatOverlay();
        bossBar = new BossHealthBar(BossHealthBar.LifeBarType.BOSS3);
        bossBanner = new BossBanner(3);
        walkerManager = new BossWalkerManager();
        spawnBoss();

        // Initialize skill buttons based on jeep color
        String jeepColor = getJeepColor();
        skillButtons = new JeepSkillButtons(jeepColor,
                this::isSkill1Ready, this::onSkill1, this::getSkill1CooldownRemaining,
                this::isSkill2Ready, this::onSkill2, this::getSkill2CooldownRemaining);

        // Load assets immediately
        loadAssets();
    }

    public void applyDriverAssets(entities.DriverProfile profile) {
        if (profile == null) {
            System.out.println("⚠️ [GreenBossFightState] No driver profile - keeping default assets");
            return;
        }
        System.out.println("🎮 [GreenBossFightState] Applying driver assets: " + profile.displayName);
        // For Green Jeep, we still need to load the skill sheets
        loadAssets();
    }

    private void loadAssets() {


        // Load Skill 1 frames from separate sheet (6 frames)
        java.awt.image.BufferedImage skill1Sheet = LoadSave.getSpriteAtlas(LoadSave.GREEN_JEEP_SKILL1);
        if (skill1Sheet != null) {
            shootFrames = new BufferedImage[6];
            for (int i = 0; i < 6; i++)
                shootFrames[i] = skill1Sheet.getSubimage(
                        i * GreenJeepProjectile.FRAME_W,
                        0,
                        GreenJeepProjectile.FRAME_W,
                        GreenJeepProjectile.FRAME_H);
            System.out.println("✓ [GreenBossFightState] Loaded Skill 1 frames: " + shootFrames.length);
        } else {
            System.err.println("❌ [GreenBossFightState] Failed to load " + LoadSave.GREEN_JEEP_SKILL1);
            shootFrames = new BufferedImage[0];
        }

        // ── Load Skill 2 heal animation (12 frames from separate skill2 sprite sheet) ──
        java.awt.image.BufferedImage skill2Sheet = LoadSave.getSpriteAtlas(LoadSave.GREEN_JEEP_SKILL2);
        if (skill2Sheet != null) {
            healAnimationFrames = new BufferedImage[12];
            for (int i = 0; i < 12; i++) {
                // Make sure dimensions match your sprite sheet
                int frameWidth = skill2Sheet.getWidth() / 12;  // 1320/12 = 110
                int frameHeight = skill2Sheet.getHeight();     // Should be 40
                healAnimationFrames[i] = skill2Sheet.getSubimage(
                        i * frameWidth,
                        0,
                        frameWidth,
                        frameHeight);
            }

        } else {
            System.err.println("❌ [GreenBossFightState] Failed to load " + LoadSave.GREEN_JEEP_SKILL2);
            healAnimationFrames = new BufferedImage[0];
        }

        System.out.println("✓ [GreenBossFightState] All assets loaded successfully");
    }

    private void buildDeathOverlay() {
        deathScreenImg = LoadSave.getSpriteAtlas(LoadSave.DEATH_SCREEN);
        deathImgW = (int) (500 * Game.SCALE * 0.5f);
        deathImgH = (int) (500 * Game.SCALE * 0.5f);
        deathImgX = (Game.GAME_WIDTH - deathImgW) / 2;
        deathImgY = (Game.GAME_HEIGHT - deathImgH) / 2;

        int btnX = (int) (374 * Game.SCALE);
        int btnY = (int) (325 * Game.SCALE);
        deathRestartBtn = new UrmButton(btnX, btnY, URM_SIZE, URM_SIZE, 1);
    }

    private void buildDefeatOverlay() {
        defeatOverlay = new BossDefeatOverlay(
                this::onNextLevel,
                this::fullReset,
                this::onMenuToExit
        );
    }

    private void spawnBoss() {
        float bx = Game.GAME_WIDTH + BossFight.LevelThree.Green.Boss3.FRAME_W * Game.SCALE;
        float by = 480;
        boss = new BossFight.LevelThree.Green.Boss3(bx, by);
    }

    // ─────────────────────────────────────────────────────────
    // SKILL BUTTONS
    // ─────────────────────────────────────────────────────────
    private String getJeepColor() {
        return "green";
    }

    private void onSkill1() {
        attemptShootGreen();
    }

    private void onSkill2() {
        attemptHeal();
    }

    private boolean isSkill1Ready() {
        return bulletsRemaining > 0 || (bulletsRemaining == 0 && shootCooldown == 0);
    }

    private boolean isSkill2Ready() {
        return System.currentTimeMillis() - healLastUsed >= HEAL_COOLDOWN;
    }

    private int getSkill1CooldownRemaining() {
        if (shootCooldown <= 0) return 0;
        return (shootCooldown + 199) / 200;
    }

    private int getSkill2CooldownRemaining() {
        long remaining = HEAL_COOLDOWN - (System.currentTimeMillis() - healLastUsed);
        if (remaining <= 0) return 0;
        return (int) ((remaining + 999) / 1000);
    }

    @Override
    public void update() {
        if (bossDefeated) {
            defeatOverlay.update();
            return;
        }

        if (playerDead) {
            updateDeathOverlay();
            return;
        }

        if (paused) {
            pauseOverlay.update();
            return;
        }

        // Skill buttons update
        skillButtons.update();

        // World scroll
        worldOffset += SCROLL_SPEED * Game.SCALE;
        if (worldOffset >= levelPixelWidth) worldOffset -= levelPixelWidth;
        cloudRenderer.update(SCROLL_SPEED * Game.SCALE);

        // Player clamping
        float leftLimit = 20 * Game.SCALE;
        if (player.getHitBox().x < leftLimit)
            player.getHitBox().x += LEFT_BORDER_PUSH * Game.SCALE;
        if (player.getHitBox().x > playerRightLimit)
            player.getHitBox().x = playerRightLimit;

        player.setWorldScrolling(false);
        player.setWorldLoopDone(true);
        player.update();

        // Cooldowns
        if (shootCooldown > 0) {
            shootCooldown--;
            if (shootCooldown == 0) canShoot = true;
        }

        // Heal animation update
        if (isHealing) {
            healAnimTick++;
            if (healAnimTick >= HEAL_ANI_SPEED) {
                healAnimTick = 0;
                healAnimIndex++;
                if (healAnimIndex >= 12) {
                    isHealing = false;
                    healAnimIndex = 0;
                }
            }
        }

        // Player bullets
        playerBullets.removeIf(pb -> {
            pb.update();
            return !pb.isActive();
        });

        // Walkers
        walkerManager.update(SCROLL_SPEED);
        obstacleManager.update(true, SCROLL_SPEED * Game.SCALE);
        Rectangle jeepHB = new Rectangle(
                (int) player.getHitBox().x,
                (int) player.getHitBox().y,
                (int) player.getHitBox().width,
                (int) player.getHitBox().height
        );

        float jeepCentreY = jeepHB.y + jeepHB.height / 2f;

        boss.update(jeepHB.x, jeepCentreY, jeepHB.width, jeepHB.height);

        // Boss bullets collision
        // In your update() method, add debug for boss bullets
        for (GravySauce.BossProjectile bullet : boss.getBullets()) {
            if (bullet.isActive()) {
                System.out.println("[GreenJeep] Boss bullet active at: " + bullet.getHitbox());
                if (bullet.getHitbox().intersects(jeepHB)) {
                    System.out.println("[GreenJeep] HIT by boss bullet!");
                    bullet.setActive(false);
                    handleJeepHit();
                }
            }
        }

        // Gravy collision
        for (GravySauce gravy : boss.getGravySauces()) {
            if (gravy.isActive() && gravy.getHitbox().intersects(jeepHB)) {
                gravy.setActive(false);
                handleJeepHit();
            }
        }
        obstacleManager.checkCollision(jeepHB, this::handleJeepHit);
        // Player bullets → boss AND obstacles
        Rectangle bossHB = boss.getHitbox();
        for (GreenJeepProjectile pb : playerBullets) {
            if (!pb.isActive()) continue;

            // Check bullet vs boss
            if (pb.getHitbox().intersects(bossHB)) {
                pb.setActive(false);
                boss.triggerHit();
                handleBossHit();
                continue;
            }

            // Check bullet vs obstacles
            boolean hitObstacle = false;
            for (EnemyCar obstacle : obstacleManager.getActiveObstacles()) {
                if (obstacle.isActive() && pb.getHitbox().intersects(obstacle.getHitBox())) {
                    obstacle.takeDamage(1);
                    pb.setActive(false);
                    hitObstacle = true;
                    System.out.println("[GreenJeep] Bullet hit obstacle!");
                    break;
                }
            }
            if (hitObstacle) continue;
        }
    }

    private void handleJeepHit() {
        player.triggerCarStruck();
        boolean dead = healthBar.takeDamage();
        if (dead) {
            playerDead = true;
            resetDeathOverlay();
        }
    }

    private void handleBossHit() {
        boolean defeated = bossBar.takeDamage();
        if (defeated) {
            bossDefeated = true;
            defeatOverlay.reset();
        }
    }

    // ─────────────────────────────────────────────────────────
    // SKILL 1: Shoot
    // ─────────────────────────────────────────────────────────
    private void fireSingleBulletGreen() {
        if (!canShoot || shootCooldown > 0 || paused || playerDead) return;
        spawnOneBulletGreen();
        bulletsRemaining--;
        if (bulletsRemaining <= 0) {
            shootCooldown = SHOOT_FULL_COOLDOWN;
            canShoot = false;
            bulletsRemaining = 0;
        }
    }

    private void attemptShootGreen() {
        if (shootCooldown > 0 || !canShoot || paused || playerDead) return;
        if (bulletsRemaining == 0 && canShoot && shootCooldown == 0)
            bulletsRemaining = MAX_BULLETS_PER_USE;
        if (bulletsRemaining > 0) fireSingleBulletGreen();
    }

    private void spawnOneBulletGreen() {
        float bx = player.getHitBox().x + player.getHitBox().width;
        float by = player.getHitBox().y;
        playerBullets.add(new GreenJeepProjectile(bx, by, shootFrames));
    }

    // ─────────────────────────────────────────────────────────
    // SKILL 2: Heal
    // ─────────────────────────────────────────────────────────
    private void attemptHeal() {
        if (paused || playerDead) return;

        long now = System.currentTimeMillis();
        if (now - healLastUsed >= HEAL_COOLDOWN) {
            performHeal();
            healLastUsed = now;
        } else {
            long remaining = HEAL_COOLDOWN - (now - healLastUsed);
            System.out.println("[GreenJeepVsBoss3] Heal on cooldown: " + (remaining / 1000) + "s remaining");
        }
    }

    private void performHeal() {
        if (healAnimationFrames == null || healAnimationFrames.length == 0) {
            System.err.println("[GreenJeepVsBoss3] Heal animation frames not loaded!");
            return;
        }

        boolean healed = healthBar.heal();

        if (healed) {
            System.out.println("[GreenJeepVsBoss3] 💚 Healed! +1 half bar");
            isHealing = true;
            healAnimTick = 0;
            healAnimIndex = 0;
        } else {
            System.out.println("[GreenJeepVsBoss3] Already at full health!");
        }
    }

    private void updateDeathOverlay() {
        if (!deathFadeDone) {
            deathAlpha = Math.min(deathAlpha + DEATH_FADE_SPEED, DEATH_FADE_MAX);
            if (deathAlpha >= DEATH_FADE_MAX) deathFadeDone = true;
        }
        if (deathFadeDone) deathRestartBtn.update();
    }

    private void renderDeathOverlay(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, deathAlpha));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        if (deathScreenImg != null) {
            float imgAlpha = Math.min(deathAlpha / DEATH_FADE_MAX, 1f);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, imgAlpha));
            g2d.drawImage(deathScreenImg, deathImgX, deathImgY, deathImgW, deathImgH, null);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
        if (deathFadeDone) deathRestartBtn.draw(g);
    }

    private void resetDeathOverlay() {
        deathAlpha = 0f;
        deathFadeDone = false;
        deathRestartBtn.resetBools();
    }

    @Override
    public void draw(Graphics g) {
        cloudRenderer.drawBackground(g);
        cloudRenderer.drawClouds(g);

        bossBanner.updatePosition(10);  // 10 pixels from top
        bossBanner.render(g);

        game.getPlaying().getLevelManager().draw(g, (int) worldOffset);
        walkerManager.render(g);
        obstacleManager.render(g);
        boss.render(g);

        for (GreenJeepProjectile pb : playerBullets) pb.render(g);
        player.render(g);

        // Draw heal animation (centered on player)
        // Draw heal animation (centered on player)
        if (isHealing && healAnimationFrames != null && healAnimIndex < healAnimationFrames.length) {
            BufferedImage healFrame = healAnimationFrames[healAnimIndex];
            if (healFrame != null) {
                // Use the actual frame dimensions from the loaded image
                int frameWidth = healFrame.getWidth();
                int frameHeight = healFrame.getHeight();

                // Scale the frame properly
                float healScale = 2.5f;
                int healW = (int)(frameWidth * Game.SCALE * healScale);
                int healH = (int)(frameHeight * Game.SCALE * healScale);

                // Center of player hitbox
                float centerX = player.getHitBox().x + player.getHitBox().width / 2f;
                float centerY = player.getHitBox().y + player.getHitBox().height / 2f;

                // Center animation on player
                int healX = (int)(centerX - healW / 2f);
                int healY = (int)(centerY - healH / 2f);

                g.drawImage(healFrame, healX, healY, healW, healH, null);
            }
        }

        // UI bars
        healthBar.render(g);
        bossBar.render(g);
        skillButtons.render(g);

        // Overlays
        if (bossDefeated) {
            defeatOverlay.render(g);
            return;
        }

        if (playerDead) {
            renderDeathOverlay(g);
            return;
        }

        if (paused) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
            pauseOverlay.draw(g);
        }
    }



    @Override
    public void keyPressed(KeyEvent e) {
        if (playerDead || bossDefeated) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                paused = !paused;
                break;
            case KeyEvent.VK_A:
                player.setLeft(true);
                break;
            case KeyEvent.VK_D:
                player.setRight(true);
                break;
            case KeyEvent.VK_W:
                player.setUp(true);
                break;
            case KeyEvent.VK_S:
                player.setDown(true);
                break;
            case KeyEvent.VK_Q:
                if (!paused) attemptHeal();
                break;
            case KeyEvent.VK_E:
                if (!paused) attemptShootGreen();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A:
                player.setLeft(false);
                break;
            case KeyEvent.VK_D:
                player.setRight(false);
                break;
            case KeyEvent.VK_W:
                player.setUp(false);
                break;
            case KeyEvent.VK_S:
                player.setDown(false);
                break;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (bossDefeated) {
            defeatOverlay.mousePressed(e);
            return;
        }
        if (playerDead) {
            if (deathFadeDone && deathRestartBtn.getBounds().contains(e.getX(), e.getY()))
                deathRestartBtn.setMousePressed(true);
        } else if (paused) {
            pauseOverlay.mousePressed(e);
        } else {
            skillButtons.mousePressed(e);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (bossDefeated) {
            defeatOverlay.mouseReleased(e);
            return;
        }
        if (playerDead) {
            if (!deathFadeDone) return;
            if (deathRestartBtn.isMousePressed() &&
                    deathRestartBtn.getBounds().contains(e.getX(), e.getY()))
                fullReset();
            deathRestartBtn.resetBools();
        } else if (paused) {
            pauseOverlay.mouseReleased(e);
        } else {
            skillButtons.mouseReleased(e);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (bossDefeated) {
            defeatOverlay.mouseMoved(e);
            return;
        }
        if (playerDead && deathFadeDone) {
            deathRestartBtn.setMouseOver(
                    deathRestartBtn.getBounds().contains(e.getX(), e.getY()));
        } else if (paused) {
            pauseOverlay.mouseMoved(e);
        } else {
            skillButtons.mouseMoved(e);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        if (paused) pauseOverlay.mouseDragged(e);
    }

    public void unpause() {
        paused = false;
    }

    public boolean isPaused() { return paused; }

    private void onNextLevel() {
        GameStates.state = GameStates.MENU;
        game.setHasActiveGame(false);
    }

    private void onMenuToExit() {
        GameStates.state = GameStates.MENU;
        game.setHasActiveGame(false);
    }

    public void fullReset() {
        healthBar.reset();
        bossBar.reset();

        float spawnX = (Game.GAME_WIDTH - player.getHitBox().width) / 2f;
        player.getHitBox().x = spawnX;
        player.getHitBox().y = 520;
        player.resetDirBooleans();

        paused = false;
        playerDead = false;
        bossDefeated = false;
        shootCooldown = 0;
        bulletsRemaining = 0;
        canShoot = true;
        playerBullets.clear();

        // Reset heal state
        isHealing = false;
        healAnimTick = 0;
        healAnimIndex = 0;
        healLastUsed = 0;

        worldOffset = 0;
        cloudRenderer.reset();
        obstacleManager.reset();
        walkerManager.resetAll();
        resetDeathOverlay();
        spawnBoss();
        player.setBossMode(true);
    }

    public void resetAll() {
        fullReset();
        if (game.getSelectedDriver() != null) {
            applyDriverAssets(game.getSelectedDriver());
        }
    }

}
