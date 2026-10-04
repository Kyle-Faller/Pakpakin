package menu;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/**
 * Pakpakin loading screen: the menu background, the loading box, a segmented progress bar (0-100)
 * and the bird flying just above the bar, following the end of the progress.
 *
 * Use setProgress(0..100) from your real loading code, or startSimulation() for a demo load.
 * onDone runs once, shortly after the bar reaches 100.
 */
public class LoadingScreen extends JPanel {
    // The box inside loading.png (x, y, width, height in pixels of the original image)
    private static final int BOX_X = 117, BOX_Y = 690, BOX_W = 1160, BOX_H = 624;

    // Bar measurements, in original-image pixels relative to the box's top-left corner
    private static final int TRACK_X = 304, TRACK_Y = 309, TRACK_W = 548;
    private static final int TRACK_CREAM_H = 89, TRACK_H = 97;   // cream part / cream + white bottom line
    private static final int BLOCK_X = 312, BLOCK_Y = 317, BLOCK_W = 40, BLOCK_H = 73, BLOCKS = 11;
    private static final double BLOCK_PITCH = 49.2;

    private static final Color CREAM = new Color(251, 229, 201);
    private static final Color GREEN = new Color(168, 214, 124);

    private static final int BIRD_WIDTH = 48;        // bird width in pixels on screen
    private static final double FLAP_SECONDS = 0.15; // time per wing frame
    private static final double FINISH_DELAY = 0.6;  // pause at 100% before onDone

    // 3x5 pixel digits for the percentage text
    private static final String[][] DIGITS = {
        {"111", "101", "101", "101", "111"}, {"010", "110", "010", "010", "111"},
        {"111", "001", "111", "100", "111"}, {"111", "001", "111", "001", "111"},
        {"101", "101", "111", "001", "001"}, {"111", "100", "111", "001", "111"},
        {"111", "100", "111", "101", "111"}, {"111", "001", "001", "001", "001"},
        {"111", "101", "111", "101", "111"}, {"111", "101", "111", "001", "111"}};
    private static final String[] PERCENT = {"101", "001", "010", "100", "101"};

    private final BufferedImage background, box;
    private final BufferedImage[] birdFrames;
    private final Runnable onDone;
    private final Timer animation;
    private final Random rnd = new Random();

    private BufferedImage boxScaled;                 // box scaled to the current panel width (cached)
    private double target = 0, shown = 0;            // wanted progress and the smoothly displayed one
    private double time, finishTimer, simTimer, simSpeed;
    private boolean simulating, finished;
    private long lastTick;

    public LoadingScreen(Runnable onDone) {
        this.onDone = onDone;
        try {
            // Put these 3 files in your resources folder (classpath root)
            background = ImageIO.read(LoadingScreen.class.getResource("/image/flappybirdbg2.png"));
            BufferedImage sheet = ImageIO.read(LoadingScreen.class.getResource("/image/loading.png"));
            box = sheet.getSubimage(BOX_X, BOX_Y, BOX_W, BOX_H);
            birdFrames = BirdFlocks.loadFrames(LoadingScreen.class.getResource("/image/bird.gif"), BIRD_WIDTH);
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("Could not load loading-screen images. Check they are on the classpath.", e);
        }
        animation = new Timer(16, e -> tick());
    }

    /** Sets the real progress, 0 to 100. The bar and bird glide toward it. */
    public void setProgress(int percent) {
        simulating = false;
        target = Math.max(0, Math.min(100, percent));
    }

    /** Fakes a load of about 3 seconds with uneven speed (for testing the screen). */
    public void startSimulation() {
        simulating = true;
    }

    // Run the animation only while the screen is on display
    @Override public void addNotify()    { super.addNotify(); lastTick = System.nanoTime(); animation.start(); }
    @Override public void removeNotify() { animation.stop(); super.removeNotify(); }

    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min((now - lastTick) / 1e9, 0.05);
        lastTick = now;
        time += dt;

        if (simulating) {                                  // random walk in speed: fast bursts and slow parts
            simTimer -= dt;
            if (simTimer <= 0) {
                simSpeed = 8 + rnd.nextDouble() * 55;
                simTimer = 0.15 + rnd.nextDouble() * 0.35;
            }
            target = Math.min(100, target + simSpeed * dt);
        }

        if (shown < target) {                              // ease toward the target so motion stays smooth
            shown = Math.min(target, shown + Math.max((target - shown) * 5 * dt, 15 * dt));
        }

        if (!finished && shown >= 99.999) {
            finishTimer += dt;
            if (finishTimer >= FINISH_DELAY) {
                finished = true;
                if (onDone != null) onDone.run();
            }
        }
        repaint();
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        int w = getWidth(), h = getHeight();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Background (same as the menu): scaled to fill, keeping aspect ratio
        double bgScale = Math.max((double) w / background.getWidth(), (double) h / background.getHeight());
        int dw = (int) (background.getWidth() * bgScale), dh = (int) (background.getHeight() * bgScale);
        g.drawImage(background, (w - dw) / 2, (h - dh) / 2, dw, dh, null);

        // Loading box, centred
        int bw = (int) (w * 0.86);
        int bh = (int) Math.round(bw * BOX_H / (double) BOX_W);
        int bx = (w - bw) / 2, by = (h - bh) / 2;
        double s = bw / (double) BOX_W;                    // box-image pixels -> screen pixels
        if (boxScaled == null || boxScaled.getWidth() != bw) boxScaled = BirdFlocks.scale(box, bw, bh);
        g.drawImage(boxScaled, bx, by, null);

        // Bar track: cream with a white bottom edge
        int trackX = bx + r(TRACK_X * s), trackY = by + r(TRACK_Y * s);
        g.setColor(CREAM);
        g.fillRect(trackX, trackY, r(TRACK_W * s), r(TRACK_CREAM_H * s));
        g.setColor(Color.WHITE);
        g.fillRect(trackX, trackY + r(TRACK_CREAM_H * s), r(TRACK_W * s), r((TRACK_H - TRACK_CREAM_H) * s));

        // Green blocks, revealed from the left up to the current progress
        int innerX = bx + r(BLOCK_X * s);
        int innerEnd = bx + r((BLOCK_X + (BLOCKS - 1) * BLOCK_PITCH + BLOCK_W) * s);
        int fillEnd = innerX + r(shown / 100.0 * (innerEnd - innerX));
        Shape oldClip = g.getClip();
        g.clipRect(innerX, 0, fillEnd - innerX, h);
        g.setColor(GREEN);
        for (int i = 0; i < BLOCKS; i++) {
            int x0 = bx + r((BLOCK_X + i * BLOCK_PITCH) * s);
            int x1 = bx + r((BLOCK_X + i * BLOCK_PITCH + BLOCK_W) * s);
            g.fillRect(x0, by + r(BLOCK_Y * s), x1 - x0, r(BLOCK_H * s));
        }
        g.setClip(oldClip);

        // Bird hovering over the end of the progress, flapping and bobbing
        BufferedImage f = birdFrames[(int) (time / FLAP_SECONDS) % birdFrames.length];
        double birdCx = fillEnd;
        double birdCy = trackY - 7 - f.getHeight() / 2.0 + Math.sin(time * 6) * 3;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(f, (int) Math.round(birdCx - f.getWidth() / 2.0), (int) Math.round(birdCy - f.getHeight() / 2.0), null);

        // Percentage to the right of the bar, in pixel digits
        int ps = Math.max(2, r(s * 11));                   // size of one "pixel" of the digits
        int textX = trackX + r(TRACK_W * s) + r(40 * s);
        int textY = trackY + r(TRACK_H * s / 2.0) - 5 * ps / 2;
        g.setColor(Color.BLACK);
        drawPixelText(g, Math.round((float) shown) + "%", textX, textY, ps);
    }

    private static int r(double v) {
        return (int) Math.round(v);
    }

    /** Draws digits and % as 3x5 blocks of size ps. */
    private static void drawPixelText(Graphics2D g, String text, int x, int y, int ps) {
        for (char c : text.toCharArray()) {
            String[] glyph = c == '%' ? PERCENT : DIGITS[c - '0'];
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 3; col++) {
                    if (glyph[row].charAt(col) == '1') g.fillRect(x + col * ps, y + row * ps, ps, ps);
                }
            }
            x += 4 * ps;                                   // 3 columns + 1 gap
        }
    }
}