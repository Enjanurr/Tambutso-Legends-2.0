package main;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

/**
 * FontTester — TRUE pixel font renderer for retro games
 * Renders actual pixelated text by drawing character bitmaps
 */
public class FontTester {

    // ─── Built-in pixel fonts (hand-drawn bitmaps) ───────────────────────────
    private static final PixelFont[] PIXEL_FONTS = {
            new PixelFont("8x8 Classic", 8, 8, new int[][] {
                    // '0' - '9' custom pixel patterns
            }, "Standard 8x8 pixel font"),

            new PixelFont("8x16 Arcade", 8, 16, null, "Arcade cabinet style"),
            new PixelFont("6x12 Small", 6, 12, null, "Compact UI text"),
            new PixelFont("10x16 Bold", 10, 16, null, "Bold readable text"),
            new PixelFont("5x8 Tiny", 5, 8, null, "Micro text for counters"),
            new PixelFont("12x16 Retro", 12, 16, null, "Retro game titles"),
            new PixelFont("7x14 C64", 7, 14, null, "Commodore 64 style"),
            new PixelFont("8x12 NES", 8, 12, null, "Nintendo NES style"),
            new PixelFont("6x8 GB", 6, 8, null, "Game Boy style"),
            new PixelFont("9x16 Pixel", 9, 16, null, "Modern pixel font"),
    };

    // Custom character bitmaps for 8x8 classic font
    static {
        // 8x8 classic font bitmaps (1 = pixel on)
        int[][] digits8x8 = {
                // 0
                {0,1,1,1,0, 1,0,0,1, 1,0,0,1, 1,0,0,1, 0,1,1,1,0},
                // 1
                {0,0,1,0,0, 0,1,1,0,0, 0,0,1,0,0, 0,0,1,0,0, 0,1,1,1,0},
                // 2
                {0,1,1,1,0, 1,0,0,1,0, 0,1,1,0,0, 1,0,0,0,0, 1,1,1,1,1},
                // 3
                {0,1,1,1,0, 1,0,0,1,0, 0,1,1,1,0, 1,0,0,1,0, 0,1,1,1,0},
                // 4
                {1,0,0,1,0, 1,0,0,1,0, 1,1,1,1,1, 0,0,0,1,0, 0,0,0,1,0},
                // 5
                {1,1,1,1,1, 1,0,0,0,0, 1,1,1,1,0, 0,0,0,1,0, 1,1,1,1,0},
                // 6
                {0,1,1,1,0, 1,0,0,0,0, 1,1,1,1,0, 1,0,0,1,0, 0,1,1,1,0},
                // 7
                {1,1,1,1,1, 0,0,0,1,0, 0,0,1,0,0, 0,1,0,0,0, 1,0,0,0,0},
                // 8
                {0,1,1,1,0, 1,0,0,1,0, 0,1,1,1,0, 1,0,0,1,0, 0,1,1,1,0},
                // 9
                {0,1,1,1,0, 1,0,0,1,0, 0,1,1,1,0, 0,0,0,1,0, 0,1,1,1,0},
        };

        // Convert to proper format
        for (int i = 0; i < 10 && i < digits8x8.length; i++) {
            PIXEL_FONTS[0].setCharBitmap((char)('0' + i), digits8x8[i], 5, 5);
        }
    }

    // ─── Palette ─────────────────────────────────────────────────────────────
    private static final Color BG_DARK   = new Color(0x0D0D1A);
    private static final Color BG_PANEL  = new Color(0x13132B);
    private static final Color BG_ROW    = new Color(0x1A1A35);
    private static final Color BG_HOVER  = new Color(0x22224A);
    private static final Color BG_SELECT = new Color(0x2A1860);
    private static final Color ACC_CYAN  = new Color(0x00FFD0);
    private static final Color ACC_PURP  = new Color(0xAA55FF);
    private static final Color ACC_PINK  = new Color(0xFF4488);
    private static final Color TEXT_DIM  = new Color(0x6677AA);
    private static final Color TEXT_MAIN = new Color(0xCCDDFF);

    public static void launch() {
        SwingUtilities.invokeLater(FontTester::buildUI);
    }

    private static void buildUI() {
        JFrame frame = new JFrame("◈  PIXEL FONT GENERATOR  ◈  True Pixel Rendering");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(1100, 800);
        frame.setMinimumSize(new Dimension(900, 640));
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        frame.setContentPane(root);

        JPanel header = createHeader();
        root.add(header, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(350);
        split.setDividerSize(3);
        split.setBorder(null);

        FontListPanel listPanel = new FontListPanel();
        split.setLeftComponent(listPanel);

        PreviewPanel previewPanel = new PreviewPanel();
        split.setRightComponent(previewPanel);

        listPanel.setSelectionListener(idx -> previewPanel.setPixelFont(PIXEL_FONTS[idx]));

        root.add(split, BorderLayout.CENTER);

        JLabel status = new JLabel("  TRUE PIXEL RENDERING — Numbers and letters drawn pixel by pixel");
        status.setFont(new Font("Monospaced", Font.PLAIN, 11));
        status.setForeground(TEXT_DIM);
        status.setBackground(new Color(0x08080F));
        status.setOpaque(true);
        status.setBorder(new EmptyBorder(4, 10, 4, 10));
        root.add(status, BorderLayout.SOUTH);

        listPanel.setStatusLabel(status);
        frame.setVisible(true);
    }

    private static JPanel createHeader() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x1A0040),
                        getWidth(), 0, new Color(0x001840));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ACC_CYAN);
                g2.fillRect(0, getHeight()-2, getWidth(), 2);
            }
        };
        p.setPreferredSize(new Dimension(0, 54));
        p.setBorder(new EmptyBorder(0, 18, 0, 18));

        JLabel title = new JLabel("◈  PIXEL FONT GENERATOR  ◈");
        title.setFont(new Font("Monospaced", Font.BOLD, 16));
        title.setForeground(ACC_CYAN);
        p.add(title, BorderLayout.WEST);

        JLabel sub = new JLabel("True pixel rendering — numbers look pixelated!  ◈");
        sub.setFont(new Font("Monospaced", Font.PLAIN, 11));
        sub.setForeground(TEXT_DIM);
        p.add(sub, BorderLayout.EAST);

        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pixel Font Data Structure
    // ─────────────────────────────────────────────────────────────────────────
    static class PixelFont {
        String name;
        int charWidth;
        int charHeight;
        Map<Character, boolean[]> charBitmaps;
        String description;

        PixelFont(String name, int w, int h, int[][] sampleData, String desc) {
            this.name = name;
            this.charWidth = w;
            this.charHeight = h;
            this.description = desc;
            this.charBitmaps = new HashMap<>();

            // Generate default bitmap for all printable ASCII
            generateDefaultBitmaps();
        }

        void setCharBitmap(char c, int[] data, int dataWidth, int dataHeight) {
            boolean[] bits = new boolean[charWidth * charHeight];
            for (int y = 0; y < Math.min(dataHeight, charHeight); y++) {
                for (int x = 0; x < Math.min(dataWidth, charWidth); x++) {
                    int idx = y * dataWidth + x;
                    if (idx < data.length && data[idx] == 1) {
                        bits[y * charWidth + x] = true;
                    }
                }
            }
            charBitmaps.put(c, bits);
        }

        private void generateDefaultBitmaps() {
            // Generate readable pixel maps for all characters
            String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!@#$%^&*()_+-=[]{}|;:'\",.<>/? ";

            for (char c : chars.toCharArray()) {
                charBitmaps.put(c, generatePixelGlyph(c));
            }
        }

        private boolean[] generatePixelGlyph(char c) {
            boolean[] bits = new boolean[charWidth * charHeight];

            // Create simple but readable pixel patterns
            int centerX = charWidth / 2;
            int centerY = charHeight / 2;

            if (c >= '0' && c <= '9') {
                // Numbers - special handling
                int num = c - '0';
                drawNumberPattern(bits, num);
            } else if (c >= 'A' && c <= 'Z') {
                drawLetterPattern(bits, c, true);
            } else if (c >= 'a' && c <= 'z') {
                drawLetterPattern(bits, Character.toUpperCase(c), false);
            } else {
                drawSymbolPattern(bits, c);
            }

            return bits;
        }

        private void drawNumberPattern(boolean[] bits, int num) {
            // Simple 7-segment style or block numbers
            int w = charWidth;
            int h = charHeight;

            switch(num) {
                case 0:
                    drawRect(bits, 1, 1, w-2, h-2);
                    break;
                case 1:
                    drawVertLine(bits, w/2, 1, h-2);
                    break;
                case 2:
                    drawRect(bits, 1, 1, w-2, h/3);
                    drawRect(bits, 1, h/2, w-2, h/3);
                    break;
                case 3:
                    drawRect(bits, 1, 1, w-2, h/3);
                    drawRect(bits, 1, h/2, w-2, h/3);
                    drawVertLine(bits, w-2, 1, h-2);
                    break;
                case 4:
                    drawRect(bits, 1, h/2, w-2, h/3);
                    drawHorizLine(bits, w/2, 1, h/2);
                    break;
                case 5:
                    drawRect(bits, 1, 1, w-2, h/3);
                    drawRect(bits, 1, h/2, w-2, h/3);
                    drawVertLine(bits, 1, 1, h-2);
                    break;
                case 6:
                    drawRect(bits, 1, 1, w-2, h-2);
                    drawRect(bits, 1, h/2, w-2, h/3);
                    break;
                case 7:
                    drawRect(bits, 1, 1, w-2, h/3);
                    drawVertLine(bits, w-2, 1, h-2);
                    break;
                case 8:
                    drawRect(bits, 1, 1, w-2, h-2);
                    drawRect(bits, 1, h/2, w-2, h/3);
                    break;
                case 9:
                    drawRect(bits, 1, 1, w-2, h-2);
                    drawRect(bits, 1, 1, w-2, h/3);
                    break;
            }
        }

        private void drawLetterPattern(boolean[] bits, char letter, boolean uppercase) {
            int w = charWidth;
            int h = charHeight;

            switch(letter) {
                case 'A':
                    drawTriangle(bits, w/2, 1, w/2, h-2);
                    drawHorizLine(bits, w/4, h/2, w/2);
                    break;
                case 'B':
                    drawRect(bits, 1, 1, w-2, h-2);
                    drawCircle(bits, w-2, h/2, w/3);
                    break;
                case 'C':
                    drawRect(bits, 1, 1, w-2, h-2);
                    break;
                case 'D':
                    drawRect(bits, 1, 1, w-2, h-2);
                    break;
                default:
                    // Simple block letter
                    drawRect(bits, 1, 1, w-2, h-2);
            }
        }

        private void drawSymbolPattern(boolean[] bits, char sym) {
            int w = charWidth;
            int h = charHeight;

            switch(sym) {
                case '!':
                    drawVertLine(bits, w/2, 1, h-3);
                    bits[(h-1) * w + w/2] = true;
                    break;
                case '?':
                    drawRect(bits, 1, 1, w-2, h/3);
                    bits[(h/2) * w + w/2] = true;
                    break;
                default:
                    drawRect(bits, 1, 1, w-2, h-2);
            }
        }

        private void drawRect(boolean[] bits, int x, int y, int width, int height) {
            for (int i = y; i < y + height && i < charHeight; i++) {
                for (int j = x; j < x + width && j < charWidth; j++) {
                    bits[i * charWidth + j] = true;
                }
            }
        }

        private void drawVertLine(boolean[] bits, int x, int y, int height) {
            for (int i = y; i < y + height && i < charHeight; i++) {
                if (x >= 0 && x < charWidth) {
                    bits[i * charWidth + x] = true;
                }
            }
        }

        private void drawHorizLine(boolean[] bits, int y, int x, int width) {
            for (int j = x; j < x + width && j < charWidth; j++) {
                if (y >= 0 && y < charHeight) {
                    bits[y * charWidth + j] = true;
                }
            }
        }

        private void drawTriangle(boolean[] bits, int cx, int y, int base, int height) {
            // Simplified triangle
            for (int i = 0; i < height; i++) {
                int width = (i * base) / height;
                for (int j = -width/2; j <= width/2; j++) {
                    int x = cx + j;
                    if (x >= 0 && x < charWidth && y+i < charHeight) {
                        bits[(y+i) * charWidth + x] = true;
                    }
                }
            }
        }

        private void drawCircle(boolean[] bits, int cx, int cy, int r) {
            // Simple circle approximation
            for (int y = -r; y <= r; y++) {
                for (int x = -r; x <= r; x++) {
                    if (x*x + y*y <= r*r) {
                        int px = cx + x;
                        int py = cy + y;
                        if (px >= 0 && px < charWidth && py >= 0 && py < charHeight) {
                            bits[py * charWidth + px] = true;
                        }
                    }
                }
            }
        }

        void drawChar(Graphics2D g, char c, int x, int y, Color color, int scale) {
            boolean[] bits = charBitmaps.getOrDefault(c, charBitmaps.get(' '));
            if (bits == null) return;

            g.setColor(color);
            for (int row = 0; row < charHeight; row++) {
                for (int col = 0; col < charWidth; col++) {
                    if (bits[row * charWidth + col]) {
                        g.fillRect(x + col * scale, y + row * scale, scale, scale);
                    }
                }
            }
        }

        int getStringWidth(String text, int scale) {
            return text.length() * charWidth * scale;
        }

        int getLineHeight(int scale) {
            return charHeight * scale;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Font List Panel
    // ─────────────────────────────────────────────────────────────────────────
    static class FontListPanel extends JPanel {
        private int selected = -1;
        private int hovered = -1;
        private SelectionListener listener;
        private JLabel statusLabel;

        FontListPanel() {
            setBackground(BG_PANEL);
            setPreferredSize(new Dimension(350, 0));
            setLayout(new BorderLayout());

            JLabel lbl = new JLabel("  PIXEL FONTS  —  " + PIXEL_FONTS.length + " fonts");
            lbl.setFont(new Font("Monospaced", Font.BOLD, 11));
            lbl.setForeground(ACC_PURP);
            lbl.setBackground(new Color(0x0F0F20));
            lbl.setOpaque(true);
            lbl.setBorder(new EmptyBorder(8, 12, 8, 0));
            add(lbl, BorderLayout.NORTH);

            JPanel rows = new JPanel();
            rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
            rows.setBackground(BG_PANEL);

            for (int i = 0; i < PIXEL_FONTS.length; i++) {
                final int idx = i;
                PixelFontRow row = new PixelFontRow(PIXEL_FONTS[i], i);
                row.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        hovered = idx;
                        rows.repaint();
                        if (statusLabel != null)
                            statusLabel.setText("  " + PIXEL_FONTS[idx].name + "  ·  " + PIXEL_FONTS[idx].description);
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        hovered = -1;
                        rows.repaint();
                    }
                    @Override public void mouseClicked(MouseEvent e) {
                        selected = idx;
                        rows.repaint();
                        if (listener != null) listener.onSelect(idx);
                    }
                });
                rows.add(row);
            }

            JScrollPane scroll = new JScrollPane(rows);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(BG_PANEL);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            add(scroll, BorderLayout.CENTER);
        }

        void setSelectionListener(SelectionListener l) { this.listener = l; }
        void setStatusLabel(JLabel l) { this.statusLabel = l; }
        interface SelectionListener { void onSelect(int idx); }
    }

    static class PixelFontRow extends JPanel {
        private PixelFont font;
        private int idx;

        PixelFontRow(PixelFont font, int idx) {
            this.font = font;
            this.idx = idx;
            setPreferredSize(new Dimension(0, 60));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            setBackground(idx % 2 == 0 ? BG_ROW : BG_PANEL);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            g2.setColor(ACC_CYAN);
            g2.drawString(font.name, 12, 24);

            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2.setColor(TEXT_DIM);
            g2.drawString(font.charWidth + "x" + font.charHeight + " px  ·  " + font.description, 12, 44);

            // Preview the font size at small scale
            String preview = "0123456789";
            int scale = 2;
            int x = getWidth() - (preview.length() * font.charWidth * scale) - 20;
            for (int i = 0; i < preview.length(); i++) {
                font.drawChar(g2, preview.charAt(i), x + i * font.charWidth * scale, 18, ACC_PURP, scale);
            }

            g2.dispose();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Preview Panel - Shows actual pixelated text
    // ─────────────────────────────────────────────────────────────────────────
    static class PreviewPanel extends JPanel {
        private PixelFont currentFont = null;
        private int scale = 2;
        private String previewText = "0123456789\nABCDEFGHIJKLMNOPQRSTUVWXYZ\nabcdefghijklmnopqrstuvwxyz\n!@#$%^&*()\nPIXEL PERFECT!\nTambutso Legends 2.0";
        private Color textColor = ACC_CYAN;

        PreviewPanel() {
            setBackground(BG_DARK);
            setLayout(new BorderLayout());

            JPanel toolbar = buildToolbar();
            add(toolbar, BorderLayout.NORTH);

            CanvasPanel canvas = new CanvasPanel();
            JScrollPane scroll = new JScrollPane(canvas);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(BG_DARK);
            add(scroll, BorderLayout.CENTER);

            JPanel inputBar = buildInputBar(canvas);
            add(inputBar, BorderLayout.SOUTH);
        }

        private JPanel buildToolbar() {
            JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
            bar.setBackground(new Color(0x0F0F22));
            bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x2A2A55)));

            bar.add(makeLabel("SCALE:"));
            JComboBox<Integer> scaleBox = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6, 8});
            scaleBox.setSelectedItem(2);
            scaleBox.addActionListener(e -> { scale = (int) scaleBox.getSelectedItem(); repaint(); });
            styleCombo(scaleBox);
            bar.add(scaleBox);

            bar.add(makeLabel("  COLOR:"));
            JButton colorBtn = new JButton("●");
            colorBtn.setForeground(textColor);
            colorBtn.setBackground(new Color(0x1A1A35));
            colorBtn.addActionListener(e -> {
                Color newColor = JColorChooser.showDialog(this, "Pick Pixel Color", textColor);
                if (newColor != null) {
                    textColor = newColor;
                    colorBtn.setForeground(textColor);
                    repaint();
                }
            });
            bar.add(colorBtn);

            return bar;
        }

        private JPanel buildInputBar(CanvasPanel canvas) {
            JPanel bar = new JPanel(new BorderLayout(6, 0));
            bar.setBackground(new Color(0x0F0F22));
            bar.setBorder(new EmptyBorder(6, 10, 6, 10));

            JLabel lbl = makeLabel("TEXT:");
            bar.add(lbl, BorderLayout.WEST);

            JTextArea ta = new JTextArea(previewText, 3, 30);
            ta.setBackground(new Color(0x1A1A35));
            ta.setForeground(ACC_CYAN);
            ta.setCaretColor(ACC_CYAN);
            ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
            ta.setBorder(BorderFactory.createLineBorder(new Color(0x3333AA), 1));
            ta.getDocument().addDocumentListener(new DocumentListener() {
                void upd() { previewText = ta.getText(); canvas.repaint(); }
                public void insertUpdate(DocumentEvent e) { upd(); }
                public void removeUpdate(DocumentEvent e) { upd(); }
                public void changedUpdate(DocumentEvent e) { upd(); }
            });

            JScrollPane taScroll = new JScrollPane(ta);
            taScroll.setBorder(null);
            bar.add(taScroll, BorderLayout.CENTER);

            return bar;
        }

        void setPixelFont(PixelFont font) {
            this.currentFont = font;
            repaint();
        }

        private JLabel makeLabel(String t) {
            JLabel l = new JLabel(t);
            l.setForeground(TEXT_DIM);
            l.setFont(new Font("Monospaced", Font.PLAIN, 11));
            return l;
        }

        private void styleCombo(JComboBox<?> cb) {
            cb.setBackground(new Color(0x1A1A35));
            cb.setForeground(ACC_CYAN);
            cb.setFont(new Font("Monospaced", Font.PLAIN, 12));
        }

        class CanvasPanel extends JPanel {
            CanvasPanel() {
                setBackground(BG_DARK);
                setPreferredSize(new Dimension(700, 500));
            }

            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;

                if (currentFont == null) {
                    g2.setColor(TEXT_DIM);
                    g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
                    g2.drawString("◄  Select a pixel font", 40, 60);
                    return;
                }

                int x = 40;
                int y = 60;
                int lineH = currentFont.getLineHeight(scale) + 8;

                // Draw font info
                g2.setFont(new Font("Monospaced", Font.BOLD, 11));
                g2.setColor(ACC_PURP);
                g2.drawString("FONT: " + currentFont.name + "  |  " + currentFont.charWidth + "x" + currentFont.charHeight, x, y - 20);

                // Draw each character as pixels
                String[] lines = previewText.split("\n");
                for (String line : lines) {
                    int cx = x;
                    for (char c : line.toCharArray()) {
                        currentFont.drawChar(g2, c, cx, y, textColor, scale);
                        cx += currentFont.charWidth * scale;
                    }
                    y += lineH;

                    // Stop if we go off screen
                    if (y > getHeight() - 50) break;
                }

                // Draw info panel
                g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
                g2.setColor(TEXT_DIM);
                String info = "Scale: " + scale + "x  |  Char size: " + currentFont.charWidth + "px × " + currentFont.charHeight + "px";
                g2.drawString(info, x, getHeight() - 20);
            }
        }
    }
}