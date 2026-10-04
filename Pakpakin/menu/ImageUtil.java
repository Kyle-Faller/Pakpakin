package menu;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Small helpers for images that come with a white background instead of a transparent one. */
public final class ImageUtil {
    private ImageUtil() {}

    private static boolean isBackground(int argb) {
        int a = argb >>> 24, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return a < 10 || (r >= 235 && g >= 235 && b >= 235);   // transparent or (nearly) white
    }

    /**
     * Returns a copy where the white background is made transparent. Only white connected to the
     * image border is removed, so white inside the artwork stays. Already-transparent images are fine.
     */
    public static BufferedImage removeWhiteBackground(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        int[] px = out.getRGB(0, 0, w, h, null, 0, w);
        boolean[] seen = new boolean[w * h];
        int[] stack = new int[w * h];
        int sp = 0;

        for (int x = 0; x < w; x++) {                       // start from every border pixel
            int top = x, bottom = (h - 1) * w + x;
            if (!seen[top] && isBackground(px[top])) { seen[top] = true; stack[sp++] = top; }
            if (!seen[bottom] && isBackground(px[bottom])) { seen[bottom] = true; stack[sp++] = bottom; }
        }
        for (int y = 0; y < h; y++) {
            int left = y * w, right = y * w + w - 1;
            if (!seen[left] && isBackground(px[left])) { seen[left] = true; stack[sp++] = left; }
            if (!seen[right] && isBackground(px[right])) { seen[right] = true; stack[sp++] = right; }
        }

        while (sp > 0) {                                    // flood fill inward
            int i = stack[--sp];
            px[i] = 0;                                      // fully transparent
            int x = i % w, y = i / w;
            if (x > 0     && !seen[i - 1] && isBackground(px[i - 1])) { seen[i - 1] = true; stack[sp++] = i - 1; }
            if (x < w - 1 && !seen[i + 1] && isBackground(px[i + 1])) { seen[i + 1] = true; stack[sp++] = i + 1; }
            if (y > 0     && !seen[i - w] && isBackground(px[i - w])) { seen[i - w] = true; stack[sp++] = i - w; }
            if (y < h - 1 && !seen[i + w] && isBackground(px[i + w])) { seen[i + w] = true; stack[sp++] = i + w; }
        }
        out.setRGB(0, 0, w, h, px, 0, w);
        return out;
    }

    /**
     * Removes the white background of every image, then crops them all to the same box (the
     * smallest box that contains the artwork of all of them), so they line up with each other.
     */
    public static BufferedImage[] cleanAndTrim(BufferedImage... images) {
        BufferedImage[] clean = new BufferedImage[images.length];
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
        for (int i = 0; i < images.length; i++) {
            clean[i] = removeWhiteBackground(images[i]);
            for (int y = 0; y < clean[i].getHeight(); y++) {
                for (int x = 0; x < clean[i].getWidth(); x++) {
                    if ((clean[i].getRGB(x, y) >>> 24) > 10) {
                        minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    }
                }
            }
        }
        BufferedImage[] out = new BufferedImage[images.length];
        for (int i = 0; i < images.length; i++) {
            out[i] = clean[i].getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
        }
        return out;
    }
}