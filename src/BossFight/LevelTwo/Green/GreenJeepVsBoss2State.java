package BossFight.LevelTwo.Green;

import BossFight.BossWalkerManager;
import BossFight.LevelTwo.Green.GreenJeepProjectile;
import BossFight.LevelTwo.NukeProjectile;
import Ui.BossDefeatOverlay;
import Ui.BossHealthBar;
import Ui.HealthBar;
import Ui.JeepSkillButtons;
import Ui.UrmButton;
import entities.Player;
import gameStates.GameStates;
import gameStates.State;
import gameStates.StateMethods;
import main.Game;
import utils.LoadSave;
import utils.ScrollingCloudLayer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static utils.Constants.Environment.*;
import static utils.Constants.UI.URMButtons.*;


public class GreenJeepVsBoss2State extends State implements StateMethods {

    // -------------------------------------------------------
    // BOSS FIGHT SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float SCROLL_SPEED                = Boss2.BOSS_SCROLL_SPEED;
    private static final float LEFT_BORDER_PUSH            = 0.3f;
    private static final float PLAYER_RIGHT_LIMIT_FRACTION = 0.50f;

    // ── Shoot settings ← ADJUST ──────────────────────────────
    private static final int MAX_BULLETS_PER_USE = 5;        // bullets per E press
    private static final int SHOOT_FULL_COOLDOWN = 4 * 200;  // 3 s at 200 UPS


    // ── Death overlay fade ← ADJUST ──────────────────────────
    private static final float DEATH_FADE_SPEED = 0.03f;
    private static final float DEATH_FADE_MAX   = 0.85f;
    // -------------------------------------------------------

    private final Player       player;
    private final HealthBar    healthBar;    // jeepney life bar (shared with Playing)
    private       BossHealthBar bossBar;     // boss life bar (new)
    private Boss2             boss;

    // ── Walkers during boss fight ─────────────────────────────
    private BossWalkerManager walkerManager;              // NEW from first version


    // ── Shoot state ───────────────────────────────────────────
    // Thread-safe list — safe to iterate during concurrent updates
    private final List<GreenJeepProjectile> playerBullets =
            new CopyOnWriteArrayList<>();
    private BufferedImage[] shootFrames;
    private int     shootCooldown   = 0;
    private int     bulletsRemaining = 0;
    private boolean canShoot        = true;



    // ── World scroll ─────────────────────────────────────────
    private float worldOffset = 0;
    private final int levelPixelWidth;

    // ── Background ───────────────────────────────────────────
    private BufferedImage backgroundImg, bigClouds, smallClouds;
    private ScrollingCloudLayer bigCloudLayer;
    private ScrollingCloudLayer smallCloudLayer;
    private static final float BIG_CLOUD_PARALLAX   = 0.3f;
    private static final float SMALL_CLOUD_PARALLAX = 0.5f;

    private final float playerRightLimit;

    // ── Pause ─────────────────────────────────────────────────
    private boolean          paused      = false;
    private BossPauseOverlay pauseOverlay;

    // ── Jeep death overlay ────────────────────────────────────
    private boolean       playerDead    = false;
    private float         deathAlpha    = 0f;
    private boolean       deathFadeDone = false;
    private UrmButton     deathRestartBtn;
    private BufferedImage deathScreenImg;
    private int deathImgW, deathImgH, deathImgX, deathImgY;

    // ── Boss defeat overlay ───────────────────────────────────
    private boolean          bossDefeated   = false;
    private BossDefeatOverlay defeatOverlay;

    // ── Skill buttons ────────────────────────────────────────
    private JeepSkillButtons skillButtons;

    // ─────────────────────────────────────────────────────────
// ── Heal cooldown ← CHANGE from slow ball to heal ──────
    private static final int HEAL_COOLDOWN = 10000;  // ← CHANGE: 10 seconds in milliseconds

    // ── Heal (Skill 2) tracking ← ADD THESE ─────────────────
    private long healLastUsed = 0;
    private BufferedImage healEffectSprite;  // Optional: visual feedback sprite
    // ── Heal animation ← ADD THESE ─────────────────────────────
    private BufferedImage[] healAnimationFrames;  // 12 frames for heal
    private int healAnimTick = 0;
    private int healAnimIndex = 0;
    private boolean isHealing = false;
    private static final int HEAL_ANI_SPEED = 20;  // ticks per frame

    public GreenJeepVsBoss2State(Game game, Player player, HealthBar healthBar) {
        super(game);
        this.player    = player;
        this.player.setBossMode(true);
        this.healthBar = healthBar;
        this.levelPixelWidth =
                LoadSave.GetLevelData()[0].length * Game.TILES_SIZE;

        this.playerRightLimit =
                Game.GAME_WIDTH * PLAYER_RIGHT_LIMIT_FRACTION
                        - player.getHitBox().width;

        // ✨ Load assets with default first (in case boss fight starts without selection)
        // loadAssets(LoadSave.PLAYER_ATLAS_1);

        pauseOverlay = new BossPauseOverlay(this);
        buildDeathOverlay();
        buildDefeatOverlay();
        bossBar = new BossHealthBar(BossHealthBar.LifeBarType.BOSS2);
        walkerManager = new BossWalkerManager();
        spawnBoss();

        // Initialize skill buttons based on jeep color
        String jeepColor = getJeepColor(); // "blue", "red", or "green"
        skillButtons = new JeepSkillButtons(jeepColor,
                this::isSkill1Ready, this::onSkill1, this::getSkill1CooldownRemaining,
                this::isSkill2Ready, this::onSkill2, this::getSkill2CooldownRemaining);
    }

    /**
     * Reload shield and shoot sprites from selected driver.
     * Called by Game when transitioning to boss fight.
     */
    public void applyDriverAssets(entities.DriverProfile profile) {
        if (profile == null) {
            System.out.println("⚠️ [BossFightState] No driver profile - keeping default assets");
            return;
        }

        System.out.println("🎮 [BossFightState] Applying driver assets: " + profile.displayName);
        loadAssets(profile.atlasPath);
    }

    // ─────────────────────────────────────────────────────────
    // ASSET LOADING
    // ─────────────────────────────────────────────────────────
// Change loadAssets() signature to accept a parameter
    private void loadAssets(String atlasPath) {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PLAYING_BACKGROUND_IMG);
        bigClouds     = LoadSave.getSpriteAtlas(LoadSave.BIG_CLOUDS);
        smallClouds   = LoadSave.getSpriteAtlas(LoadSave.SMALL_CLOUDS);
        initCloudLayers();

        // ── Load shield from main atlas ───────────────────────────
        java.awt.image.BufferedImage mainSheet = LoadSave.getSpriteAtlas(LoadSave.PLAYER_ATLAS_2);


        // ── Load Skill 1 frames (6 frames from separate skill1 sprite sheet) ──
        java.awt.image.BufferedImage skill1Sheet = LoadSave.getSpriteAtlas(LoadSave.GREEN_JEEP_SKILL1);
        if (skill1Sheet != null) {
            shootFrames = new BufferedImage[6];
            for (int i = 0; i < 6; i++)
                shootFrames[i] = skill1Sheet.getSubimage(
                        i * BossFight.LevelOne.Green.GreenJeepProjectile.FRAME_W,
                        0,  // Row 0 (only one row in skill sheet)
                        BossFight.LevelOne.Green.GreenJeepProjectile.FRAME_W,
                        BossFight.LevelOne.Green.GreenJeepProjectile.FRAME_H);

            System.out.println("✓ [GreenJeepBossFightState] Loaded Skill 1 frames: " + shootFrames.length);
        } else {
            System.err.println("❌ [GreenJeepBossFightState] Failed to load " + LoadSave.GREEN_JEEP_SKILL1);
        }


        // ── Load Skill 2 heal animation (12 frames from separate skill2 sprite sheet) ──
        java.awt.image.BufferedImage skill2Sheet = LoadSave.getSpriteAtlas(LoadSave.GREEN_JEEP_SKILL2);
        if (skill2Sheet != null) {
            healAnimationFrames = new BufferedImage[12];
            int frameWidth = skill2Sheet.getWidth() / 12;
            int frameHeight = skill2Sheet.getHeight();
            for (int i = 0; i < 12; i++)
                healAnimationFrames[i] = skill2Sheet.getSubimage(
                        i * frameWidth,
                        0,
                        frameWidth,
                        frameHeight);

            System.out.println("✓ [GreenJeepBossFightState] Loaded Skill 2 heal frames: " + healAnimationFrames.length);
        } else {
            System.err.println("❌ [GreenJeepBossFightState] Failed to load " + LoadSave.GREEN_JEEP_SKILL2);
        }

        System.out.println("✓ [GreenJeepBossFightState] All assets loaded successfully");
    }


    private void buildDeathOverlay() {
        deathScreenImg = LoadSave.getSpriteAtlas(LoadSave.DEATH_SCREEN);
        deathImgW = (int)(500 * Game.SCALE * 0.5f);
        deathImgH = (int)(500 * Game.SCALE * 0.5f);
        deathImgX = (Game.GAME_WIDTH  - deathImgW) / 2;
        deathImgY = (Game.GAME_HEIGHT - deathImgH) / 2;

        int btnX = (int)(374 * Game.SCALE);
        int btnY = (int)(325 * Game.SCALE);
        deathRestartBtn = new UrmButton(btnX, btnY, URM_SIZE, URM_SIZE, 1);
    }

    private void buildDefeatOverlay() {
        defeatOverlay = new BossDefeatOverlay(
                this::onNextLevel,                            // Next → advance to next level
                this::fullReset,                              // Restart → full reset
                this::onMenuToExit                           // Menu → back to menu
        );
    }

    /**
     * Called when Next button clicked on BossDefeatOverlay.
     * Advances to next level.
     */
    private void onNextLevel() {
        game.getPlaying().setBossFightActive(false);
        player.setBossMode(false);
        game.getPlaying().advanceToNextLevel();
        game.getPlaying().showMissionForCurrentLevel();
        GameStates.state = GameStates.PLAYING;
    }

    private void onMenuToExit() {
        player.setBossMode(false);  // Reset boss mode before returning to menu
        GameStates.state = GameStates.MENU;
    }

    private void spawnBoss() {
        float bx = Game.GAME_WIDTH + Boss2.FRAME_W * Game.SCALE;
        float by = 480;
        boss = new Boss2(bx, by);
    }

    private String getJeepColor() {
        // Return the color based on the state class
        return "green";
    }

    private void onSkill1() {
        // Trigger the existing Skill 1 (shoot/projectile) logic
        // Call the same code that's triggered by E key
        attemptShootGreen();
    }

    private void onSkill2() {
        // Trigger the existing Skill 2 logic
        // Call the same code that's triggered by Q key
        attemptHeal();
    }

    private boolean isSkill1Ready() {
        // Ready when bullets available or can reload (not on cooldown)
        return bulletsRemaining > 0 || (bulletsRemaining == 0 && shootCooldown == 0);
    }

    private boolean isSkill2Ready() {
        // Ready when heal not on cooldown
        return System.currentTimeMillis() - healLastUsed >= HEAL_COOLDOWN;
    }

    private int getSkill1CooldownRemaining() {
        // Return remaining cooldown in seconds for display
        if (shootCooldown <= 0) return 0;
        return (shootCooldown + 199) / 200;  // Round up to nearest second
    }

    private int getSkill2CooldownRemaining() {
        // Return remaining cooldown in seconds for display
        long remaining = HEAL_COOLDOWN - (System.currentTimeMillis() - healLastUsed);
        if (remaining <= 0) return 0;
        return (int) ((remaining + 999) / 1000);  // Convert ms to seconds, round up
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    @Override
    public void update() {

        // ── Boss defeat screen ────────────────────────────────
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

        skillButtons.update();

        // ── World scroll ──────────────────────────────────────
        worldOffset += SCROLL_SPEED * Game.SCALE;
        if (worldOffset >= levelPixelWidth) worldOffset -= levelPixelWidth;

        bigCloudLayer.update(SCROLL_SPEED * Game.SCALE);
        smallCloudLayer.update(SCROLL_SPEED * Game.SCALE);

        // ── Player clamping ───────────────────────────────────
        float leftLimit = 20 * Game.SCALE;
        if (player.getHitBox().x < leftLimit)
            player.getHitBox().x += LEFT_BORDER_PUSH * Game.SCALE;
        if (player.getHitBox().x > playerRightLimit)
            player.getHitBox().x = playerRightLimit;

        // ── Player update ─────────────────────────────────────
        player.setWorldScrolling(false);
        player.setWorldLoopDone(true);
        player.update();

        // ── Cooldowns ─────────────────────────────────────────
        if (shootCooldown > 0) {
            shootCooldown--;
            if (shootCooldown == 0) canShoot = true;
        }
// ── Heal animation update ← ADD THIS ────────────────────────
        if (isHealing) {
            healAnimTick++;
            if (healAnimTick >= HEAL_ANI_SPEED) {
                healAnimTick = 0;
                healAnimIndex++;
                if (healAnimIndex >= 12) {  // 12 frames total
                    isHealing = false;
                    healAnimIndex = 0;
                }
            }
        }

        // ── Player bullets (CopyOnWriteArrayList — thread-safe) ──
        playerBullets.removeIf(pb -> { pb.update(); return !pb.isActive(); });

        // ── Walkers ── NEW from first version ─────────────────────
        walkerManager.update(SCROLL_SPEED);

        Rectangle jeepHB = new Rectangle(
                (int) player.getHitBox().x,
                (int) player.getHitBox().y,
                (int) player.getHitBox().width,
                (int) player.getHitBox().height
        );

        float jeepCentreY = jeepHB.y + jeepHB.height / 2f;

        boss.update(
                jeepHB.x,
                jeepCentreY,
                jeepHB.width,
                jeepHB.height
        );

        // Boss bullets → jeep
        for (NukeProjectile.BossProjectile bp : boss.getBullets()) {
            if (bp.isActive() && bp.getHitbox().intersects(jeepHB)) {
                bp.setActive(false);
                handleJeepHit();
            }
        }

        // Nukes → jeep (animated, scrolling projectiles)
        for (NukeProjectile.Nuke nuke : boss.getNukes()) {
            if (nuke.isActive() && nuke.getHitbox().intersects(jeepHB)) {
                nuke.setActive(false);
                handleJeepHit();
            }
        }

        // Player bullets → boss
        Rectangle bossHB = boss.getHitbox();
        for (GreenJeepProjectile pb : playerBullets) {
            if (pb.isActive() && pb.getHitbox().intersects(bossHB)) {
                pb.setActive(false);
                boss.triggerHit();
                handleBossHit();
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // HIT HANDLING
    // ─────────────────────────────────────────────────────────
    private void handleJeepHit() {

        player.triggerCarStruck();
        boolean dead = healthBar.takeDamage();  // ← healthBar handles HP
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
    // SHOOT Player
    // ─────────────────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────
// SHOOT Player (GREEN JEEP)
// ─────────────────────────────────────────────────────────
    private void fireSingleBulletGreen() {  // ← RENAME
        if (!canShoot || shootCooldown > 0 || paused || playerDead) return;

        spawnOneBulletGreen();  // ← RENAME

        bulletsRemaining--;

        if (bulletsRemaining <= 0) {
            shootCooldown    = SHOOT_FULL_COOLDOWN; // now 4s
            canShoot         = false;
            bulletsRemaining = 0;
        }
    }

    private void attemptShootGreen() {  // ← RENAME
        if (shootCooldown > 0 || !canShoot || paused || playerDead) return;

        if (bulletsRemaining == 0 && canShoot && shootCooldown == 0)
            bulletsRemaining = MAX_BULLETS_PER_USE;

        if (bulletsRemaining > 0)
            fireSingleBulletGreen();  // ← RENAME
    }

    private void spawnOneBulletGreen() {  // ← RENAME
        float bx = player.getHitBox().x + player.getHitBox().width;
        float by = player.getHitBox().y;

        playerBullets.add(new GreenJeepProjectile(bx, by, shootFrames));  // ← CHANGE type
    }

    // ─────────────────────────────────────────────────────────
// HEAL (GREEN JEEP SKILL 2)
// ─────────────────────────────────────────────────────────
    private void attemptHeal() {
        if (paused || playerDead) return;

        long now = System.currentTimeMillis();
        if (now - healLastUsed >= HEAL_COOLDOWN) {
            performHeal();
            healLastUsed = now;
        } else {
            // Optional: feedback for cooldown
            long remaining = HEAL_COOLDOWN - (now - healLastUsed);
            System.out.println("[GreenJeep] Heal on cooldown: " + (remaining / 1000) + "s remaining");
        }
    }

    private void performHeal() {
        if (healAnimationFrames == null) {
            System.err.println("[GreenJeep] Heal animation frames not loaded!");
            return;
        }

        boolean healed = healthBar.heal();

        if (healed) {
            System.out.println("[GreenJeep] Healed! +1 half bar");

            // ← TRIGGER ANIMATION ─────────────────────────────
            isHealing = true;
            healAnimTick = 0;
            healAnimIndex = 0;
        } else {
            System.out.println("[GreenJeep] Already at full health!");
        }
    }

    // ─────────────────────────────────────────────────────────
    // JEEP DEATH OVERLAY
    // ─────────────────────────────────────────────────────────
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
        deathAlpha    = 0f;
        deathFadeDone = false;
        deathRestartBtn.resetBools();
    }

    // ─────────────────────────────────────────────────────────
    // DRAW
    // ─────────────────────────────────────────────────────────
    @Override
    public void draw(Graphics g) {
        if (backgroundImg != null)
            g.drawImage(backgroundImg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        drawClouds(g);

        game.getPlaying().getLevelManager().draw(g, (int) worldOffset);

        // ── Walkers behind boss ── NEW from first version ─────────
        walkerManager.render(g);

        boss.render(g);

        for (BossFight.LevelTwo.Green.GreenJeepProjectile pb : playerBullets) pb.render(g);

        player.render(g);

        if (isHealing && healAnimationFrames != null && healAnimIndex < healAnimationFrames.length) {

            BufferedImage healFrame = healAnimationFrames[healAnimIndex];

            if (healFrame != null) {

                float healScale = 2.5f;

                int healW = (int)(healAnimationFrames[0].getWidth() * Game.SCALE * healScale);
                int healH = (int)(healAnimationFrames[0].getHeight() * Game.SCALE * healScale);

                // Center of player hitbox
                float centerX = player.getHitBox().x + player.getHitBox().width / 2f;
                float centerY = player.getHitBox().y + player.getHitBox().height / 2f;

                // Center animation on player
                int healX = (int)(centerX - healW / 2f);
                int healY = (int)(centerY - healH / 2f);

                g.drawImage(healFrame, healX, healY, healW, healH, null);
            }
        }


        // ── UI bars ───────────────────────────────────────────
        healthBar.render(g);     // jeep life — upper left
        bossBar.render(g);       // boss life — upper right

        // ── Skill buttons ────────────────────────────────────
        skillButtons.render(g);

        // ── Overlays (on top of everything) ───────────────────
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

    private void drawClouds(Graphics g) {
        bigCloudLayer.draw(g);
        smallCloudLayer.draw(g);
    }

    // ─────────────────────────────────────────────────────────
    // INPUT
    // ─────────────────────────────────────────────────────────
    @Override
    public void keyPressed(KeyEvent e) {
        if (playerDead || bossDefeated) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE: paused = !paused; break;
            case KeyEvent.VK_A: player.setLeft(true);  break;
            case KeyEvent.VK_D: player.setRight(true); break;
            case KeyEvent.VK_W: player.setUp(true);    break;
            case KeyEvent.VK_S: player.setDown(true);  break;
            case KeyEvent.VK_Q:
                if (!paused) attemptHeal();  // ← CHANGE to heal
                break;

            case KeyEvent.VK_E:
                if (!paused) attemptShootGreen();  // ← CHANGE method name
                break;


        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: player.setLeft(false);  break;
            case KeyEvent.VK_D: player.setRight(false); break;
            case KeyEvent.VK_W: player.setUp(false);    break;
            case KeyEvent.VK_S: player.setDown(false);  break;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (bossDefeated) { defeatOverlay.mousePressed(e); return; }
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
        if (bossDefeated) { defeatOverlay.mouseReleased(e); return; }
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
        if (bossDefeated) { defeatOverlay.mouseMoved(e); return; }
        if (playerDead && deathFadeDone) {
            deathRestartBtn.setMouseOver(
                    deathRestartBtn.getBounds().contains(e.getX(), e.getY()));
        } else if (paused) {
            pauseOverlay.mouseMoved(e);
        } else {
            skillButtons.mouseMoved(e);
        }
    }

    @Override public void mouseClicked(MouseEvent e) {}

    public void mouseDragged(MouseEvent e) {
        if (paused) pauseOverlay.mouseDragged(e);
    }

    // ─────────────────────────────────────────────────────────
    // PUBLIC CONTROL METHODS
    // ─────────────────────────────────────────────────────────
    public void unpause() { paused = false; }

    public void fullReset() {
        healthBar.reset();
        bossBar.reset();

        float spawnX = (Game.GAME_WIDTH - player.getHitBox().width) / 2f;
        player.getHitBox().x = spawnX;
        player.getHitBox().y = 520;
        player.resetDirBooleans();
        player.setBossMode(false);
        paused           = false;
        playerDead       = false;
        bossDefeated     = false;
        shootCooldown    = 0;
        bulletsRemaining = 0;
        canShoot         = true;
        playerBullets.clear();

        // ── Reset heal state ← UPDATE ──────────────────────────────
        isHealing = false;
        healAnimTick = 0;
        healAnimIndex = 0;
        healLastUsed = 0;

        worldOffset = 0;
        initCloudLayers();

        walkerManager.resetAll();   // NEW from first version
        resetDeathOverlay();
        spawnBoss();
        player.setBossMode(true);  // Restore boss mode — was cleared above for hitbox reset
    }

    public void resetAll() {
        fullReset();
        // ✨ Reapply driver assets after reset
        if (game.getSelectedDriver() != null) {
            applyDriverAssets(game.getSelectedDriver());
        }}
    private void initCloudLayers() {
        int bigCloudCount = (Game.GAME_WIDTH / BIG_CLOUD_WIDTH) + 3;
        int smallCloudCount = (Game.GAME_WIDTH / SMALL_CLOUD_WIDTH) + 3;
        bigCloudLayer = new ScrollingCloudLayer(
                bigClouds, BIG_CLOUD_WIDTH, BIG_CLOUD_HEIGHT,
                BIG_CLOUD_PARALLAX, bigCloudCount, (int)(40 * Game.SCALE));
        smallCloudLayer = new ScrollingCloudLayer(
                smallClouds, SMALL_CLOUD_WIDTH, SMALL_CLOUD_HEIGHT,
                SMALL_CLOUD_PARALLAX, smallCloudCount, (int)(60 * Game.SCALE));
    }
    public boolean isPaused() { return paused; }

    public Player getPlayer() { return player; }
}
