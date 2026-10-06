package screen;

import javax.swing.SwingUtilities;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.Rectangle;


public class Victory extends ResultScreen {
      private void playVictoryMusic() {
        try {
            AudioInputStream audio =
                AudioSystem.getAudioInputStream(
                    Victory.class.getResource("/music/victory.wav")
                );

            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            clip.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Victory() {
        super("victory.png", new Rectangle(55, 120, 1215, 746),
              new Rectangle(405, 842, 638, 520),
              "next.png", new Rectangle(175, 1437, 357, 385),
              new Rectangle(875, 1432, 367, 395),
              false);
              playVictoryMusic();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> show(new Victory(), "Victory"));
    }
}