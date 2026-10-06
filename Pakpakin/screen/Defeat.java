package screen;

import javax.swing.SwingUtilities;
import java.awt.Rectangle;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;



public class Defeat extends ResultScreen {
  private void playDefeatMusic() {
    try {
        AudioInputStream audio =
            AudioSystem.getAudioInputStream(
                Defeat.class.getResource("/music/defeat.wav")
            );

        Clip clip = AudioSystem.getClip();
        clip.open(audio);
        clip.start();

    } catch (Exception e) {
        e.printStackTrace();
    }
}
    public Defeat() {
        super("defeat.png", new Rectangle(60, 25, 1231, 769),
              new Rectangle(398, 770, 636, 530),
              "retry.png", new Rectangle(288, 1455, 360, 385),
              new Rectangle(812, 1460, 356, 380),
              true);
              playDefeatMusic();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> show(new Defeat(), "Defeat"));
    }
}