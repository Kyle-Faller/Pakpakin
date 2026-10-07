package menu;
import javax.swing.*;



import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;


public class DifficultySelector extends JComponent {
    private static final double ARROW_HEIGHT = 0.60;
    private static final double ARROW_GAP = 0.054;   
    private final BufferedImage[] labels;            
    private final MenuButton left, right;
    private final Consumer<Difficulty> onChange;
    private Difficulty current = Difficulty.EASY;
    private Rectangle center = new Rectangle();

    public DifficultySelector(BufferedImage[] labels, BufferedImage leftArrow, BufferedImage rightArrow,
                              Consumer<Difficulty> onChange) {
        this.labels = labels;
        this.onChange = onChange;
        setLayout(null);
        left  = new MenuButton(leftArrow,  () -> select(current.previous()));
        right = new MenuButton(rightArrow, () -> select(current.next()));
        add(left);
        add(right);
    }

    public Difficulty getDifficulty() {
        return current;
    }

    private void select(Difficulty d) {
        current = d;
        repaint();
        if (onChange != null) onChange.accept(d);
    }

   
    public void place(int x, int y, int bw, int bh) {
        int arrowH = (int) Math.round(bh * ARROW_HEIGHT);
        int arrowW = (int) Math.round(arrowH * left.aspectRatio());
        int gap = (int) Math.round(bw * ARROW_GAP);
        int side = arrowW + gap;

        setBounds(x - side, y, bw + 2 * side, bh);
        center = new Rectangle(side, 0, bw, bh);
        int ay = (bh - arrowH) / 2;
        left.setBounds(0, ay, arrowW, arrowH);
        right.setBounds(side + bw + gap, ay, arrowW, arrowH);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(labels[current.ordinal()], center.x, center.y, center.width, center.height, null);
        g2.dispose();
    }
}