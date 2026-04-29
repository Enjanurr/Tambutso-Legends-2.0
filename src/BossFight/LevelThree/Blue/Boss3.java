package BossFight.LevelThree.Blue;

import BossFight.LevelThree.GravySauce;
import main.Game;
import utils.LoadSave;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Boss3 {

    // ── Sprite sheet dimensions ───────────────────────────────
    public static final int SHEET_COLS = 5;
    public static final int FRAME_W    = 115;  // 550 / 5
    public static final int FRAME_H    = 79;   // 316 / 4
    public static final int ROWS       = 6;    // 6 rows now

    // ── Row indices ───────────────────────────────────────────
    public static final int ROW_SHOOT = 0;           // Bullet frames
    public static final int ROW_SHIELD_FORM = 1;     // Shield forming animation
    public static final int ROW_SHIELD_FORMED = 2;   // Shield formed (blocking)
    public static final int ROW_RUNNING = 3;         // Running animation
    public static final int ROW_GRAVY = 4;           // Gravy dumping
    public static final int ROW_HIT = 5;             // Taking damage

    // ── Frame counts per row ──────────────────────────────────
    private static final int[] FRAME_COUNTS = { 5, 5, 1, 5, 4, 2 };

    // -------------------------------------------------------
    // BOSS SETTINGS
    // -------------------------------------------------------
    public static final float BOSS_RIGHT_MARGIN = 10f;
    public static final float FOLLOW_Y_DELAY = 0.05f;
    public static final float BOSS_SCROLL_SPEED = 0.8f;

    // ── Wander settings ───────────────────────────────────────
    private static final float WANDER_SPEED = 0.1f;
    private static final int WANDER_CHANGE_MIN = 40;
    private static final int WANDER_CHANGE_MAX = 120;

    // ── Timing constants (in ticks, assuming 200 ticks/sec) ──
    private static final int FOLLOW_TICKS = 4 * 200;      // 4 seconds
    private static final int SHOOT_TICKS = 5 * 200;       // 5 seconds shooting
    private static final int SHIELD_DURATION = 6 * 200;   // 6 seconds shield active
    private static final int SHIELD_COOLDOWN = 4 * 200;   // 4 seconds cooldown
    private static final int GRAVY_TICKS = 6 * 200;       // 6 seconds dumping
    private static final int HIT_ANIM_TICKS = 90;         // Hit animation duration

    private static final int BULLET_DELAY = 200;          // 1 second between bullets
    private static final int MAX_BULLETS = 10;            // Bullets per shoot phase
    private static final int MAX_GRAVY = 3;               // Always 3 gravy piles
    private static final int SHIELD_MAX_HITS = 4;         // Shield breaks after 4 hits

    // ── Animation speeds (ticks per frame) ───────────────────
    public static final int ANI_SPEED_RUNNING = 20;
    public static final int ANI_SPEED_SHOOT = 10;

    public static final int ANI_SPEED_HIT = 20;

    // ── Skill 2 (Gravy) animation phase durations ────────────
    private static final int S2_COL0_TICKS = 30;
    private static final int S2_LOOP_SPEED = 15;
    private static final int S2_COL3_TICKS = 30;
    private static final int S2_GRAVY_DELAY = 60;

    // ── Shield formation animation ────────────────────────────
    private static final int SHIELD_FORM_COL0_TICKS = 30;
    private static final int SHIELD_FORM_LOOP_SPEED = 15;
    private static final int SHIELD_FORM_DURATION = 90;

    // ── Vertical spacing ──────────────────────────────────────
    private static final float VERTICAL_GAP = 50f;

    // ── Road lane boundaries ──────────────────────────────────
    private static final float LANE_TOP_PRE_SCALE = 10f;
    private static final float LANE_BOTTOM_PRE_SCALE = 17f;
    private static final float ALIGN_THRESHOLD = 4f * Game.SCALE;

    // ── Position & size ───────────────────────────────────────
    private float x, y;
    private final int width, height;
    private final float lockedX;
    private final float laneTopY;
    private final float laneBotY;

    // ── Animations ────────────────────────────────────────────
    private final BufferedImage[][] frames;
    private int currentRow = ROW_RUNNING;
    private int aniTick = 0;
    private int aniIndex = 0;

    // ── State machine ─────────────────────────────────────────
    public enum BossState { FOLLOW, SHOOT, SHIELD_FORMING, SHIELD_ACTIVE, SHIELD_COOLDOWN, GRAVY_DUMP, HIT }
    private BossState state = BossState.FOLLOW;
    private BossState stateAfterHit = BossState.FOLLOW;
    private int stateTick = 0;

    // ── Shoot variables ───────────────────────────────────────
    private boolean shootFiring = false;
    private float shootTargetY = 0f;
    private int bulletsFired = 0;
    private int bulletTick = 0;

    // ── Shield variables ──────────────────────────────────────
    private boolean shieldActive = false;
    private int shieldHitsRemaining = SHIELD_MAX_HITS;
    private int shieldTick = 0;
    private int shieldFormTick = 0;
    private int shieldFormLoopTick = 0;
    private int shieldFormLoopIndex = 1;
    private int shieldFormPhase = 0;

    // ── Gravy variables ───────────────────────────────────────
    private int gravyLaid = 0;
    private int gravyTick = 0;
    private int gravyLoopTick = 0;
    private int gravyLoopIndex = 1;
    private int gravySpawnTick = 0;
    private int gravyPhase = 0;

    // ── Hit animation ─────────────────────────────────────────
    private int hitTick = 0;

    // ── Wander variables ──────────────────────────────────────
    private float wanderDir = 0f;
    private int wanderChangeTick = 0;
    private int wanderInterval = 80;

    // ── Spawned objects ───────────────────────────────────────
    private final List<GravySauce.BossProjectile> bullets = new ArrayList<>();
    private final List<GravySauce> gravySauces = new ArrayList<>();
    private BufferedImage[] bulletFrames;

    private BufferedImage[] shieldFormFrames;   // Row 1 - forming animation
    private BufferedImage[] shieldFormedFrames; // Row 2 - formed shield (static)
    private BufferedImage gravyImage;

    private final Random rng = new Random();
    private int shieldAniIndex = 0;
    private static final int SHIELD_ANI_SPEED = 15;
    // ── Combo system ← ADD THESE ───────────────────────────────
    private static final float SHIELD_COMBO_CHANCE = 0.3f;  // 30% chance during shoot
    private boolean comboShieldTriggered = false;  // Track if shield activated during this shoot phase
    private int comboShieldCheckTick = 0;
    private static final int COMBO_SHIELD_CHECK_DELAY = 100;  // Check after 0.5s of shooting

    private boolean isComboShieldForming = false;
    // ─────────────────────────────────────────────────────────
    public Boss3(float startX, float startY) {
        this.width = (int)(FRAME_W * Game.SCALE);
        this.height = (int)(FRAME_H * Game.SCALE);

        this.lockedX = Game.GAME_WIDTH - width - (BOSS_RIGHT_MARGIN * Game.SCALE);
        this.laneTopY = LANE_TOP_PRE_SCALE * Game.TILES_SIZE;
        this.laneBotY = LANE_BOTTOM_PRE_SCALE * Game.TILES_SIZE - height;

        this.x = lockedX;
        this.y = clampY(startY);

        this.frames = new BufferedImage[ROWS][SHEET_COLS];
        loadFrames();

        wanderInterval = WANDER_CHANGE_MIN + rng.nextInt(WANDER_CHANGE_MAX - WANDER_CHANGE_MIN);
        wanderDir = rng.nextBoolean() ? 1f : -1f;
    }

    private void loadFrames() {
        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.BOSS3_ATLAS);
        if (sheet == null) {
            System.err.println("[Boss3 Red] Could not load " + LoadSave.BOSS3_ATLAS);
            return;
        }

        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < SHEET_COLS; col++) {
                // ── Special case: ROW_GRAVY has 5 columns total ─────
                int maxCols = (row == ROW_GRAVY) ? 5 : FRAME_COUNTS[row];
                if (col < maxCols)
                    frames[row][col] = sheet.getSubimage(
                            col * FRAME_W, row * FRAME_H, FRAME_W, FRAME_H);
            }

        // Load bullet frames from row 0
        bulletFrames = new BufferedImage[FRAME_COUNTS[ROW_SHOOT]];
        for (int i = 0; i < bulletFrames.length; i++)
            bulletFrames[i] = frames[ROW_SHOOT][i];

        // Load shield forming frames from row 1
        shieldFormFrames = new BufferedImage[FRAME_COUNTS[ROW_SHIELD_FORM]];
        for (int i = 0; i < FRAME_COUNTS[ROW_SHIELD_FORM]; i++) {
            shieldFormFrames[i] = frames[ROW_SHIELD_FORM][i];
        }

        // Load formed shield frames from row 2
        shieldFormedFrames = new BufferedImage[FRAME_COUNTS[ROW_SHIELD_FORMED]];
        for (int i = 0; i < FRAME_COUNTS[ROW_SHIELD_FORMED]; i++) {
            shieldFormedFrames[i] = frames[ROW_SHIELD_FORMED][i];
        }



        // Fallback to column 3
        gravyImage = frames[ROW_GRAVY][4];

    }

    private float clampY(float candidateY) {
        if (candidateY < laneTopY) candidateY = laneTopY;
        if (candidateY > laneBotY) candidateY = laneBotY;
        return candidateY;
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update(float jeepX, float jeepY, float jeepWidth, float jeepHeight) {
        updateBullets();
        updateGravy();
        updateStateMachine(jeepX, jeepY);
        updateAnimation();
    }
    private BossState lastSkillUsed = BossState.FOLLOW;  // Track skill rotationws
    private void updateStateMachine(float jeepX, float jeepY) {
        stateTick++;
        x = lockedX;

        switch (state) {

            case FOLLOW:
                currentRow = ROW_RUNNING;
                wanderY();
                if (stateTick >= FOLLOW_TICKS) {
                    // Always shoot on first rotation, then vary
                    if (lastSkillUsed != BossState.SHOOT) {
                        enterShoot();
                        lastSkillUsed = BossState.SHOOT;  // Track last skill
                    } else {
                        // After shooting, pick between shield or gravy
                        if (rng.nextBoolean()) {
                            enterShieldForming();
                            lastSkillUsed = BossState.SHIELD_FORMING;
                        } else {
                            enterGravyDump();
                            lastSkillUsed = BossState.GRAVY_DUMP;
                        }
                    }
                }
                break;

            case SHOOT:
                currentRow = ROW_RUNNING;

                if (!shootFiring) {
                    shootTargetY = jeepY;
                    followJeepY(shootTargetY);

                    float bossCentreY = y + height / 2f;
                    float diff = Math.abs(bossCentreY - shootTargetY);
                    if (diff <= ALIGN_THRESHOLD) {
                        y = clampY(shootTargetY - height / 2f);
                        shootFiring = true;
                        bulletTick = BULLET_DELAY;

                        comboShieldTriggered = false;
                        comboShieldCheckTick = 0;
                    }
                } else {
                    followJeepY(jeepY);

                    bulletTick++;
                    if (bulletTick >= BULLET_DELAY && bulletsFired < MAX_BULLETS) {
                        fireBullet();
                        bulletTick = 0;
                        bulletsFired++;
                    }

                    // Combo: Activate shield while shooting
                    if (!comboShieldTriggered && !shieldActive) {
                        comboShieldCheckTick++;
                        if (comboShieldCheckTick >= COMBO_SHIELD_CHECK_DELAY) {
                            if (rng.nextFloat() < SHIELD_COMBO_CHANCE) {
                                System.out.println("[Boss3] 🛡️ COMBO: Activating shield while shooting!");
                                activateComboShield();
                                comboShieldTriggered = true;
                            }
                            comboShieldCheckTick = 0;
                        }
                    }

                    // ← ADD THIS - Update shield animation during combo
                    if (isComboShieldForming) {
                        updateShieldFormSequence();
                    }

                    // Update shield if active during shooting
                    if (shieldActive) {
                        shieldTick++;
                        if (shieldTick >= SHIELD_DURATION) {
                            deactivateShield();
                        }
                    }
                }

                if (stateTick >= SHOOT_TICKS) enterFollow();
                break;

            case SHIELD_FORMING:
                currentRow = ROW_RUNNING;
                wanderY();
                updateShieldFormSequence();
                if (shieldFormPhase >= 2) {
                    enterShieldActive();
                }
                break;

            case SHIELD_ACTIVE:
                currentRow = ROW_RUNNING;
                wanderY();
                shieldTick++;
                if (shieldTick >= SHIELD_DURATION) {
                    enterShieldCooldown();
                }
                break;

            case SHIELD_COOLDOWN:
                currentRow = ROW_RUNNING;
                wanderY();
                shieldTick++;
                if (shieldTick >= SHIELD_COOLDOWN) {
                    enterFollow();
                }
                break;

            case GRAVY_DUMP:
                wanderY();
                updateGravySequence();
                if (stateTick >= GRAVY_TICKS) enterFollow();
                break;

            case HIT:
                currentRow = ROW_HIT;
                hitTick++;
                if (hitTick >= HIT_ANIM_TICKS) {
                    hitTick = 0;
                    state = stateAfterHit;
                    stateTick = 0;
                }
                break;
        }
    }
    private void followJeepY(float jeepCenterY) {
        float targetTopY = jeepCenterY - height / 2f;
        y += (targetTopY - y) * FOLLOW_Y_DELAY;
        y = clampY(y);
    }

    private void wanderY() {
        wanderChangeTick++;
        if (wanderChangeTick >= wanderInterval) {
            wanderChangeTick = 0;
            wanderInterval = WANDER_CHANGE_MIN + rng.nextInt(WANDER_CHANGE_MAX - WANDER_CHANGE_MIN);
            int roll = rng.nextInt(3);
            wanderDir = (roll == 0) ? -1f : (roll == 1) ? 1f : 0f;
        }

        float nextY = y + wanderDir * WANDER_SPEED * Game.SCALE;
        if (nextY < laneTopY) { nextY = laneTopY; wanderDir = 1f; }
        else if (nextY > laneBotY) { nextY = laneBotY; wanderDir = -1f; }
        y = nextY;
    }

    // ── SHIELD FORMATION SEQUENCE ─────────────────────────────
    private void updateShieldFormSequence() {
        shieldFormTick++;

        switch (shieldFormPhase) {
            case 0:
                if (shieldFormTick >= SHIELD_FORM_COL0_TICKS) {
                    shieldFormTick = 0;
                    shieldFormPhase = 1;
                }
                break;

            case 1:
                shieldFormLoopTick++;
                if (shieldFormLoopTick >= SHIELD_FORM_LOOP_SPEED) {
                    shieldFormLoopTick = 0;
                    shieldFormLoopIndex = (shieldFormLoopIndex == 1) ? 2 : 1;
                    shieldAniIndex = shieldFormLoopIndex;
                }

                if (shieldFormTick >= SHIELD_FORM_DURATION) {
                    shieldFormTick = 0;
                    shieldFormPhase = 2;
                }
                break;

            case 2:
                shieldActive = true;
                shieldHitsRemaining = SHIELD_MAX_HITS;
                shieldFormPhase = 3;
                isComboShieldForming = false;  // ← ADD THIS
                break;

            case 3:
                shieldAniIndex = 0;
                break;
        }
    }
    // ── GRAVY SEQUENCE ────────────────────────────────────────
    private void updateGravySequence() {
        gravyTick++;

        switch (gravyPhase) {
            case 0:
                currentRow = ROW_GRAVY;
                aniIndex = 0;
                if (gravyTick >= S2_COL0_TICKS) {
                    gravyTick = 0;
                    gravyLoopTick = 0;
                    gravyLoopIndex = 1;
                    gravySpawnTick = 0;
                    gravyLaid = 0;
                    gravyPhase = 1;
                }
                break;

            case 1:
                currentRow = ROW_GRAVY;
                gravyLoopTick++;
                if (gravyLoopTick >= S2_LOOP_SPEED) {
                    gravyLoopTick = 0;
                    gravyLoopIndex = (gravyLoopIndex == 1) ? 2 : 1;
                }
                aniIndex = gravyLoopIndex;

                gravySpawnTick++;
                if (gravySpawnTick >= S2_GRAVY_DELAY && gravyLaid < MAX_GRAVY) {
                    layGravyVertical(gravyLaid);
                    gravySpawnTick = 0;
                }

                if (gravyLaid >= MAX_GRAVY) {
                    gravyTick = 0;
                    gravyPhase = 2;
                }
                break;

            // ── Phase 2: closing (col 3) ────────────────────────
            case 2:
                currentRow = ROW_GRAVY;
                aniIndex = 3;
                if (gravyTick >= S2_COL3_TICKS) {
                    gravyTick = 0;
                    currentRow = ROW_RUNNING;

                    // ── Reset animation counters ← ADD THESE ────────────
                    aniIndex = 0;      // Start running animation from frame 0
                    aniTick = 0;       // Reset animation timer

                    gravyPhase = 3;

                    System.out.println("[Boss3] Gravy phase 3: returning to running animation");
                }
                break;

            case 3:
                currentRow = ROW_RUNNING;
                break;
        }
    }

    // ── Transitions ───────────────────────────────────────────
    // Combo shield activation

    private void activateComboShield() {
        shieldHitsRemaining = SHIELD_MAX_HITS;
        shieldFormPhase = 0;
        shieldFormTick = 0;
        shieldAniIndex = 0;
        shieldTick = 0;
        isComboShieldForming = true;  // ← ADD THIS
        System.out.println("[Boss3] Shield forming during SHOOT combo!");
    }
    // Finalize shield after forming
    private void finalizeShieldActivation() {
        shieldActive = true;
        shieldTick = 0;
        System.out.println("[Boss3] Combo shield active with " + shieldHitsRemaining + " hits!");
    }

    // Deactivate shield
    private void deactivateShield() {
        shieldActive = false;
        shieldFormPhase = 0;
        shieldTick = 0;
        System.out.println("[Boss3] Shield deactivated (time expired)");
    }
    private void enterShoot() {
        state = BossState.SHOOT;
        stateTick = 0;
        bulletsFired = 0;
        bulletTick = 0;
        shootFiring = false;
        shootTargetY = y;

        // Reset combo shield tracking
        comboShieldTriggered = false;
        comboShieldCheckTick = 0;

        System.out.println("[Boss3] 💥 Entering SHOOT phase!");
    }

    private void enterShieldForming() {
        state = BossState.SHIELD_FORMING;
        stateTick = 0;
        shieldFormTick = 0;
        shieldFormLoopTick = 0;
        shieldFormLoopIndex = 1;
        shieldFormPhase = 0;
        shieldAniIndex = 0;  // Reset to first formation frame
        currentRow = ROW_RUNNING;
        System.out.println("[Boss3] 🛡️ Forming Shield...");
    }

    private void enterShieldActive() {
        state = BossState.SHIELD_ACTIVE;
        stateTick = 0;
        shieldTick = 0;
        shieldActive = true;
        shieldHitsRemaining = SHIELD_MAX_HITS;
        currentRow = ROW_SHIELD_FORMED;
        System.out.println("[Boss3] 🛡️ Shield Active! (" + SHIELD_MAX_HITS + " hits to break)");
    }

    private void enterShieldCooldown() {
        state = BossState.SHIELD_COOLDOWN;
        stateTick = 0;
        shieldTick = 0;
        shieldActive = false;
        currentRow = ROW_RUNNING;
        System.out.println("[Boss3] Shield cooling down...");
    }

    private void enterGravyDump() {
        state = BossState.GRAVY_DUMP;
        stateTick = 0;
        gravyLaid = 0;
        gravyTick = 0;
        gravyLoopTick = 0;
        gravyLoopIndex = 1;
        gravySpawnTick = 0;
        gravyPhase = 0;
        currentRow = ROW_GRAVY;
        System.out.println("[Boss3] 💧 Dumping Gravy!");
    }

    private void enterFollow() {
        state = BossState.FOLLOW;
        stateTick = 0;
        shootFiring = false;
        bulletsFired = 0;
        bulletTick = 0;
        currentRow = ROW_RUNNING;
        System.out.println("[Boss3] 🏃 Back to FOLLOW state");
    }

    public void triggerHit() {
        if (state == BossState.HIT) return;

        if (shieldActive) {
            shieldHitsRemaining--;
            System.out.println("[Boss3] 🛡️ Shield hit! Remaining: " + shieldHitsRemaining);
            if (shieldHitsRemaining <= 0) {
                shieldActive = false;
                System.out.println("[Boss3] 💥 Shield broken!");
            }
            return;
        }

        stateAfterHit = state;
        state = BossState.HIT;
        hitTick = 0;
        stateTick = 0;
        aniIndex = 0;
    }

    // ── Bullet & Gravy Helpers ─────────────────────────────────
    private void fireBullet() {
        float bx = x;
        float bulletH = GravySauce.BossProjectile.FRAME_H * Game.SCALE;
        float byCentre = y + height / 2f - bulletH / 2f;

        float bulletTopLimit = laneTopY;
        float bulletBotLimit = LANE_BOTTOM_PRE_SCALE * Game.TILES_SIZE - bulletH;
        if (byCentre < bulletTopLimit) byCentre = bulletTopLimit;
        if (byCentre > bulletBotLimit) byCentre = bulletBotLimit;

        bullets.add(new GravySauce.BossProjectile(bx, byCentre, bulletFrames));
        System.out.println("[Boss3] 💥 Bullet " + (bulletsFired + 1) + " fired!");
    }

    private void layGravyVertical(int gravyIndex) {
        float gravyH = GravySauce.PILE_H * Game.SCALE;
        float gap = VERTICAL_GAP * Game.SCALE;

        float colCentreY = y + height / 2f;
        float offsetY = (gravyIndex - 1) * (gravyH + gap);

        float px = x + width * 0.25f;
        float py = colCentreY + offsetY - gravyH / 2f;

        float gravyTop = laneTopY;
        float gravyBot = LANE_BOTTOM_PRE_SCALE * Game.TILES_SIZE - gravyH;
        if (py < gravyTop) py = gravyTop;
        if (py > gravyBot) py = gravyBot;

        gravySauces.add(new GravySauce(px, py, gravyImage));
        gravyLaid++;
        System.out.println("[Boss3] 💧 Gravy " + (gravyIndex + 1) + " dumped!");
    }

    private void updateBullets() {
        bullets.removeIf(b -> { b.update(); return !b.isActive(); });
    }

    private void updateGravy() {
        gravySauces.removeIf(g -> { g.update(BOSS_SCROLL_SPEED * Game.SCALE); return !g.isActive(); });
    }

    // ── Animation ─────────────────────────────────────────────
    private void updateAnimation() {
        // Skip auto-animation for SHIELD_FORMING
        if (state == BossState.SHIELD_FORMING) {
            return;
        }

        // ── Skip GRAVY_DUMP only during phases 0-2 ← CHANGE THIS ──
        if (state == BossState.GRAVY_DUMP && gravyPhase < 3) {
            return;  // Manual control during gravy drop phases
        }
        // Once gravyPhase == 3, animation resumes normally

        int speed;
        switch (currentRow) {
            case ROW_SHOOT: speed = ANI_SPEED_SHOOT; break;
            case ROW_HIT: speed = ANI_SPEED_HIT; break;
            default: speed = ANI_SPEED_RUNNING; break;
        }

        aniTick++;
        if (aniTick >= speed) {
            aniTick = 0;
            int maxFrames = FRAME_COUNTS[currentRow];
            aniIndex = (aniIndex + 1) % maxFrames;
        }
    }

    // ─────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────
    public void render(Graphics g) {
        List<GravySauce> gravyCopy = new ArrayList<>(gravySauces);
        for (GravySauce gravy : gravyCopy) gravy.render(g);

        List<GravySauce.BossProjectile> bulletsCopy = new ArrayList<>(bullets);
        for (GravySauce.BossProjectile bullet : bulletsCopy) bullet.render(g);

        // ── Draw shield (including combo shield) ─────────────────
        if ((state == BossState.SHIELD_FORMING || isComboShieldForming) && shieldFormFrames != null) {
            // Drawing forming shield (row 1)
            BufferedImage shieldFrame = shieldFormFrames[shieldAniIndex];
            if (shieldFrame != null) {
                int shieldW = (int)(FRAME_W * Game.SCALE);
                int shieldH = (int)(FRAME_H * Game.SCALE);
                int shieldX = (int)x - (int)(55 * Game.SCALE);
                int shieldY = (int)y;
                g.drawImage(shieldFrame, shieldX, shieldY, shieldW, shieldH, null);
            }
        } else if (shieldActive && shieldFormedFrames != null) {
            // Drawing formed shield (row 2) - static
            BufferedImage shieldFrame = shieldFormedFrames[0];
            if (shieldFrame != null) {
                int shieldW = (int)(FRAME_W * Game.SCALE);
                int shieldH = (int)(FRAME_H * Game.SCALE);
                int shieldX = (int)x - (int)(55 * Game.SCALE);
                int shieldY = (int)y;
                g.drawImage(shieldFrame, shieldX, shieldY, shieldW, shieldH, null);
                drawShieldHitCounter(g, shieldX, shieldY, shieldW, shieldH);
            }
        }

        // Draw boss
        int safeIndex = Math.min(aniIndex, FRAME_COUNTS[currentRow] - 1);
        BufferedImage frame = frames[currentRow][safeIndex];
        if (frame != null)
            g.drawImage(frame, (int)x, (int)y, width, height, null);
    }

    // ── Shield hit counter display ← NEW METHOD ─────────────────
    // ── Shield hit counter display ← SIMPLIFIED ─────────────────
    private void drawShieldHitCounter(Graphics g, int shieldX, int shieldY,
                                      int shieldW, int shieldH) {
        if (shieldHitsRemaining <= 0) return;

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculate center position
        int centerX = shieldX + shieldW / 2;
        int centerY = shieldY + shieldH / 2;

        String hitText = String.valueOf(shieldHitsRemaining);
        Font font = new Font("Arial", Font.BOLD, (int)(24 * Game.SCALE));  // ← Smaller
        g2d.setFont(font);

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(hitText);
        int textHeight = fm.getAscent();
        int textX = centerX - textWidth / 2;
        int textY = centerY + textHeight / 2 - (int)(15 * Game.SCALE);  // ← Moved up

        // Black outline (2 pixels)
        g2d.setColor(Color.BLACK);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx != 0 || dy != 0) {
                    g2d.drawString(hitText, textX + dx, textY + dy);
                }
            }
        }

        // White number
        g2d.setColor(Color.WHITE);
        g2d.drawString(hitText, textX, textY);
    }
    // ─────────────────────────────────────────────────────────
    // GETTERS
    // ─────────────────────────────────────────────────────────
    private static final float HB_INSET_PERCENT = 0.6f;
    private static final int X_OFFSET = 0;
    private static final int Y_OFFSET = 20;

    public Rectangle getHitbox() {
        int insetX = (int)(width * HB_INSET_PERCENT / 2);
        int insetY = (int)(height * HB_INSET_PERCENT / 2);
        return new Rectangle(
                (int)x + insetX + X_OFFSET,
                (int)y + insetY + Y_OFFSET,
                width - (insetX * 2),
                height - (insetY * 2));
    }

    public List<GravySauce.BossProjectile> getBullets() { return bullets; }
    public List<GravySauce> getGravySauces() { return gravySauces; }
    public float getX() { return x; }
    public float getY() { return y; }
    public boolean isShieldActive() { return shieldActive; }
    public int getShieldHitsRemaining() { return shieldHitsRemaining; }
}