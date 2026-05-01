package BossFight.LevelThree.Blue;

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


public class BlueJeepVsBoss3State extends State implements StateMethods {

    // -------------------------------------------------------
    // BOSS FIGHT SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float SCROLL_SPEED                = BossFight.LevelThree.Blue.Boss3.BOSS_SCROLL_SPEED;
    private static final float LEFT_BORDER_PUSH            = 0.3f;
    private static final float PLAYER_RIGHT_LIMIT_FRACTION = 0.50f;

    // ── Shoot settings ← ADJUST ──────────────────────────────
    private static final int MAX_BULLETS_PER_USE = 5;        // bullets per E press
    private static final int SHOOT_FULL_COOLDOWN = 3 * 200;  // 3 s at 200 UPS

    // ── Shield cooldown ← ADJUST ─────────────────────────────
    private static final int SHIELD_DESTROYED_COOLDOWN = 3 * 200; // 3 s after shield destroyed

    // ── Death overlay fade ← ADJUST ──────────────────────────
    private static final float DEATH_FADE_SPEED = 0.03f;
    private static final float DEATH_FADE_MAX   = 0.85f;
    // -------------------------------------------------------

    private final Player       player;
    private final HealthBar    healthBar;    // jeepney life bar (shared with Playing)
    private       BossHealthBar bossBar;     // boss life bar (new)
    private BossFight.LevelThree.Blue.Boss3 boss;

    // ── Walkers during boss fight ─────────────────────────────
    private BossWalkerManager walkerManager;              // NEW from first version

    // ── Shield state: 0=none, 1=full, 2=half ─────────────────
    private int  shieldState    = 0;
    private int  shieldCooldown = 0;

    // ── Shoot state ───────────────────────────────────────────
    // Thread-safe list — safe to iterate during concurrent updates
    private final List<BlueJeepProjectile> playerBullets =
            new CopyOnWriteArrayList<>();
    private BufferedImage[] shootFrames;
    private int     shootCooldown   = 0;
    private int     bulletsRemaining = 0;
    private boolean canShoot        = true;

    private BufferedImage shieldFull, shieldHalf;

    // ── World scroll ─────────────────────────────────────────
    private float worldOffset = 0;
    private final int levelPixelWidth;

    // ── Background ───────────────────────────────────────────
    private BufferedImage backgroundImg, bigClouds, smallClouds;
    private float bigCloudOffset   = 0f;
    private float smallCloudOffset = 0f;
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
    public BlueJeepVsBoss3State(Game game, Player player, HealthBar healthBar) {
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
        bossBar = new BossHealthBar(BossHealthBar.LifeBarType.BOSS3);
        walkerManager = new BossWalkerManager();
        spawnBoss();

        // Initialize skill buttons based on jeep color
        String jeepColor = getJeepColor();
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

        // ✨ Add leading slash if missing
        if (!atlasPath.startsWith("/")) {
            atlasPath = "/" + atlasPath;
        }

        java.io.InputStream is = getClass().getResourceAsStream(atlasPath);

        if (is == null) {
            System.out.println("❌ [BossFightState] Failed to load atlas: " + atlasPath);
            // Fall back to default
            is = getClass().getResourceAsStream("/" + LoadSave.PLAYER_ATLAS_3);
        }

        try {
            java.awt.image.BufferedImage sheet = javax.imageio.ImageIO.read(is);

            shieldFull = sheet.getSubimage(0 * 110, 3 * 40, 110, 40);
            shieldHalf = sheet.getSubimage(1 * 110, 3 * 40, 110, 40);

            shootFrames = new BufferedImage[BlueJeepProjectile.FRAME_COUNT];
            for (int i = 0; i < BlueJeepProjectile.FRAME_COUNT; i++)
                shootFrames[i] = sheet.getSubimage(
                        i * BlueJeepProjectile.FRAME_W,
                        BlueJeepProjectile.SPRITE_ROW * BlueJeepProjectile.FRAME_H,
                        BlueJeepProjectile.FRAME_W,
                        BlueJeepProjectile.FRAME_H);

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

    private void spawnBoss() {
        float bx = Game.GAME_WIDTH + BossFight.LevelThree.Blue.Boss3.FRAME_W * Game.SCALE;
        float by = 480;
        boss = new BossFight.LevelThree.Blue.Boss3(bx, by);
    }

    // ─────────────────────────────────────────────────────────
    // SKILL BUTTONS
    // ─────────────────────────────────────────────────────────
    private String getJeepColor() {
        return "blue";
    }

    private void onSkill1() {
        attemptShootBlue();
    }

    private void onSkill2() {
        if (shieldCooldown == 0 && shieldState == 0)
            shieldState = 1;
    }

    private boolean isSkill1Ready() {
        return bulletsRemaining > 0 || (bulletsRemaining == 0 && shootCooldown == 0);
    }

    private boolean isSkill2Ready() {
        return shieldCooldown == 0 && shieldState == 0;
    }

    private int getSkill1CooldownRemaining() {
        if (shootCooldown <= 0) return 0;
        return (shootCooldown + 199) / 200;
    }

    private int getSkill2CooldownRemaining() {
        if (shieldCooldown <= 0) return 0;
        return (shieldCooldown + 199) / 200;
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

        // ── Skill buttons update ─────────────────────────────
        skillButtons.update();

        // ── World scroll ──────────────────────────────────────
        worldOffset += SCROLL_SPEED * Game.SCALE;
        if (worldOffset >= levelPixelWidth) worldOffset -= levelPixelWidth;

        bigCloudOffset += SCROLL_SPEED * BIG_CLOUD_PARALLAX;
        if (bigCloudOffset >= BIG_CLOUD_WIDTH) bigCloudOffset -= BIG_CLOUD_WIDTH;
        smallCloudOffset += SCROLL_SPEED * SMALL_CLOUD_PARALLAX;
        if (smallCloudOffset >= SMALL_CLOUD_WIDTH) smallCloudOffset -= SMALL_CLOUD_WIDTH;

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
        if (shieldCooldown > 0) shieldCooldown--;

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

// ← ADD BULLET COLLISION HERE
// Check boss bullets collision with player
        for (GravySauce.BossProjectile bullet : boss.getBullets()) {
            if (bullet.isActive() && bullet.getHitbox().intersects(jeepHB)) {
                bullet.setActive(false);
                handleJeepHit();
                System.out.println("[BossFightState] Player hit by boss bullet!");
            }
        }

// Check gravy collisions
        for (GravySauce car : boss.getGravySauces()) {
            if (car.isActive() && car.getHitbox().intersects(jeepHB)) {
                car.setActive(false);
                handleJeepHit();
            }
        }

        // Player bullets → boss
        Rectangle bossHB = boss.getHitbox();
        for (BlueJeepProjectile pb : playerBullets) {
            if (pb.isActive() && pb.getHitbox().intersects(bossHB)) {
                pb.setActive(false);
                boss.triggerHit();  // ← This handles shield absorption automatically

                // Only damage boss health if shield is DOWN
                if (!boss.isShieldActive()) {
                    handleBossHit();
                    System.out.println("[BossFightState] 💥 Boss hit! Shield was down.");
                } else {
                    System.out.println("[BossFightState] 🛡️ Shield absorbed hit! Remaining: " + boss.getShieldHitsRemaining());
                }
            }
        }

    }

    // ─────────────────────────────────────────────────────────
    // HIT HANDLING
    // ─────────────────────────────────────────────────────────
    private void handleJeepHit() {
        if (shieldState == 1) {
            shieldState = 2;                         // full → half
        } else if (shieldState == 2) {
            shieldState    = 0;                      // half → destroyed
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
    // SHOOT Player
    // ─────────────────────────────────────────────────────────
    private void fireSingleBulletBlue() {
        if (!canShoot || shootCooldown > 0 || paused || playerDead) return;
        spawnOneBulletBlue();
        bulletsRemaining--;
        if (bulletsRemaining <= 0) {
            shootCooldown    = SHOOT_FULL_COOLDOWN;
            canShoot         = false;
            bulletsRemaining = 0;
        }
    }

    private void attemptShootBlue() {
        if (shootCooldown > 0 || !canShoot || paused || playerDead) return;
        if (bulletsRemaining == 0 && canShoot && shootCooldown == 0)
            bulletsRemaining = MAX_BULLETS_PER_USE;
        if (bulletsRemaining > 0) fireSingleBulletBlue();
    }

    private void spawnOneBulletBlue() {
        float bx = player.getHitBox().x + player.getHitBox().width;
        float by = player.getHitBox().y;
        playerBullets.add(new BlueJeepProjectile(bx, by, shootFrames));
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

        for (BlueJeepProjectile pb : playerBullets) pb.render(g);

        player.render(g);

        // Shield overlay
        if (shieldState > 0) {
            BufferedImage shieldImg = (shieldState == 1) ? shieldFull : shieldHalf;
            if (shieldImg != null) {
                int sw = (int)(110 * Game.SCALE);
                int sh = (int)(40  * Game.SCALE);
                int sx = (int)(player.getHitBox().x - 21 * Game.SCALE);
                int sy = (int)(player.getHitBox().y - 4  * Game.SCALE);
                g.drawImage(shieldImg, sx, sy, sw, sh, null);
            }
        }

        // ── UI bars ───────────────────────────────────────────
        healthBar.render(g);     // jeep life — upper left
        bossBar.render(g);       // boss life — upper right
        skillButtons.render(g);  // skill buttons

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
                if (!paused && shieldCooldown == 0 && shieldState == 0)
                    shieldState = 1;
                break;
            case KeyEvent.VK_E:
                if (!paused) attemptShootBlue();
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

        paused           = false;
        playerDead       = false;
        bossDefeated     = false;
        shieldState      = 0;
        shieldCooldown   = 0;
        shootCooldown    = 0;
        bulletsRemaining = 0;
        canShoot         = true;
        playerBullets.clear();

        worldOffset      = 0;
        bigCloudOffset   = 0;
        smallCloudOffset = 0;

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

    // ─────────────────────────────────────────────────────────
    // BOSS DEFEAT CALLBACKS
    // ─────────────────────────────────────────────────────────
    private void onNextLevel() {
        GameStates.state = GameStates.MENU;
        game.setHasActiveGame(false);
    }

    private void onMenuToExit() {
        GameStates.state = GameStates.MENU;
        game.setHasActiveGame(false);
    }
}