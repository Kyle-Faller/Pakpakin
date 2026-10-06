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

public class MainMenu extends JPanel {
    private final BufferedImage background, title;
    private final MenuButton play, profile, quit;
    private final DifficultySelector difficulty;  
    private final BirdFlocks flocks;
    private final Timer animation;   
    private long lastTick;
    private Clip menuMusic;
    private Consumer<Difficulty> playListener;   
    private Runnable profileListener;          
    public MainMenu() {
        setLayout(null); 

        try {
         
            background = ImageIO.read(MainMenu.class.getResource("/image/flappybirdbg2.png"));
            BufferedImage titleSheet = ImageIO.read(MainMenu.class.getResource("/image/title.png"));
            BufferedImage buttonSheet = ImageIO.read(MainMenu.class.getResource("/image/buttonui.png"));
            
          
            title = titleSheet.getSubimage(93, 302, 1223, 692);

            
            flocks = new BirdFlocks(MainMenu.class.getResource("/image/bird.gif"), 40);

           
            play    = new MenuButton(buttonSheet.getSubimage(344,  162, 748, 390), this::onPlay);
            profile = new MenuButton(buttonSheet.getSubimage(344, 1037, 748, 390), this::onProfile);
            quit    = new MenuButton(buttonSheet.getSubimage(344, 1479, 748, 390), this::onQuit);

           
            BufferedImage[] labels = ImageUtil.cleanAndTrim(
                    ImageIO.read(MainMenu.class.getResource("/image/easy.png")),
                    ImageIO.read(MainMenu.class.getResource("/image/medium.png")),
                    ImageIO.read(MainMenu.class.getResource("/image/hard.png")));
            BufferedImage arrowSheet = ImageUtil.removeWhiteBackground(
                    ImageIO.read(MainMenu.class.getResource("/image/extrabuttons.png")));
            BufferedImage rightArrow = arrowSheet.getSubimage(598,  756, 222, 236);   
            BufferedImage leftArrow  = arrowSheet.getSubimage(594, 1092, 222, 236);   
            difficulty = new DifficultySelector(labels, leftArrow, rightArrow, this::onDifficultyChanged);
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("Could not load menu images. Check they are on the classpath.", e);
        }

        add(play); add(difficulty); add(profile); add(quit);

        animation = new Timer(16, e -> {                 
           
            long now = System.nanoTime();
            double dt = Math.min((now - lastTick) / 1e9, 0.05);
            lastTick = now;
            flocks.update(dt, getWidth(), getHeight(), titleBounds());
            repaint();
        });
    }

   
    @Override public void addNotify()    { 
        super.addNotify(); 
        lastTick = System.nanoTime(); 
        animation.start(); 
        playMenuMusic();
    }
    @Override public void removeNotify() {
        animation.stop();
        stopMenuMusic();                       
        super.removeNotify();
    }

    private void stopMenuMusic() {
        if (menuMusic != null) {
            menuMusic.stop();
            menuMusic.close();
            menuMusic = null;
        }
    }

    
    public void setPlayListener(Consumer<Difficulty> listener) { this.playListener = listener; }

   
    public void setProfileListener(Runnable listener) { this.profileListener = listener; }

    
    public Difficulty getDifficulty() { return difficulty.getDifficulty(); }

    
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

    private static final double BTN_WIDTH = 0.36;  
    private static final double GAP_TITLE = 0.03; 
    private static final double GAP_BTN   = 0.008; 

    private int buttonHeight() {
        return (int) (getWidth() * BTN_WIDTH / play.aspectRatio());
    }
    

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

    @Override public void doLayout() {
        int w = getWidth(), h = getHeight();
        int bw = (int) (w * BTN_WIDTH);
        int bh = buttonHeight();
        int gap = (int) (h * GAP_BTN);
        Rectangle t = titleBounds();
        int x = (w - bw) / 2;
        int y = t.y + t.height + (int) (h * GAP_TITLE);

        play.setBounds(x, y, bw, bh);        y += bh + gap;
        difficulty.place(x, y, bw, bh);      y += bh + gap;   
        profile.setBounds(x, y, bw, bh);     y += bh + gap;
        quit.setBounds(x, y, bw, bh);
    }

    
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

        flocks.draw(g2);                                 

        Rectangle t = titleBounds();
        g2.drawImage(title, t.x, t.y, t.width, t.height, null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pakpakin");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            MainMenu menu = new MainMenu();
            menu.setPreferredSize(new Dimension(360, 640)); 
            frame.setContentPane(menu);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}