package menu;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Flocks of birds (animated from bird.gif) that migrate horizontally across the background:
 * they enter from one side of the screen, cross it, and leave from the other side.
 */
public class BirdFlocks {
    private static final double FLAP_SECONDS = 0.15; // time per wing frame
    private static final int MAX_FLOCKS = 3;         // flocks on screen at once
    private static final double MIN_SCALE = 0.55;    // smallest (farthest) flock size
    private static final int MIN_BIRDS = 3, MAX_BIRDS = 7;

    /** One bird in a flock: its spot relative to the flock leader. */
    private static class Bird {
        double behind;   // how far behind the leader (in the flying direction)
        double dy;       // vertical offset from the leader
        double phase;    // wing-flap offset so the birds don't flap in sync
        double bob;      // bobbing offset
    }

    private static class Flock {
        final List<Bird> birds = new ArrayList<>();
        double x, y;     // leader position
        double speed;    // pixels per second
        double scale;    // 1.0 = full size, smaller = farther away (and slower)
        double tail;     // distance from the leader to the last bird
        int dir;         // +1 flies left to right, -1 flies right to left
    }

    private final BufferedImage[] frames;
    private final List<Flock> flocks = new ArrayList<>();
    private final Random rnd = new Random();
    private double time, spawnIn;
    private boolean started;

    /** @param gif the animated bird gif; @param width bird width in pixels on screen (at scale 1.0) */
    public BirdFlocks(URL gif, int width) throws IOException {
        frames = loadFrames(gif, width);
    }

    /**
     * Reads the animated gif and returns its frames, cropped to the same box and scaled to the given
     * width. Also used by the loading screen.
     */
    public static BufferedImage[] loadFrames(URL gif, int width) throws IOException {
        BufferedImage[] raw = readFrames(gif);

        // Crop all frames to the same box (the union of the bird's pixels) so they line up
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
        for (BufferedImage f : raw) {
            for (int py = 0; py < f.getHeight(); py++) {
                for (int px = 0; px < f.getWidth(); px++) {
                    if ((f.getRGB(px, py) >>> 24) > 10) {
                        minX = Math.min(minX, px); maxX = Math.max(maxX, px);
                        minY = Math.min(minY, py); maxY = Math.max(maxY, py);
                    }
                }
            }
        }
        int cw = maxX - minX + 1, ch = maxY - minY + 1;
        int height = Math.max(1, width * ch / cw);

        BufferedImage[] out = new BufferedImage[raw.length];
        for (int i = 0; i < raw.length; i++) {
            out[i] = scale(raw[i].getSubimage(minX, minY, cw, ch), width, height);
        }
        return out;
    }

    // ---------------------------------------------------------------- loading

    private static BufferedImage[] readFrames(URL gif) throws IOException {
        if (gif == null) {
            throw new IOException("bird.gif was not found on the classpath. Put it in your resources "
                    + "folder (next to the other images), then rebuild/refresh the project.");
        }
        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        try (InputStream is = gif.openStream(); ImageInputStream in = ImageIO.createImageInputStream(is)) {
            reader.setInput(in);
            int n = reader.getNumImages(true);
            BufferedImage[] out = new BufferedImage[n];
            for (int i = 0; i < n; i++) out[i] = reader.read(i);
            return out;
        } finally {
            reader.dispose();
        }
    }

    /** Downscales in halving steps so the result stays smooth instead of aliased. */
    public static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage cur = src;
        int cw = src.getWidth(), ch = src.getHeight();
        while (cw > w * 2 || ch > h * 2) {
            cw = Math.max(w, cw / 2);
            ch = Math.max(h, ch / 2);
            cur = resize(cur, cw, ch);
        }
        return resize(cur, w, h);
    }

    public static BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    // ---------------------------------------------------------------- flocks

    /** Creates a flock with a random size, direction, formation and height. */
    private Flock createFlock(int w, int h, Rectangle avoid, boolean alreadyOnScreen) {
        Flock f = new Flock();
        f.dir = rnd.nextBoolean() ? 1 : -1;
        f.scale = MIN_SCALE + rnd.nextDouble() * (1 - MIN_SCALE);
        f.speed = 45 + 50 * f.scale;                       // farther flocks look slower

        double bw = frames[0].getWidth() * f.scale;
        int n = MIN_BIRDS + rnd.nextInt(MAX_BIRDS - MIN_BIRDS + 1);
        boolean vee = rnd.nextBoolean();                   // V formation, or a looser bunch
        double maxDy = 0;
        for (int i = 0; i < n; i++) {
            Bird b = new Bird();
            if (i > 0) {
                if (vee) {
                    int row = (i + 1) / 2;
                    int side = (i % 2 == 1) ? -1 : 1;
                    b.behind = row * bw * 1.15 + (rnd.nextDouble() - 0.5) * bw * 0.25;
                    b.dy = side * row * bw * 0.7 + (rnd.nextDouble() - 0.5) * bw * 0.25;
                } else {
                    b.behind = rnd.nextDouble() * bw * 3;
                    b.dy = (rnd.nextDouble() - 0.5) * bw * 1.8;
                }
            }
            b.phase = rnd.nextDouble() * frames.length;
            b.bob = rnd.nextDouble() * Math.PI * 2;
            f.birds.add(b);
            f.tail = Math.max(f.tail, b.behind);
            maxDy = Math.max(maxDy, Math.abs(b.dy));
        }

        f.y = pickHeight(h, avoid, maxDy * 0.5 + bw * 0.5);
        if (alreadyOnScreen) f.x = f.dir > 0 ? w * 0.15 : w * 0.85;
        else f.x = f.dir > 0 ? -bw : w + bw;               // just off the entry side
        return f;
    }

    /** Picks a height that keeps the flock out from behind the title sign (where it would be hidden). */
    private double pickHeight(int h, Rectangle avoid, double pad) {
        double lo1 = pad + 4, hi1 = avoid.y - 12 - pad;                       // above the title
        double lo2 = avoid.y + avoid.height + 12 + pad, hi2 = h - pad - 4;    // below the title
        double len1 = Math.max(0, hi1 - lo1), len2 = Math.max(0, hi2 - lo2);
        if (len1 + len2 <= 0) return h * 0.85;
        double r = rnd.nextDouble() * (len1 + len2);
        return r < len1 ? lo1 + r : lo2 + (r - len1);
    }

    private void addFlock(Flock f) {                      // keep sorted so far flocks draw first (behind)
        int i = 0;
        while (i < flocks.size() && flocks.get(i).scale <= f.scale) i++;
        flocks.add(i, f);
    }

    // ---------------------------------------------------------------- update / draw

    /**
     * Moves the flocks and spawns new ones.
     * @param dt    seconds since the last update
     * @param avoid area (the title sign) that flocks should fly around
     */
    public void update(double dt, int w, int h, Rectangle avoid) {
        if (w <= 0 || h <= 0) return;
        time += dt;

        if (!started) {                                   // start with one flock already in view
            addFlock(createFlock(w, h, avoid, true));
            spawnIn = 2 + rnd.nextDouble() * 2;
            started = true;
        }

        spawnIn -= dt;
        if (spawnIn <= 0) {
            if (flocks.size() < MAX_FLOCKS) {
                addFlock(createFlock(w, h, avoid, false));
                spawnIn = 3 + rnd.nextDouble() * 4;       // next flock in 3-7 seconds
            } else {
                spawnIn = 1;
            }
        }

        double bw = frames[0].getWidth();
        for (Iterator<Flock> it = flocks.iterator(); it.hasNext(); ) {
            Flock f = it.next();
            f.x += f.dir * f.speed * dt;
            boolean gone = f.dir > 0 ? f.x - f.tail > w + bw : f.x + f.tail < -bw;
            if (gone) it.remove();                        // whole flock has left the screen
        }
    }

    public void draw(Graphics2D g2) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int n = frames.length;

        for (Flock f : flocks) {
            for (Bird b : f.birds) {
                double x = f.x - f.dir * b.behind;
                double y = f.y + b.dy + Math.sin(time * 2 + b.bob) * 3 * f.scale;   // gentle bob
                BufferedImage img = frames[(int) Math.floorMod((long) Math.floor(time / FLAP_SECONDS + b.phase), (long) n)];

                Graphics2D gb = (Graphics2D) g.create();
                gb.translate(x, y);
                gb.scale(f.dir * f.scale, f.scale);       // negative x scale mirrors the bird for leftward flocks
                gb.drawImage(img, -img.getWidth() / 2, -img.getHeight() / 2, null);
                gb.dispose();
            }
        }
        g.dispose();
    }
}