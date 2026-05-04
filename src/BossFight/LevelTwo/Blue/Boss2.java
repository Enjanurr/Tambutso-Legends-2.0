package BossFight.LevelTwo.Blue;

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
    public static final int FRAME_W    = 110;
    public static final int FRAME_H    = 79;
    public static final int FRAME_W_SKILL1   = 36;
    public static final int FRAME_H_SKILL1   = 34;
    public static final int FRAME_W_SKILL2   = 60;
    public static final int FRAME_H_SKILL2   = 60;
    public static final int ROWS       = 3;

    // ── Row indices ───────────────────────────────────────────
    public static final int ROW_SKILL1  = 0;
    public static final int ROW_RUNNING = 1;
    public static final int ROW_SKILL2  = 0;
    public static final int ROW_HIT     = 2;

    // ── Frame counts per row ──────────────────────────────────
    private static final int[] FRAME_COUNTS = { 5, 5, 2 };

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
    private static final int FOLLOW_TICKS = 3 * 200;      // 3 seconds
    private static final int SKILL1_TICKS = 6 * 200;      // 6 seconds shooting
    private static final int WAIT_TICKS = 2 * 200;        // 2 seconds wait
    private static final int SKILL2_TICKS = 5 * 200;      // 5 seconds dumping
    private static final int HIT_ANIM_TICKS = 30;

    private static final int BULLET_DELAY = 150;          // 0.75 sec between bullets
    private static final int MAX_BULLETS = 12;            // 12 bullets per phase
    private static final int MAX_NUKES = 3;

    // ── Animation speeds ─────────────────────────────────────
    public static final int ANI_SPEED_RUNNING = 20;
    public static final int ANI_SPEED_SKILL1 = 10;
    public static final int ANI_SPEED_HIT = 20;

    // Skill 2 animation phase durations
    private static final int S2_COL0_TICKS = 30;
    private static final int S2_LOOP_SPEED = 15;
    private static final int S2_COL3_TICKS = 30;
    private static final int S2_PILE_DELAY = 60;

    // ── Vertical spacing ──────────────────────────────────────
    private static final float PILE_VERTICAL_GAP = 50f;

    // ── Road lane boundaries ──────────────────────────────────
    private static final float LANE_TOP_PRE_SCALE = 10f;
    private static final float LANE_BOTTOM_PRE_SCALE = 17f;
    private static final float ALIGN_THRESHOLD = 4f * Game.SCALE;

    // ── Skill 2 spawn positioning ─────────────────────────────
    private static final float SKILL2_SPAWN_OFFSET_X = 100f;

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
    public enum BossState { FOLLOW, SKILL1, SKILL2, WAIT_AFTER_SKILL, HIT }
    private BossState state = BossState.FOLLOW;
    private BossState stateAfterHit = BossState.FOLLOW;
    private int stateTick = 0;

    // ── Skill 1 variables ─────────────────────────────────────
    private boolean skill1Firing = false;
    private float skill1TargetY = 0f;
    private int bulletsFired = 0;
    private int bulletTick = 0;

    // ── Skill 2 variables ─────────────────────────────────────
    private int nukesDeployed = 0;
    private int nukeTick = 0;
    private int skill2Phase = 0;
    private int s2LoopTick = 0;
    private int s2LoopIndex = 1;
    private int nukeSpawnTick = 0;

    // ── Hit animation ─────────────────────────────────────────
    private int hitTick = 0;

    // ── Wander variables ──────────────────────────────────────
    private float wanderDir = 0f;
    private int wanderChangeTick = 0;
    private int wanderInterval = 80;

    // ── Spawned objects ───────────────────────────────────────
    private final List<NukeProjectile.BossProjectile> bullets = new ArrayList<>();
    private final List<NukeProjectile.Nuke> nukes = new ArrayList<>();
    private BufferedImage[] bulletFrames;
    private BufferedImage[] nukeFrames;

    // ── Jeep position tracking ────────────────────────────────
    private float jeepX = 0f;
    private float jeepY = 0f;
    private float jeepWidth = 0f;
    private float jeepHeight = 0f;

    private final Random rng = new Random();

    public Boss2(float startX, float startY) {
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
        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.BOSS2_ATLAS);
        System.out.println("Boss 2 loaded");
        if (sheet == null) {
            System.err.println("[Boss2] Could not load " + LoadSave.BOSS2_ATLAS);
            return;
        }

        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < SHEET_COLS; col++) {
                int maxCols = (row == ROW_SKILL2) ? 5 : FRAME_COUNTS[row];
                if (col < maxCols)
                    frames[row][col] = sheet.getSubimage(
                            col * FRAME_W, row * FRAME_H, FRAME_W, FRAME_H);
            }

        // Load Skill 1 frames
        BufferedImage skillSheet1 = LoadSave.getSpriteAtlas(LoadSave.BOSS2_SKILL1);
        if (skillSheet1 != null) {
            bulletFrames = new BufferedImage[4];
            for (int i = 0; i < 4; i++) {
                bulletFrames[i] = skillSheet1.getSubimage(
                        i * FRAME_W_SKILL1, 0, FRAME_W_SKILL1, FRAME_H_SKILL1);
            }
            System.out.println("✓ Loaded Skill 1 frames from separate sheet");
        } else {
            System.err.println("❌ Could not load " + LoadSave.BOSS2_SKILL1);
        }

        // Load Skill 2 frames
        BufferedImage skillSheet2 = LoadSave.getSpriteAtlas(LoadSave.BOSS2_SKILL2);
        if (skillSheet2 != null) {
            nukeFrames = new BufferedImage[18];
            for (int i = 0; i < 18; i++) {
                nukeFrames[i] = skillSheet2.getSubimage(
                        i * FRAME_W_SKILL2, 0, FRAME_W_SKILL2, FRAME_H_SKILL2);
            }
            System.out.println("✓ Loaded Skill 2 frames from separate sheet");
        } else {
            System.err.println("❌ Could not load " + LoadSave.BOSS2_SKILL2);
        }
    }

    private float clampY(float candidateY) {
        if (candidateY < laneTopY) candidateY = laneTopY;
        if (candidateY > laneBotY) candidateY = laneBotY;
        return candidateY;
    }

    public void update(float jeepX, float jeepY, float jeepWidth, float jeepHeight) {
        this.jeepX = jeepX;
        this.jeepY = jeepY;
        this.jeepWidth = jeepWidth;
        this.jeepHeight = jeepHeight;

        updateBullets();
        updateNukes();
        updateStateMachine();
        updateAnimation();
    }

    private void updateStateMachine() {


        stateTick++;
        x = lockedX;

        switch (state) {
            case FOLLOW:
                currentRow = ROW_RUNNING;
                wanderY();
                if (stateTick >= FOLLOW_TICKS) {
                    // ✅ FIX: Start with RANDOM instead of SKILL1
                    enterRandom();
                }
                break;

            case SKILL1:
                currentRow = ROW_RUNNING;

                if (!skill1Firing) {
                    skill1TargetY = jeepY + jeepHeight / 2f;
                    followJeepY(skill1TargetY);

                    float bossCentreY = y + height / 2f;
                    float diff = Math.abs(bossCentreY - skill1TargetY);
                    if (diff <= ALIGN_THRESHOLD) {
                        y = clampY(skill1TargetY - height / 2f);
                        skill1Firing = true;
                        bulletTick = BULLET_DELAY;
                    }
                } else {
                    followJeepY(jeepY + jeepHeight / 2f);

                    bulletTick++;
                    if (bulletTick >= BULLET_DELAY && bulletsFired < MAX_BULLETS) {
                        fireBullet();
                        bulletTick = 0;
                        bulletsFired++;
                    }
                }

                if (stateTick >= SKILL1_TICKS) {
                    enterWait();
                }
                break;

            case SKILL2:
                wanderY();
                updateSkill2Sequence();
                if (stateTick >= SKILL2_TICKS) {
                    enterWait();
                }
                break;

            case WAIT_AFTER_SKILL:
                currentRow = ROW_RUNNING;
                wanderY();
                if (stateTick >= WAIT_TICKS) {
                    enterRandom();  // Random next skill
                }
                break;

            case HIT:
                currentRow = ROW_HIT;
                hitTick++;
                if (hitTick >= HIT_ANIM_TICKS) {
                    hitTick = 0;
                    state = stateAfterHit;
                    stateTick = 0;
                    aniIndex = 0;
                }
                break;
        }
    }

    // Random skill selection


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

    private void updateSkill2Sequence() {
        nukeTick++;

        switch (skill2Phase) {
            case 0:
                currentRow = ROW_SKILL2;
                aniIndex = 0;
                if (nukeTick >= S2_COL0_TICKS) {
                    nukeTick = 0;
                    s2LoopTick = 0;
                    s2LoopIndex = 1;
                    nukeSpawnTick = 0;
                    nukesDeployed = 0;
                    skill2Phase = 1;
                }
                break;

            case 1:
                currentRow = ROW_SKILL2;
                s2LoopTick++;
                if (s2LoopTick >= S2_LOOP_SPEED) {
                    s2LoopTick = 0;
                    s2LoopIndex = (s2LoopIndex == 1) ? 2 : 1;
                }
                aniIndex = s2LoopIndex;

                nukeSpawnTick++;
                if (nukeSpawnTick >= S2_PILE_DELAY && nukesDeployed < MAX_NUKES) {
                    spawnNukeWave();
                    nukeSpawnTick = 0;
                }

                if (nukesDeployed >= MAX_NUKES) {
                    nukeTick = 0;
                    skill2Phase = 2;
                }
                break;

            case 2:
                currentRow = ROW_SKILL2;
                aniIndex = 3;
                if (nukeTick >= S2_COL3_TICKS) {
                    nukeTick = 0;
                    currentRow = ROW_RUNNING;
                    aniIndex = 0;
                    skill2Phase = 3;
                }
                break;

            case 3:
                currentRow = ROW_RUNNING;
                break;
        }
    }

    private void spawnNukeWave() {
        for (int i = 0; i < 3; i++) {
            layNukeVertical(i);
        }
    }

    private void layNukeVertical(int pileIndex) {
        float nukeH = NukeProjectile.Nuke.FRAME_H * Game.SCALE;
        float gap = PILE_VERTICAL_GAP * Game.SCALE;

        float colCentreY = jeepY + jeepHeight / 2f;
        float offsetY = (pileIndex - 1) * (nukeH + gap);

        float px = jeepX + jeepWidth + SKILL2_SPAWN_OFFSET_X * Game.SCALE;
        float py = colCentreY + offsetY - nukeH / 2f;

        float nukeTop = laneTopY;
        float nukeBot = LANE_BOTTOM_PRE_SCALE * Game.TILES_SIZE - nukeH;
        if (py < nukeTop) py = nukeTop;
        if (py > nukeBot) py = nukeBot;

        nukes.add(new NukeProjectile.Nuke(px, py, nukeFrames));
        nukesDeployed++;
    }

    private void fireBullet() {
        float bx = x;
        float bulletH = NukeProjectile.BossProjectile.FRAME_H * Game.SCALE;
        float byCentre = y + height / 2f - bulletH / 2f;

        float bulletTopLimit = laneTopY;
        float bulletBotLimit = LANE_BOTTOM_PRE_SCALE * Game.TILES_SIZE - bulletH;
        if (byCentre < bulletTopLimit) byCentre = bulletTopLimit;
        if (byCentre > bulletBotLimit) byCentre = bulletBotLimit;

        bullets.add(new NukeProjectile.BossProjectile(bx, byCentre, bulletFrames));
    }

    private void updateBullets() {
        bullets.removeIf(b -> { b.update(); return !b.isActive(); });
    }

    private void updateNukes() {
        nukes.removeIf(n -> { n.update(BOSS_SCROLL_SPEED * Game.SCALE); return !n.isActive(); });
    }

    private void updateAnimation() {
        if (state == BossState.SKILL2 && skill2Phase < 3) return;

        if (state == BossState.HIT && aniIndex >= FRAME_COUNTS[ROW_HIT] - 1) {
            return;
        }

        int speed;
        switch (currentRow) {
            case ROW_SKILL1: speed = ANI_SPEED_SKILL1; break;
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

    // ── State Transitions ─────────────────────────────────────
    private void enterRandom() {
        if (rng.nextBoolean()) {
            enterSkill1();
        } else {
            enterSkill2();
        }
    }

    private void enterSkill1() {
        state = BossState.SKILL1;
        stateTick = 0;
        bulletsFired = 0;
        bulletTick = 0;
        skill1Firing = false;
        skill1TargetY = y;
        System.out.println("[Boss2] 💥 Entering SKILL1 (Shoot) phase!");
    }

    private void enterSkill2() {
        state = BossState.SKILL2;
        stateTick = 0;
        nukesDeployed = 0;
        nukeTick = 0;
        s2LoopTick = 0;
        s2LoopIndex = 1;
        nukeSpawnTick = 0;
        skill2Phase = 0;
        currentRow = ROW_SKILL2;
        System.out.println("[Boss2] 💧 Entering SKILL2 (Nuke) phase!");
    }

    private void enterWait() {
        state = BossState.WAIT_AFTER_SKILL;
        stateTick = 0;
        System.out.println("[Boss2] ⏸️ Wait state - preparing next skill");
    }

    public void triggerHit() {
        if (state == BossState.HIT) return;
        stateAfterHit = state;
        state = BossState.HIT;
        hitTick = 0;
        stateTick = 0;
        aniIndex = 0;
        aniTick = 0;
        System.out.println("[Boss2] 💥 Hit!");
    }

    // ── RENDER ─────────────────────────────────────────────────
    public void render(Graphics g) {
        List<NukeProjectile.Nuke> nukesCopy = new ArrayList<>(nukes);
        for (NukeProjectile.Nuke n : nukesCopy) n.render(g);

        List<NukeProjectile.BossProjectile> bulletsCopy = new ArrayList<>(bullets);
        for (NukeProjectile.BossProjectile b : bulletsCopy) b.render(g);

        int safeIndex = Math.min(aniIndex, FRAME_COUNTS[currentRow] - 1);
        BufferedImage frame = frames[currentRow][safeIndex];
        if (frame != null)
            g.drawImage(frame, (int) x, (int) y, width, height, null);
    }

    // ── GETTERS ────────────────────────────────────────────────
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

    public List<NukeProjectile.BossProjectile> getBullets() { return bullets; }
    public List<NukeProjectile.Nuke> getNukes() { return nukes; }
    public float getX() { return x; }
    public float getY() { return y; }
}