package inputs;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import gameStates.GameStates;
import main.Game;
import main.GamePanel;

public class MouseInputs implements MouseListener, MouseMotionListener {
    private GamePanel gamePanel;


    public MouseInputs(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        switch (GameStates.state) {
            case CHAR_SELECT:

                gamePanel.getGame().getCharSelectState().mouseClicked(e);
                break;
            case PLAYING:
                gamePanel.getGame().getPlaying().mouseClicked(e);
                break;
            case BLUE_JEEP_VS_BOSS3:
                gamePanel.getGame().getBlueJeepVsBoss3State().mouseClicked(e);
                break;

            case RED_JEEP_VS_BOSS3:
                gamePanel.getGame().getRedJeepVsBoss3State().mouseClicked(e);
                break;

            case GREEN_JEEP_VS_BOSS3:
                gamePanel.getGame().getGreenJeepVsBoss3State().mouseClicked(e);
                break;
            default:
                break;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        switch (GameStates.state) {
            case CHAR_SELECT:
                gamePanel.getGame().getCharSelectState().mousePressed(e);
                break;
            case MENU:
                gamePanel.getGame().getMenu().mousePressed(e);
                break;
            case OPTIONS:
                gamePanel.getGame().getOptions().mousePressed(e);
                break;
            case INTRO:
                gamePanel.getGame().getIntroOverlay().mousePressed(e);
                break;
            case PLAYING:
                gamePanel.getGame().getPlaying().mousePressed(e);
                break;
            case BLUE_JEEP_VS_BOSS3:
                gamePanel.getGame().getBlueJeepVsBoss3State().mousePressed(e);
                break;

            case RED_JEEP_VS_BOSS3:
                gamePanel.getGame().getRedJeepVsBoss3State().mousePressed(e);
                break;

            case GREEN_JEEP_VS_BOSS3:
                gamePanel.getGame().getGreenJeepVsBoss3State().mousePressed(e);
                break;
            default:
                break;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        switch (GameStates.state) {
            case CHAR_SELECT:
                gamePanel.getGame().getCharSelectState().mouseReleased(e);
                break;
            case MENU:
                gamePanel.getGame().getMenu().mouseReleased(e);
                break;
            case OPTIONS:
                gamePanel.getGame().getOptions().mouseReleased(e);
                break;
            case INTRO:
                gamePanel.getGame().getIntroOverlay().mouseReleased(e);
                break;
            case PLAYING:
                gamePanel.getGame().getPlaying().mouseReleased(e);
                break;
            case BLUE_JEEP_VS_BOSS3:
                gamePanel.getGame().getBlueJeepVsBoss3State().mouseReleased(e);
                break;

            case RED_JEEP_VS_BOSS3:
                gamePanel.getGame().getRedJeepVsBoss3State().mouseReleased(e);
                break;

            case GREEN_JEEP_VS_BOSS3:
                gamePanel.getGame().getGreenJeepVsBoss3State().mouseReleased(e);
                break;
            default:
                break;
        }
    }

    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e)  {}

    @Override
    public void mouseDragged(MouseEvent e) {
        switch (GameStates.state) {
            case OPTIONS:
                gamePanel.getGame().getOptions().mouseDragged(e);
                break;
            case PLAYING:
                gamePanel.getGame().getPlaying().mouseDragged(e);
                break;
            case BLUE_JEEP_VS_BOSS3:
                gamePanel.getGame().getBlueJeepVsBoss3State().mouseDragged(e);
                break;

            case RED_JEEP_VS_BOSS3:
                gamePanel.getGame().getRedJeepVsBoss3State().mouseDragged(e);
                break;

            case GREEN_JEEP_VS_BOSS3:
                gamePanel.getGame().getGreenJeepVsBoss3State().mouseDragged(e);
                break;
            default:
                break;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        switch (GameStates.state) {
            case CHAR_SELECT:
                gamePanel.getGame().getCharSelectState().mouseMoved(e);
                break;
            case MENU:
                gamePanel.getGame().getMenu().mouseMoved(e);
                break;
            case OPTIONS:
                gamePanel.getGame().getOptions().mouseMoved(e);
                break;
            case INTRO:
                gamePanel.getGame().getIntroOverlay().mouseMoved(e);
                break;
            case PLAYING:
                gamePanel.getGame().getPlaying().mouseMoved(e);
                break;
            case BLUE_JEEP_VS_BOSS3:
                gamePanel.getGame().getBlueJeepVsBoss3State().mouseMoved(e);
                break;

            case RED_JEEP_VS_BOSS3:
                gamePanel.getGame().getRedJeepVsBoss3State().mouseMoved(e);
                break;

            case GREEN_JEEP_VS_BOSS3:
                gamePanel.getGame().getGreenJeepVsBoss3State().mouseMoved(e);
                break;
            default:
                break;
        }
    }
}
