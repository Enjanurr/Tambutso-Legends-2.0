package BossFight.LevelTwo.Red;

import BossFight.LevelTwo.NukeProjectile;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class Boss2 {

    // ── Sprite sheet dimensions ───────────────────────────────
    public static final int SHEET_COLS = 5;
    public static final int FRAME_W    = 110;  // 550 / 5
    public static final int FRAME_H    = 79;   // 316 / 4
    public static final int ROWS       = 4;

    public static final int FRAME_W_SKILL1   = 36;  // 550 / 5
    public static final int FRAME_H_SKILL1    = 34;   // 316 / 4
    public static final int FRAME_W_SKILL2  = 60;  // 550 / 5
    public static final int FRAME_H_SKILL2    = 60;   // 316 / 4

    // ── Row indices ───────────────────────────────────────────
    public static final int ROW_SKILL1  = 0;   // bullet frames only
    public static final int ROW_RUNNING = 1;
    public static final int ROW_SKILL2  = 0;
    public static final int ROW_HIT     = 2;
    public static final int ROW_STUN    = 3;
    // ── Frame counts per row ──────────────────────────────────
    private static final int[] FRAME_COUNTS = { 5, 5, 2, 4};

    // -------------------------------------------------------
    // BOSS SETTINGS  ← ADJUST
    // -------------------------------------------------------
    /** How close (in pre-scale pixels) to the right border the boss sits. */
    public static final float BOSS_RIGHT_MARGIN = 10f;

    /** Lerp factor for vertical follow during Skill 1 targeting (0=no follow, 1=instant). */
    public static final float FOLLOW_Y_DELAY    = 0.05f;

    // ── Tweak 1: wander settings (used in ALL non-Skill1 states) ← ADJUST ──
    /** Speed of random vertical wander (pre-scale px/tick). */
    private static final float WANDER_SPEED     = 0.1f;
    /** Ticks between random direction changes during wander. */
    private static final int   WANDER_CHANGE_MIN = 40;
    private static final int   WANDER_CHANGE_MAX = 120;

    private static final int FOLLOW_TICKS   = 4 * 200; // 4 s
    private static final int SKILL1_TICKS   = 10 * 200; // 4 s window for firing
    private static final int WAIT_TICKS     = 2 * 200; // 2 s wait between phases
    private static final int SKILL2_TICKS   = 6 * 200; // 6 s window for piles
    private static final int HIT_ANIM_TICKS =  90;  // hit animation duration

    private static final int BULLET_DELAY  =  200; // 1 s between bullets
    private static final int MAX_BULLETS   = 10;        // bullets per Skill 1 phase
    private static final int MAX_NUKES    = 3;        // always 3 piles per Skill 2

    // ── Per-row animation speeds (ticks per frame) ← ADJUST ──
    // -------------------------------------------------------
    public static final int ANI_SPEED_RUNNING  = 20;
    public static final int ANI_SPEED_SKILL1   = 10;
    public static final int ANI_SPEED_HIT      = 20;
    public static final int ANI_SPEED_STUN = 15;
    // -------------------------------------------------------

    // Skill 2 animation phase durations (ticks) ← ADJUST ─────
    private static final int S2_COL0_TICKS  = 30;
    private static final int S2_LOOP_SPEED  = 15;
    private static final int S2_COL3_TICKS  = 30;
    private static final int S2_PILE_DELAY  = 60;  // ticks between pile spawns
    // ─────────────────────────────────────────────────────────

    // ── Tweak 2: vertical pile spacing ← ADJUST ──────────────
    // -------------------------------------------------------
    /** Vertical gap between the 3 garbage piles (pre-scale pixels). */
    private static final float PILE_VERTICAL_GAP = 50f;
    // -------------------------------------------------------

    // ── Road lane boundaries (pixel Y, post-scale) ───────────
    private static final float LANE_TOP_PRE_SCALE    = 10f;
    private static final float LANE_BOTTOM_PRE_SCALE = 17f;
    // -------------------------------------------------------

    // ── Scroll speed ──────────────────────────────────────────
    // -------------------------------------------------------
    public static final float BOSS_SCROLL_SPEED = 0.8f;
    // -------------------------------------------------------

    // ── Position & size ───────────────────────────────────────
    private float x, y;
    private final int width, height;

    // ── Computed locked X and lane pixel bounds ───────────────
    private final float lockedX;
    private final float laneTopY;
    private final float laneBotY;

    // ── Animations ────────────────────────────────────────────
    private final BufferedImage[][] frames;
    private int currentRow = ROW_RUNNING;
    private int aniTick    = 0;
    private int aniIndex   = 0;

    // ── State machine ─────────────────────────────────────────
    public enum BossState { FOLLOW, SKILL1, WAIT_AFTER1, SKILL2, WAIT_AFTER2, RANDOM, HIT,STUN  }
    private BossState state         = BossState.FOLLOW;
    private BossState stateAfterHit = BossState.FOLLOW;
    private int       stateTick     = 0;

    // ── Skill 1 bullet tracking ───────────────────────────────
    private int bulletsFired = 0;
    private int bulletTick   = 0;

    // ── Skill 1 reposition sub-phase ─────────────────────────
    /**
     * false = still repositioning (running toward jeep Y)
     * true  = aligned with jeep, now firing
     */
    private boolean skill1Firing       = false;
    /** Cached jeep Y captured when SKILL1 starts — target for repositioning. */
    private float   skill1TargetY      = 0f;
    /**
     * How close (in pixels, post-scale) the boss Y must be to the target
     * before it counts as "aligned" and starts firing.
     */
    // -------------------------------------------------------
    // REPOSITION ALIGNMENT THRESHOLD  ← ADJUST
    // -------------------------------------------------------
    private static final float ALIGN_THRESHOLD = 4f * Game.SCALE;

    // ── Skill 2 pile tracking ─────────────────────────────────
    private int nukesDeployed    = 0;
    private int nukeTick   = 0;
    /**
     * Skill 2 phase:
     *   0 = col 0 (tailgate closed, startup)
     *   1 = loop cols 1-2 (tailgate open, laying piles one by one vertically)
     *   2 = col 3 (tailgate closing, play once)
     *   3 = wait in Running before state timer exits
     */
    private int skill2Phase  = 0;
    private int s2LoopTick   = 0;
    private int s2LoopIndex  = 1;
    private int nukeSpawnTick = 0;

    // ── Hit animation ─────────────────────────────────────────
    private int hitTick = 0;

    // ── Wander (all non-Skill1 states) ────────────────────────
    private float wanderDir        = 0f;
    private int   wanderChangeTick = 0;
    private int   wanderInterval   = 80;

    // ── Spawned objects ───────────────────────────────────────
    // ── Spawned objects ───────────────────────────────────────
    private final List<NukeProjectile.BossProjectile> bullets = new ArrayList<>();
    private final List<NukeProjectile.Nuke>    nukes   = new ArrayList<>();
    private BufferedImage[]            bulletFrames;
    private BufferedImage[]            nukeFrames;

    private final Random rng = new Random();

    private BufferedImage skillSheet1;  // ← NEW: separate sheet for Skill 1
    private BufferedImage skillSheet2;  // ← NEW: separate sheet for Skill 2
   // __________________________
    // Stun Effect
    //_________________
    private boolean stunned = false;
    private int stunTick = 0;
    private static final int STUN_DURATION = 3 * 200; // 3 seconds

    //___________________
    // ── Slow effect constants ← CORRECTED ───────────────────
    private static final int   SLOW_DURATION = 3 * 200;     // 3 seconds at 200 UPS
    private static final float SLOW_SPEED_MULT = 0.5f;      // 50% movement speed
    private static final float SLOW_FIRE_MULT = 1.5f;       // 50% slower firing (1.5x delay multiplier)

    // ── Slow state tracking ────────────────────────────────
    private boolean slowed = false;
    private int slowTick = 0;
    //_______________

    // ─────────────────────────────────────────────────────────
    // ── Jeep position for Skill 2 targeting ───────────────
    private float jeepX = 0f;
    private float jeepY = 0f;
    private float jeepWidth = 0f;
    private float jeepHeight = 0f;

    // ── Skill 2 spawn positioning (relative to jeep) ──────
    private static final float SKILL2_SPAWN_OFFSET_X = -80f;   // spawn left of jeep
    private static final float SKILL2_SPAWN_OFFSET_Y = 0f;     // center on jeep Y

    public Boss2(float startX, float startY) {
        this.width  = (int)(FRAME_W * Game.SCALE);
        this.height = (int)(FRAME_H * Game.SCALE);

        this.lockedX = Game.GAME_WIDTH - width - (BOSS_RIGHT_MARGIN * Game.SCALE);
        this.laneTopY = LANE_TOP_PRE_SCALE * Game.TILES_SIZE;
        this.laneBotY = LANE_BOTTOM_PRE_SCALE * Game.TILES_SIZE - height;

        this.x = lockedX;
        this.y = clampY(startY);

        this.frames = new BufferedImage[ROWS][SHEET_COLS];
        loadFrames();

        wanderInterval = WANDER_CHANGE_MIN + rng.nextInt(WANDER_CHANGE_MAX - WANDER_CHANGE_MIN);
        wanderDir      = rng.nextBoolean() ? 1f : -1f;
    }

    private void loadFrames() {
        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.BOSS2_ATLAS);
        if (sheet == null) {
            System.err.println("[Boss1] Could not load " + LoadSave.BOSS2_ATLAS);
            return;
        }
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < SHEET_COLS; col++) {
                int maxCols = (row == ROW_SKILL2) ? 5 : FRAME_COUNTS[row];
                if (col < maxCols)
                    frames[row][col] = sheet.getSubimage(
                            col * FRAME_W, row * FRAME_H, FRAME_W, FRAME_H);
            }

        // ── Load Skill 1 from separate sheet ─────────────────
        skillSheet1 = LoadSave.getSpriteAtlas(LoadSave.BOSS2_SKILL1);  // ← NEW constant
        if (skillSheet1 != null) {
            bulletFrames = new BufferedImage[4];  // 4 frames instead of 5
            for (int i = 0; i < 4; i++) {
                bulletFrames[i] = skillSheet1.getSubimage(
                        i * FRAME_W_SKILL1, 0, FRAME_W_SKILL1, FRAME_H_SKILL1);  // Row 0 only
            }
            System.out.println("✓ Loaded Skill 1 frames from separate sheet");
        } else {
            System.err.println("❌ Could not load " + LoadSave.BOSS2_SKILL1);
        }

        skillSheet2 = LoadSave.getSpriteAtlas(LoadSave.BOSS2_SKILL2);  // ← NEW constant
        if (skillSheet2 != null) {
            nukeFrames = new BufferedImage[18];  // 4 frames instead of 5
            for (int i = 0; i < 18; i++) {
                nukeFrames[i] = skillSheet2.getSubimage(
                        i * FRAME_W_SKILL2, 0, FRAME_W_SKILL2, FRAME_H_SKILL2);  // Row 0 only
            }
            System.out.println("✓ Loaded Skill 2 frames from separate sheet");
        } else {
            System.err.println("❌ Could not load " + LoadSave.BOSS2_SKILL2);
        }
    }

    private void updateStunState() {
        currentRow = ROW_STUN;

        // Update stun animation
        aniTick++;
        if (aniTick >= ANI_SPEED_STUN) {
            aniTick = 0;
            aniIndex = (aniIndex + 1) % FRAME_COUNTS[ROW_STUN];
        }

        stunTick++;

        if (stunTick >= STUN_DURATION) {
            stunned = false;
            stunTick = 0;
            state = stateAfterHit;
            stateTick = 0;
            aniIndex = 0;
            currentRow = ROW_RUNNING;
            System.out.println("[Boss2] Stun ended");
        }
    }

    private float clampY(float candidateY) {
        if (candidateY < laneTopY)  candidateY = laneTopY;
        if (candidateY > laneBotY)  candidateY = laneBotY;
        return candidateY;
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update(float jeepX, float jeepY, float jeepWidth, float jeepHeight) {
        this.jeepX = jeepX;
        this.jeepY = jeepY;
        this.jeepWidth = jeepWidth;
        this.jeepHeight = jeepHeight;

        updateBullets();
        updateNukes();
        updateStateMachine(jeepX, jeepY);
        updateAnimation();
    }

    // ─────────────────────────────────────────────────────────
    // Jeep skill effect
    // ─────────────────────────────────────────────────────────

    // ── Public API for applying slow effect ─────────────────
    // ── Public API for applying slow effect ─────────────────
    public void applySlowEffect() {
        slowed = true;
        slowTick = 0;
        System.out.println("[Boss1] Slowed! Duration: 3 seconds. Movement & firing reduced by 50%.");
    }



    public boolean isSlowed() {
        return slowed;
    }
    public void applyStun() {
        if (state == BossState.STUN || stunned) return;
        stateAfterHit = state;
        state = BossState.STUN;
        stunned = true;
        stunTick = 0;
        aniIndex = 0;
        aniTick = 0;
        currentRow = ROW_STUN;
        System.out.println("[Boss2] ⚡ Stunned! Duration: 3 seconds.");
    }
    private void updateStateMachine(float jeepX, float jeepY) {
        if (stunned) {
            updateStunState();
            return;
        }


        stateTick++;
        x = lockedX;

        switch (state) {

            // ── FOLLOW: wander freely (Tweak 1) ───────────────
            case FOLLOW:
                currentRow = ROW_RUNNING;
                wanderY();                          // free movement, not targeting
                if (stateTick >= FOLLOW_TICKS) enterSkill1();
                break;

            // ── SKILL1: reposition first, then fire ───────────
            case SKILL1:
                currentRow = ROW_RUNNING;   // running animation throughout

                if (!skill1Firing) {
                    // ── Sub-phase A: run toward target Y ─────────
                    // jeepY is now the CENTRE of the jeep hitbox (passed from BossFightState)
                    skill1TargetY = jeepY;
                    followJeepY(skill1TargetY);

                    // Compare boss centre to jeep hitbox centre
                    float bossCentreY = y + height / 2f;
                    float diff = Math.abs(bossCentreY - skill1TargetY);
                    if (diff <= ALIGN_THRESHOLD) {
                        // Snap boss centre exactly onto target and begin firing
                        y             = clampY(skill1TargetY - height / 2f);
                        skill1Firing  = true;
                        bulletTick    = BULLET_DELAY; // fire first bullet immediately
                    }
                } else {
                    // ── Sub-phase B: fire bullets at jeep hitbox centre ──
                    // Keep gently tracking so we stay on target if jeep moves
                    followJeepY(jeepY);

                    bulletTick++;
                    if (bulletTick >= BULLET_DELAY && bulletsFired < MAX_BULLETS) {
                        fireBullet();
                        bulletTick   = 0;
                        bulletsFired++;
                    }
                }

                if (stateTick >= SKILL1_TICKS) enterWait(BossState.WAIT_AFTER1);
                break;

            // ── WAIT after Skill 1: wander freely (Tweak 1) ──
            case WAIT_AFTER1:
                currentRow = ROW_RUNNING;
                wanderY();
                if (stateTick >= WAIT_TICKS) enterSkill2();
                break;

            // ── SKILL2: wander freely while laying (Tweak 1) ─
            // (Red Jeep deploys stun nukes instead of garbage piles)
            case SKILL2:
                wanderY();                          // free movement, not targeting
                updateSkill2Sequence();
                if (stateTick >= SKILL2_TICKS) enterWait(BossState.WAIT_AFTER2);
                break;

            // ── WAIT after Skill 2: wander freely (Tweak 1) ──
            case WAIT_AFTER2:
                currentRow = ROW_RUNNING;
                wanderY();
                if (stateTick >= WAIT_TICKS) enterRandom();
                break;

            // ── RANDOM: Choose next skill and IMMEDIATELY transition ──
            case RANDOM:
                // ✅ FIX: Don't stay in RANDOM state, pick and transition once
                if (rng.nextBoolean()) {
                    enterSkill1();
                } else {
                    enterSkill2();
                }
                break;

            case HIT:
                currentRow = ROW_HIT;
                hitTick++;
                if (hitTick >= HIT_ANIM_TICKS) {
                    hitTick   = 0;
                    state     = stateAfterHit;
                    stateTick = 0;
                }
                break;
            case STUN:
                // Stun handled in updateStunState()
                break;
        }
    }

    // ── Vertical movement ─────────────────────────────────────
    /** Smooth lerp — aligns the BOSS'S CENTRE to the jeep hitbox centre Y. */
    /** Smooth lerp — aligns the BOSS'S CENTRE to the jeep hitbox centre Y. */
    /** Smooth lerp — aligns the BOSS'S CENTRE to the jeep hitbox centre Y. */
    private void followJeepY(float jeepCenterY) {
        // ── Apply slow multiplier to lerp speed ────────────────
        float baseLerp = FOLLOW_Y_DELAY;
        float effectiveLerp = slowed ? baseLerp * SLOW_SPEED_MULT : baseLerp;

        // Convert: we want (y + height/2) to approach jeepCenterY
        float targetTopY = jeepCenterY - height / 2f;
        y += (targetTopY - y) * effectiveLerp;
        y = clampY(y);
    }

    /** Free random wander — used in all other states. */
    /** Free random wander — used in all other states. */
    /** Free random wander — used in all other states. */
    private void wanderY() {
        wanderChangeTick++;
        if (wanderChangeTick >= wanderInterval) {
            wanderChangeTick = 0;
            wanderInterval = WANDER_CHANGE_MIN + rng.nextInt(WANDER_CHANGE_MAX - WANDER_CHANGE_MIN);
            int roll = rng.nextInt(3);
            wanderDir = (roll == 0) ? -1f : (roll == 1) ? 1f : 0f;
        }

        // ── Apply slow multiplier to movement speed ────────────
        float baseSpeed = WANDER_SPEED * Game.SCALE;
        float effectiveSpeed = slowed ? baseSpeed * SLOW_SPEED_MULT : baseSpeed;

        float nextY = y + wanderDir * effectiveSpeed;
        if (nextY < laneTopY) { nextY = laneTopY; wanderDir = 1f; }
        else if (nextY > laneBotY) { nextY = laneBotY; wanderDir = -1f; }
        y = nextY;
    }

    // ─────────────────────────────────────────────────────────
    // SKILL 2 SEQUENCE
    //
    // Phase 0: col 0 — tailgate closed (startup)
    // Phase 1: loop cols 1-2 — spawn 3 piles VERTICALLY with equal spacing
    // Phase 2: col 3 — tailgate closing (play once)
    // Phase 3: idle in Running until SKILL2_TICKS expires
    // ─────────────────────────────────────────────────────────
    private void updateSkill2Sequence() {
        nukeTick++;

        switch (skill2Phase) {

            // ── Phase 0: col 0 — closed start ────────────────
            case 0:
                currentRow = ROW_SKILL2;
                aniIndex   = 0;
                if (nukeTick >= S2_COL0_TICKS) {
                    nukeTick      = 0;
                    s2LoopTick    = 0;
                    s2LoopIndex   = 1;
                    nukeSpawnTick = 0;
                    nukesDeployed     = 0;
                    skill2Phase   = 1;
                }
                break;

            // ── Phase 1: loop cols 1-2, spawn piles vertically ─
            case 1:
                currentRow = ROW_SKILL2;
                s2LoopTick++;
                if (s2LoopTick >= S2_LOOP_SPEED) {
                    s2LoopTick  = 0;
                    s2LoopIndex = (s2LoopIndex == 1) ? 2 : 1;
                }
                aniIndex = s2LoopIndex;

                nukeSpawnTick++;
                if (nukeSpawnTick >= S2_PILE_DELAY && nukesDeployed < MAX_NUKES) {
                    layNukesVertical(nukesDeployed);  // Tweak 2: vertical positioning
                    nukeSpawnTick = 0;
                }

                if (nukesDeployed >= MAX_NUKES) {
                    nukeTick    = 0;
                    skill2Phase = 2;
                }
                break;

            // ── Phase 2: col 3 — tailgate closing ────────────
            case 2:
                currentRow = ROW_SKILL2;
                aniIndex   = 3;
                if (nukeTick >= S2_COL3_TICKS) {
                    nukeTick   = 0;
                    currentRow = ROW_RUNNING;
                    aniIndex   = 0;
                    skill2Phase = 3;
                }
                break;

            // ── Phase 3: Running, let SKILL2_TICKS expire ────
            case 3:
                currentRow = ROW_RUNNING;
                break;
        }
    }

    // ── Transitions ───────────────────────────────────────────
    private void enterSkill1() {
        state         = BossState.SKILL1;
        stateTick     = 0;
        bulletsFired  = 0;
        bulletTick    = 0;
        skill1Firing  = false;   // always start with repositioning
        skill1TargetY = y;       // will be updated to real jeep Y on first tick
    }

    private void enterSkill2() {
        state         = BossFight.LevelTwo.Red.Boss2.BossState.SKILL2;
        stateTick     = 0;
        nukesDeployed = 0;
        nukeTick      = 0;
        s2LoopTick    = 0;
        s2LoopIndex   = 1;
        nukeSpawnTick = 0;
        skill2Phase   = 0;
        currentRow    = ROW_SKILL2;
    }

    private void enterWait(BossState next) {
        state            = next;
        stateTick        = 0;
        wanderChangeTick = 0;
        wanderInterval   = WANDER_CHANGE_MIN + rng.nextInt(WANDER_CHANGE_MAX - WANDER_CHANGE_MIN);
        wanderDir        = rng.nextBoolean() ? 1f : -1f;
    }

    private void enterRandom() {
        state     = BossState.RANDOM;
        stateTick = 0;
    }

    public void triggerHit() {
        if (state == BossState.HIT || state == BossState.STUN) return;
        stateAfterHit = state;
        state = BossState.HIT;
        hitTick = 0;
        stateTick = 0;
        aniIndex = 0;
    }

    // ─────────────────────────────────────────────────────────
    // PROJECTILE & PILE HELPERS
    // ─────────────────────────────────────────────────────────
    /**
     * Fires a bullet from the left edge of the boss, vertically centred on the boss
     * (which during Skill 1 is aligned to the jeep hitbox centre).
     * The spawn Y is the boss's own centre, lane-clamped so it never exits the road.
     */
    private void fireBullet() {
        float bx = x;
        float bulletH = NukeProjectile.BossProjectile.FRAME_H * Game.SCALE;
        float byCentre = y + height / 2f - bulletH / 2f;

        // ── Allow all 3 lanes (upper, middle, lower) ────────────
        float bulletTopLimit = laneTopY;
        float bulletBotLimit = Game.GAME_HEIGHT - bulletH;  // ← CHANGE: use full screen height

        if (byCentre < bulletTopLimit) byCentre = bulletTopLimit;
        if (byCentre > bulletBotLimit) byCentre = bulletBotLimit;

        bullets.add(new NukeProjectile.BossProjectile(bx, byCentre, bulletFrames));
    }

    /**
     * Tweak 2: lays the pileIndex-th pile in a vertical column.
     * pileIndex 0 = top, 1 = middle, 2 = bottom.
     * Piles are equally spaced by PILE_VERTICAL_GAP.
     * The column is centred on the current lane mid-point.
     */
    /**
     * Deploys 3 animated nukes in a vertical column CENTERED ON THE JEEP.
     * pileIndex: 0=top, 1=middle, 2=bottom
     * Nukes spawn left of jeep and animate in place.
     */
    private void layNukesVertical(int pileIndex) {
        float nukeH = NukeProjectile.Nuke.FRAME_H * Game.SCALE;
        float gap = PILE_VERTICAL_GAP * Game.SCALE;

        // ── Centre of jeep hitbox ──────────────────────────────
        float jeepCentreX = jeepX + jeepWidth / 2f;
        float jeepCentreY = jeepY + jeepHeight / 2f;

        // ── Vertical column: pile 0=top, 1=mid, 2=bot ──────────
        // Base offset: anchored to lane-based stacking
        float baseOffsetY = (pileIndex - 1) * (nukeH + gap);

        // ── Add controlled randomness to nearby lanes only ─────
        // Random offset: ±50% of lane gap
        float randomOffset = (rng.nextFloat() - 0.5f) * gap;
        float offsetY = baseOffsetY + randomOffset;

        // ── Spawn X: left of jeep ──────────────────────────────
        float px = jeepCentreX + SKILL2_SPAWN_OFFSET_X;

        // ── Spawn Y: CRITICAL FIX ──────────────────────────────
        // jeepCentreY is the CENTER of the jeep hitbox.
        // To align nuke CENTRE with jeep CENTRE, subtract half nuke height.
        // Then add the lane offset.

        float py = jeepCentreY - nukeH / 2f + offsetY * 0.7f + SKILL2_SPAWN_OFFSET_Y;

        // ── Clamp to lane boundaries (ensure stays in road) ────
        float nukeTop = laneTopY;
        float nukeBot = Game.GAME_HEIGHT - nukeH;

        if (py < nukeTop) py = nukeTop;
        if (py > nukeBot) py = nukeBot;

        System.out.println("[Boss2] Nuke " + (pileIndex + 1)
                + " | jeepCentre=(" + jeepCentreX + ", " + jeepCentreY + ")"
                + " | topLeftY=" + py
                + " | baseOffset=" + baseOffsetY
                + " | randomOffset=" + randomOffset);

        nukes.add(new NukeProjectile.Nuke(px, py, nukeFrames));
        nukesDeployed++;
    }
    /*
        private void layNukesVertical(int pileIndex) {
            float nukeH = NukeProjectile.Nuke.FRAME_H * Game.SCALE;
            float gap = PILE_VERTICAL_GAP * Game.SCALE;

            float colCentreY = y + height / 2f;
            float offsetY = (pileIndex - 1) * (nukeH + gap);

            float px = x + width * 0.25f;
            float py = colCentreY + offsetY - nukeH / 2f;

            float nukeTop = laneTopY;
            float nukeBot = Game.GAME_HEIGHT - nukeH;  // ← CHANGE: use full screen height instead

            if (py < nukeTop) py = nukeTop;
            if (py > nukeBot) py = nukeBot;

            System.out.println("[Boss2] Spawning nuke " + (pileIndex + 1) + " at (" + px + ", " + py + ")");
            nukes.add(new NukeProjectile.Nuke(px, py, nukeFrames));
            nukesDeployed++;
        }

     */

    private void updateBullets() {
        bullets.removeIf(b -> { b.update(); return !b.isActive(); });
    }

    private void updateNukes() {nukes.removeIf(p -> { p.update(BOSS_SCROLL_SPEED * Game.SCALE); return !p.isActive(); });}

    // ── Animation ─────────────────────────────────────────────
    private void updateAnimation() {
        if (state == BossState.SKILL2 && currentRow == ROW_SKILL2) return;
        // Skip animation update during stun (handled by updateStunState)
        if (state == BossState.STUN) return;

        int speed;
        switch (currentRow) {
            case ROW_SKILL1: speed = ANI_SPEED_SKILL1; break;
            case ROW_HIT:    speed = ANI_SPEED_HIT; break;
            default:         speed = ANI_SPEED_RUNNING; break;
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
        List<NukeProjectile.Nuke> nukesCopy = new ArrayList<>(nukes);
        for (NukeProjectile.Nuke n : nukesCopy) n.render(g);

        int safeRow = Math.min(currentRow, ROWS - 1);
        int safeIndex = Math.min(aniIndex, FRAME_COUNTS[safeRow] - 1);
        if (safeIndex < 0) safeIndex = 0;

        BufferedImage frame = frames[safeRow][safeIndex];
        if (frame != null)
            g.drawImage(frame, (int) x, (int) y, width, height, null);

        List<NukeProjectile.BossProjectile> bulletsCopy = new ArrayList<>(bullets);
        for (NukeProjectile.BossProjectile b : bulletsCopy) b.render(g);
    }
    private static final float HB_INSET_PERCENT = 0.6f;
    private static final int X_OFFSET = 0;
    private static final int Y_OFFSET = 20;

    public Rectangle getHitbox() {
        int insetX = (int)(width * HB_INSET_PERCENT / 2);
        int insetY = (int)(height * HB_INSET_PERCENT / 2);
        return new Rectangle(
                (int) x + insetX + X_OFFSET,
                (int) y + insetY + Y_OFFSET,
                width - (insetX * 2),
                height - (insetY * 2));
    }

    public List<NukeProjectile.BossProjectile> getBullets()      { return bullets; }
    public List<NukeProjectile.Nuke>    getNukes() { return nukes;   }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getLaneTopY()  { return laneTopY; }
    public float getLaneBotY()  { return laneBotY; }
}