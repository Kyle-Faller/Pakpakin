
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import menu.BirdFlocks;
import menu.MainMenu;
import ui.Pixelfont;

/**
 * Information screen: a wooden sign hanging from two chains that shows the player's name and
 * highest score, plus a "Change Name" button.
 *
 * Shows the player's name (from Name.java) and their highest score, which is 0 until they have played.
 * Main calls setPlayerName(...) and setHighScore(...) to keep both up to date.
 */
public class Info extends JPanel {
    // Crops of the parts inside the uploaded images (x, y, width, height in original pixels)
    private static final int BOARD_X = 352, BOARD_Y = 729, BOARD_W = 711, BOARD_H = 553;   // scoreboard.png
    private static final int BTN_X = 175, BTN_Y = 166, BTN_W = 1055, BTN_H = 546;          // change.png (Change Name button)
    private static final int CHAIN_L_X = 523, CHAIN_R_X = 866, CHAIN_Y = 689, CHAIN_W = 36, CHAIN_H = 24; // one repeating chain segment

    private static final Color ORANGE = new Color(255, 105, 25);
    private static final Color GREEN = new Color(160, 208, 120);
    private static final Color OUTLINE = new Color(110, 45, 15);

    private final BufferedImage background, board, button, menuIcon, chainL, chainR;
    private BufferedImage boardScaled, buttonScaled, menuScaled, chainLScaled, chainRScaled;   // cached at the current panel size

    private String playerName = "Player";   // replaced by the name typed on the Name screen
    private int highScore = 0;              // 0 until the player has played (Main sets the saved best score)
    private Runnable onChangeName;
    private Runnable onBack;                          // called to leave this screen (Menu button, or ESC)

    private final Rectangle hitBox = new Rectangle();    // clickable area of the Change Name button
    private final Rectangle menuHit = new Rectangle();   // clickable area of the Menu button
    private static final int NONE = 0, CHANGE = 1, MENU = 2;
    private int hover = NONE, pressed = NONE;            // which button the mouse is over / holding down

    /** Quick test: Info <-> MainMenu (and Name). The real app is Main.java, which also opens the games. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pakpakin");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            Info info = new Info(null);
            MainMenu menu = new MainMenu();
            info.setPreferredSize(new Dimension(360, 640));
            menu.setPreferredSize(new Dimension(360, 640));

            info.setOnBack(() -> swap(frame, menu));                  // Menu button -> main menu
            menu.setProfileListener(() -> swap(frame, info));         // Profile -> this screen
            info.setOnChangeName(() -> swap(frame, new Name(newName -> {
                info.setPlayerName(newName);
                swap(frame, info);
            })));

            frame.setContentPane(info);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void swap(JFrame frame, JPanel screen) {
        frame.setContentPane(screen);
        frame.revalidate();
        frame.repaint();
        screen.requestFocusInWindow();
    }

    /** @param onChangeName called when the player presses the Change Name button */
    public Info(Runnable onChangeName) {
        this.onChangeName = onChangeName;
        try {
            // Put these files in your resources folder (next to the other images)
            background = read("image/flappybirdbg2.png");
            BufferedImage sheet = read("image/scoreboard.png");
            board = sheet.getSubimage(BOARD_X, BOARD_Y, BOARD_W, BOARD_H);
            chainL = sheet.getSubimage(CHAIN_L_X, CHAIN_Y, CHAIN_W, CHAIN_H);
            chainR = sheet.getSubimage(CHAIN_R_X, CHAIN_Y, CHAIN_W, CHAIN_H);
            button = read("image/change.png").getSubimage(BTN_X, BTN_Y, BTN_W, BTN_H);
            menuIcon = read("image/menu.png");
        } catch (IOException e) {
            throw new RuntimeException("Could not load info-screen images. Check they are on the classpath.", e);
        }

        setFocusable(true);
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { setHover(buttonAt(e.getPoint())); }
            @Override public void mouseExited(MouseEvent e) { setHover(NONE); }
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                pressed = buttonAt(e.getPoint());
                repaint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                int was = pressed;
                pressed = NONE;
                repaint();
                if (was != NONE && buttonAt(e.getPoint()) == was) {
                    if (was == CHANGE) changeName();
                    else goBack();
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_C) changeName();
                else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) goBack();
            }
        });
    }

    private static BufferedImage read(String file) throws IOException {
        URL url = Info.class.getResource("/" + file);
        if (url == null) {
            throw new IOException(file + " was not found on the classpath. Put it in your resources "
                    + "folder (next to the other images), then rebuild/refresh the project.");
        }
        return ImageIO.read(url);
    }

    // ---------------------------------------------------------------- data

    public String getPlayerName() { return playerName; }
    public int getHighScore() { return highScore; }

    public void setPlayerName(String name) {
        this.playerName = (name == null || name.isBlank()) ? "Player" : name;
        repaint();
    }

    public void setHighScore(int score) {
        this.highScore = score;
        repaint();
    }

    public void setOnChangeName(Runnable onChangeName) {
        this.onChangeName = onChangeName;
    }

    /** Sets what happens when the player leaves this screen (Menu button, or ESC). */
    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    private void changeName() {
        if (onChangeName != null) onChangeName.run();
    }

    private void goBack() {
        if (onBack != null) onBack.run();
    }

    private int buttonAt(Point p) {
        if (hitBox.contains(p)) return CHANGE;
        if (menuHit.contains(p)) return MENU;
        return NONE;
    }

    private void setHover(int h) {
        if (h == hover) return;
        hover = h;
        setCursor(Cursor.getPredefinedCursor(h != NONE ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    // ---------------------------------------------------------------- painting

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        int w = getWidth(), h = getHeight();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Background (same as the menu): scaled to fill, keeping aspect ratio
        double bgScale = Math.max((double) w / background.getWidth(), (double) h / background.getHeight());
        int dw = (int) (background.getWidth() * bgScale), dh = (int) (background.getHeight() * bgScale);
        g.drawImage(background, (w - dw) / 2, (h - dh) / 2, dw, dh, null);

        // Sign, centred on the panel
        int bw = (int) (w * 0.84), bh = r(bw * BOARD_H / (double) BOARD_W);
        int bx = (w - bw) / 2, by = (h - bh) / 2;
        double s = bw / (double) BOARD_W;

        int cw = Math.max(1, r(CHAIN_W * s)), ch = Math.max(1, r(CHAIN_H * s));
        if (boardScaled == null || boardScaled.getWidth() != bw) boardScaled = BirdFlocks.scale(board, bw, bh);
        if (chainLScaled == null || chainLScaled.getWidth() != cw || chainLScaled.getHeight() != ch) {
            chainLScaled = BirdFlocks.scale(chainL, cw, ch);
            chainRScaled = BirdFlocks.scale(chainR, cw, ch);
        }

        // Chains: repeat one link segment from the top of the sign up past the top of the screen
        int chainLx = bx + r((CHAIN_L_X - BOARD_X) * s);
        int chainRx = bx + r((CHAIN_R_X - BOARD_X) * s);
        for (int y = by + r(3 * s); y > 0; y -= ch) {
            g.drawImage(chainLScaled, chainLx, y - ch, null);
            g.drawImage(chainRScaled, chainRx, y - ch, null);
        }
        g.drawImage(boardScaled, bx, by, null);

        int maxW = r(0.80 * bw);
        int left = bx + r(0.104 * bw);

        // Title
        int tps = Math.max(1, r(bw * 0.77 / 65));
        while (tps > 1 && Pixelfont.width("Information", tps) > maxW) tps--;
        drawTwoTone(g, "Information", 6, bx + (bw - Pixelfont.width("Information", tps)) / 2,
                by + r(0.125 * bh), tps);

        // Name / score rows. The labels are as big as the sign allows ("Highest Score:" fills the width);
        // the values are a little smaller. Sizes are fractional so they use all the room available.
        double ls = Math.min(maxW / 83.0, 0.43 * bh / 28.7);   // label scale
        double vs = 0.85 * ls;                                 // value scale (a little smaller than the labels)
        int y = by + r(0.25 * bh);
        drawScaled(g, "Name:", 99, left, y, ls);
        y += r(7 * ls + 0.7 * ls);
        drawScaled(g, playerName, 0, left, y, valueScale(playerName, vs, maxW));
        y += r(7 * vs + 1.4 * ls);
        drawScaled(g, "Highest Score:", "Highest ".length(), left, y, ls);
        y += r(7 * ls + 0.7 * ls);
        String score = String.valueOf(highScore);
        drawScaled(g, score, 0, left, y, valueScale(score, vs, maxW));

        // Change Name button (the picture already contains its label), centred, hanging over the bottom edge of the sign
        int btnW = r(0.62 * bw);
        int btnH = r(btnW * BTN_H / (double) BTN_W);
        int btnX = bx + (bw - btnW) / 2, btnY = by + r(0.685 * bh);
        if (buttonScaled == null || buttonScaled.getWidth() != btnW || buttonScaled.getHeight() != btnH) {
            buttonScaled = BirdFlocks.scale(button, btnW, btnH);
        }
        hitBox.setBounds(btnX - 4, btnY - 4, btnW + 8, btnH + 8);

        drawButton(g, buttonScaled, btnX, btnY, btnW, btnH, CHANGE, s);

        // Menu button (list icon) in the top-left corner: goes back to the main menu
        int mw = r(0.15 * w), mh = r(mw * menuIcon.getHeight() / (double) menuIcon.getWidth());
        int mx = r(0.04 * w), my = r(0.03 * h);
        if (menuScaled == null || menuScaled.getWidth() != mw || menuScaled.getHeight() != mh) {
            menuScaled = BirdFlocks.scale(menuIcon, mw, mh);
        }
        menuHit.setBounds(mx - 2, my - 2, mw + 4, mh + 4);
        drawButton(g, menuScaled, mx, my, mw, mh, MENU, s);
    }

    /** Draws a button image; it swells a little on hover and sinks a little while pressed. */
    private void drawButton(Graphics2D g, BufferedImage img, int x, int y, int w, int h, int id, double s) {
        int push = (pressed == id && hover == id) ? Math.max(1, r(2 * s)) : 0;
        int grow = hover == id ? Math.max(1, w / 24) : 0;
        g.drawImage(img, x - grow, y - grow + push, w + 2 * grow, h + 2 * grow, null);
    }

    /** Text where the characters before 'split' are orange and the rest green, with a dark outline-shadow. */
    private static void drawTwoTone(Graphics2D g, String text, int split, int x, int y, int ps) {
        int shadow = Math.max(1, ps / 2);
        split = Math.min(split, text.length());
        String a = text.substring(0, split), b = text.substring(split);
        Pixelfont.drawShadowed(g, a, x, y, ps, ORANGE, OUTLINE, shadow);
        if (!b.isEmpty()) {
            Pixelfont.drawShadowed(g, b, x + split * 6 * ps, y, ps, GREEN, OUTLINE, shadow);
        }
    }

    /** The value scale, reduced if the text would not fit the sign. */
    private static double valueScale(String text, double scale, int maxW) {
        return Math.min(scale, maxW / (double) Math.max(1, Pixelfont.width(text, 1)));
    }

    /**
     * Text at any (fractional) size: drawn once at 1x into a tiny image, then enlarged with
     * nearest-neighbour so it stays crisp. Characters before 'split' are orange, the rest green
     * (a split of 0 means a plain orange value). Has a dark drop shadow.
     */
    private static void drawScaled(Graphics2D g, String text, int split, int x, int y, double scale) {
        int dw = Math.max(1, r(Pixelfont.width(text, 1) * scale)), dh = Math.max(1, r(7 * scale));
        int off = Math.max(1, r(scale / 2));
        g.drawImage(textImage(text, split, true), x + off, y + off, dw, dh, null);
        g.drawImage(textImage(text, split, false), x, y, dw, dh, null);
    }

    private static BufferedImage textImage(String text, int split, boolean shadow) {
        BufferedImage img = new BufferedImage(Math.max(1, Pixelfont.width(text, 1)), Pixelfont.height(1),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D ig = img.createGraphics();
        int cut = split <= 0 ? text.length() : Math.min(split, text.length());
        Pixelfont.draw(ig, text.substring(0, cut), 0, 0, 1, shadow ? OUTLINE : ORANGE);
        if (cut < text.length()) {
            Pixelfont.draw(ig, text.substring(cut), cut * 6, 0, 1, shadow ? OUTLINE : GREEN);
        }
        ig.dispose();
        return img;
    }

    private static int r(double v) {
        return (int) Math.round(v);
    }
}