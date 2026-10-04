package screen;

import javax.swing.SwingUtilities;
import java.awt.Rectangle;

/** Defeat screen: left = retry, right = main menu. Add the panel to your window like MainMenu. */
public class Defeat extends ResultScreen {
    public Defeat() {
        super("defeat.png", new Rectangle(60, 25, 1231, 769),
              new Rectangle(398, 770, 636, 530),
              "retry.png", new Rectangle(288, 1455, 360, 385),
              new Rectangle(812, 1460, 356, 380),
              true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> show(new Defeat(), "Defeat"));
    }
}