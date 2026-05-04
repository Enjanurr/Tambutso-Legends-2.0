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
            case INTRO:

            case PLAYING:
                gamePanel.getGame().getPlaying().mouseClicked(e);
                break;
            case BLUE_JEEP_VS_BOSS1:
                gamePanel.getGame().getBlueJeepVsBoss1State().mouseClicked(e);
                break;
            case RED_JEEP_VS_BOSS1:
                gamePanel.getGame().getRedJeepVsBoss1State().mouseClicked(e);
                break;
            case GREEN_JEEP_VS_BOSS1:
                gamePanel.getGame().getGreenJeepVsBoss1State().mouseClicked(e);
                break;
            case BLUE_JEEP_VS_BOSS2:
                gamePanel.getGame().getBlueJeepVsBoss2State().mouseClicked(e);
                break;
            case RED_JEEP_VS_BOSS2:
                gamePanel.getGame().getRedJeepVsBoss2State().mouseClicked(e);
                break;
            case GREEN_JEEP_VS_BOSS2:
                gamePanel.getGame().getGreenJeepVsBoss2State().mouseClicked(e);
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
            case PLAYING:
                gamePanel.getGame().getPlaying().mousePressed(e);
                break;
            case BLUE_JEEP_VS_BOSS1:
                gamePanel.getGame().getBlueJeepVsBoss1State().mousePressed(e);
                break;
            case RED_JEEP_VS_BOSS1:
                gamePanel.getGame().getRedJeepVsBoss1State().mousePressed(e);
                break;
            case GREEN_JEEP_VS_BOSS1:
                gamePanel.getGame().getGreenJeepVsBoss1State().mousePressed(e);
                break;
            case BLUE_JEEP_VS_BOSS2:
                gamePanel.getGame().getBlueJeepVsBoss2State().mousePressed(e);
                break;
            case RED_JEEP_VS_BOSS2:
                gamePanel.getGame().getRedJeepVsBoss2State().mousePressed(e);
                break;
            case GREEN_JEEP_VS_BOSS2:
                gamePanel.getGame().getGreenJeepVsBoss2State().mousePressed(e);
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
            case PLAYING:
                gamePanel.getGame().getPlaying().mouseReleased(e);
                break;
            case BLUE_JEEP_VS_BOSS1:
                gamePanel.getGame().getBlueJeepVsBoss1State().mouseReleased(e);
                break;
            case RED_JEEP_VS_BOSS1:
                gamePanel.getGame().getRedJeepVsBoss1State().mouseReleased(e);
                break;
            case GREEN_JEEP_VS_BOSS1:
                gamePanel.getGame().getGreenJeepVsBoss1State().mouseReleased(e);
                break;
            case BLUE_JEEP_VS_BOSS2:
                gamePanel.getGame().getBlueJeepVsBoss2State().mouseReleased(e);
                break;
            case RED_JEEP_VS_BOSS2:
                gamePanel.getGame().getRedJeepVsBoss2State().mouseReleased(e);
                break;
            case GREEN_JEEP_VS_BOSS2:
                gamePanel.getGame().getGreenJeepVsBoss2State().mouseReleased(e);
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
            case INTRO:
            case PLAYING:
                gamePanel.getGame().getPlaying().mouseDragged(e);
                break;
            case BLUE_JEEP_VS_BOSS1:
                gamePanel.getGame().getBlueJeepVsBoss1State().mouseDragged(e);
                break;
            case RED_JEEP_VS_BOSS1:
                gamePanel.getGame().getRedJeepVsBoss1State().mouseDragged(e);
                break;
            case GREEN_JEEP_VS_BOSS1:
                gamePanel.getGame().getGreenJeepVsBoss1State().mouseDragged(e);
                break;
            case BLUE_JEEP_VS_BOSS2:
                gamePanel.getGame().getBlueJeepVsBoss2State().mouseDragged(e);
                break;
            case RED_JEEP_VS_BOSS2:
                gamePanel.getGame().getRedJeepVsBoss2State().mouseDragged(e);
                break;
            case GREEN_JEEP_VS_BOSS2:
                gamePanel.getGame().getGreenJeepVsBoss2State().mouseDragged(e);
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
            case PLAYING:
                gamePanel.getGame().getPlaying().mouseMoved(e);
                break;
            case BLUE_JEEP_VS_BOSS1:
                gamePanel.getGame().getBlueJeepVsBoss1State().mouseMoved(e);
                break;
            case RED_JEEP_VS_BOSS1:
                gamePanel.getGame().getRedJeepVsBoss1State().mouseMoved(e);
                break;
            case GREEN_JEEP_VS_BOSS1:
                gamePanel.getGame().getGreenJeepVsBoss1State().mouseMoved(e);
                break;
            case BLUE_JEEP_VS_BOSS2:
                gamePanel.getGame().getBlueJeepVsBoss2State().mouseMoved(e);
                break;
            case RED_JEEP_VS_BOSS2:
                gamePanel.getGame().getRedJeepVsBoss2State().mouseMoved(e);
                break;
            case GREEN_JEEP_VS_BOSS2:
                gamePanel.getGame().getGreenJeepVsBoss2State().mouseMoved(e);
                break;
            default:
                break;
        }
    }
}
