package BossFight.LevelThree.Red.state;

import BossFight.LevelThree.Red.entity.*;
import BossFight.LevelThree.Red.overlay.*;
import BossFight.LevelThree.Red.projectile.*;

import BossFight.core.BossObstacleManager;
import BossFight.core.BossWalkerManager;
import BossFight.render.BuildingRenderer;
import BossFight.render.CloudRenderer;
import BossFight.LevelThree.shared.GravySauce;
import Ui.buttons.*;
import Ui.hud.*;
import Ui.overlays.*;
import Ui.text.*;
import Ui.audio.*;
import entities.actors.EnemyCar;
import entities.actors.Player;
import gameStates.core.GameStates;
import gameStates.core.State;
import gameStates.core.StateMethods;
import main.Game;
import utils.LoadSave;
import utils.ScrollingCloudLayer;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static utils.Constants.UI.URMButtons.*;

public class RedJeepVsBoss3State extends State implements StateMethods {

    // -------------------------------------------------------
    // BOSS FIGHT SETTINGS
    // -------------------------------------------------------

    private BossBanner bossBanner;
    private BuildingRenderer buildingRenderer;
    private CloudRenderer cloudRenderer;
    private BossObstacleManager obstacleManager;

    private static final float SCROLL_SPEED = Boss3.BOSS_SCROLL_SPEED;
    private static final float LEFT_BORDER_PUSH = 0.3f;
    private static final float PLAYER_RIGHT_LIMIT_FRACTION = 0.50f;

    // Shoot settings
    private static final int MAX_BULLETS_PER_USE = 5;
    private static final int SHOOT_FULL_COOLDOWN = 3 * 200;

    // Shield cooldown
    private static final int SHIELD_DESTROYED_COOLDOWN = 3 * 200;

    // Death overlay fade
    private static final float DEATH_FADE_SPEED = 0.03f;
    private static final float DEATH_FADE_MAX = 0.85f;

    // Slow Ball (Skill 2) settings
    private static final int SKILL2_COOLDOWN = 7000;

    private final Player player;
    private final HealthBar healthBar;
    private BossHealthBar bossBar;
    private Boss3 boss;

    private BossWalkerManager walkerManager;

    // Shield state
    private int shieldState = 0;
    private int shieldCooldown = 0;

    // Shoot state
    private final List<RedJeepProjectile> playerBullets = new CopyOnWriteArrayList<>();
    private BufferedImage[] shootFrames;
    private int shootCooldown = 0;
    private int bulletsRemaining = 0;
    private boolean canShoot = true;

    // Slow Ball (Skill 2) tracking
    private final List<SlowBallProjectile> slowBalls = new CopyOnWriteArrayList<>();
    private long skill2LastUsed = 0;
    private BufferedImage[] slowBallFrames;

    private BufferedImage shieldFull, shieldHalf;

    // World scroll
    private float worldOffset = 0;
    private final int levelPixelWidth;

    // Background

    private final float playerRightLimit;

    // Pause
    private boolean paused = false;
    private BossPauseOverlay pauseOverlay;
    private PauseOverlayButton pauseButton;

    // Jeep death overlay
    private boolean playerDead = false;
    private float deathAlpha = 0f;
    private boolean deathFadeDone = false;
    private UrmButton deathRestartBtn;
    private BufferedImage deathScreenImg;
    private int deathImgW, deathImgH, deathImgX, deathImgY;

    // ADD THIS MISSING FIELD
    private boolean bossDefeated = false;

    // Game completion overlay (Boss 3 only)
    private GameCompletionOverlay completionOverlay;
    private boolean showCompletionOverlay = false;

    // Credits overlay (Boss 3 only)
    private CreditsOverlay creditsOverlay;

    // Skill buttons
    private JeepSkillButtons skillButtons;
    // -------------------------------------------------------

    public RedJeepVsBoss3State(Game game, Player player, HealthBar healthBar) {
        super(game);
        this.player = player;
        player.setBossMode(true);
        this.healthBar = healthBar;

        cloudRenderer = new CloudRenderer();
        buildingRenderer = new BuildingRenderer();
        obstacleManager = new BossObstacleManager(game);

        this.levelPixelWidth = LoadSave.GetLevelData()[0].length * Game.TILES_SIZE;
        this.playerRightLimit = Game.GAME_WIDTH * PLAYER_RIGHT_LIMIT_FRACTION - player.getHitBox().width;

        pauseOverlay = new BossPauseOverlay(this);

        // Pause button: bottom-right corner
        float pauseBtnScale = 0.8f;
        int pauseBtnW = (int)(126 * Game.SCALE * pauseBtnScale);
        int pauseBtnH = (int)(42 * Game.SCALE * pauseBtnScale);
        int   pauseBtnX = Game.GAME_WIDTH  - pauseBtnW - (int)(-62 * Game.SCALE);  // ← ADJUST: right margin
        int   pauseBtnY = Game.GAME_HEIGHT - pauseBtnH - (int)(2 * Game.SCALE);  // ← ADJUST: bottom margin
        pauseButton = new PauseOverlayButton(pauseBtnX, pauseBtnY, pauseBtnScale, () -> {
            paused = true;
            System.out.println("[RedJeepVsBoss3] Pause button clicked");
        });

        buildDeathOverlay();
        buildCompletionOverlay();
        bossBar = new BossHealthBar(BossHealthBar.LifeBarType.BOSS3);
        bossBanner = new BossBanner(3);
        walkerManager = new BossWalkerManager();
        spawnBoss();

        String jeepColor = getJeepColor();
        skillButtons = new JeepSkillButtons(jeepColor,
                this::isSkill1Ready, this::onSkill1, this::getSkill1CooldownRemaining,
                this::isSkill2Ready, this::onSkill2, this::getSkill2CooldownRemaining);
    }

    public void applyDriverAssets(entities.profile.DriverProfile profile) {
        if (profile == null) {
            System.out.println("⚠️ [RedBossFightState] No driver profile - keeping default assets");
            return;
        }
        System.out.println("🎮 [RedBossFightState] Applying driver assets: " + profile.displayName);
        loadAssets(profile.atlasPath);
    }

    private void loadAssets(String atlasPath) {


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
        deathImgX = (Game.GAME_WIDTH - deathImgW) / 2;
        deathImgY = (Game.GAME_HEIGHT - deathImgH) / 2;

        int btnX = (int)(374 * Game.SCALE);
        int btnY = (int)(325 * Game.SCALE);
        deathRestartBtn = new UrmButton(btnX, btnY, URM_SIZE, URM_SIZE, 1);
    }

    private void buildCompletionOverlay() {
        completionOverlay = new GameCompletionOverlay(this::onNextToCredits);
    }

    private void initCreditsOverlay() {
        creditsOverlay = new CreditsOverlay(() -> {
            creditsOverlay = null;
            GameStates.state = GameStates.MENU;
            game.setHasActiveGame(false);
            game.markNeedsFullReset();
            System.out.println("[RedJeepVsBoss3] Credits closed — returning to MENU, full reset flagged");
        });
    }

    private void onNextToCredits() {
        if (creditsOverlay == null) {
            initCreditsOverlay();
        }
        creditsOverlay.open();
        if (completionOverlay != null) completionOverlay.close();
        showCompletionOverlay = false;
        System.out.println("[RedJeepVsBoss3] Completion overlay closed, credits overlay opened");
    }

    private void spawnBoss() {
        float bx = Game.GAME_WIDTH + Boss3.FRAME_W * Game.SCALE;
        float by = 480;
        boss = new Boss3(bx, by);
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
        // Credits overlay
        if (creditsOverlay != null && creditsOverlay.isOpen()) {
            creditsOverlay.update();
            return;
        }

        // Game completion overlay
        if (showCompletionOverlay && completionOverlay != null) {
            completionOverlay.update();
            return;
        }

        if (showCompletionOverlay) {
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
        if (pauseButton != null) pauseButton.update();

        // World scroll
        worldOffset += SCROLL_SPEED * Game.SCALE;
        if (worldOffset >= levelPixelWidth) worldOffset -= levelPixelWidth;
        cloudRenderer.update(SCROLL_SPEED * Game.SCALE);
        buildingRenderer.update(true, SCROLL_SPEED * Game.SCALE);
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

        playerBullets.removeIf(pb -> { pb.update(); return !pb.isActive(); });
        slowBalls.removeIf(ball -> { ball.update(); return !ball.isActive(); });

        walkerManager.update(SCROLL_SPEED);
        obstacleManager.update(true, SCROLL_SPEED * Game.SCALE);
        Rectangle jeepHB = new Rectangle(
                (int) player.getHitBox().x,
                (int) player.getHitBox().y,
                (int) player.getHitBox().width,
                (int) player.getHitBox().height);

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
        obstacleManager.checkCollision(jeepHB, this::handleJeepHit);
        // Slow ball collision with boss
        Rectangle bossHB = boss.getHitbox();
        for (SlowBallProjectile ball : slowBalls) {
            if (ball.isActive() && ball.getHitbox().intersects(bossHB)) {
                ball.setActive(false);
                boss.applyStun();  // Apply slow effect to boss
                System.out.println("[RedJeepVsBoss3] Slow ball hit boss! Slow effect applied.");
            }
        }

        for (RedJeepProjectile pb : playerBullets) {
            if (!pb.isActive()) continue;

            // Check bullet vs boss
            if (pb.getHitbox().intersects(bossHB)) {
                pb.setActive(false);
                boss.triggerHit();
                if (!boss.isShieldActive()) {
                    handleBossHit();
                }
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
        if (defeated && !bossDefeated) {
            bossDefeated = true;
            game.getAudioPlayer().playLevelClearThenMenuTheme();
            System.out.println("[RedJeepVsBoss3] Boss defeated! Recording time to leaderboard...");

            // ── CRITICAL: Record time to leaderboard ──
            game.getLeaderboardManager().completeGame();

            System.out.println("[RedJeepVsBoss3] Showing completion overlay...");
            showCompletionOverlay = true;

            // STOP THE TIMER AND SAVE TO LEADERBOARD
            game.getLeaderboardManager().completeGame();
            System.out.println("🎉 RED JEEP BEAT BOSS 3! Time: " +
                    game.getLeaderboardManager().getCurrentPlayer().getFormattedBestTime());

            if (completionOverlay != null) {
                completionOverlay.reset();
            }
        }
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
        playerBullets.add(new RedJeepProjectile(bx, by, shootFrames));
    }

    private void attemptSlowBall() {
        if (paused || playerDead) return;

        long now = System.currentTimeMillis();
        if (now - skill2LastUsed >= SKILL2_COOLDOWN) {
            fireSlowBall();
            skill2LastUsed = now;
        }
    }

    private void fireSlowBall() {
        float spawnX = player.getHitBox().x + player.getHitBox().width;
        float spawnY = player.getHitBox().y + player.getHitBox().height / 2f
                - (SlowBallProjectile.FRAME_H * Game.SCALE) / 2f;

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
        cloudRenderer.drawBackground(g);
        cloudRenderer.drawClouds(g);
        buildingRenderer.render(g);

        bossBanner.updatePosition(10);
        bossBanner.render(g);

        game.getPlaying().getLevelManager().draw(g, (int) worldOffset);

        walkerManager.render(g);
        obstacleManager.render(g);
        boss.render(g);

        for (RedJeepProjectile pb : playerBullets) pb.render(g);
        for (SlowBallProjectile ball : slowBalls) ball.render(g);
        player.render(g);

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

        if (creditsOverlay != null && creditsOverlay.isOpen()) {
            creditsOverlay.draw(g);
            return;
        }

        if (showCompletionOverlay && completionOverlay != null) {
            completionOverlay.render(g);
            return;
        }

        if (playerDead) {
            renderDeathOverlay(g);
            return;
        }

        if (pauseButton != null && !paused && !playerDead) {
            pauseButton.draw(g);
        }

        if (paused) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);
            pauseOverlay.draw(g);
        }
    }



    @Override
    public void keyPressed(KeyEvent e) {
        if (playerDead || showCompletionOverlay) return;

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
        if (creditsOverlay != null && creditsOverlay.isOpen()) {
            creditsOverlay.mousePressed(e);
            return;
        }
        if (showCompletionOverlay && completionOverlay != null) {
            completionOverlay.mousePressed(e);
            return;
        }
        if (playerDead) {
            if (deathFadeDone && deathRestartBtn.getBounds().contains(e.getX(), e.getY()))
                deathRestartBtn.setMousePressed(true);
        } else if (paused) {
            pauseOverlay.mousePressed(e);
        } else {
            if (pauseButton != null && pauseButton.getBounds().contains(e.getX(), e.getY())) {
                pauseButton.mousePressed(e);
            } else {
                skillButtons.mousePressed(e);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (creditsOverlay != null && creditsOverlay.isOpen()) {
            creditsOverlay.mouseReleased(e);
            return;
        }
        if (showCompletionOverlay && completionOverlay != null) {
            completionOverlay.mouseReleased(e);
            return;
        }
        if (playerDead) {
            if (!deathFadeDone) return;
            if (deathRestartBtn.isMousePressed() && deathRestartBtn.getBounds().contains(e.getX(), e.getY()))
                fullReset();
            deathRestartBtn.resetBools();
        } else if (paused) {
            pauseOverlay.mouseReleased(e);
        } else {
            if (pauseButton != null && pauseButton.getBounds().contains(e.getX(), e.getY())) {
                pauseButton.mouseReleased(e);
            } else {
                skillButtons.mouseReleased(e);
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (creditsOverlay != null && creditsOverlay.isOpen()) {
            creditsOverlay.mouseMoved(e);
            return;
        }
        if (showCompletionOverlay && completionOverlay != null) {
            completionOverlay.mouseMoved(e);
            return;
        }
        if (playerDead && deathFadeDone) {
            deathRestartBtn.setMouseOver(deathRestartBtn.getBounds().contains(e.getX(), e.getY()));
        } else if (paused) {
            pauseOverlay.mouseMoved(e);
        } else {
            if (pauseButton != null) pauseButton.mouseMoved(e);
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

        paused = false;
        playerDead = false;
        bossDefeated = false;
        showCompletionOverlay = false;
        shieldState = 0;
        shieldCooldown = 0;
        shootCooldown = 0;
        bulletsRemaining = 0;
        canShoot = true;
        playerBullets.clear();
        slowBalls.clear();
        skill2LastUsed = 0;

        worldOffset = 0;
        cloudRenderer.reset();
        obstacleManager.reset();
        buildingRenderer.reset();
        walkerManager.resetAll();
        resetDeathOverlay();
        spawnBoss();
        player.setBossMode(true);

        creditsOverlay = null;
        if (completionOverlay != null) completionOverlay.close();
    }

    public void resetAll() {
        fullReset();
        creditsOverlay = null;
        if (game.getSelectedDriver() != null) {
            applyDriverAssets(game.getSelectedDriver());
        }
    }



    public boolean isPaused() { return paused; }

    private void onNextLevel() {
        game.getPlaying().handleBossVictoryNext();
    }

    private void onMenuToExit() {
        GameStates.state = GameStates.MENU;
        game.setHasActiveGame(false);
        creditsOverlay = null;
    }
}









