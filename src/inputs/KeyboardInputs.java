package inputs;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import gameStates.GameStates;
import main.Game;
import main.GamePanel;

public class KeyboardInputs implements KeyListener {
    private GamePanel gamePanel;

    public KeyboardInputs(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        switch (GameStates.state) {
            case CHAR_SELECT:
                gamePanel.getGame().getCharSelectState().keyReleased(e);
                break;
            case MENU:
                gamePanel.getGame().getMenu().keyReleased(e);
                break;
            case OPTIONS:
                gamePanel.getGame().getOptions().keyReleased(e);
                break;
            case PLAYING:
                gamePanel.getGame().getPlaying().keyReleased(e);
                break;

            // ── Boss Fight States ← CHANGE ──────────────────────
            case BLUE_JEEP_VS_BOSS3:
                gamePanel.getGame().getBlueJeepVsBoss3State().keyReleased(e);
                break;

            case RED_JEEP_VS_BOSS3:
                gamePanel.getGame().getRedJeepVsBoss3State().keyReleased(e);
                break;

            case GREEN_JEEP_VS_BOSS3:
                gamePanel.getGame().getGreenJeepVsBoss3State().keyReleased(e);
                break;


            default:
                break;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (GameStates.state) {
            case CHAR_SELECT:
                gamePanel.getGame().getCharSelectState().keyPressed(e);
                break;
            case MENU:
                gamePanel.getGame().getMenu().keyPressed(e);
                break;
            case OPTIONS:
                gamePanel.getGame().getOptions().keyPressed(e);
                break;
            case PLAYING:
                gamePanel.getGame().getPlaying().keyPressed(e);
                break;

            // ── Boss Fight States ← CHANGE ──────────────────────
            case BLUE_JEEP_VS_BOSS3:
                gamePanel.getGame().getBlueJeepVsBoss3State().keyPressed(e);
                break;

            case RED_JEEP_VS_BOSS3:
                gamePanel.getGame().getRedJeepVsBoss3State().keyPressed(e);
                break;

            case GREEN_JEEP_VS_BOSS3:
                gamePanel.getGame().getGreenJeepVsBoss3State().keyPressed(e);
                break;

            default:
                break;
        }
    }
}