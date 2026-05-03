package BossFight.LevelOne.Red;

import BossFight.BossObstacleManager;
import BossFight.BossWalkerManager;
import BossFight.CloudRenderer;
import BossFight.LevelOne.GarbagePile;
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

import static utils.Constants.UI.URMButtons.URM_SIZE;

public class RedJeepVsBoss1State extends State implements StateMethods {
    private BossBanner bossBanner;
    private CloudRenderer cloudRenderer;

    // -------------------------------------------------------
    // BOSS FIGHT SETTINGS
    // -------------------------------------------------------
    private static final float SCROLL_SPEED = BossFight.LevelOne.Red.Boss1.BOSS_SCROLL_SPEED;
    private static final float LEFT_BORDER_PUSH = 0.3f;
    private static final float PLAYER_RIGHT_LIMIT_FRACTION = 0.50f;

    // Shoot settings
    private static final int MAX_BULLETS_PER_USE = 5;
    private static final int SHOOT_FULL_COOLDOWN = 3 * 200;

    // Death overlay fade
    private static final float DEATH_FADE_SPEED = 0.03f;
    private static final float DEATH_FADE_MAX = 0.85f;

    private final Player player;
    private final HealthBar healthBar;
    private BossHealthBar bossBar;
    private BossFight.LevelOne.Red.Boss1 boss;

    private BossWalkerManager walkerManager;

    // Shoot state
    private final List<RedJeepProjectile> playerBullets = new CopyOnWriteArrayList<>();
    private BufferedImage[] shootFramesSkill1;
    private int shootCooldown = 0;
    private int bulletsRemaining = 0;
    private boolean canShoot = true;

    // World scroll
    private float worldOffset = 0;
    private final int levelPixelWidth;

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

    // Slow Ball (Skill 2)
    private final List<SlowBallProjectile> slowBalls = new CopyOnWriteArrayList<>();
    private long skill2LastUsed = 0;
    private static final int SKILL2_COOLDOWN = 7000;
    private BufferedImage[] slowBallFrames;
    private BossObstacleManager obstacleManager;

    public RedJeepVsBoss1State(Game game, Player player, HealthBar healthBar) {
        super(game);
        this.player = player;
        this.player.setBossMode(true);
        this.healthBar = healthBar;
        this.cloudRenderer = new CloudRenderer();
        obstacleManager = new BossObstacleManager();  // ← ADD THIS
        this.levelPixelWidth = LoadSave.GetLevelData()[0].length * Game.TILES_SIZE;
        this.playerRightLimit = Game.GAME_WIDTH * PLAYER_RIGHT_LIMIT_FRACTION - player.getHitBox().width;

        pauseOverlay = new BossPauseOverlay(this);
        buildDeathOverlay();
        buildDefeatOverlay();
        bossBar = new BossHealthBar(BossHealthBar.LifeBarType.BOSS1);
        bossBanner = new BossBanner(1);
        walkerManager = new BossWalkerManager();
        spawnBoss();

        String jeepColor = getJeepColor();
        skillButtons = new JeepSkillButtons(jeepColor,
                this::isSkill1Ready, this::onSkill1, this::getSkill1CooldownRemaining,
                this::isSkill2Ready, this::onSkill2, this::getSkill2CooldownRemaining);
    }

    public void applyDriverAssets(entities.DriverProfile profile) {
        if (profile == null) {
            System.out.println("⚠️ [BossFightState] No driver profile - keeping default assets");
            return;
        }
        System.out.println("🎮 [BossFightState] Applying driver assets: " + profile.displayName);
        loadAssets(profile.atlasPath);
    }

    private void loadAssets(String atlasPath) {
        if (!atlasPath.startsWith("/")) {
            atlasPath = "/" + atlasPath;
        }

        java.io.InputStream is = getClass().getResourceAsStream(atlasPath);

        if (is == null) {
            System.out.println("❌ [BossFightState] Failed to load atlas: " + atlasPath);
            is = getClass().getResourceAsStream("/" + LoadSave.PLAYER_ATLAS_1);
        }

        try {
            java.awt.image.BufferedImage sheet = javax.imageio.ImageIO.read(is);

            shootFramesSkill1 = new BufferedImage[RedJeepProjectile.FRAME_COUNT];
            for (int i = 0; i < RedJeepProjectile.FRAME_COUNT; i++)
                shootFramesSkill1[i] = sheet.getSubimage(
                        i * RedJeepProjectile.FRAME_W,
                        RedJeepProjectile.SPRITE_ROW * RedJeepProjectile.FRAME_H,
                        RedJeepProjectile.FRAME_W,
                        RedJeepProjectile.FRAME_H);

            BufferedImage[] slowBallFrames_Temp = new BufferedImage[SlowBallProjectile.FRAME_COUNT];
            for (int i = 0; i < SlowBallProjectile.FRAME_COUNT; i++) {
                slowBallFrames_Temp[i] = sheet.getSubimage(
                        i * SlowBallProjectile.FRAME_W,
                        SlowBallProjectile.SPRITE_ROW * SlowBallProjectile.FRAME_H,
                        SlowBallProjectile.FRAME_W,
                        SlowBallProjectile.FRAME_H);
            }
            slowBallFrames = slowBallFrames_Temp;

            System.out.println("✓ [BossFightState] Skill 1 frames loaded: " + shootFramesSkill1.length);
            System.out.println("✓ [BossFightState] Skill 2 frames loaded: " + slowBallFrames.length);
            System.out.println("✓ [BossFightState] Loaded assets from: " + atlasPath);

        } catch (Exception e) {
            System.err.println("[BossFightState] Could not load jeepney rows: " + e.getMessage());
        } finally {
            try { if (is != null) is.close(); } catch (Exception ignored) {}
        }
    }

    private void buildDeathOverlay() {
        deathScreenImg = LoadSave.getSpriteAtlas(LoadSave.DEATH_SCREEN);
        deathImgW = (int)(500 * Game.SCALE * 0.5f);
        deathImgH = (int)(500 * Game.SCALE * 0.5f);
        deathImgX = (Game.GAME_WIDTH - deathImgW) / 2;
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

    private void onNextLevel() {
        game.getPlaying().setBossFightActive(false);
        player.setBossMode(false);
        game.getPlaying().advanceToNextLevel();
        game.getPlaying().showMissionForCurrentLevel();
        GameStates.state = GameStates.PLAYING;
    }

    private void onMenuToExit() {
        player.setBossMode(false);
        GameStates.state = GameStates.MENU;
    }

    private void spawnBoss() {
        float bx = Game.GAME_WIDTH + BossFight.LevelOne.Blue.Boss1.FRAME_W * Game.SCALE;
        float by = 480;
        boss = new Boss1(bx, by);
    }

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

        // Player bullets
        playerBullets.removeIf(pb -> { pb.update(); return !pb.isActive(); });

        // Slow balls
        slowBalls.removeIf(ball -> {
            ball.update();
            return !ball.isActive();
        });

        // Walkers
        walkerManager.update(SCROLL_SPEED);
        obstacleManager.update(true, SCROLL_SPEED * Game.SCALE);  // ← ADD THIS
        // Boss
        float jeepCentreY = player.getHitBox().y + player.getHitBox().height / 2f;
        boss.update(player.getHitBox().x, jeepCentreY);

        // Collisions
        Rectangle jeepHB = new Rectangle(
                (int) player.getHitBox().x, (int) player.getHitBox().y,
                (int) player.getHitBox().width, (int) player.getHitBox().height);

        for (GarbagePile.BossProjectile bp : boss.getBullets()) {
            if (bp.isActive() && bp.getHitbox().intersects(jeepHB)) {
                bp.setActive(false);
                handleJeepHit();
            }
        }

        for (GarbagePile pile : boss.getGarbagePiles()) {
            if (pile.isActive() && pile.getHitbox().intersects(jeepHB)) {
                pile.setActive(false);
                handleJeepHit();
            }
        }

// ── ADD OBSTACLE COLLISION ──
        obstacleManager.checkCollision(jeepHB, this::handleJeepHit);
// Player bullets → boss AND obstacles
        Rectangle bossHB = boss.getHitbox();
        for (RedJeepProjectile pb : playerBullets) {
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
                    System.out.println("[RedJeep] Bullet hit obstacle!");
                    break;
                }
            }
            if (hitObstacle) continue;
        }

// Slow ball collision with boss
        for (SlowBallProjectile ball : slowBalls) {
            if (ball.isActive() && ball.getHitbox().intersects(bossHB)) {
                ball.setActive(false);
                boss.applyStun();
                System.out.println("[RedJeep] Slow ball hit boss! Stun effect applied.");
            }
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

    private void attemptSlowBall() {
        if (paused || playerDead) return;

        long now = System.currentTimeMillis();
        if (now - skill2LastUsed >= SKILL2_COOLDOWN) {
            fireSlowBall();
            skill2LastUsed = now;
        } else {
            long remaining = SKILL2_COOLDOWN - (now - skill2LastUsed);
            System.out.println("[RedJeep] Slow ball on cooldown: " + (remaining / 1000) + "s remaining");
        }
    }

    private void fireSlowBall() {
        float spawnX = player.getHitBox().x + player.getHitBox().width;
        float spawnY = player.getHitBox().y + player.getHitBox().height / 2f
                - (SlowBallProjectile.FRAME_H * Game.SCALE) / 2f;

        SlowBallProjectile ball = new SlowBallProjectile(spawnX, spawnY, slowBallFrames);
        slowBalls.add(ball);
        System.out.println("[RedJeep] Slow ball fired!");
    }

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
        playerBullets.add(new RedJeepProjectile(bx, by, shootFramesSkill1));
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
        obstacleManager.render(g);  // ← ADD THIS
        boss.render(g);

        for (RedJeepProjectile pb : playerBullets) pb.render(g);
        for (SlowBallProjectile ball : slowBalls) ball.render(g);
        player.render(g);

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

    @Override
    public void keyPressed(KeyEvent e) {
        if (playerDead || bossDefeated) return;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE: paused = !paused; break;
            case KeyEvent.VK_A: player.setLeft(true); break;
            case KeyEvent.VK_D: player.setRight(true); break;
            case KeyEvent.VK_W: player.setUp(true); break;
            case KeyEvent.VK_S: player.setDown(true); break;
            case KeyEvent.VK_Q:
                if (!paused) attemptSlowBall();
                break;
            case KeyEvent.VK_E:
                if (!paused) attemptShootRed();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_A: player.setLeft(false); break;
            case KeyEvent.VK_D: player.setRight(false); break;
            case KeyEvent.VK_W: player.setUp(false); break;
            case KeyEvent.VK_S: player.setDown(false); break;
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

    @Override
    public void mouseClicked(MouseEvent e) {}

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
        player.setBossMode(false);
        paused = false;
        playerDead = false;
        bossDefeated = false;

        shootCooldown = 0;
        bulletsRemaining = 0;
        canShoot = true;
        playerBullets.clear();

        slowBalls.clear();
        skill2LastUsed = 0;

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

    public boolean isPaused() { return paused; }
    public Player getPlayer() { return player; }
}