package main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class SpriteAnimationTester extends JPanel implements Runnable {


    private static final String SPRITE_PATH  = "/Person/girl.png"; // ← ADJUST: path to sprite sheet
    private static final int    FRAME_WIDTH  = 61;   // ← ADJUST: width of each frame
    private static final int    FRAME_HEIGHT = 60;   // ← ADJUST: height of each frame
    private static final int    COLUMNS      = 12;   // ← ADJUST: columns in the sheet
    private static final int    ROWS         = 2;    // ← ADJUST: rows in the sheet
    private static final float  SCALE        = 3f;   // ← ADJUST: render scale (zoom)


    private static final int ANI_SPEED = 10; // ← ADJUST: ticks per frame (lower = faster)
    // -------------------------------------------------------

    private BufferedImage[][] frames;
    private int currentRow = 0;
    private int currentCol = 0;
    private int aniTick    = 0;

    private Thread thread;
    private final int UPS = 60;

    // Window size
    private static final int WIN_W = 800;
    private static final int WIN_H = 600;

    public SpriteAnimationTester() {
        setPreferredSize(new Dimension(WIN_W, WIN_H));
        setBackground(Color.WHITE);
        loadSprite();
        setupControls();
        startLoop();
    }

    private void loadSprite() {
        try {
            InputStream is = getClass().getResourceAsStream(SPRITE_PATH);
            if (is == null) {
                System.err.println("Could not find sprite: " + SPRITE_PATH);
                return;
            }
            BufferedImage sheet = ImageIO.read(is);
            frames = new BufferedImage[ROWS][COLUMNS];
            for (int r = 0; r < ROWS; r++)
                for (int c = 0; c < COLUMNS; c++)
                    frames[r][c] = sheet.getSubimage(
                            c * FRAME_WIDTH,
                            r * FRAME_HEIGHT,
                            FRAME_WIDTH,
                            FRAME_HEIGHT
                    );
            System.out.println("Loaded: " + SPRITE_PATH +
                    " | sheet: " + sheet.getWidth() + "x" + sheet.getHeight() +
                    " | frames: " + ROWS + " rows x " + COLUMNS + " cols");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean paused = false;

    private void setupControls() {
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        // Previous row
                        currentRow = (currentRow - 1 + ROWS) % ROWS;
                        currentCol = 0;
                        aniTick    = 0;
                        System.out.println("Row: " + currentRow);
                        break;
                    case KeyEvent.VK_DOWN:
                        // Next row
                        currentRow = (currentRow + 1) % ROWS;
                        currentCol = 0;
                        aniTick    = 0;
                        System.out.println("Row: " + currentRow);
                        break;
                    case KeyEvent.VK_SPACE:
                        paused = !paused;
                        System.out.println(paused ? "Paused" : "Resumed");
                        break;
                    case KeyEvent.VK_LEFT:
                        // Step back one frame manually (while paused)
                        if (paused) {
                            currentCol = (currentCol - 1 + COLUMNS) % COLUMNS;
                            System.out.println("Frame: " + currentCol);
                        }
                        break;
                    case KeyEvent.VK_RIGHT:
                        // Step forward one frame manually (while paused)
                        if (paused) {
                            currentCol = (currentCol + 1) % COLUMNS;
                            System.out.println("Frame: " + currentCol);
                        }
                        break;
                }
            }
        });
    }

    private void startLoop() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        double timePerUpdate = 1_000_000_000.0 / UPS;
        long prevTime = System.nanoTime();
        double delta = 0;

        while (true) {
            long now = System.nanoTime();
            delta += (now - prevTime) / timePerUpdate;
            prevTime = now;

            if (delta >= 1) {
                if (!paused) tick();
                repaint();
                delta--;
            }
        }
    }

    private void tick() {
        aniTick++;
        if (aniTick >= ANI_SPEED) {
            aniTick = 0;
            currentCol = (currentCol + 1) % COLUMNS;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawGrid(g);
        drawSprite(g);
        drawHUD(g);
    }

    private void drawGrid(Graphics g) {
        g.setColor(new Color(200, 200, 200)); // thin light gray lines

        // Vertical center line
        g.drawLine(WIN_W / 2, 0, WIN_W / 2, WIN_H);

        // Horizontal center line
        g.drawLine(0, WIN_H / 2, WIN_W, WIN_H / 2);

        // Optional faint full grid every 50px
        g.setColor(new Color(230, 230, 230));
        for (int x = 0; x < WIN_W; x += 50) g.drawLine(x, 0, x, WIN_H);
        for (int y = 0; y < WIN_H; y += 50) g.drawLine(0, y, WIN_W, y);

        // Re-draw center lines darker on top of the grid
        g.setColor(new Color(180, 180, 180));
        g.drawLine(WIN_W / 2, 0, WIN_W / 2, WIN_H);
        g.drawLine(0, WIN_H / 2, WIN_W, WIN_H / 2);
    }


    private void drawSprite(Graphics g) {
        if (frames == null) {
            g.setColor(Color.RED);
            g.drawString("No sprite loaded — check SPRITE_PATH", 50, WIN_H / 2);
            return;
        }

        int drawW = (int)(FRAME_WIDTH  * SCALE);
        int drawH = (int)(FRAME_HEIGHT * SCALE);
        int drawX = WIN_W / 2 - drawW / 2;
        int drawY = WIN_H / 2 - drawH / 2;

        // Draw frame bounding box
        g.setColor(new Color(100, 180, 255, 80));
        g.fillRect(drawX, drawY, drawW, drawH);
        g.setColor(new Color(100, 180, 255));
        g.drawRect(drawX, drawY, drawW, drawH);

        // Draw the sprite frame
        g.drawImage(frames[currentRow][currentCol], drawX, drawY, drawW, drawH, null);
    }

    private void drawHUD(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("Monospaced", Font.PLAIN, 13));

        int x = 12;
        int y = 20;
        int lineH = 18;

        g.drawString("Sprite : " + SPRITE_PATH,          x, y);
        g.drawString("Sheet  : " + COLUMNS + " cols x " + ROWS + " rows", x, y += lineH);
        g.drawString("Frame  : " + FRAME_WIDTH + " x " + FRAME_HEIGHT + " px", x, y += lineH);
        g.drawString("Scale  : " + SCALE + "x",          x, y += lineH);
        g.drawString("Row    : " + currentRow + " / " + (ROWS - 1), x, y += lineH);
        g.drawString("Col    : " + currentCol + " / " + (COLUMNS - 1), x, y += lineH);
        g.drawString("Speed  : " + ANI_SPEED + " ticks/frame", x, y += lineH);
        g.drawString(paused ? "[ PAUSED ]" : "[ PLAYING ]", x, y += lineH);

        // Controls reminder
        g.setColor(Color.GRAY);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        int cy = WIN_H - 70;
        g.drawString("UP / DOWN  — switch row", x, cy);
        g.drawString("SPACE      — pause / resume", x, cy + 14);
        g.drawString("LEFT / RIGHT — step frame (while paused)", x, cy + 28);
    }

    // ── Entry point ───────────────────────────────────────────
    public static void launch() {
        JFrame frame = new JFrame("Sprite Animation Tester");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        SpriteAnimationTester tester = new SpriteAnimationTester();
        frame.add(tester);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}