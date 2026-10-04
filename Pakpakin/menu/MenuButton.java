package menu;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/** A clickable image button with hover and pressed effects. */
public class MenuButton extends JComponent {
    private final BufferedImage image;
    private boolean hover, pressed;

    public MenuButton(BufferedImage image, Runnable onClick) {
        this.image = image;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hover = false; pressed = false; repaint(); }
            @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
            @Override public void mouseReleased(MouseEvent e) {
                boolean click = pressed && contains(e.getPoint());
                pressed = false;
                repaint();
                if (click && onClick != null) onClick.run();
            }
        });
    }

    /** Width / height of the source image, used by the menu for layout. */
    public double aspectRatio() {
        return (double) image.getWidth() / image.getHeight();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        // Nearest neighbor keeps pixel art crisp when scaled
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        int w = getWidth(), h = getHeight();
        int inset = hover && !pressed ? -w / 40 : 0;   // grow slightly on hover
        int yOff  = pressed ? h / 30 : 0;              // sink slightly when pressed
        g2.drawImage(image, inset, inset + yOff, w - 2 * inset, h - 2 * inset, null);

        if (hover) {                                   // subtle brighten
            g2.setComposite(AlphaComposite.SrcAtop.derive(0.12f));
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
        }
        g2.dispose();
    }
}