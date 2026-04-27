package BossFight.LevelThree.Red;

import BossFight.BossWalkerManager;
import BossFight.LevelThree.GravySauce;
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

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static utils.Constants.Environment.*;
import static utils.Constants.UI.URMButtons.*;

public class RedJeepVsBoss3State extends State implements StateMethods {

    // -------------------------------------------------------
    // BOSS FIGHT SETTINGS
    // -------------------------------------------------------
    private static final float SCROLL_SPEED                = BossFight.LevelThree.Red.Boss3.BOSS_SCROLL_SPEED;
    private static final float LEFT_BORDER_PUSH            = 0.3f;
    private static final float PLAYER_RIGHT_LIMIT_FRACTION = 0.50f;

    // Shoot settings
    private static final int MAX_BULLETS_PER_USE = 5;
    private static final int SHOOT_FULL_COOLDOWN = 3 * 200;

    // Shield cooldown
    private static final int SHIELD_DESTROYED_COOLDOWN = 3 * 200;

    // Death overlay fade
    private static final float DEATH_FADE_SPEED = 0.03f;
    private static final float DEATH_FADE_MAX   = 0.85f;

    // Slow Ball (Skill 2) settings
    private static final int SKILL2_COOLDOWN = 7000;  // 7 seconds in milliseconds

    private final Player       player;
    private final HealthBar    healthBar;
    private       BossHealthBar bossBar;
    private BossFight.LevelThree.Red.Boss3 boss;

    private BossWalkerManager walkerManager;

    // Shield state
    private int  shieldState    = 0;
    private int  shieldCooldown = 0;

    // Shoot state
    private final List<RedJeepProjectile> playerBullets = new CopyOnWriteArrayList<>();
    private BufferedImage[] shootFrames;
    private int     shootCooldown   = 0;
    private int     bulletsRemaining = 0;
    private boolean canShoot        = true;

    // Slow Ball (Skill 2) tracking
    private final List<SlowBallProjectile> slowBalls = new CopyOnWriteArrayList<>();
    private long skill2LastUsed = 0;
    private BufferedImage[] slowBallFrames;

    private BufferedImage shieldFull, shieldHalf;

    // World scroll
    private float worldOffset = 0;
    private final int levelPixelWidth;

    // Background
    private BufferedImage backgroundImg, bigClouds, smallClouds;
    private float bigCloudOffset   = 0f;
    private float smallCloudOffset = 0f;
    private static final float BIG_CLOUD_PARALLAX   = 0.3f;
    private static final float SMALL_CLOUD_PARALLAX = 0.5f;

    private final float playerRightLimit;

    // Pause
    private boolean          paused      = false;
    private BossPauseOverlay pauseOverlay;

    // Jeep death overlay
    private boolean       playerDead    = false;
    private float         deathAlpha    = 0f;
    private boolean       deathFadeDone = false;
    private UrmButton     deathRestartBtn;
    private BufferedImage deathScreenImg;
    private int deathImgW, deathImgH, deathImgX, deathImgY;

    // Boss defeat overlay
    private boolean          bossDefeated   = false;
    private BossDefeatOverlay defeatOverlay;

    // Skill buttons
    private JeepSkillButtons skillButtons;

    // -------------------------------------------------------
    public RedJeepVsBoss3State(Game game, Player player, HealthBar healthBar) {
        super(game);
        this.player    = player;
        this.healthBar = healthBar;
        this.levelPixelWidth = LoadSave.GetLevelData()[0].length * Game.TILES_SIZE;
        this.playerRightLimit = Game.GAME_WIDTH * PLAYER_RIGHT_LIMIT_FRACTION - player.getHitBox().width;

        pauseOverlay = new BossPauseOverlay(this);
        buildDeathOverlay();
        buildDefeatOverlay();
        bossBar = new BossHealthBar();
        walkerManager = new BossWalkerManager();
        spawnBoss();

        // Initialize skill buttons based on jeep color
        String jeepColor = getJeepColor();
        skillButtons = new JeepSkillButtons(jeepColor,
                this::isSkill1Ready, this::onSkill1, this::getSkill1CooldownRemaining,
                this::isSkill2Ready, this::onSkill2, this::getSkill2CooldownRemaining);
    }

    public void applyDriverAssets(entities.DriverProfile profile) {
        if (profile == null) {
            System.out.println("⚠️ [RedBossFightState] No driver profile - keeping default assets");
            return;
        }
        System.out.println("🎮 [RedBossFightState] Applying driver assets: " + profile.displayName);
        loadAssets(profile.atlasPath);
    }

    private void loadAssets(String atlasPath) {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PLAYING_BACKGROUND_IMG);
        bigClouds     = LoadSave.getSpriteAtlas(LoadSave.BIG_CLOUDS);
        smallClouds   = LoadSave.getSpriteAtlas(LoadSave.SMALL_CLOUDS);

        if (!atlasPath.startsWith("/")) {
            atlasPath = "/" + atlasPath;
        }

        java.io.InputStream is = getClass().getResourceAsStream(atlasPath);

        if (is == null) {
            System.out.println("❌ [RedBossFightState] Failed to load atlas: " + atlasPath);
            is = getClass().getResourceAsStream("/" + LoadSave.PLAYER_ATLAS_3);
        }

        try {
            java.awt.image.BufferedImage sheet = javax.imageio.ImageIO.read(is);

            shieldFull = sheet.getSubimage(0 * 110, 3 * 40, 110, 40);
            shieldHalf = sheet.getSubimage(1 * 110, 3 * 40, 110, 40);

            // Load Skill 1 frames
            shootFrames = new BufferedImage[RedJeepProjectile.FRAME_COUNT];
            for (int i = 0; i < RedJeepProjectile.FRAME_COUNT; i++)
                shootFrames[i] = sheet.getSubimage(
                        i * RedJeepProjectile.FRAME_W,
                        RedJeepProjectile.SPRITE_ROW * RedJeepProjectile.FRAME_H,
                        RedJeepProjectile.FRAME_W,
                        RedJeepProjectile.FRAME_H);

            // Load Skill 2 slow ball frames
            slowBallFrames = new BufferedImage[SlowBallProjectile.FRAME_COUNT];
            for (int i = 0; i < SlowBallProjectile.FRAME_COUNT; i++) {
                slowBallFrames[i] = sheet.getSubimage(
                        i * SlowBallProjectile.FRAME_W,
                        SlowBallProjectile.SPRITE_ROW * SlowBallProjectile.FRAME_H,
                        SlowBallProjectile.FRAME_W,
                        SlowBallProjectile.FRAME_H);
            }

            System.out.println("✓ [RedBossFightState] Loaded assets from: " + atlasPath);
            System.out.println("  Skill 1 frames: " + shootFrames.length);
            System.out.println("  Skill 2 frames: " + slowBallFrames.length);

        } catch (Exception e) {
            System.err.println("[RedBossFightState] Could not load jeepney rows: " + e.getMessage());
        } finally {
            try { if (is != null) is.close(); } catch (Exception ignored) {}
        }
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
                this::onNextLevel,
                this::fullReset,
                this::onMenuToExit
        );
    }

    private void spawnBoss() {
        float bx = Game.GAME_WIDTH + BossFight.LevelThree.Red.Boss3.FRAME_W * Game.SCALE;
        float by = 480;
        boss = new BossFight.LevelThree.Red.Boss3(bx, by);
    }

    // ─────────────────────────────────────────────────────────
    // SKILL BUTTONS
    // ─────────────────────────────────────────────────────────
    private String getJeepColor() {
        return "red";
    }

    private void onSkill1() {
        attemptShootRed();
    }

    private void onSkill2() {
        attemptSlowBall();
    }

    private boolean isSkill1Ready() {
        return bulletsRemaining > 0 || (bulletsRemaining == 0 && shootCooldown == 0);
    }

    private boolean isSkill2Ready() {
        return System.currentTimeMillis() - skill2LastUsed >= SKILL2_COOLDOWN;
    }

    private int getSkill1CooldownRemaining() {
        if (shootCooldown <= 0) return 0;
        return (shootCooldown + 199) / 200;
    }

    private int getSkill2CooldownRemaining() {
        long remaining = SKILL2_COOLDOWN - (System.currentTimeMillis() - skill2LastUsed);
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

        bigCloudOffset += SCROLL_SPEED * BIG_CLOUD_PARALLAX;
        if (bigCloudOffset >= BIG_CLOUD_WIDTH) bigCloudOffset -= BIG_CLOUD_WIDTH;
        smallCloudOffset += SCROLL_SPEED * SMALL_CLOUD_PARALLAX;
        if (smallCloudOffset >= SMALL_CLOUD_WIDTH) smallCloudOffset -= SMALL_CLOUD_WIDTH;

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
        if (shieldCooldown > 0) shieldCooldown--;

        // Player bullets
        playerBullets.removeIf(pb -> { pb.update(); return !pb.isActive(); });

        // Slow balls update
        slowBalls.removeIf(ball -> {
            ball.update();
            return !ball.isActive();
        });

        // Walkers
        walkerManager.update(SCROLL_SPEED);

        Rectangle jeepHB = new Rectangle(
                (int) player.getHitBox().x,
                (int) player.getHitBox().y,
                (int) player.getHitBox().width,
                (int) player.getHitBox().height
        );

        float jeepCentreY = jeepHB.y + jeepHB.height / 2f;

        boss.update(jeepHB.x, jeepCentreY, jeepHB.width, jeepHB.height);

        // Boss bullets collision
        for (GravySauce.BossProjectile bullet : boss.getBullets()) {
            if (bullet.isActive() && bullet.getHitbox().intersects(jeepHB)) {
                bullet.setActive(false);
                handleJeepHit();
            }
        }

        // Gravy collision
        for (GravySauce gravy : boss.getGravySauces()) {
            if (gravy.isActive() && gravy.getHitbox().intersects(jeepHB)) {
                gravy.setActive(false);
                handleJeepHit();
            }
        }

        // Slow ball collision with boss
        Rectangle bossHB = boss.getHitbox();
        for (SlowBallProjectile ball : slowBalls) {
            if (ball.isActive() && ball.getHitbox().intersects(bossHB)) {
                ball.setActive(false);
                boss.applySlowEffect();  // Apply slow effect to boss
                System.out.println("[RedJeepVsBoss3] Slow ball hit boss! Slow effect applied.");
            }
        }

        // Player bullets to boss
        for (RedJeepProjectile pb : playerBullets) {
            if (pb.isActive() && pb.getHitbox().intersects(bossHB)) {
                pb.setActive(false);
                boss.triggerHit();

                if (!boss.isShieldActive()) {
                    handleBossHit();
                }
            }
        }
    }

    private void handleJeepHit() {
        if (shieldState == 1) {
            shieldState = 2;
        } else if (shieldState == 2) {
            shieldState = 0;
            shieldCooldown = SHIELD_DESTROYED_COOLDOWN;
        } else {
            player.triggerCarStruck();
            boolean dead = healthBar.takeDamage();
            if (dead) {
                playerDead = true;
                resetDeathOverlay();
            }
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
    private void fireSingleBulletRed() {
        if (!canShoot || shootCooldown > 0 || paused || playerDead) return;
        spawnOneBulletRed();
        bulletsRemaining--;
        if (bulletsRemaining <= 0) {
            shootCooldown = SHOOT_FULL_COOLDOWN;
            canShoot = false;
            bulletsRemaining = 0;
        }
    }

    private void attemptShootRed() {
        if (shootCooldown > 0 || !canShoot || paused || playerDead) return;
        if (bulletsRemaining == 0 && canShoot && shootCooldown == 0)
            bulletsRemaining = MAX_BULLETS_PER_USE;
        if (bulletsRemaining > 0) fireSingleBulletRed();
    }

    private void spawnOneBulletRed() {
        float bx = player.getHitBox().x + player.getHitBox().width;
        float by = player.getHitBox().y;
        playerBullets.add(new RedJeepProjectile(bx, by, shootFrames));
    }

    // ─────────────────────────────────────────────────────────
    // SKILL 2: Slow Ball
    // ─────────────────────────────────────────────────────────
    private void attemptSlowBall() {
        if (paused || playerDead) return;

        long now = System.currentTimeMillis();
        if (now - skill2LastUsed >= SKILL2_COOLDOWN) {
            fireSlowBall();
            skill2LastUsed = now;
        } else {
            long remaining = SKILL2_COOLDOWN - (now - skill2LastUsed);
            System.out.println("[RedJeepVsBoss3] Slow ball on cooldown: " + (remaining / 1000) + "s remaining");
        }
    }

    private void fireSlowBall() {
        // Spawn from jeep RIGHT EDGE, centered vertically
        float spawnX = player.getHitBox().x + player.getHitBox().width;
        float spawnY = player.getHitBox().y + player.getHitBox().height / 2f
                - (SlowBallProjectile.FRAME_H * Game.SCALE) / 2f;

        // Create slow ball with loaded frames
        SlowBallProjectile ball = new SlowBallProjectile(spawnX, spawnY, slowBallFrames);
        slowBalls.add(ball);

        System.out.println("[RedJeepVsBoss3] Slow ball fired!");
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
        if (backgroundImg != null)
            g.drawImage(backgroundImg, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);
        drawClouds(g);

        game.getPlaying().getLevelManager().draw(g, (int) worldOffset);
        walkerManager.render(g);
        boss.render(g);

        // Player Skill 1 bullets
        for (RedJeepProjectile pb : playerBullets) pb.render(g);

        // Player Skill 2 slow balls
        for (SlowBallProjectile ball : slowBalls) ball.render(g);

        player.render(g);

        // Shield overlay
        if (shieldState > 0) {
            BufferedImage shieldImg = (shieldState == 1) ? shieldFull : shieldHalf;
            if (shieldImg != null) {
                int sw = (int)(110 * Game.SCALE);
                int sh = (int)(40 * Game.SCALE);
                int sx = (int)(player.getHitBox().x - 21 * Game.SCALE);
                int sy = (int)(player.getHitBox().y - 4 * Game.SCALE);
                g.drawImage(shieldImg, sx, sy, sw, sh, null);
            }
        }

        healthBar.render(g);
        bossBar.render(g);
        skillButtons.render(g);

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
        int bigTilesNeeded = (Game.GAME_WIDTH / BIG_CLOUD_WIDTH) + 2;
        for (int i = 0; i < bigTilesNeeded; i++) {
            int dx = (int)(i * BIG_CLOUD_WIDTH - bigCloudOffset);
            g.drawImage(bigClouds, dx, (int)(40 * Game.SCALE),
                    BIG_CLOUD_WIDTH, BIG_CLOUD_HEIGHT, null);
        }
        int smallTilesNeeded = (Game.GAME_WIDTH / SMALL_CLOUD_WIDTH) + 2;
        for (int i = 0; i < smallTilesNeeded; i++) {
            int dx = (int)(i * SMALL_CLOUD_WIDTH - smallCloudOffset);
            g.drawImage(smallClouds, dx, (int)(60 * Game.SCALE),
                    SMALL_CLOUD_WIDTH, SMALL_CLOUD_HEIGHT, null);
        }
    }

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
                if (!paused) attemptSlowBall();  // Q for Skill 2 (Slow Ball)
                break;
            case KeyEvent.VK_E:
                if (!paused) attemptShootRed();  // E for Skill 1 (Shoot)
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

    public void unpause() { paused = false; }

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
        shieldState = 0;
        shieldCooldown = 0;
        shootCooldown = 0;
        bulletsRemaining = 0;
        canShoot = true;
        playerBullets.clear();
        slowBalls.clear();  // Clear slow balls
        skill2LastUsed = 0;

        worldOffset = 0;
        bigCloudOffset = 0;
        smallCloudOffset = 0;

        walkerManager.resetAll();
        resetDeathOverlay();
        spawnBoss();
    }

    public void resetAll() {
        fullReset();
        if (game.getSelectedDriver() != null) {
            applyDriverAssets(game.getSelectedDriver());
        }
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
}