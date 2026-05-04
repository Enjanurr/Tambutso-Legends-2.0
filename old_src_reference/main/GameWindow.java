package main;

import javax.swing.JFrame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.Dimension;
import java.awt.Toolkit;

public class GameWindow {
      private JFrame jframe;

      public GameWindow(GamePanel gamePanel){
          jframe = new JFrame();
          jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          jframe.add(gamePanel);
          jframe.setResizable(false);
          jframe.pack();

          Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
          int x = (screenSize.width  - jframe.getWidth())  / 2;
          int y = (screenSize.height - jframe.getHeight()) / 2;
          jframe.setLocation(x, y);
          jframe.setVisible(true);

          jframe.addWindowFocusListener(new WindowFocusListener() {

              @Override
              public void windowLostFocus(WindowEvent e) {
                  gamePanel.getGame().windowFocusLost();
              }

              @Override
              public void windowGainedFocus(WindowEvent e) {

              }
          });

      }
}
