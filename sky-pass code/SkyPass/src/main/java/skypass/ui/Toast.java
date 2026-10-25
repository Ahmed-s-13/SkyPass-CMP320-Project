package skypass.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import skypass.ui.anim.Animator;
import skypass.ui.anim.PlaneGlyph;

/**
 * A short message that rises in at the bottom of the window and
 * leaves on its own. Errors stay longer and carry a red plane.
 */
public final class Toast extends JPanel {

    public enum Kind { SUCCESS, ERROR, INFO }

    private static Toast current;

    private final Kind kind;
    private final JLabel text;

    private Toast(Kind kind, String message) {
        super(new BorderLayout());
        this.kind = kind;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 48, 14, 18));
        text = new JLabel("<html><div style='width:280px'>" + escape(message) + "</div></html>");
        text.setFont(Theme.body());
        text.setForeground(Color.WHITE);
        add(text, BorderLayout.CENTER);
    }

    public static void success(Component anywhere, String message) {
        show(anywhere, Kind.SUCCESS, message);
    }

    public static void error(Component anywhere, String message) {
        show(anywhere, Kind.ERROR, message);
    }

    public static void info(Component anywhere, String message) {
        show(anywhere, Kind.INFO, message);
    }

    public static void show(Component anywhere, Kind kind, String message) {
        JRootPane root = anywhere == null ? null : SwingUtilities.getRootPane(anywhere);
        if (root == null) {
            return;
        }
        JLayeredPane layers = root.getLayeredPane();
        if (current != null && current.getParent() != null) {
            current.getParent().remove(current);
            layers.repaint();
        }
        Toast toast = new Toast(kind, message);
        current = toast;
        Dimension size = toast.getPreferredSize();
        // Bottom centre of the window, where it covers neither the header nor the form buttons
        int x = (layers.getWidth() - size.width) / 2;
        int finalY = layers.getHeight() - size.height - 20;
        toast.setBounds(x, layers.getHeight(), size.width, size.height);
        layers.add(toast, JLayeredPane.POPUP_LAYER);

        // Rise into place, hold, then drop back out.
        Animator.play(380, Animator::easeOutCubic, p -> {
            toast.setLocation(x, (int) Math.round(layers.getHeight() + (finalY - layers.getHeight()) * p));
        }, null);
        Timer hold = new Timer(kind == Kind.ERROR ? 6500 : 3200, e -> toast.dismiss(layers));
        hold.setRepeats(false);
        hold.start();
    }

    private void dismiss(JLayeredPane layers) {
        if (getParent() == null) {
            return;
        }
        int startY = getY();
        Animator.play(260, t -> t * t, p -> setLocation(getX(), (int) Math.round(startY + (layers.getHeight() - startY) * p)),
                () -> {
                    if (getParent() != null) {
                        getParent().remove(this);
                        layers.repaint();
                    }
                });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // A toast floats over the page, so it is the one thing with a shadow.
        for (int i = 0; i < 6; i++) {
            g2.setColor(new Color(16, 24, 40, 10));
            g2.fillRoundRect(i / 2, 3 + i / 2, getWidth() - i, getHeight() - i - 1, 14, 14);
        }
        g2.setColor(Theme.INK);
        g2.fillRoundRect(3, 0, getWidth() - 6, getHeight() - 6, 12, 12);
        Color mark = switch (kind) {
            case SUCCESS -> new Color(0x6CE0A4);
            case ERROR -> new Color(0xFF8A8A);
            default -> new Color(0xF59E6B);
        };
        PlaneGlyph.paint(g2, 26, (getHeight() - 6) / 2.0, 18, kind == Kind.ERROR ? Math.PI / 5 : -Math.PI / 8, mark);
        g2.dispose();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
