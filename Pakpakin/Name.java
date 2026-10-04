import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import menu.BirdFlocks;
import ui.Pixelfont;
/**
 * Name entry screen: the bird asks "Please enter your name" in a speech bubble and the player types
 * the name on the scroll. Press ENTER to confirm; the name is passed to onDone.
 */
public class Name extends JPanel {
    private static final int MAX_NAME = 12;

    // Crops of the parts inside the uploaded images (x, y, width, height in original pixels)
    private static final int BUB_X = 424, BUB_Y = 694, BUB_W = 565, BUB_H = 611;   // chat.png
    private static final int SCR_X = 263, SCR_Y = 800, SCR_W = 898, SCR_H = 368;   // scroll.png
    // Writing area inside the bubble / scroll, relative to each crop
    private static final int BUB_TEXT_X = 81, BUB_TEXT_Y = 116, BUB_TEXT_W = 402, BUB_TEXT_H = 390;
    private static final int SCR_TEXT_CX = 449, SCR_TEXT_CY = 184, SCR_TEXT_W = 690;

    private static final int BIRD_WIDTH = 84;         // bird width in pixels on screen
    private static final double FLAP_SECONDS = 0.15;  // time per wing frame
    private static final double CHARS_PER_SECOND = 22; // bubble typewriter speed
    private static final double CONFIRM_DELAY = 1.4;  // how long the bird's reply shows before moving on

    private static final Color BROWN = new Color(120, 60, 10);
    private static final Color NAVY = new Color(10, 40, 90);

    private final BufferedImage background, bubble, scroll;
    private final BufferedImage[] birdFrames;
    private final Consumer<String> onDone;
    private final Timer animation;
    private final StringBuilder name = new StringBuilder();

    private BufferedImage bubbleScaled, scrollScaled;   // cached at the current panel size
    private String message = "Please enter your name";
    private double time, messageAge, confirmTimer;
    private boolean confirmed;
    private long lastTick;

    public static void main(String[] args) {
    JFrame frame = new JFrame("Pakpakin");
    frame.setSize(320, 640);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    Name namePanel = new Name(name -> {
        System.out.println("Player: " + name);
    });

    frame.add(namePanel, BorderLayout.CENTER);

    frame.setLocationRelativeTo(null); // centers the WINDOW on screen
    frame.setVisible(true);
}
    /** @param onDone called with the typed name after the player presses ENTER */
    public Name(Consumer<String> onDone) {
        this.onDone = onDone;
        try {
            // Put these files in your resources folder (classpath root)
            background = read("image/flappybirdbg2.png");
            BufferedImage chat = read("image/chat.png");
            BufferedImage scrollSheet = read("image/scroll.png");
            bubble = chat.getSubimage(BUB_X, BUB_Y, BUB_W, BUB_H);
            scroll = scrollSheet.getSubimage(SCR_X, SCR_Y, SCR_W, SCR_H);
            birdFrames = BirdFlocks.loadFrames(Name.class.getResource("image/bird.gif"), BIRD_WIDTH);
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException("Could not load name-screen images. Check they are on the classpath.", e);
        }

        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); }
        });
        addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) { handleKey(e.getKeyChar()); }
        });
        animation = new Timer(16, e -> tick());
    }

    private static BufferedImage read(String file) throws IOException {
        URL url = Name.class.getResource("/" + file);
        if (url == null) {
            throw new IOException(file + " was not found on the classpath. Put it in your resources "
                    + "folder (next to the other images), then rebuild/refresh the project.");
        }
        return ImageIO.read(url);
    }

    // ---------------------------------------------------------------- input

    private void handleKey(char c) {
        if (confirmed) return;
        if (c == '\n' || c == '\r') {
            submit();
        } else if (c == '\b' || c == 127) {
            if (name.length() > 0) name.setLength(name.length() - 1);
        } else if (name.length() < MAX_NAME && allowed(c)) {
            if (c != ' ' || name.length() > 0) name.append(c);   // no leading space
        }
    }

    private static boolean allowed(char c) {
        return c < 127 && Pixelfont.supports(c) && (Character.isLetterOrDigit(c) || " -_.".indexOf(c) >= 0);
    }

    private void submit() {
        String typed = name.toString().trim();
        if (typed.isEmpty()) {
            say("I need a name first!");
        } else {
            confirmed = true;
            confirmTimer = 0;
            say("Nice to meet you, " + typed + "!");
        }
    }

    private void say(String text) {
        message = text;
        messageAge = 0;       // restarts the typewriter effect
    }

    // ---------------------------------------------------------------- animation

    // Run the animation only while the screen is on display
    @Override public void addNotify() {
        super.addNotify();
        lastTick = System.nanoTime();
        animation.start();
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    @Override public void removeNotify() {
        animation.stop();
        super.removeNotify();
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min((now - lastTick) / 1e9, 0.05);
        lastTick = now;
        time += dt;
        messageAge += dt;

        if (confirmed) {
            confirmTimer += dt;
            if (confirmTimer >= CONFIRM_DELAY) {
                animation.stop();                  // only report once
                if (onDone != null) onDone.accept(name.toString().trim());
                return;
            }
        }
        repaint();
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

        // Sizes
        int bubW = (int) (w * 0.46), bubH = r(bubW * BUB_H / (double) BUB_W);
        int scrW = (int) (w * 0.86), scrH = r(scrW * SCR_H / (double) SCR_W);
        int birdH = birdFrames[0].getHeight();

        // Vertical stack (bubble, bird, scroll, hint), centred on the panel
        int groupH = bubH + 12 + birdH / 2 + 16 + scrH + 14 + Pixelfont.height(2);
        int bubY = (h - groupH) / 2;
        int bubX = (int) (w * 0.40);
        int birdCx = (int) (w * 0.28);
        int birdCy = bubY + bubH + 12;
        int scrY = birdCy + birdH / 2 + 16;
        int scrX = (w - scrW) / 2;

        if (bubbleScaled == null || bubbleScaled.getWidth() != bubW) bubbleScaled = BirdFlocks.scale(bubble, bubW, bubH);
        if (scrollScaled == null || scrollScaled.getWidth() != scrW) scrollScaled = BirdFlocks.scale(scroll, scrW, scrH);
        g.drawImage(bubbleScaled, bubX, bubY, null);
        g.drawImage(scrollScaled, scrX, scrY, null);

        // Bird: flaps and bobs in place, facing the bubble
        BufferedImage f = birdFrames[(int) (time / FLAP_SECONDS) % birdFrames.length];
        double bob = Math.sin(time * 3) * 4;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(f, birdCx - f.getWidth() / 2, (int) Math.round(birdCy - f.getHeight() / 2.0 + bob), null);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        drawBubbleText(g, bubX, bubY, bubW / (double) BUB_W);
        drawName(g, scrX, scrY, scrW / (double) SCR_W);

        if (name.length() > 0 && !confirmed) {                 // hint under the scroll
            String hint = "Press ENTER to continue";
            int hx = (w - Pixelfont.width(hint, 2)) / 2;
            Pixelfont.drawShadowed(g, hint, hx, scrY + scrH + 14, 2, Color.WHITE, NAVY, 1);
        }
    }

    /** The bird's words, wrapped to fit the bubble and revealed one letter at a time. */
    private void drawBubbleText(Graphics2D g, int bubX, int bubY, double s) {
        int ps = 2;
        int areaX = bubX + r(BUB_TEXT_X * s), areaY = bubY + r(BUB_TEXT_Y * s);
        int areaW = r(BUB_TEXT_W * s), areaH = r(BUB_TEXT_H * s);

        List<String> lines = wrap(message, areaW, ps);
        int lineH = Pixelfont.height(ps) + 5;
        int y = areaY + (areaH - (lines.size() * lineH - 5)) / 2;
        int remaining = (int) (messageAge * CHARS_PER_SECOND);

        for (String line : lines) {
            String shown = line.substring(0, Math.max(0, Math.min(line.length(), remaining)));
            int x = areaX + (areaW - Pixelfont.width(line, ps)) / 2;   // centred by the full line, so letters don't jump
            Pixelfont.draw(g, shown, x, y, ps, Color.BLACK);
            remaining -= line.length() + 1;
            y += lineH;
        }
    }

    /** The typed name on the scroll (white with a shadow), a blinking cursor, or a hint when empty. */
    private void drawName(Graphics2D g, int scrX, int scrY, double s) {
        int cx = scrX + r(SCR_TEXT_CX * s), cy = scrY + r(SCR_TEXT_CY * s);
        int maxW = r(SCR_TEXT_W * s);
        String text = name.toString();

        if (text.isEmpty()) {                                   // placeholder
            String ph = "Type your name";
            int ps = 3;
            int x = cx - Pixelfont.width(ph, ps) / 2, y = cy - Pixelfont.height(ps) / 2;
            Pixelfont.drawShadowed(g, ph, x, y, ps, new Color(255, 255, 255, 150), new Color(120, 60, 10, 90), 2);
            return;
        }

        int ps = 4;                                             // biggest size that still fits
        while (ps > 2 && Pixelfont.width(text, ps) + 2 * ps > maxW) ps--;
        int cursorW = 3 * ps;
        int total = Pixelfont.width(text, ps) + ps + cursorW;
        int x = cx - total / 2, y = cy - Pixelfont.height(ps) / 2;
        int shadow = Math.max(1, ps / 2);

        Pixelfont.drawShadowed(g, text, x, y, ps, Color.WHITE, BROWN, shadow);
        if (!confirmed && ((int) (time * 2)) % 2 == 0) {        // blinking underscore cursor
            int cxr = x + Pixelfont.width(text, ps) + ps + ps;
            g.setColor(BROWN);
            g.fillRect(cxr + shadow, y + Pixelfont.height(ps) - ps + shadow, cursorW, ps);
            g.setColor(Color.WHITE);
            g.fillRect(cxr, y + Pixelfont.height(ps) - ps, cursorW, ps);
        }
    }

    /** Word-wraps text to the given pixel width (long words are split). */
    private static List<String> wrap(String text, int maxW, int ps) {
        List<String> lines = new ArrayList<>();
        String cur = "";
        for (String word : text.split(" ")) {
            while (Pixelfont.width(word, ps) > maxW) {          // a single word too long for a line
                int n = 1;
                while (n < word.length() && Pixelfont.width(word.substring(0, n + 1), ps) <= maxW) n++;
                if (!cur.isEmpty()) { lines.add(cur); cur = ""; }
                lines.add(word.substring(0, n));
                word = word.substring(n);
            }
            String trial = cur.isEmpty() ? word : cur + " " + word;
            if (!cur.isEmpty() && Pixelfont.width(trial, ps) > maxW) {
                lines.add(cur);
                cur = word;
            } else {
                cur = trial;
            }
        }
        if (!cur.isEmpty()) lines.add(cur);
        return lines;
    }

    private static int r(double v) {
        return (int) Math.round(v);
    }
}