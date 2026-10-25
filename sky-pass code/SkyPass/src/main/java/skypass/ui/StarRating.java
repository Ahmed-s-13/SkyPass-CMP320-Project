package skypass.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import skypass.ui.anim.Animator;

/** A 1-5 rating picker drawn as stars. Click or use the arrow keys. */
public class StarRating extends JComponent {

    private static final int STAR = 22;
    private static final int GAP = 6;
    private static final Color EMPTY = new Color(0xD0D5DD);

    private int value;
    private int hover;
    private double pop = 1;   // brief scale-up of the star just picked
    private Runnable onChange;

    public StarRating() {
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(5 * STAR + 4 * GAP + 4, STAR + 8));
        getAccessibleContext().setAccessibleName("Rating from 1 to 5");
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                hover = starAt(e.getX());
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = 0;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                setValue(starAt(e.getX()));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_UP) {
                    setValue(Math.min(5, value + 1));
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    setValue(Math.max(1, value - 1));
                }
            }
        });
        addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) { repaint(); }
            public void focusLost(java.awt.event.FocusEvent e) { repaint(); }
        });
    }

    public int getValue() {
        return value;
    }

    public void setValue(int v) {
        int old = value;
        value = Math.max(0, Math.min(5, v));
        if (value != old && value > 0) {
            Animator.play(240, Animator::easeOutCubic, p -> {
                pop = 1 + 0.25 * Math.sin(p * Math.PI);
                repaint();
            }, null);
            if (onChange != null) {
                onChange.run();
            }
        }
        repaint();
    }

    public void onChange(Runnable r) {
        onChange = r;
    }

    private int starAt(int x) {
        return Math.max(1, Math.min(5, x / (STAR + GAP) + 1));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int shown = hover > 0 ? hover : value;
        for (int i = 0; i < 5; i++) {
            double scale = (i + 1 == value) ? pop : 1;
            double cx = 2 + i * (STAR + GAP) + STAR / 2.0;
            double cy = getHeight() / 2.0;
            g2.setColor(i < shown ? Theme.ACCENT : EMPTY);
            g2.fill(star(cx, cy, STAR / 2.0 * scale));
        }
        if (isFocusOwner()) {
            g2.setColor(new Color(0x55C9440F, true));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        }
        g2.dispose();
    }

    static Path2D star(double cx, double cy, double r) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double radius = i % 2 == 0 ? r : r * 0.45;
            double a = -Math.PI / 2 + i * Math.PI / 5;
            double x = cx + radius * Math.cos(a);
            double y = cy + radius * Math.sin(a);
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        p.closePath();
        return p;
    }

    /** Draws a rating as five small stars inside a table cell. */
    public static TableCellRenderer renderer() {
        return new DataTable.CellRenderer() {
            private int stars;

            @Override
            public java.awt.Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                     boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, "", selected, focus, row, col);
                stars = value instanceof Number n ? n.intValue() : 0;
                getAccessibleContext().setAccessibleName(stars + " out of 5");
                return this;
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 0; i < 5; i++) {
                    g2.setColor(i < stars ? Theme.ACCENT : EMPTY);
                    g2.fill(star(16 + i * 13, getHeight() / 2.0, 5.5));
                }
                g2.dispose();
            }
        };
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
