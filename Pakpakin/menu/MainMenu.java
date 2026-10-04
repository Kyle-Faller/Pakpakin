package menu;
import javax.imageio.ImageIO;
import javax.swing.*;



import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.Consumer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
/** Pakpakin main menu: cliff background + title sign + Play / difficulty selector / Profile / Quit buttons. */
public class MainMenu extends JPanel {
    private final BufferedImage background, title;
    private final MenuButton play, profile, quit;
    private final DifficultySelector difficulty;   // [ < ] Easy / Medium / Hard [ > ]
    private final BirdFlocks flocks;
    private final Timer animation;   // drives the flocks (javax.swing.Timer)
    private long lastTick;
    private Clip menuMusic;
    private Consumer<Difficulty> playListener;   // called when Play is clicked, with the chosen difficulty
    private Runnable profileListener;            // called when Profile is clicked
    public MainMenu() {
        setLayout(null); // we position everything ourselves

        try {
            // Put these 3 files in your resources folder (classpath root)
            background = ImageIO.read(MainMenu.class.getResource("/image/flappybirdbg2.png"));
            BufferedImage titleSheet = ImageIO.read(MainMenu.class.getResource("/image/title.png"));
            BufferedImage buttonSheet = ImageIO.read(MainMenu.class.getResource("/image/buttonui.png"));
            
            // Crop title.png around the sign (x, y, w, h). The crop is padded on the right
            // so the sign is centred and the bird overhangs on the left.
            title = titleSheet.getSubimage(93, 302, 1223, 692);

            // bird.gif: animated bird sprite used for the migrating flocks (width in pixels on screen)
            flocks = new BirdFlocks(MainMenu.class.getResource("/image/bird.gif"), 40);

            // buttonui.png is one sheet with 4 stacked buttons: crop each one (x, y, w, h)
            play    = new MenuButton(buttonSheet.getSubimage(344,  162, 748, 390), this::onPlay);
            profile = new MenuButton(buttonSheet.getSubimage(344, 1037, 748, 390), this::onProfile);
            quit    = new MenuButton(buttonSheet.getSubimage(344, 1479, 748, 390), this::onQuit);

            // Difficulty row. easy/medium/hard.png are the centre labels and extrabuttons.png holds the
            // two arrows. These images have a white background, so it is removed first.
            BufferedImage[] labels = ImageUtil.cleanAndTrim(
                    ImageIO.read(MainMenu.class.getResource("/image/easy.png")),
                    ImageIO.read(MainMenu.class.getResource("/image/medium.png")),
                    ImageIO.read(MainMenu.class.getResource("/image/hard.png")));
            BufferedImage arrowSheet = ImageUtil.removeWhiteBackground(
                    ImageIO.read(MainMenu.class.getResource("/image/extrabuttons.png")));
            BufferedImage rightArrow = arrowSheet.getSubimage(598,  756, 222, 236);   // top arrow  >
            BufferedImage leftArrow  = arrowSheet.getSubimage(594, 1092, 222, 236);   // bottom arrow <
            difficulty = new DifficultySelector(labels, leftArrow, rightArrow, this::onDifficultyChanged);
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("Could not load menu images. Check they are on the classpath.", e);
        }

        add(play); add(difficulty); add(profile); add(quit);

        animation = new Timer(16, e -> {                  // ~60 frames per second
           
            long now = System.nanoTime();
            double dt = Math.min((now - lastTick) / 1e9, 0.05);
            lastTick = now;
            flocks.update(dt, getWidth(), getHeight(), titleBounds());
            repaint();
        });
    }

    // Run the animation only while the menu is on screen
    @Override public void addNotify()    { 
        super.addNotify(); 
        lastTick = System.nanoTime(); 
        animation.start(); 
        playMenuMusic();
    }
    @Override public void removeNotify() {
        animation.stop();
        stopMenuMusic();                       // the menu music must not keep playing under the game
        super.removeNotify();
    }

    private void stopMenuMusic() {
        if (menuMusic != null) {
            menuMusic.stop();
            menuMusic.close();
            menuMusic = null;
        }
    }

    /** Sets what happens when Play is clicked; it receives the difficulty chosen with the arrows. */
    public void setPlayListener(Consumer<Difficulty> listener) { this.playListener = listener; }

    /** Sets what happens when Profile is clicked (opens the Info screen). */
    public void setProfileListener(Runnable listener) { this.profileListener = listener; }

    /** The difficulty currently chosen with the arrows. Use it when starting the game. */
    public Difficulty getDifficulty() { return difficulty.getDifficulty(); }

    // ---- Button actions: hook these up to your game ----
    private void onPlay() {
        if (playListener != null) playListener.accept(getDifficulty());
        else System.out.println("Play (" + getDifficulty() + ")");
    }
    private void onDifficultyChanged(Difficulty d) { System.out.println("Difficulty: " + d); }
    private void onProfile() {
        if (profileListener != null) profileListener.run();
        else System.out.println("Profile");
    }
    private void onQuit()    { System.exit(0); }

    private static final double BTN_WIDTH = 0.36;  // button width as fraction of panel width
    private static final double GAP_TITLE = 0.03;  // space between title and buttons (fraction of height)
    private static final double GAP_BTN   = 0.008; // space between buttons (fraction of height)

    private int buttonHeight() {
        return (int) (getWidth() * BTN_WIDTH / play.aspectRatio());
    }
    
    /** Title + buttons form one group that is centred vertically and horizontally. */
    private Rectangle titleBounds() {
        int w = getWidth(), h = getHeight();
        int tw = (int) (w * 0.90);
        int th = (int) (tw * title.getHeight() / (double) title.getWidth());
        int groupH = th + (int) (h * GAP_TITLE) + 4 * buttonHeight() + 3 * (int) (h * GAP_BTN);
        return new Rectangle((w - tw) / 2, (h - groupH) / 2, tw, th);
    }
     private void playMenuMusic() {
    try {
        AudioInputStream audio = AudioSystem.getAudioInputStream(
            MainMenu.class.getResource("/music/menu.wav")
        );

        menuMusic = AudioSystem.getClip();
        menuMusic.open(audio);
        menuMusic.loop(Clip.LOOP_CONTINUOUSLY);
        menuMusic.start();

    } catch (Exception e) {
        e.printStackTrace();
    }
}
    /** Positions the buttons below the title, centred. */
    @Override public void doLayout() {
        int w = getWidth(), h = getHeight();
        int bw = (int) (w * BTN_WIDTH);
        int bh = buttonHeight();
        int gap = (int) (h * GAP_BTN);
        Rectangle t = titleBounds();
        int x = (w - bw) / 2;
        int y = t.y + t.height + (int) (h * GAP_TITLE);

        play.setBounds(x, y, bw, bh);        y += bh + gap;
        difficulty.place(x, y, bw, bh);      y += bh + gap;   // arrows sit on both sides of the centre label
        profile.setBounds(x, y, bw, bh);     y += bh + gap;
        quit.setBounds(x, y, bw, bh);
    }

    /** Draws the background (scaled to fill, keeping aspect ratio) and the title on top. */
    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        double scale = Math.max((double) getWidth() / background.getWidth(),
                                (double) getHeight() / background.getHeight());
        int dw = (int) (background.getWidth() * scale);
        int dh = (int) (background.getHeight() * scale);
        g2.drawImage(background, (getWidth() - dw) / 2, (getHeight() - dh) / 2, dw, dh, null);

        flocks.draw(g2);                                 // migrating flocks: part of the background

        Rectangle t = titleBounds();
        g2.drawImage(title, t.x, t.y, t.width, t.height, null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pakpakin");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            MainMenu menu = new MainMenu();
            menu.setPreferredSize(new Dimension(360, 640)); // 360 x 640 (width x height)
            frame.setContentPane(menu);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}