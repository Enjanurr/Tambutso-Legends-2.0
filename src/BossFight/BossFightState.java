package BossFight;

import Ui.BossDefeatOverlay;
import Ui.BossHealthBar;
import Ui.HealthBar;
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

/**
 * Boss Fight game state.
 *
 * Tweaks applied:
 *   T1 – World scrolls at a constant slow pace (SCROLL_SPEED).
 *   T2 – Boss X locked near right border; Y wanders freely except during Skill 1.
 *   T3 – Player cannot move past screen centre toward boss.
 *   T4 – Boss running animation plays while shooting (Boss1).
 *   T5 – Skill 2: loop cols 1-2 while laying; col 3 once on finish; back to Running.
 *   T6 – ESC opens BossPauseOverlay.
 *   T7 – Death overlay appears when health = 0.
 *   T8 – Shoot: 5 bullets total with 3 second cooldown (single fire per E press).
 *   T9 – Shield: Q activates only (cannot deactivate by key); 2 hits destroy it;
 *         3 s cooldown after destruction.
 *  T10 – Boss has a health bar (BossHealthBar); 16 hits to defeat.
 *  T11 – Boss defeat overlay (BossDefeatOverlay) on boss death.
 *  T12 – Thread-safe CopyOnWriteArrayList for all projectile/pile lists.
 *  T13 – Walkers spawn on all 4 lanes during boss fight (BossWalkerManager).
 */
public class BossFightState extends State implements StateMethods {

    // -------------------------------------------------------
    // BOSS FIGHT SETTINGS  ← ADJUST
    // -------------------------------------------------------
    private static final float SCROLL_SPEED                = Boss1.BOSS_SCROLL_SPEED;
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
    private Boss1              boss;

    // ── Walkers during boss fight ─────────────────────────────
    private BossWalkerManager walkerManager;              // NEW from first version

    // ── Shield state: 0=none, 1=full, 2=half ─────────────────
    private int  shieldState    = 0;
    private int  shieldCooldown = 0;

    // ── Shoot state ───────────────────────────────────────────
    // Thread-safe list — safe to iterate during concurrent updates
    private final List<PlayerProjectile> playerBullets =
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

    // ─────────────────────────────────────────────────────────
    public BossFightState(Game game, Player player, HealthBar healthBar) {
        super(game);
        this.player    = player;
        this.healthBar = healthBar;
        this.levelPixelWidth =
                LoadSave.GetLevelData()[0].length * Game.TILES_SIZE;

        this.playerRightLimit =
                Game.GAME_WIDTH * PLAYER_RIGHT_LIMIT_FRACTION
                        - player.getHitBox().width;

        loadAssets();
        pauseOverlay = new BossPauseOverlay(this);
        buildDeathOverlay();
        buildDefeatOverlay();
        bossBar = new BossHealthBar();
        walkerManager = new BossWalkerManager();   // NEW from first version
        spawnBoss();
    }

    // ─────────────────────────────────────────────────────────
    // ASSET LOADING
    // ─────────────────────────────────────────────────────────
    private void loadAssets() {
        backgroundImg = LoadSave.getSpriteAtlas(LoadSave.PLAYING_BACKGROUND_IMG);
        bigClouds     = LoadSave.getSpriteAtlas(LoadSave.BIG_CLOUDS);
        smallClouds   = LoadSave.getSpriteAtlas(LoadSave.SMALL_CLOUDS);

        java.io.InputStream is =
                getClass().getResourceAsStream(LoadSave.PLAYER_ATLAS);
        try {
            java.awt.image.BufferedImage sheet =
                    javax.imageio.ImageIO.read(is);

            shieldFull = sheet.getSubimage(0 * 110, 3 * 40, 110, 40);
            shieldHalf = sheet.getSubimage(1 * 110, 3 * 40, 110, 40);

            shootFrames = new BufferedImage[PlayerProjectile.FRAME_COUNT];
            for (int i = 0; i < PlayerProjectile.FRAME_COUNT; i++)
                shootFrames[i] = sheet.getSubimage(
                        i * PlayerProjectile.FRAME_W,
                        PlayerProjectile.SPRITE_ROW * PlayerProjectile.FRAME_H,
                        PlayerProjectile.FRAME_W,
                        PlayerProjectile.FRAME_H);

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
                this::fullReset,                              // Restart → full reset
                () -> GameStates.state = GameStates.MENU     // Menu → back to menu
        );
    }

    private void spawnBoss() {
        float bx = Game.GAME_WIDTH + Boss1.FRAME_W * Game.SCALE;
        float by = 480;
        boss = new Boss1(bx, by);
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

        // ── Boss ──────────────────────────────────────────────
        float jeepCentreY = player.getHitBox().y + player.getHitBox().height / 2f;
        boss.update(player.getHitBox().x, jeepCentreY);

        // ── Collisions ────────────────────────────────────────
        Rectangle jeepHB = new Rectangle(
                (int) player.getHitBox().x,     (int) player.getHitBox().y,
                (int) player.getHitBox().width, (int) player.getHitBox().height);

        // Boss bullets → jeep
        for (BossProjectile bp : boss.getBullets()) {
            if (bp.isActive() && bp.getHitbox().intersects(jeepHB)) {
                bp.setActive(false);
                handleJeepHit();
            }
        }

        // Garbage piles → jeep
        for (GarbagePile pile : boss.getGarbagePiles()) {
            if (pile.isActive() && pile.getHitbox().intersects(jeepHB)) {
                pile.setActive(false);
                handleJeepHit();
            }
        }

        // Player bullets → boss
        Rectangle bossHB = boss.getHitbox();
        for (PlayerProjectile pb : playerBullets) {
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
    // SHOOT
    // ─────────────────────────────────────────────────────────
    private void fireSingleBullet() {
        if (!canShoot || shootCooldown > 0 || paused || playerDead) return;
        spawnOneBullet();
        bulletsRemaining--;
        if (bulletsRemaining <= 0) {
            shootCooldown    = SHOOT_FULL_COOLDOWN;
            canShoot         = false;
            bulletsRemaining = 0;
        }
    }

    private void attemptShoot() {
        if (shootCooldown > 0 || !canShoot || paused || playerDead) return;
        if (bulletsRemaining == 0 && canShoot && shootCooldown == 0)
            bulletsRemaining = MAX_BULLETS_PER_USE;
        if (bulletsRemaining > 0) fireSingleBullet();
    }

    private void spawnOneBullet() {
        float bx = player.getHitBox().x + player.getHitBox().width;
        float by = player.getHitBox().y;
        playerBullets.add(new PlayerProjectile(bx, by, shootFrames));
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

        for (PlayerProjectile pb : playerBullets) pb.render(g);

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
                if (!paused) attemptShoot();
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

        walkerManager.resetAll();   // NEW from first version
        resetDeathOverlay();
        spawnBoss();
    }

    public void resetAll() { fullReset(); }
    public boolean isPaused() { return paused; }
}
