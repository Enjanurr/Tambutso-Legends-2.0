package gameStates;

import main.Game;
import utils.LoadSave;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class GameIntroState extends State implements StateMethods {
    // Sprite sheet layout: logo = TAMBUTSO row + 3 LEGENDS rows.
    private static final int FRAME_HEIGHT = 396;

    // Sprite sheet layout: intro jeep uses the running row from red_jeep.png.
    private static final int JEEP_FRAME_WIDTH = 110;
    private static final int JEEP_FRAME_HEIGHT = 40;
    private static final int JEEP_FRAME_COUNT = 4;
    private static final int JEEP_RUNNING_ROW = 0;

    // Sprite sheet layout: 3 LEGENDS frames, 3 fireball frames, 6 explosion frames.
    private static final int LEGENDS_FRAME_COUNT = 3;
    private static final int FIREBALL_FRAME_COUNT = 3;
    private static final int FIREBALL_FRAME_WIDTH = 709;
    private static final int EXPLOSION_COLS = 3;
    private static final int EXPLOSION_ROWS = 2;
    private static final int EXPLOSION_FRAME_COUNT = EXPLOSION_COLS * EXPLOSION_ROWS;
    private static final int EXPLOSION_FRAME_SIZE = 1003;

    // Animation sequences.
    private static final int[] LEGENDS_FRAME_SEQUENCE = {0, 1, 2, 1};

    // Master pacing and per-phase pacing.
    // Increase pace values to make the intro faster, decrease them to make it slower.
    private static final float INTRO_PACE = 0.5f;
    private static final float JEEP_PACE = 0.5f;
    private static final float FIREBALL_PACE = 1.0f;
    private static final float EXPLOSION_PACE = 1.0f;
    private static final float LOGO_PACE = 1.0f;

    // Size controls.
    // Increase scale values to make each element larger on screen.
    private static final float JEEP_SCALE = 0.5f;
    private static final float FIREBALL_SCALE = 2.0f;
    private static final float EXPLOSION_SCALE = 2.0f;
    private static final float LOGO_SCALE = 1.0f;

    // Base timing values used by the pacing helpers below.
    private static final int BASE_JEEP_ANIM_SPEED = 10;
    private static final int BASE_JEEP_TRAVEL_TICKS = 140;
    private static final int BASE_LEGENDS_ANIM_SPEED = 16;
    private static final int BASE_FIREBALL_ANIM_SPEED = 7;
    private static final int BASE_EXPLOSION_ANIM_SPEED = 8;
    private static final int BASE_FIREBALL_FALL_TICKS = 36;
    private static final int BASE_HOLD_TICKS = 3000;
    private static final int BASE_PRESS_KEY_DELAY_TICKS = 250;
    private static final int BASE_PRESS_KEY_BLINK_TICKS = 34;
    private static final float BASE_FADE_SPEED = 0.01f;

    // On-screen layout and position tuning.
    private static final float JEEP_WIDTH_RATIO = 0.18f;
    private static final int JEEP_TARGET_OFFSET_X = -24;
    private static final int JEEP_TARGET_OFFSET_Y = 200;
    private static final int IMPACT_OFFSET_X = 0;
    private static final int IMPACT_OFFSET_Y = -200;
    private static final float FIREBALL_HEIGHT_RATIO = 0.34f;
    private static final float EXPLOSION_SIZE_RATIO = 0.50f;
    private static final int LEGENDS_OFFSET_X = 0;
    private static final int LEGENDS_OFFSET_Y = -200;
    private static final float PRESS_KEY_SCALE = 0.5f;
    private static final int PRESS_KEY_OFFSET_Y = -32;

    // Loaded sprite frames.
    private final BufferedImage tambutsoFrame;
    private final BufferedImage pressKeyImage;
    private final BufferedImage[] jeepFrames = new BufferedImage[JEEP_FRAME_COUNT];
    private final BufferedImage[] legendsFrames = new BufferedImage[LEGENDS_FRAME_COUNT];
    private final BufferedImage[] fireballFrames = new BufferedImage[FIREBALL_FRAME_COUNT];
    private final BufferedImage[] explosionFrames = new BufferedImage[EXPLOSION_FRAME_COUNT];

    // Animation state.
    private int jeepFrameIndex;
    private int jeepAnimTick;
    private int jeepTravelTick;
    private int legendsFrameIndex;
    private int legendsAnimTick;
    private int fireballFrameIndex;
    private int fireballAnimTick;
    private int fireballFallTick;
    private int explosionFrameIndex;
    private int explosionAnimTick;
    private int holdTick;
    private float introAlpha = 1f;

    private enum Phase {
        JEEP_ENTRY,
        FIREBALL_ENTRY,
        EXPLOSION_REVEAL,
        SHOW_LOGO,
        FADE_TO_MENU
    }

    private Phase phase = Phase.JEEP_ENTRY;

    public GameIntroState(Game game) {
        super(game);

        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.GAME_LOGO_SHEET);
        BufferedImage jeepSheet = LoadSave.getSpriteAtlas(LoadSave.PLAYER_ATLAS_1);
        BufferedImage fireballSheet = LoadSave.getSpriteAtlas(LoadSave.INTRO_FIREBALL);
        BufferedImage explosionSheet = LoadSave.getSpriteAtlas(LoadSave.INTRO_EXPLOSION);
        pressKeyImage = LoadSave.getSpriteAtlas(LoadSave.INTRO_PRESS_KEY);
        if (sheet == null || jeepSheet == null || fireballSheet == null || explosionSheet == null || sheet.getHeight() < FRAME_HEIGHT * 4) {
            tambutsoFrame = null;
            return;
        }

        for (int i = 0; i < JEEP_FRAME_COUNT; i++) {
            jeepFrames[i] = jeepSheet.getSubimage(
                    i * JEEP_FRAME_WIDTH,
                    JEEP_RUNNING_ROW * JEEP_FRAME_HEIGHT,
                    JEEP_FRAME_WIDTH,
                    JEEP_FRAME_HEIGHT);
        }

        tambutsoFrame = sheet.getSubimage(0, 0, sheet.getWidth(), FRAME_HEIGHT);
        // Slice the 3 animated LEGENDS frames from the rows below TAMBUTSO.
        for (int i = 0; i < LEGENDS_FRAME_COUNT; i++) {
            legendsFrames[i] = sheet.getSubimage(0, (i + 1) * FRAME_HEIGHT, sheet.getWidth(), FRAME_HEIGHT);
        }

        for (int i = 0; i < FIREBALL_FRAME_COUNT; i++) {
            fireballFrames[i] = fireballSheet.getSubimage(
                    i * FIREBALL_FRAME_WIDTH,
                    0,
                    FIREBALL_FRAME_WIDTH,
                    fireballSheet.getHeight());
        }

        for (int row = 0; row < EXPLOSION_ROWS; row++) {
            for (int col = 0; col < EXPLOSION_COLS; col++) {
                int index = row * EXPLOSION_COLS + col;
                explosionFrames[index] = explosionSheet.getSubimage(
                        col * EXPLOSION_FRAME_SIZE,
                        row * EXPLOSION_FRAME_SIZE,
                        EXPLOSION_FRAME_SIZE,
                        EXPLOSION_FRAME_SIZE);
            }
        }
    }

    @Override
    public void update() {
        if (tambutsoFrame == null) {
            GameStates.state = GameStates.MENU;
            return;
        }

        updateLegendsAnimation();

        switch (phase) {
            case JEEP_ENTRY:
                updateJeepAnimation();
                jeepTravelTick++;
                if (jeepTravelTick >= getJeepTravelTicks()) {
                    phase = Phase.FIREBALL_ENTRY;
                }
                break;
            case FIREBALL_ENTRY:
                updateFireballAnimation();
                fireballFallTick++;
                if (fireballFallTick >= getFireballFallTicks()) {
                    phase = Phase.EXPLOSION_REVEAL;
                    game.getAudioPlayer().playIntroExplosionSfx();
                }
                break;
            case EXPLOSION_REVEAL:
                if (updateExplosionAnimation()) {
                    phase = Phase.SHOW_LOGO;
                }
                break;
            case SHOW_LOGO:
                holdTick++;
                if (holdTick >= getLogoHoldTicks()) {
                    phase = Phase.FADE_TO_MENU;
                }
                break;
            case FADE_TO_MENU:
                introAlpha = Math.max(0f, introAlpha - getFadeSpeed());
                if (introAlpha <= 0f) {
                    GameStates.state = GameStates.MENU;
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (phase == Phase.FADE_TO_MENU) {
            game.getMenu().draw(g);
        }

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, introAlpha));
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT);

        if (phase == Phase.EXPLOSION_REVEAL || phase == Phase.SHOW_LOGO || phase == Phase.FADE_TO_MENU) {
            drawLogo(g2d, getLogoRevealAlpha());
        }

        if (shouldDrawPressKeyPrompt()) {
            drawPressKeyPrompt(g2d);
        }

        if (phase == Phase.JEEP_ENTRY || phase == Phase.FIREBALL_ENTRY) {
            drawJeep(g2d);
        }

        if (phase == Phase.FIREBALL_ENTRY) {
            drawFireball(g2d);
        } else if (phase == Phase.EXPLOSION_REVEAL) {
            drawExplosion(g2d);
        }

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void updateLegendsAnimation() {
        legendsAnimTick++;
        if (legendsAnimTick < getLegendsAnimSpeed()) {
            return;
        }

        legendsAnimTick = 0;
        legendsFrameIndex = (legendsFrameIndex + 1) % LEGENDS_FRAME_SEQUENCE.length;
    }

    private void updateJeepAnimation() {
        jeepAnimTick++;
        if (jeepAnimTick < getJeepAnimSpeed()) {
            return;
        }

        jeepAnimTick = 0;
        jeepFrameIndex = (jeepFrameIndex + 1) % JEEP_FRAME_COUNT;
    }

    private void updateFireballAnimation() {
        fireballAnimTick++;
        if (fireballAnimTick < getFireballAnimSpeed()) {
            return;
        }

        fireballAnimTick = 0;
        fireballFrameIndex = (fireballFrameIndex + 1) % FIREBALL_FRAME_COUNT;
    }

    private boolean updateExplosionAnimation() {
        explosionAnimTick++;
        if (explosionAnimTick < getExplosionAnimSpeed()) {
            return false;
        }

        explosionAnimTick = 0;
        explosionFrameIndex++;
        return explosionFrameIndex >= EXPLOSION_FRAME_COUNT;
    }

    private void drawLogo(Graphics2D g2d, float alpha) {
        if (tambutsoFrame == null) {
            return;
        }

        BufferedImage legendsFrame = legendsFrames[LEGENDS_FRAME_SEQUENCE[legendsFrameIndex]];
        int sourceWidth = tambutsoFrame.getWidth();
        int sourceHeight = tambutsoFrame.getHeight() + legendsFrame.getHeight();

        float widthScale = (Game.GAME_WIDTH * 0.84f * LOGO_SCALE) / sourceWidth;
        float heightScale = (Game.GAME_HEIGHT * 0.70f * LOGO_SCALE) / sourceHeight;
        float scale = Math.min(widthScale, heightScale);

        int drawWidth = Math.round(sourceWidth * scale);
        int drawFrameHeight = Math.round(FRAME_HEIGHT * scale);
        int totalHeight = drawFrameHeight * 2;
        int drawX = (Game.GAME_WIDTH - drawWidth) / 2;
        int drawY = (Game.GAME_HEIGHT - totalHeight) / 2;
        // Apply offsets in source-space so they scale with the logo size.
        int legendsX = drawX + Math.round(LEGENDS_OFFSET_X * scale);
        int legendsY = drawY + drawFrameHeight + Math.round(LEGENDS_OFFSET_Y * scale);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, introAlpha * alpha));
        g2d.drawImage(tambutsoFrame, drawX, drawY, drawWidth, drawFrameHeight, null);
        g2d.drawImage(legendsFrame, legendsX, legendsY, drawWidth, drawFrameHeight, null);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, introAlpha));
    }

    private void drawJeep(Graphics2D g2d) {
        BufferedImage frame = jeepFrames[jeepFrameIndex];
        if (frame == null) {
            return;
        }

        int drawWidth = getJeepDrawWidth();
        int drawHeight = getJeepDrawHeight(frame, drawWidth);
        int drawX = getCurrentJeepX(drawWidth);
        int drawY = getCurrentJeepY(drawHeight);

        g2d.drawImage(frame, drawX, drawY, drawWidth, drawHeight, null);
    }

    private void drawFireball(Graphics2D g2d) {
        BufferedImage frame = fireballFrames[fireballFrameIndex];
        if (frame == null) {
            return;
        }

        int drawHeight = Math.round(Game.GAME_HEIGHT * FIREBALL_HEIGHT_RATIO * FIREBALL_SCALE);
        int drawWidth = Math.round(drawHeight * (frame.getWidth() / (float) frame.getHeight()));
        int drawX = getImpactCenterX() - drawWidth / 2;
        int startY = -drawHeight;
        int targetY = getImpactCenterY() - drawHeight / 2;
        float progress = Math.min(1f, fireballFallTick / (float) getFireballFallTicks());
        float easedProgress = 1f - (float) Math.pow(1f - progress, 3);
        int drawY = Math.round(startY + (targetY - startY) * easedProgress);

        g2d.drawImage(frame, drawX, drawY, drawWidth, drawHeight, null);
    }

    private void drawExplosion(Graphics2D g2d) {
        int frameIndex = Math.min(explosionFrameIndex, EXPLOSION_FRAME_COUNT - 1);
        BufferedImage frame = explosionFrames[frameIndex];
        if (frame == null) {
            return;
        }

        int drawSize = Math.round(Game.GAME_HEIGHT * EXPLOSION_SIZE_RATIO * EXPLOSION_SCALE);
        int drawX = getImpactCenterX() - drawSize / 2;
        int drawY = getImpactCenterY() - drawSize / 2;

        g2d.drawImage(frame, drawX, drawY, drawSize, drawSize, null);
    }

    private void drawPressKeyPrompt(Graphics2D g2d) {
        if (pressKeyImage == null) {
            return;
        }

        int drawWidth = Math.round(pressKeyImage.getWidth() * PRESS_KEY_SCALE);
        int drawHeight = Math.round(pressKeyImage.getHeight() * PRESS_KEY_SCALE);
        int drawX = (Game.GAME_WIDTH - drawWidth) / 2;
        int drawY = Game.GAME_HEIGHT - drawHeight + PRESS_KEY_OFFSET_Y;

        g2d.drawImage(pressKeyImage, drawX, drawY, drawWidth, drawHeight, null);
    }

    private float getLogoRevealAlpha() {
        if (phase == Phase.EXPLOSION_REVEAL) {
            return Math.min(1f, explosionFrameIndex / (float) EXPLOSION_FRAME_COUNT);
        }
        return 1f;
    }

    private boolean shouldDrawPressKeyPrompt() {
        if ((phase != Phase.SHOW_LOGO && phase != Phase.FADE_TO_MENU) || holdTick < getPressKeyDelayTicks()) {
            return false;
        }

        int blinkTick = holdTick - getPressKeyDelayTicks();
        int blinkWindow = getPressKeyBlinkTicks();
        return (blinkTick / blinkWindow) % 2 == 0;
    }

    private int getJeepAnimSpeed() {
        return scaleTicks(BASE_JEEP_ANIM_SPEED, JEEP_PACE);
    }

    private int getJeepTravelTicks() {
        return scaleTicks(BASE_JEEP_TRAVEL_TICKS, JEEP_PACE);
    }

    private int getJeepDrawWidth() {
        return Math.round(Game.GAME_WIDTH * JEEP_WIDTH_RATIO * JEEP_SCALE);
    }

    private int getJeepDrawHeight(BufferedImage frame, int drawWidth) {
        return Math.round(drawWidth * (frame.getHeight() / (float) frame.getWidth()));
    }

    private int getCurrentJeepX(int drawWidth) {
        int centerX = Game.GAME_WIDTH / 2 + Math.round(JEEP_TARGET_OFFSET_X * JEEP_SCALE);
        int targetX = centerX - drawWidth / 2;
        int startX = -drawWidth;
        float progress = Math.min(1f, jeepTravelTick / (float) getJeepTravelTicks());
        float easedProgress = 1f - (float) Math.pow(1f - progress, 3);
        return Math.round(startX + (targetX - startX) * easedProgress);
    }

    private int getCurrentJeepY(int drawHeight) {
        return Game.GAME_HEIGHT / 2 - drawHeight / 2 + Math.round(JEEP_TARGET_OFFSET_Y * JEEP_SCALE);
    }

    private int getImpactCenterX() {
        int drawWidth = getJeepDrawWidth();
        return getCurrentJeepX(drawWidth) + drawWidth / 2 + Math.round(IMPACT_OFFSET_X * JEEP_SCALE);
    }

    private int getImpactCenterY() {
        BufferedImage frame = jeepFrames[0];
        if (frame == null) {
            return Game.GAME_HEIGHT / 2;
        }

        int drawWidth = getJeepDrawWidth();
        int drawHeight = getJeepDrawHeight(frame, drawWidth);
        return getCurrentJeepY(drawHeight) + drawHeight / 2 + Math.round(IMPACT_OFFSET_Y * JEEP_SCALE);
    }

    private int getLegendsAnimSpeed() {
        return scaleTicks(BASE_LEGENDS_ANIM_SPEED, LOGO_PACE);
    }

    private int getFireballAnimSpeed() {
        return scaleTicks(BASE_FIREBALL_ANIM_SPEED, FIREBALL_PACE);
    }

    private int getExplosionAnimSpeed() {
        return scaleTicks(BASE_EXPLOSION_ANIM_SPEED, EXPLOSION_PACE);
    }

    private int getFireballFallTicks() {
        return scaleTicks(BASE_FIREBALL_FALL_TICKS, FIREBALL_PACE);
    }

    private int getLogoHoldTicks() {
        return scaleTicks(BASE_HOLD_TICKS, LOGO_PACE);
    }

    private int getPressKeyDelayTicks() {
        return Math.min(getLogoHoldTicks(), scaleTicks(BASE_PRESS_KEY_DELAY_TICKS, LOGO_PACE));
    }

    private int getPressKeyBlinkTicks() {
        return scaleTicks(BASE_PRESS_KEY_BLINK_TICKS, LOGO_PACE);
    }

    private float getFadeSpeed() {
        return scaleAlpha(BASE_FADE_SPEED, LOGO_PACE);
    }

    private int scaleTicks(int baseTicks, float phasePace) {
        return Math.max(1, Math.round(baseTicks / (INTRO_PACE * phasePace)));
    }

    private float scaleAlpha(float baseAlpha, float phasePace) {
        return Math.max(0.0001f, baseAlpha * INTRO_PACE * phasePace);
    }

    public boolean hasLogoRevealStarted() {
        return phase == Phase.SHOW_LOGO
                || phase == Phase.FADE_TO_MENU
                || (phase == Phase.EXPLOSION_REVEAL && explosionFrameIndex > 0);
    }

    private void skipIntro() {
        phase = Phase.FADE_TO_MENU;
        holdTick = getLogoHoldTicks();
        introAlpha = Math.max(introAlpha, 1f);
    }

    @Override public void mouseClicked(MouseEvent e) { skipIntro(); }
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void keyPressed(KeyEvent e) { skipIntro(); }
    @Override public void keyReleased(KeyEvent e) {}
}
