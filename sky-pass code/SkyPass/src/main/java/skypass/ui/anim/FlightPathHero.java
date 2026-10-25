package skypass.ui.anim;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import javax.swing.JComponent;
import skypass.ui.Theme;

/**
 * The Overview's main block: the next departure drawn as its route.
 * Airport codes sit at each end of a dashed arc and a plane flies it
 * on a slow loop, leaving a contrail. The loop only runs while visible.
 */
public class FlightPathHero extends JComponent {

    private String fromCode = "", fromCity = "", toCode = "", toCity = "";
    private String detail = "";
    private String duration = "";
    private boolean empty = true;
    private double t;
    private Animator loop;

    public FlightPathHero() {
        setPreferredSize(new Dimension(600, 236));
        setMinimumSize(new Dimension(500, 236));
        getAccessibleContext().setAccessibleName("Next departure route");
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    start();
                } else if (loop != null) {
                    loop.stop();
                }
            }
        });
    }

    public void setRoute(String fromCode, String fromCity, String toCode, String toCity,
                         String detail, String duration) {
        this.fromCode = fromCode;
        this.fromCity = fromCity;
        this.toCode = toCode;
        this.toCity = toCity;
        this.detail = detail;
        this.duration = duration;
        this.empty = false;
        getAccessibleContext().setAccessibleDescription(fromCity + " to " + toCity + ", " + detail);
        repaint();
    }

    public void setEmpty(String message) {
        empty = true;
        detail = message;
        repaint();
    }

    private void start() {
        if (loop == null || !loop.isRunning()) {
            loop = Animator.loop(5200, p -> {
                t = p;
                repaint();
            });
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setColor(Theme.SKY);
        g2.fillRoundRect(0, 0, w, h, Theme.CARD_RADIUS, Theme.CARD_RADIUS);

        if (empty) {
            g2.setFont(Theme.body());
            g2.setColor(Theme.SKY_INK_2);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(detail, (w - fm.stringWidth(detail)) / 2, h / 2);
            g2.dispose();
            return;
        }

        // Airport codes at both ends
        g2.setFont(Theme.figure(46));
        FontMetrics big = g2.getFontMetrics();
        int codeY = 104;
        g2.setColor(Color.WHITE);
        g2.drawString(fromCode, 32, codeY);
        int toW = big.stringWidth(toCode);
        g2.drawString(toCode, w - 32 - toW, codeY);
        g2.setFont(Theme.body());
        g2.setColor(Theme.SKY_INK_2);
        g2.drawString(fromCity, 34, codeY + 24);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(toCity, w - 32 - fm.stringWidth(toCity), codeY + 24);

        // The route between them
        double x0 = 32 + big.stringWidth(fromCode) + 36;
        double x1 = w - 32 - toW - 36;
        double eased = Animator.easeInOutCubic(Math.min(1, t / 0.86));
        double alpha = t < 0.06 ? t / 0.06 : t > 0.86 ? Math.max(0, 1 - (t - 0.86) / 0.1) : 1;
        new RouteArc(x0, codeY - 16, x1, codeY - 16, 58)
                .paint(g2, eased, new Color(255, 255, 255, 80), new Color(255, 255, 255, 190),
                        new Color(0xF2703A), 28, alpha);

        // The flight in words, along the bottom
        g2.setFont(Theme.body());
        g2.setColor(new Color(255, 255, 255, 225));
        g2.drawString(detail, 34, h - 30);
        g2.setColor(Theme.SKY_INK_2);
        g2.drawString(duration, w - 32 - fm.stringWidth(duration), h - 30);
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
