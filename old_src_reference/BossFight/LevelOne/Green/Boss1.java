package BossFight.LevelOne.Green;

import BossFight.LevelOne.GarbagePile;
import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class Boss1 {

    // ── Sprite sheet dimensions ───────────────────────────────
    public static final int SHEET_COLS = 5;
    public static final int FRAME_W    = 110;  // 550 / 5
    public static final int FRAME_H    = 79;   // 316 / 4
    public static final int ROWS       = 4;

    // ── Row indices boss skilss ───────────────────────────────────────────
    public static final int ROW_SKILL1  = 0;   // bullet frames only
    public static final int ROW_RUNNING = 1;
    public static final int ROW_SKILL2  = 2;
    public static final int ROW_HIT     = 3;

    // ── Frame counts per row ──────────────────────────────────
    private static final int[] FRAME_COUNTS = { 5, 5, 4, 2 };

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
    private static final int HIT_ANIM_TICKS = 1 * 90;  // hit animation duration

    private static final int BULLET_DELAY  = 1 * 200; // 1 s between bullets
    private static final int MAX_BULLETS   = 10;        // bullets per Skill 1 phase
    private static final int MAX_PILES     = 3;        // always 3 piles per Skill 2

    // ── Per-row animation speeds (ticks per frame) ← ADJUST ──
    // -------------------------------------------------------
    public static final int ANI_SPEED_RUNNING  = 20;
    public static final int ANI_SPEED_SKILL1   = 10;
    public static final int ANI_SPEED_HIT      = 20;
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
    public enum BossState { FOLLOW, SKILL1, WAIT_AFTER1, SKILL2, WAIT_AFTER2, RANDOM, HIT }
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
    private int pilesLaid    = 0;
    private int pileTick     = 0;
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
    private int pileSpawnTick = 0;

    // ── Hit animation ─────────────────────────────────────────
    private int hitTick = 0;

    // ── Wander (all non-Skill1 states) ────────────────────────
    private float wanderDir        = 0f;
    private int   wanderChangeTick = 0;
    private int   wanderInterval   = 80;

    // ── Spawned objects ───────────────────────────────────────
    private final List<GarbagePile.BossProjectile> bullets = new ArrayList<>();
    private final List<GarbagePile>    piles   = new ArrayList<>();
    private BufferedImage[]            bulletFrames;
    private BufferedImage              pileImage;

    private final Random rng = new Random();

    // ─────────────────────────────────────────────────────────

    // ── Shoot settings ← CHANGE cooldown to 4s ──────────────
    private static final int MAX_BULLETS_PER_USE = 5;
    private static final int SHOOT_FULL_COOLDOWN = 4 * 200;  // ← CHANGE: 4s (was 3s)

    // ── Heal cooldown ← CHANGE from slow ball to heal ──────
    private static final int HEAL_COOLDOWN = 10000;  // ← CHANGE: 10 seconds in milliseconds
    public Boss1(float startX, float startY) {
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
        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.BOSS1_ATLAS);
        if (sheet == null) {
            System.err.println("[Boss1] Could not load " + LoadSave.BOSS1_ATLAS);
            return;
        }
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < SHEET_COLS; col++) {
                int maxCols = (row == ROW_SKILL2) ? 5 : FRAME_COUNTS[row];
                if (col < maxCols)
                    frames[row][col] = sheet.getSubimage(
                            col * FRAME_W, row * FRAME_H, FRAME_W, FRAME_H);
            }

        bulletFrames = new BufferedImage[FRAME_COUNTS[ROW_SKILL1]];
        for (int i = 0; i < bulletFrames.length; i++)
            bulletFrames[i] = frames[ROW_SKILL1][i];

        pileImage = frames[ROW_SKILL2][4];
    }

    private float clampY(float candidateY) {
        if (candidateY < laneTopY)  candidateY = laneTopY;
        if (candidateY > laneBotY)  candidateY = laneBotY;
        return candidateY;
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────
    public void update(float jeepX, float jeepY) {
        updateBullets();
        updatePiles();
        updateStateMachine(jeepX, jeepY);
        updateAnimation();
    }

    private void updateStateMachine(float jeepX, float jeepY) {
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

            case RANDOM:
                if (rng.nextBoolean()) enterSkill1(); else enterSkill2();
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
        }
    }

    // ── Vertical movement ─────────────────────────────────────
    /** Smooth lerp — aligns the BOSS'S CENTRE to the jeep hitbox centre Y. */
    private void followJeepY(float jeepCenterY) {
        // Convert: we want (y + height/2) to approach jeepCenterY
        float targetTopY = jeepCenterY - height / 2f;
        y += (targetTopY - y) * FOLLOW_Y_DELAY;
        y  = clampY(y);
    }

    /** Free random wander — used in all other states. */
    private void wanderY() {
        wanderChangeTick++;
        if (wanderChangeTick >= wanderInterval) {
            wanderChangeTick = 0;
            wanderInterval   = WANDER_CHANGE_MIN + rng.nextInt(WANDER_CHANGE_MAX - WANDER_CHANGE_MIN);
            int roll = rng.nextInt(3);
            wanderDir = (roll == 0) ? -1f : (roll == 1) ? 1f : 0f;
        }

        float nextY = y + wanderDir * WANDER_SPEED * Game.SCALE;
        if (nextY < laneTopY) { nextY = laneTopY; wanderDir = 1f;  }
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
        pileTick++;

        switch (skill2Phase) {

            // ── Phase 0: col 0 — closed start ────────────────
            case 0:
                currentRow = ROW_SKILL2;
                aniIndex   = 0;
                if (pileTick >= S2_COL0_TICKS) {
                    pileTick      = 0;
                    s2LoopTick    = 0;
                    s2LoopIndex   = 1;
                    pileSpawnTick = 0;
                    pilesLaid     = 0;
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

                pileSpawnTick++;
                if (pileSpawnTick >= S2_PILE_DELAY && pilesLaid < MAX_PILES) {
                    layGarbagePileVertical(pilesLaid);  // Tweak 2: vertical positioning
                    pileSpawnTick = 0;
                }

                if (pilesLaid >= MAX_PILES) {
                    pileTick    = 0;
                    skill2Phase = 2;
                }
                break;

            // ── Phase 2: col 3 — tailgate closing ────────────
            case 2:
                currentRow = ROW_SKILL2;
                aniIndex   = 3;
                if (pileTick >= S2_COL3_TICKS) {
                    pileTick   = 0;
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
        state         = BossState.SKILL2;
        stateTick     = 0;
        pilesLaid     = 0;
        pileTick      = 0;
        s2LoopTick    = 0;
        s2LoopIndex   = 1;
        pileSpawnTick = 0;
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
        if (state == BossState.HIT) return;
        stateAfterHit = state;
        state         = BossState.HIT;
        hitTick       = 0;
        stateTick     = 0;
        aniIndex      = 0;
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
        float bx      = x;                              // left edge of boss → travels left
        float bulletH = GarbagePile.BossProjectile.FRAME_H * Game.SCALE;
        // Boss centre Y — this is already aligned to jeep hitbox centre during Skill 1
        float byCentre = y + height / 2f - bulletH / 2f;

        // Clamp within road lanes so bullets never fly off-road
        float bulletTopLimit = laneTopY;
        float bulletBotLimit = LANE_BOTTOM_PRE_SCALE * Game.TILES_SIZE - bulletH;
        if (byCentre < bulletTopLimit) byCentre = bulletTopLimit;
        if (byCentre > bulletBotLimit) byCentre = bulletBotLimit;

        bullets.add(new GarbagePile.BossProjectile(bx, byCentre, bulletFrames));
    }

    /**
     * Tweak 2: lays the pileIndex-th pile in a vertical column.
     * pileIndex 0 = top, 1 = middle, 2 = bottom.
     * Piles are equally spaced by PILE_VERTICAL_GAP.
     * The column is centred on the current lane mid-point.
     */
    private void layGarbagePileVertical(int pileIndex) {
        float pileH   = GarbagePile.PILE_H * Game.SCALE;
        float gap     = PILE_VERTICAL_GAP * Game.SCALE;

        // Centre of the 3-pile column = boss's current vertical centre
        float colCentreY = y + height / 2f;
        // Offsets: pile 0 is top, pile 2 is bottom
        float offsetY = (pileIndex - 1) * (pileH + gap); // -1 → top, 0 → mid, +1 → bot

        float px = x + width * 0.25f;          // slightly left of boss centre
        float py = colCentreY + offsetY - pileH / 2f;

        // Clamp so piles never land outside the road
        float pileTop = laneTopY;
        float pileBot = LANE_BOTTOM_PRE_SCALE * Game.TILES_SIZE - pileH;
        if (py < pileTop) py = pileTop;
        if (py > pileBot) py = pileBot;

        piles.add(new GarbagePile(px, py, pileImage));
        pilesLaid++;
    }

    private void updateBullets() {
        bullets.removeIf(b -> { b.update(); return !b.isActive(); });
    }

    private void updatePiles() {
        piles.removeIf(p -> { p.update(BOSS_SCROLL_SPEED * Game.SCALE); return !p.isActive(); });
    }

    // ── Animation ─────────────────────────────────────────────
    private void updateAnimation() {
        if (state == BossState.SKILL2 && currentRow == ROW_SKILL2) return;

        int speed;
        switch (currentRow) {
            case ROW_SKILL1: speed = ANI_SPEED_SKILL1;  break;
            case ROW_HIT:    speed = ANI_SPEED_HIT;     break;
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
        for (GarbagePile p : piles) p.render(g);

        int safeIndex = Math.min(aniIndex, FRAME_COUNTS[currentRow] - 1);
        BufferedImage frame = frames[currentRow][safeIndex];
        if (frame != null)
            g.drawImage(frame, (int) x, (int) y, width, height, null);

        for (GarbagePile.BossProjectile b : bullets) b.render(g);
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
                (int) x + insetX + X_OFFSET,
                (int) y + insetY + Y_OFFSET,
                width - (insetX * 2),
                height - (insetY * 2));
    }

    public List<GarbagePile.BossProjectile> getBullets()      { return bullets; }
    public List<GarbagePile>    getGarbagePiles() { return piles;   }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getLaneTopY()  { return laneTopY; }
    public float getLaneBotY()  { return laneBotY; }
}