package screen;

import menu.MainMenu;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;



public abstract class ResultScreen extends JPanel {
    static final int W = 1414, H = 2000;
    static final Color TEXT_GREEN = new Color(0x3F, 0x6E, 0x0F);

    private final BufferedImage bg, banner, board, leftImg, menuImg;
    private final Rectangle bannerR, boardR, leftR, menuR;
    private final boolean twoLineScores;

    private int currentScore = 123;
    private int highScore = 123;
    private Rectangle hover;
    private Runnable leftListener, menuListener;

    protected ResultScreen(String bannerFile, Rectangle bannerR, Rectangle boardR,
                           String leftFile, Rectangle leftR, Rectangle menuR,
                           boolean twoLineScores) {
        this.bannerR = bannerR; this.boardR = boardR;
        this.leftR = leftR;     this.menuR = menuR;
        this.twoLineScores = twoLineScores;

        bg      = load("flappybirdbg2.png");
        banner  = load(bannerFile);
        board   = load("scoreboard.png");
        leftImg = load(leftFile);
        menuImg = load("menu.png");

        setPreferredSize(new Dimension(360, 640));   

        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                Point p = toDesign(e.getPoint());
                hover = ResultScreen.this.leftR.contains(p) ? ResultScreen.this.leftR
                      : ResultScreen.this.menuR.contains(p) ? ResultScreen.this.menuR : null;
                setCursor(Cursor.getPredefinedCursor(hover != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                repaint();
            }
            @Override public void mouseExited(MouseEvent e) { hover = null; repaint(); }
            @Override public void mouseClicked(MouseEvent e) {
                Point p = toDesign(e.getPoint());
                if (ResultScreen.this.leftR.contains(p)) onLeft();
                else if (ResultScreen.this.menuR.contains(p)) onMenu();
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

   
    public void setScores(int current, int high) { currentScore = current; highScore = high; repaint(); }

    public void setLeftListener(Runnable r) { leftListener = r; }
   
    public void setMenuListener(Runnable r) { menuListener = r; }

    private void onLeft() {
        if (leftListener != null) leftListener.run();
        else System.out.println(getClass().getSimpleName() + ": left button clicked");
    }

    private void onMenu() {
        if (menuListener != null) { menuListener.run(); return; }
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof JFrame f) {
            f.setContentPane(new MainMenu());
            f.revalidate();
            f.repaint();
        }
    }

  
    private double scale() { return getWidth() / (double) W; }
    private int offY()     { return (int) ((getHeight() - H * scale()) / 2); }

    private Point toDesign(Point p) {
        double s = scale();
        return new Point((int) (p.x / s), (int) ((p.y - offY()) / s));
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        if (bg != null) {  
            double c = Math.max(getWidth() / (double) bg.getWidth(), getHeight() / (double) bg.getHeight());
            int bw = (int) (bg.getWidth() * c), bh = (int) (bg.getHeight() * c);
            g.drawImage(bg, (getWidth() - bw) / 2, (getHeight() - bh) / 2, bw, bh, null);
        }

        g.translate(0, offY());
        g.scale(scale(), scale());

        draw(g, banner, bannerR);
        draw(g, board, boardR);
        drawButton(g, leftImg, leftR);
        drawButton(g, menuImg, menuR);

        g.setColor(TEXT_GREEN);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 46));
        int cx = boardR.x + boardR.width / 2;
        int top = boardR.y + 200;
        if (twoLineScores) {
            center(g, "Current Score:", cx, top);
            center(g, String.valueOf(currentScore), cx, top + 48);
            center(g, "High Score:", cx, top + 94);
            center(g, String.valueOf(highScore), cx, top + 142);
        } else {
            center(g, "Current Score: " + currentScore, cx, top + 10);
            center(g, "High Score: " + highScore, cx, top + 102);
        }
        g.dispose();
    }

    private void drawButton(Graphics2D g, BufferedImage img, Rectangle r) {
        if (r == hover) draw(g, img, new Rectangle(r.x - 6, r.y - 6, r.width + 12, r.height + 12));
        else draw(g, img, r);
    }

    private void draw(Graphics2D g, BufferedImage img, Rectangle r) {
        if (img != null) g.drawImage(img, r.x, r.y, r.width, r.height, null);
    }

    private void center(Graphics2D g, String s, int cx, int baseline) {
        g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, baseline);
    }

    private static BufferedImage load(String name) {
        try {
            BufferedImage img = ImageIO.read(ResultScreen.class.getResource("/image/" + name));
            
            return name.equals("flappybirdbg2.png") ? img : trim(img);
        } catch (Exception e) {
            System.err.println("Could not load /image/" + name);
            return null;
        }
    }

   
    private static BufferedImage trim(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                int a = argb >>> 24, r = (argb >> 16) & 255, gr = (argb >> 8) & 255, b = argb & 255;
                boolean empty = a < 20 || (r > 245 && gr > 245 && b > 245);
                if (!empty) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) return src;   
        return src.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    
    protected static void show(ResultScreen screen, String title) {
        JFrame f = new JFrame(title);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setContentPane(screen);
        f.setResizable(false);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}