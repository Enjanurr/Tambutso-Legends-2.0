package gameStates;

import entities.DriverProfile;
import main.Game;
import utils.LoadSave;
import Ui.UrmButton;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import static utils.Constants.UI.URMButtons.*;

public class CharSelectState extends State implements StateMethods {

    // ── Animation ─────────────────────────────
    private static final int ANIM_SPEED = 20;
    private static final int FRAMES_PER_ROW = DriverProfile.FRAMES_IDLE;

    private BufferedImage backgroundImgPink;

    // [driver][row][frame]
    private final BufferedImage[][][] frames;

    private int animTick = 0;
    private int animFrame = 0;

    // ── Layout ─────────────────────────────
    private static final int DISPLAY_SCALE = 4;
    private final int[] drawX;

    private int hoveredIndex = -1;
    private int selectedIndex = -1;

    private UrmButton selectBtn;

    private static final int SELECT_BTN_X =
            (int)(374 * Game.SCALE);

    private static final int SELECT_BTN_Y =
            (int)(325 * Game.SCALE);


    public CharSelectState(Game game) {
        super(game);
        System.out.println("[CharSelectState] Constructor called");

        backgroundImgPink =
                LoadSave.getSpriteAtlas(LoadSave.MENU_BACKGROUND_IMG);

        DriverProfile[] roster = DriverProfile.ALL;

        frames = new BufferedImage[roster.length][2][FRAMES_PER_ROW];

        drawX = new int[roster.length];

        // ── Load sprite rows (row0 = running, row1 = idle)
        for (int i = 0; i < roster.length; i++) {

            BufferedImage sheet =
                    LoadSave.getSpriteAtlas(roster[i].atlasPath);

            if (sheet == null) continue;

            for (int row = 0; row < 2; row++) {

                for (int f = 0; f < FRAMES_PER_ROW; f++) {

                    int frameW = (int)roster[i].width;

                    int frameH = (int)roster[i].height;

                    int srcX = f * frameW;

                    int srcY = row * frameH;

                    if (srcX + frameW <= sheet.getWidth() && srcY + frameH <= sheet.getHeight()) {

                        frames[i][row][f] = sheet.getSubimage(srcX, srcY, frameW, frameH);
                    }
                }
            }
        }

        // ── Position drivers evenly
        int spacing = Game.GAME_WIDTH / (roster.length + 1);

        for (int i = 0; i < roster.length; i++) {

            int dispW = (int)(roster[i].width * DISPLAY_SCALE);

            drawX[i] = spacing * (i + 1) - dispW / 2;
        }

        selectBtn = new UrmButton(SELECT_BTN_X, SELECT_BTN_Y, URM_SIZE, URM_SIZE, 0);
    }


    // ───────────────────────────────────────
    @Override
    public void update() {

        animTick++;

        if (animTick >= ANIM_SPEED) {

            animTick = 0;

            animFrame = (animFrame + 1) % FRAMES_PER_ROW;
        }

        selectBtn.update();
    }


    // ───────────────────────────────────────
    @Override
    public void draw(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        g2.drawImage(backgroundImgPink, 0, 0, Game.GAME_WIDTH, Game.GAME_HEIGHT, null);

        DriverProfile[] roster = DriverProfile.ALL;

        for (int i = 0; i < roster.length; i++) {

            int row = 1; // idle default

            if (i == hoveredIndex)
                row = 0; // running animation


            BufferedImage frame = frames[i][row][animFrame];

            if (frame == null)
                continue;


            int scale = DISPLAY_SCALE;

            if (i == selectedIndex)
                scale = DISPLAY_SCALE + 1;


            int dispW = (int)(roster[i].width * scale);

            int dispH = (int)(roster[i].height * scale);


            int baseW = (int)(roster[i].width
                            * DISPLAY_SCALE);

            int centerX = drawX[i] + baseW / 2;

            int adjustedX = centerX - dispW / 2;


            int drawY = Game.GAME_HEIGHT / 2 - dispH / 2;


            g2.drawImage(frame, adjustedX, drawY, dispW, dispH, null
            );
        }

        selectBtn.draw(g2);
    }


    // ───────────────────────────────────────
    @Override
    public void mouseMoved(MouseEvent e) {

        hoveredIndex = -1;

        DriverProfile[] roster =
                DriverProfile.ALL;

        for (int i = 0;
             i < roster.length;
             i++) {

            int dispW = (int)(roster[i].width * DISPLAY_SCALE);

            int dispH = (int)(roster[i].height * DISPLAY_SCALE);

            int drawY = Game.GAME_HEIGHT / 2 - dispH / 2;

            Rectangle bounds = new Rectangle(drawX[i], drawY, dispW, dispH);

            if (bounds.contains(e.getPoint())) {

                hoveredIndex = i;
                break;
            }
        }

        selectBtn.setMouseOver(
                selectBtn.getBounds().contains(e.getPoint())
        );
    }


    // ───────────────────────────────────────
    @Override
    public void mousePressed(MouseEvent e) {

        DriverProfile[] roster = DriverProfile.ALL;

        for (int i = 0; i < roster.length; i++) {

            int dispW = (int)(roster[i].width * DISPLAY_SCALE);

            int dispH = (int)(roster[i].height * DISPLAY_SCALE);

            int drawY = Game.GAME_HEIGHT / 2 - dispH / 2;

            Rectangle bounds = new Rectangle(drawX[i], drawY, dispW, dispH);

            if (bounds.contains(e.getPoint())) {
                selectedIndex = i;
                return;
            }
        }

        if (selectBtn.getBounds().contains(e.getPoint()))

            selectBtn.setMousePressed(true);
    }


    // ───────────────────────────────────────
    @Override
    public void mouseReleased(MouseEvent e) {

        if (selectBtn.isMousePressed() && selectBtn.getBounds().contains(e.getPoint())) {
            if (selectedIndex != -1) {
                DriverProfile selected = DriverProfile.ALL[selectedIndex];
                game.setSelectedDriver(selected);

                // ✨ DEBUG OUTPUT
                System.out.println("═══════════════════════════════");
                System.out.println("SELECTED: " + selected.displayName);
                System.out.println("Atlas: " + selected.atlasPath);
                System.out.println("Speed: " + selected.maxSpeed);
                System.out.println("═══════════════════════════════");

                System.out.println("[CharSelectState] Confirm clicked, starting intro");
                game.startIntroOverlay();
            }
        }

        selectBtn.resetBools();
    }


    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void keyPressed(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

}