package entities;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;

import static utils.Constants.EnemyConstants.*;

public class EnemyCar extends Enemy {

    // Enemy type catalogue with scale factor and health
    public enum EnemyType {
        TAXI(LoadSave.GSM_ATLAS,         90, 31, 4, 0.5f, 1.0f, 3),
        JEEP(LoadSave.EJEEP_ATLAS,       90, 38, 4, 0.5f, 1.0f, 3),
        MOTORCYCLE(LoadSave.MOTORCYCLE,  114, 87, 4, 0.5f, 0.45f, 3),
        CART(LoadSave.CART,              90, 85, 4, 0.5f, 0.65f, 3);

        public final String atlas;
        public final int    frameW, frameH, frameCount;
        public final float  speed;
        public final float  scale;
        public final int    maxHealth;

        EnemyType(String atlas, int frameW, int frameH, int frameCount, float speed, float scale, int maxHealth) {
            this.atlas      = atlas;
            this.frameW     = frameW;
            this.frameH     = frameH;
            this.frameCount = frameCount;
            this.speed      = speed;
            this.scale      = scale;
            this.maxHealth  = maxHealth;
        }
    }

    private final EnemyType type;
    private BufferedImage[][] frames;
    private int aniTick = 0;
    private int aniIndex = 0;
    private boolean active = true;
    private boolean movingLeft = true;
    private int currentHealth;
    private boolean isInvincible = false;
    private int invincibleTimer = 0;
    private static final int INVINCIBLE_DURATION = 15;

    // ── ADD THIS FLAG ─────────────────────────────────────────
    private boolean showHealthBar = false;  // Only true for boss fights

    public EnemyCar(float x, float y, EnemyType type) {
        super(x, y,
                (int)(type.frameW * Game.SCALE * type.scale),
                (int)(type.frameH * Game.SCALE * type.scale),
                0);
        this.type = type;
        this.currentHealth = type.maxHealth;
        loadFrames();

        float scaledW = type.frameW * Game.SCALE * type.scale;
        float scaledH = type.frameH * Game.SCALE * type.scale;

        initHitbox(x + 4 * Game.SCALE * type.scale,
                y + 2 * Game.SCALE * type.scale,
                scaledW - 20 * Game.SCALE * type.scale,
                scaledH - 8 * Game.SCALE * type.scale);
    }

    // ── ADD SETTER FOR HEALTH BAR ─────────────────────────────
    public void setShowHealthBar(boolean show) {
        this.showHealthBar = show;
    }

    public boolean takeDamage(int damage) {
        if (!active || isInvincible) return false;

        currentHealth -= damage;
        isInvincible = true;
        invincibleTimer = INVINCIBLE_DURATION;

        System.out.println("[EnemyCar] " + type.name() + " hit! Health: " + currentHealth + "/" + type.maxHealth);

        if (currentHealth <= 0) {
            active = false;
            System.out.println("[EnemyCar] " + type.name() + " DESTROYED!");
            return true;
        }
        return false;
    }

    public int getCurrentHealth() { return currentHealth; }
    public int getMaxHealth() { return type.maxHealth; }

    private void loadFrames() {
        BufferedImage sheet = LoadSave.getSpriteAtlas(type.atlas);
        if (sheet == null ||
                sheet.getWidth()  < type.frameW * type.frameCount ||
                sheet.getHeight() < type.frameH) {
            System.err.println("[EnemyCar] Bad atlas for " + type.name()
                    + " (" + type.atlas + ") — disabling.");
            active = false;
            return;
        }
        frames = new BufferedImage[1][type.frameCount];
        for (int i = 0; i < type.frameCount; i++) {
            BufferedImage original = sheet.getSubimage(i * type.frameW, 0, type.frameW, type.frameH);
            if (type.scale != 1.0f) {
                int scaledW = (int)(type.frameW * type.scale);
                int scaledH = (int)(type.frameH * type.scale);
                frames[0][i] = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = frames[0][i].createGraphics();
                g2d.drawImage(original, 0, 0, scaledW, scaledH, null);
                g2d.dispose();
            } else {
                frames[0][i] = original;
            }
        }
    }

    public void update(boolean worldScrolling, float scrollSpeed) {
        movingLeft = worldScrolling;

        if (movingLeft) {
            x -= type.speed * Game.SCALE;
            x -= scrollSpeed;
        } else {
            x += type.speed * Game.SCALE;
        }

        if (hitBox != null) {
            hitBox.x = x + 4 * Game.SCALE * type.scale;
            hitBox.y = y + 2 * Game.SCALE * type.scale;
        }

        if (movingLeft && x + width < 0) active = false;
        if (!movingLeft && x > Game.GAME_WIDTH) active = false;
        if (x < -3000 || x > Game.GAME_WIDTH + 3000) active = false;

        if (isInvincible) {
            invincibleTimer--;
            if (invincibleTimer <= 0) {
                isInvincible = false;
            }
        }

        aniTick++;
        if (aniTick >= ENEMY_ANI_SPEED) {
            aniTick = 0;
            aniIndex = (aniIndex + 1) % type.frameCount;
        }
    }

    public void render(Graphics g) {
        if (!active || frames == null) return;

        // Flash white when hit
        if (isInvincible && (invincibleTimer % 6 < 3)) {
            g.setColor(Color.WHITE);
            g.fillRect((int) x, (int) y, width, height);
        }

        g.drawImage(frames[0][aniIndex], (int) x, (int) y, width, height, null);

        // ── ONLY DRAW HEALTH BAR IF FLAG IS TRUE ──
        if (showHealthBar && currentHealth < type.maxHealth) {
            drawHealthBar(g);
        }
    }

    private void drawHealthBar(Graphics g) {
        int barWidth = width;
        int barHeight = 4;
        int barX = (int) x;
        int barY = (int) y - barHeight - 2;

        float healthPercent = (float) currentHealth / type.maxHealth;
        int currentBarWidth = (int)(barWidth * healthPercent);

        g.setColor(Color.RED);
        g.fillRect(barX, barY, barWidth, barHeight);
        g.setColor(Color.GREEN);
        g.fillRect(barX, barY, currentBarWidth, barHeight);
    }

    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
    public EnemyType getType() { return type; }
}