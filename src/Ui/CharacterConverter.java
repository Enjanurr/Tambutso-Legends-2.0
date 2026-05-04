package Ui;

import main.Game;
import utils.LoadSave;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class CharacterConverter {

    private int spriteWidth;
    private int spriteHeight;
    private BufferedImage[][] sprites;
    private final Map<Character, int[]> charMap;

    private float scale = 1.0f;
    private float characterSpacing = 2.0f;

    public CharacterConverter() {
        this(1.0f, 2.0f);
    }

    public CharacterConverter(float scale, float spacing) {
        this.scale = scale;
        this.characterSpacing = spacing;
        charMap = new HashMap<>();
        loadSprites();
        buildCharacterMap();
    }

    private void loadSprites() {
        BufferedImage sheet = LoadSave.getSpriteAtlas(LoadSave.CHARACTERS_SPRITE);
        if (sheet == null) {
            System.err.println("[CharacterConverter] Failed to load sprite sheet");
            return;
        }

        int cols = 10;  // 10 columns
        int rows = 5;   // 5 rows

        spriteWidth = sheet.getWidth() / cols;
        spriteHeight = sheet.getHeight() / rows;

        System.out.println("[CharacterConverter] Loaded: " + sheet.getWidth() + "x" + sheet.getHeight()
                + " -> " + cols + "x" + rows + ", sprite: " + spriteWidth + "x" + spriteHeight);

        sprites = new BufferedImage[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                sprites[row][col] = sheet.getSubimage(
                        col * spriteWidth, row * spriteHeight, spriteWidth, spriteHeight);
            }
        }
    }

    private void buildCharacterMap() {
        // =========================================================
        // Row 0: A-J (col 0-9)
        // =========================================================
        charMap.put('A', new int[]{0, 0});
        charMap.put('B', new int[]{0, 1});
        charMap.put('C', new int[]{0, 2});
        charMap.put('D', new int[]{0, 3});
        charMap.put('E', new int[]{0, 4});
        charMap.put('F', new int[]{0, 5});
        charMap.put('G', new int[]{0, 6});
        charMap.put('H', new int[]{0, 7});
        charMap.put('I', new int[]{0, 8});
        charMap.put('J', new int[]{0, 9});

        // =========================================================
        // Row 1: K-T (col 0-9)
        // =========================================================
        charMap.put('K', new int[]{1, 0});
        charMap.put('L', new int[]{1, 1});
        charMap.put('M', new int[]{1, 2});
        charMap.put('N', new int[]{1, 3});
        charMap.put('O', new int[]{1, 4});
        charMap.put('P', new int[]{1, 5});
        charMap.put('Q', new int[]{1, 6});
        charMap.put('R', new int[]{1, 7});
        charMap.put('S', new int[]{1, 8});
        charMap.put('T', new int[]{1, 9});

        // =========================================================
        // Row 2: U-Z (col 0-5 only; col 6-9 are empty)
        // =========================================================
        charMap.put('U', new int[]{2, 0});
        charMap.put('V', new int[]{2, 1});
        charMap.put('W', new int[]{2, 2});
        charMap.put('X', new int[]{2, 3});
        charMap.put('Y', new int[]{2, 4});
        charMap.put('Z', new int[]{2, 5});

        // =========================================================
        // Row 3: Numbers 0-9 (col 0-9)
        // =========================================================
        charMap.put('0', new int[]{3, 0});
        charMap.put('1', new int[]{3, 1});
        charMap.put('2', new int[]{3, 2});
        charMap.put('3', new int[]{3, 3});
        charMap.put('4', new int[]{3, 4});
        charMap.put('5', new int[]{3, 5});
        charMap.put('6', new int[]{3, 6});
        charMap.put('7', new int[]{3, 7});
        charMap.put('8', new int[]{3, 8});
        charMap.put('9', new int[]{3, 9});

        // =========================================================
        // Row 4: Symbols (UPDATED LAYOUT - May 2026)
        // =========================================================
        // Col 0: = (equals)
        // Col 1: x / X (multiplication)
        // Col 2: : (colon)
        // Col 3: ? (question mark)
        // Col 4: ! (exclamation)
        // Col 5: ( (left parenthesis)
        // Col 6: ) (right parenthesis)
        // Col 7: + (plus)
        // Col 8: - (minus/hyphen)
        // Col 9: (empty - do not use)

        charMap.put('=', new int[]{4, 0});  // Equals sign
        charMap.put('x', new int[]{4, 1});  // Multiplication (lowercase)
        charMap.put('X', new int[]{4, 1});  // Multiplication (uppercase)
        charMap.put(':', new int[]{4, 2});  // Colon
        charMap.put('?', new int[]{4, 3});  // Question mark
        charMap.put('!', new int[]{4, 4});  // Exclamation point
        charMap.put('(', new int[]{4, 5});  // Left parenthesis
        charMap.put(')', new int[]{4, 6});  // Right parenthesis
        charMap.put('+', new int[]{4, 7});  // Plus sign
        charMap.put('-', new int[]{4, 8});  // Minus/hyphen
        // Col 9 is empty - no mapping

        // Lowercase letters map to uppercase sprites
        for (char c = 'a'; c <= 'z'; c++) {
            int[] upper = charMap.get(Character.toUpperCase(c));
            if (upper != null) charMap.put(c, upper);
        }

        // Debug print to verify mappings
        System.out.println("[CharacterConverter] Symbol mappings loaded (Updated May 2026):");
        System.out.println("  '=' -> row 4, col 0 = " + (charMap.get('=') != null ? "FOUND" : "MISSING"));
        System.out.println("  'x' -> row 4, col 1 = " + (charMap.get('x') != null ? "FOUND" : "MISSING"));
        System.out.println("  ':' -> row 4, col 2 = " + (charMap.get(':') != null ? "FOUND" : "MISSING"));
        System.out.println("  '?' -> row 4, col 3 = " + (charMap.get('?') != null ? "FOUND" : "MISSING"));
        System.out.println("  '!' -> row 4, col 4 = " + (charMap.get('!') != null ? "FOUND" : "MISSING"));
        System.out.println("  '(' -> row 4, col 5 = " + (charMap.get('(') != null ? "FOUND" : "MISSING"));
        System.out.println("  ')' -> row 4, col 6 = " + (charMap.get(')') != null ? "FOUND" : "MISSING"));
        System.out.println("  '+' -> row 4, col 7 = " + (charMap.get('+') != null ? "FOUND" : "MISSING"));
        System.out.println("  '-' -> row 4, col 8 = " + (charMap.get('-') != null ? "FOUND" : "MISSING"));
    }

    public int getStringWidth(String text) {
        if (text == null || text.isEmpty()) return 0;
        int charWidth = (int)(spriteWidth * Game.SCALE * scale);
        int spacing = (int)(characterSpacing * Game.SCALE);
        return text.length() * charWidth + (text.length() - 1) * spacing;
    }

    public int getCharHeight() {
        return (int)(spriteHeight * Game.SCALE * scale);
    }

    public void drawString(Graphics g, String text, int x, int y) {
        if (sprites == null || text == null || text.isEmpty()) return;

        int charWidth = (int)(spriteWidth * Game.SCALE * scale);
        int charHeight = (int)(spriteHeight * Game.SCALE * scale);
        int spacing = (int)(characterSpacing * Game.SCALE);

        int currentX = x;

        for (char c : text.toCharArray()) {
            if (c == ' ') {
                currentX += charWidth + spacing;
                continue;
            }

            // Direct lookup first, then try uppercase for letters
            int[] pos = charMap.get(c);
            if (pos == null && Character.isLetter(c)) {
                pos = charMap.get(Character.toUpperCase(c));
            }

            if (pos != null && pos[0] < sprites.length && pos[1] < sprites[0].length) {
                BufferedImage sprite = sprites[pos[0]][pos[1]];
                if (sprite != null) {
                    g.drawImage(sprite, currentX, y, charWidth, charHeight, null);
                }
            } else {
                // Fallback - draw character as text for debugging
                g.setColor(Color.RED);
                g.fillRect(currentX, y, charWidth, charHeight);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Monospaced", Font.PLAIN, charHeight - 2));
                g.drawString(String.valueOf(c), currentX + 2, y + charHeight - 4);
                System.err.println("[CharacterConverter] Missing mapping for: '" + c + "' (ASCII " + (int)c + ")");
            }

            currentX += charWidth + spacing;
        }
    }

    public void drawNumber(Graphics g, int number, int x, int y) {
        drawString(g, String.valueOf(number), x, y);
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setCharacterSpacing(float spacing) {
        this.characterSpacing = spacing;
    }
}