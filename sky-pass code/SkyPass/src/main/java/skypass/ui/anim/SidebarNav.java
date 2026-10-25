package skypass.ui.anim;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;
import javax.swing.JComponent;
import skypass.ui.Theme;

/**
 * The left navigation. The active item sits on a neutral pill that
 * slides to whichever item is picked, with a small plane riding along
 * and banking in the direction of travel.
 */
public class SidebarNav extends JComponent {

    public static final int WIDTH = 232;
    private static final int ITEM_H = 36;
    private static final int PITCH = 38;
    private static final int X = 12;

    private final String[] labels;
    private final String[] groups;      // group heading shown above an item, or null
    private final int[] itemY;
    private final String footer;
    private IntConsumer onSelect;

    private int selected = 0;
    private int hover = -1;
    private double pillY;
    private double bank;                // plane roll while the pill moves
    private Animator slide;
    private boolean keyboard;           // show the focus ring only once the keyboard is used

    public SidebarNav(String[] labels, String[] groups, String footer) {
        this.labels = labels;
        this.groups = groups;
        this.footer = footer;
        this.itemY = new int[labels.length];
        int y = 104;
        for (int i = 0; i < labels.length; i++) {
            if (groups[i] != null) {
                y += i == 0 ? 0 : 18;
                y += 26;                  // room for the group heading
            }
            itemY[i] = y;
            y += PITCH;
        }
        pillY = itemY[0];
        setPreferredSize(new Dimension(WIDTH, 600));
        setFocusable(true);
        getAccessibleContext().setAccessibleName("Main navigation");

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int i = itemAt(e.getY());
                if (i != hover) {
                    hover = i;
                    setCursor(i >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int i = itemAt(e.getY());
                if (i >= 0) {
                    select(i);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keyboard = true;
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    select(Math.min(labels.length - 1, selected + 1));
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    select(Math.max(0, selected - 1));
                }
            }
        });
        addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) { repaint(); }
            public void focusLost(java.awt.event.FocusEvent e) { repaint(); }
        });
    }

    public void onSelect(IntConsumer listener) {
        onSelect = listener;
    }

    public int selected() {
        return selected;
    }

    public void select(int index) {
        if (index == selected && slide == null) {
            return;
        }
        int from = (int) pillY;
        int to = itemY[index];
        double direction = Math.signum(to - from);
        selected = index;
        if (slide != null) {
            slide.stop();
        }
        slide = Animator.play(340, Animator::easeOutCubic, p -> {
            pillY = from + (to - from) * p;
            bank = direction * Math.sin(p * Math.PI) * 0.6;
            repaint();
        }, () -> {
            slide = null;
            bank = 0;
            repaint();
        });
        if (onSelect != null) {
            onSelect.accept(index);
        }
    }

    private int itemAt(int y) {
        for (int i = 0; i < labels.length; i++) {
            if (y >= itemY[i] && y < itemY[i] + ITEM_H) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Theme.GROUND);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Product mark and name
        PlaneGlyph.paint(g2, 34, 44, 26, -Math.PI / 4, Theme.ACCENT);
        g2.setColor(Theme.INK);
        g2.setFont(Theme.figure(18));
        g2.drawString("SkyPass", 56, 50);
        g2.setFont(Theme.small());
        g2.setColor(Theme.INK_3);
        g2.drawString("Flight booking admin", 56, 68);

        int w = getWidth() - 2 * X;
        // Hover tint
        if (hover >= 0 && hover != selected) {
            g2.setColor(Theme.HOVER);
            g2.fillRoundRect(X, itemY[hover], w, ITEM_H, 10, 10);
        }
        // Sliding pill + the plane that rides it
        g2.setColor(Theme.PILL);
        g2.fillRoundRect(X, (int) Math.round(pillY), w, ITEM_H, 10, 10);
        PlaneGlyph.paint(g2, X + 18, pillY + ITEM_H / 2.0, 15, bank, Theme.ACCENT);

        for (int i = 0; i < labels.length; i++) {
            if (groups[i] != null) {
                g2.setFont(Theme.small());
                g2.setColor(Theme.INK_3);
                g2.drawString(groups[i], X + 12, itemY[i] - 10);
            }
            boolean active = i == selected;
            g2.setFont(active ? Theme.strong() : Theme.body());
            g2.setColor(active ? Theme.INK : Theme.INK_2);
            FontMetrics fm = g2.getFontMetrics();
            int baseline = itemY[i] + (ITEM_H + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(labels[i], X + 36, baseline);
        }

        if (isFocusOwner() && keyboard) {
            g2.setColor(new java.awt.Color(0x66C9440F, true));
            g2.drawRoundRect(X, itemY[selected], w, ITEM_H, 10, 10);
        }

        // Connection footer
        g2.setFont(Theme.small());
        g2.setColor(Theme.OK);
        g2.fillOval(X + 12, getHeight() - 34, 7, 7);
        g2.setColor(Theme.INK_3);
        g2.drawString(footer, X + 26, getHeight() - 27);
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
