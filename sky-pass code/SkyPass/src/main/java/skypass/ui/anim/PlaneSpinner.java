package skypass.ui.anim;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import skypass.ui.Theme;

/** Loading indicator: a small plane circling a holding pattern. */
public class PlaneSpinner extends JComponent {

    private Animator loop;
    private double t;

    public PlaneSpinner() {
        setPreferredSize(new Dimension(22, 22));
        setVisible(false);
        getAccessibleContext().setAccessibleName("Loading");
    }

    public void start() {
        setVisible(true);
        if (loop == null || !loop.isRunning()) {
            loop = Animator.loop(1100, p -> {
                t = p;
                repaint();
            });
        }
    }

    public void stop() {
        if (loop != null) {
            loop.stop();
        }
        setVisible(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double cx = getWidth() / 2.0, cy = getHeight() / 2.0, r = Math.min(cx, cy) - 4;
        g2.setColor(Theme.LINE);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            g2.fillOval((int) Math.round(cx + r * Math.cos(a)) - 1, (int) Math.round(cy + r * Math.sin(a)) - 1, 2, 2);
        }
        double a = t * Math.PI * 2;
        PlaneGlyph.paint(g2, cx + r * Math.cos(a), cy + r * Math.sin(a), 11, a + Math.PI / 2, Theme.ACCENT);
        g2.dispose();
    }

    /** Plain JComponents have no accessibility context; give this one a name screen readers can read. */
    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
            };
        }
        return accessibleContext;
    }
}
