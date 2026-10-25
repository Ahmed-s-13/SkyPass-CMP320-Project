package skypass.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Timer;

/** Small builders so every screen is assembled from the same parts. */
public final class Ui {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);
    public static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH);
    public static final DateTimeFormatter COMPACT = DateTimeFormatter.ofPattern("d MMM yy, HH:mm", Locale.ENGLISH);
    // STRICT so impossible dates like 2001-02-30 are rejected instead of quietly becoming 28 Feb
    public static final DateTimeFormatter INPUT_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT);
    public static final DateTimeFormatter INPUT_DATE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT);

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DecimalFormat WHOLE = new DecimalFormat("#,##0");

    private Ui() {
    }

    // ---------------- Text ----------------

    public static JLabel label(String text, java.awt.Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    public static JLabel title(String text) {
        return label(text, Theme.title(), Theme.INK);
    }

    public static JLabel heading(String text) {
        return label(text, Theme.heading(), Theme.INK);
    }

    public static JLabel muted(String text) {
        return label(text, Theme.body(), Theme.INK_3);
    }

    // ---------------- Inputs ----------------

    public static JTextField field(String placeholder) {
        JTextField f = new JTextField();
        f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        f.setFont(Theme.body());
        f.putClientProperty(FlatClientProperties.STYLE, "margin: 6,10,6,10");
        return f;
    }

    public static JTextField search(String placeholder, Runnable onChange) {
        JTextField f = field(placeholder);
        f.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        f.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new SearchIcon());
        f.setPreferredSize(new Dimension(260, 36));
        // Wait until typing pauses before querying the database.
        Timer debounce = new Timer(280, e -> onChange.run());
        debounce.setRepeats(false);
        f.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { debounce.restart(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { debounce.restart(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { debounce.restart(); }
        });
        return f;
    }

    public static <T> JComboBox<T> combo() {
        JComboBox<T> c = new JComboBox<>();
        c.setFont(Theme.body());
        c.setMaximumRowCount(12);
        return c;
    }

    // ---------------- Buttons ----------------

    public static JButton primary(String text, ActionListener action) {
        JButton b = new JButton(text);
        b.setFont(Theme.strong());
        b.putClientProperty(FlatClientProperties.STYLE,
                "background: #C9440F; foreground: #FFFFFF; borderWidth: 0; focusWidth: 0;"
                + "hoverBackground: #B23A0B; pressedBackground: #9C3309; margin: 7,16,7,16");
        b.addActionListener(action);
        return b;
    }

    public static JButton secondary(String text, ActionListener action) {
        JButton b = new JButton(text);
        b.setFont(Theme.body());
        b.putClientProperty(FlatClientProperties.STYLE,
                "background: #FFFFFF; foreground: #101828; borderColor: #D0D5DD;"
                + "hoverBackground: #F2F4F7; pressedBackground: #E6E9EF; focusWidth: 0; margin: 7,14,7,14");
        b.addActionListener(action);
        return b;
    }

    /** Destructive action: quiet at rest, red text so it is never mistaken for Save. */
    public static JButton danger(String text, ActionListener action) {
        JButton b = new JButton(text);
        b.setFont(Theme.body());
        b.putClientProperty(FlatClientProperties.STYLE,
                "background: #FFFFFF; foreground: #C0262D; borderColor: #F1C4C6;"
                + "hoverBackground: #FDF0F0; pressedBackground: #FADCDD; focusWidth: 0; margin: 7,14,7,14");
        b.addActionListener(action);
        return b;
    }

    // ---------------- Layout ----------------

    /** Page header: title + one-line summary on the left, controls on the right. */
    public static JPanel pageHeader(String title, JLabel summary, JComponent... controls) {
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t = title(title);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        // let the summary use the full width so its last letters are never clipped
        summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        left.add(t);
        left.add(Box.createVerticalStrut(4));
        left.add(summary);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.S2, 0));
        right.setOpaque(false);
        for (JComponent c : controls) {
            right.add(c);
        }
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(left, BorderLayout.CENTER);   // CENTER: the summary gets all the room it needs
        header.add(right, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.S5, 0));
        return header;
    }

    /** Standard page body: 32px gutter on all sides of the white sheet. */
    public static JPanel page() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.SHEET);
        p.setBorder(BorderFactory.createEmptyBorder(28, Theme.S6, Theme.S6, Theme.S6));
        return p;
    }

    /** A flat panel with a 1px rule and rounded corners: no shadow. */
    public static JPanel card(java.awt.LayoutManager layout) {
        JPanel p = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.CARD_RADIUS, Theme.CARD_RADIUS);
                g2.setColor(Theme.LINE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.CARD_RADIUS, Theme.CARD_RADIUS);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBackground(Theme.SHEET);
        p.setBorder(BorderFactory.createEmptyBorder(Theme.S4, Theme.S4, Theme.S4, Theme.S4));
        return p;
    }

    /** The CRUD layout: the list on the left, a 340px form on the right. */
    public static JPanel workspace(JComponent main, JComponent side) {
        JPanel body = new JPanel(new BorderLayout(Theme.S5, 0));
        body.setOpaque(false);
        side.setPreferredSize(new Dimension(340, 10));
        body.add(main, BorderLayout.CENTER);
        body.add(side, BorderLayout.EAST);
        return body;
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(BorderFactory.createEmptyBorder());
        s.getViewport().setBackground(Theme.SHEET);
        s.getVerticalScrollBar().setUnitIncrement(16);
        return s;
    }

    public static JPanel row(int gap, JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        p.setOpaque(false);
        for (JComponent c : items) {
            p.add(c);
        }
        return p;
    }

    // ---------------- Formatting ----------------

    public static String money(BigDecimal amount) {
        return amount == null ? "–" : MONEY.format(amount);
    }

    public static String whole(double n) {
        return WHOLE.format(n);
    }

    public static String date(LocalDate d) {
        return d == null ? "–" : DATE.format(d);
    }

    public static String dateTime(LocalDateTime d) {
        return d == null ? "–" : DATE_TIME.format(d);
    }

    /** 470 minutes becomes "7h 50m". */
    public static String duration(long minutes) {
        return (minutes / 60) + "h " + String.format("%02d", minutes % 60) + "m";
    }

    /** CONFIRMED becomes Confirmed, PREMIUM_ECONOMY becomes Premium economy. */
    public static String nice(String constant) {
        if (constant == null || constant.isEmpty()) {
            return "–";
        }
        String s = constant.replace('_', ' ').toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** A small magnifier drawn in code, used as the search field's leading icon. */
    private static final class SearchIcon implements javax.swing.Icon {
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.INK_3);
            g2.setStroke(new java.awt.BasicStroke(1.5f));
            g2.drawOval(x + 1, y + 1, 9, 9);
            g2.drawLine(x + 9, y + 9, x + 13, y + 13);
            g2.dispose();
        }

        public int getIconWidth() {
            return 14;
        }

        public int getIconHeight() {
            return 14;
        }
    }
}
