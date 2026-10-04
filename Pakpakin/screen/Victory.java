package screen;

import javax.swing.SwingUtilities;
import java.awt.Rectangle;

/** Victory screen: left = next, right = main menu. Add the panel to your window like MainMenu. */
public class Victory extends ResultScreen {
    public Victory() {
        super("victory.png", new Rectangle(55, 120, 1215, 746),
              new Rectangle(405, 842, 638, 520),
              "next.png", new Rectangle(175, 1437, 357, 385),
              new Rectangle(875, 1432, 367, 395),
              false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> show(new Victory(), "Victory"));
    }
}