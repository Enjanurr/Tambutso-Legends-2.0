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
    public static final int FRAME_W    = 110;
    public static final int FRAME_H    = 79;
    public static final int ROWS       = 4;

    // ── Row indices ───────────────────────────────────────────
    public static final int ROW_SKILL1  = 0;
    public static final int ROW_RUNNING = 1;
    public static final int ROW_SKILL2  = 2;
    public static final int ROW_HIT     = 3;

    // ── Frame counts per row ──────────────────────────────────
    private static final int[] FRAME_COUNTS = { 5, 5, 4, 2 };

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

    // ── Timing constants (REDUCED for faster gameplay) ────────
    private static final int FOLLOW_TICKS = 3 * 200;      // 3 seconds (was 4)
    private static final int SKILL1_TICKS = 6 * 200;      // 6 seconds (was 10)
    private static final int WAIT_TICKS = 2 * 200;        // 2 seconds
    private static final int SKILL2_TICKS = 5 * 200;      // 5 seconds (was 6)
    private static final int HIT_ANIM_TICKS = 30;         // Faster hit recovery

    private static final int BULLET_DELAY = 150;          // 0.75 sec (was 200)
    private static final int MAX_BULLETS = 12;            // 12 bullets (was 10)
    private static final int MAX_PILES = 3;

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

    // ── State machine (SIMPLIFIED) ───────────────────────────
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
    private int pilesLaid = 0;
    private int pileTick = 0;
    private int skill2Phase = 0;
    private int s2LoopTick = 0;
    private int s2LoopIndex = 1;
    private int pileSpawnTick = 0;

    // ── Hit animation ─────────────────────────────────────────
    private int hitTick = 0;

    // ── Wander variables ──────────────────────────────────────
    private float wanderDir = 0f;
    private int wanderChangeTick = 0;
    private int wanderInterval = 80;

    // ── Spawned objects ───────────────────────────────────────
    private final List<GarbagePile.BossProjectile> bullets = new ArrayList<>();
    private final List<GarbagePile> piles = new ArrayList<>();
    private BufferedImage[] bulletFrames;
    private BufferedImage pileImage;

    private final Random rng = new Random();

    public Boss1(float startX, float startY) {
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
        if (candidateY < laneTopY) candidateY = laneTopY;
        if (candidateY > laneBotY) candidateY = laneBotY;
        return candidateY;
    }

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
            case FOLLOW:
                currentRow = ROW_RUNNING;
                wanderY();
                if (stateTick >= FOLLOW_TICKS) {
                    enterRandom();  // ✓ 50/50 chance for first skill
                }
                break;

            case SKILL1:
                currentRow = ROW_RUNNING;

                if (!skill1Firing) {
                    skill1TargetY = jeepY;
                    followJeepY(skill1TargetY);

                    float bossCentreY = y + height / 2f;
                    float diff = Math.abs(bossCentreY - skill1TargetY);
                    if (diff <= ALIGN_THRESHOLD) {
                        y = clampY(skill1TargetY - height / 2f);
                        skill1Firing = true;
                        bulletTick = BULLET_DELAY;
                    }
                } else {
                    followJeepY(jeepY);

                    bulletTick++;
                    if (bulletTick >= BULLET_DELAY && bulletsFired < MAX_BULLETS) {
                        fireBullet();
                        bulletTick = 0;
                        bulletsFired++;
                    }
                }

                if (stateTick >= SKILL1_TICKS) {
                    enterWait();  // ✓ FIXED: Go to wait state
                }
                break;

            case SKILL2:
                wanderY();
                updateSkill2Sequence();
                if (stateTick >= SKILL2_TICKS) {
                    enterWait();  // ✓ Go to wait state
                }
                break;

            case WAIT_AFTER_SKILL:
                currentRow = ROW_RUNNING;
                wanderY();
                if (stateTick >= WAIT_TICKS) {
                    enterRandom();  // ✓ 50/50 chance for next skill
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

    // ── RANDOM SKILL SELECTION (50/50) ────────────────────────
    private void enterRandom() {
        if (rng.nextBoolean()) {
            enterSkill1();
            System.out.println("[Boss1] 🎲 50/50: Chose SKILL1 (Bullets)");
        } else {
            enterSkill2();
            System.out.println("[Boss1] 🎲 50/50: Chose SKILL2 (Garbage Piles)");
        }
    }

    private void enterWait() {
        state = BossState.WAIT_AFTER_SKILL;
        stateTick = 0;
        System.out.println("[Boss1] ⏸️ Wait state - preparing next skill");
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

    private void updateSkill2Sequence() {
        pileTick++;

        switch (skill2Phase) {
            case 0:
                currentRow = ROW_SKILL2;
                aniIndex = 0;
                if (pileTick >= S2_COL0_TICKS) {
                    pileTick = 0;
                    s2LoopTick = 0;
                    s2LoopIndex = 1;
                    pileSpawnTick = 0;
                    pilesLaid = 0;
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

                pileSpawnTick++;
                if (pileSpawnTick >= S2_PILE_DELAY && pilesLaid < MAX_PILES) {
                    layGarbagePileVertical(pilesLaid);
                    pileSpawnTick = 0;
                }

                if (pilesLaid >= MAX_PILES) {
                    pileTick = 0;
                    skill2Phase = 2;
                }
                break;

            case 2:
                currentRow = ROW_SKILL2;
                aniIndex = 3;
                if (pileTick >= S2_COL3_TICKS) {
                    pileTick = 0;
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

    // ── RANDOM SKILL SELECTION (50/50) ────────────────────────


    private void enterSkill1() {
        state = BossState.SKILL1;
        stateTick = 0;
        bulletsFired = 0;
        bulletTick = 0;
        skill1Firing = false;
        skill1TargetY = y;
        System.out.println("[Boss1 Green] 💥 Entering SKILL1 (Shoot) phase!");
    }

    private void enterSkill2() {
        state = BossState.SKILL2;
        stateTick = 0;
        pilesLaid = 0;
        pileTick = 0;
        s2LoopTick = 0;
        s2LoopIndex = 1;
        pileSpawnTick = 0;
        skill2Phase = 0;
        currentRow = ROW_SKILL2;
        System.out.println("[Boss1 Green] 💧 Entering SKILL2 (Garbage Piles) phase!");
    }


    private void fireBullet() {
        float bx = x;
        float bulletH = GarbagePile.BossProjectile.FRAME_H * Game.SCALE;
        float byCentre = y + height / 2f - bulletH / 2f;

        float bulletTopLimit = laneTopY;
        float bulletBotLimit = LANE_BOTTOM_PRE_SCALE * Game.TILES_SIZE - bulletH;
        if (byCentre < bulletTopLimit) byCentre = bulletTopLimit;
        if (byCentre > bulletBotLimit) byCentre = bulletBotLimit;

        bullets.add(new GarbagePile.BossProjectile(bx, byCentre, bulletFrames));
    }

    private void layGarbagePileVertical(int pileIndex) {
        float pileH   = GarbagePile.PILE_H * Game.SCALE;
        float gap     = PILE_VERTICAL_GAP * Game.SCALE;

        float colCentreY = y + height / 2f;
        float offsetY = (pileIndex - 1) * (pileH + gap);

        float px = x + width * 0.25f;
        float py = colCentreY + offsetY - pileH / 2f;

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

    private void updateAnimation() {
        if (state == BossState.SKILL2 && skill2Phase < 3) return;
        if (state == BossState.HIT && aniIndex >= FRAME_COUNTS[ROW_HIT] - 1) return;

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

    public void triggerHit() {
        if (state == BossState.HIT) return;
        stateAfterHit = state;
        state = BossState.HIT;
        hitTick = 0;
        stateTick = 0;
        aniIndex = 0;
        aniTick = 0;
        System.out.println("[Boss1 Green] 💥 Hit!");
    }

    public void render(Graphics g) {
        List<GarbagePile> pilesCopy = new ArrayList<>(piles);
        for (GarbagePile p : pilesCopy) p.render(g);

        List<GarbagePile.BossProjectile> bulletsCopy = new ArrayList<>(bullets);
        for (GarbagePile.BossProjectile b : bulletsCopy) b.render(g);

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

    public List<GarbagePile.BossProjectile> getBullets() { return bullets; }
    public List<GarbagePile> getGarbagePiles() { return piles; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getLaneTopY() { return laneTopY; }
    public float getLaneBotY() { return laneBotY; }
}