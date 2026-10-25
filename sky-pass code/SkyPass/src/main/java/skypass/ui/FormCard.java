package skypass.ui;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * The form on the right of every page: a heading, labelled fields with
 * room for an error under each, and a button row pinned to the bottom.
 */
public class FormCard extends JPanel {

    private final JLabel heading = Ui.heading("");
    private final JLabel subheading = Ui.label("", Theme.small(), Theme.INK_3);
    private final JPanel body = new TrackingPanel(new GridBagLayout());
    private final JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.S2, 0));
    private final Map<JComponent, JLabel> errors = new LinkedHashMap<>();
    private int row;
    private boolean failed;

    public FormCard(String title) {
        super(new BorderLayout());
        setOpaque(false);
        setBackground(Theme.SHEET);
        setBorder(BorderFactory.createEmptyBorder(Theme.S5, Theme.S5, Theme.S5, Theme.S5));

        JPanel top = new JPanel(new BorderLayout(0, 2));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(subheading, BorderLayout.SOUTH);
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.S4, 0));
        setHeading(title, "");

        body.setOpaque(false);
        JScrollPane scroll = Ui.scroll(body);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(Theme.S4, -Theme.S2, 0, 0));

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    public final void setHeading(String title, String detail) {
        heading.setText(title);
        subheading.setText(detail);
        subheading.setVisible(detail != null && !detail.isEmpty());
    }

    /** Adds one labelled field across the full width. */
    public <C extends JComponent> C field(String label, C input) {
        place(block(label, input), 0, 2);
        row++;
        return input;
    }

    /** Adds two labelled fields side by side, in two equal columns. */
    public void pair(String label1, JComponent input1, String label2, JComponent input2) {
        JPanel both = new JPanel(new java.awt.GridLayout(1, 2, Theme.S3, 0));
        both.setOpaque(false);
        both.add(block(label1, input1));
        both.add(block(label2, input2));
        place(both, 0, 2);
        row++;
    }

    /** Adds any component (a note, a sub-table) across the full width. */
    public void extra(JComponent c) {
        place(c, 0, 2);
        row++;
    }

    /** Pushes everything up; call after the last field. */
    public void end() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row++;
        gc.weighty = 1;
        gc.fill = GridBagConstraints.VERTICAL;
        body.add(new JLabel(), gc);
    }

    public void buttons(JComponent... buttons) {
        footer.removeAll();
        for (JComponent b : buttons) {
            footer.add(b);
        }
        footer.revalidate();
        footer.repaint();
    }

    // ---------------- Errors ----------------

    public void clearErrors() {
        failed = false;
        for (Map.Entry<JComponent, JLabel> e : errors.entrySet()) {
            e.getKey().putClientProperty(FlatClientProperties.OUTLINE, null);
            e.getValue().setText("");
            e.getValue().setVisible(false);
        }
    }

    /** Marks a field red and writes the reason under it. */
    public void error(JComponent input, String message) {
        JLabel label = errors.get(input);
        input.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
        }
        if (!failed) {
            input.requestFocusInWindow();
        }
        failed = true;
    }

    public boolean ok() {
        return !failed;
    }

    // ---------------- Internals ----------------

    private JPanel block(String label, JComponent input) {
        JPanel block = new JPanel(new BorderLayout(0, 6));
        block.setOpaque(false);
        JLabel l = Ui.label(label, Theme.small(), Theme.INK_2);
        l.setLabelFor(input);
        JLabel error = Ui.label("", Theme.small(), Theme.DANGER);
        error.setVisible(false);
        errors.put(input, error);
        // The input keeps its natural height; spare height (when the neighbouring
        // field shows an error) goes below it, under the error line.
        error.setVerticalAlignment(JLabel.TOP);
        error.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        JPanel rest = new JPanel(new BorderLayout());
        rest.setOpaque(false);
        rest.add(wrap(input), BorderLayout.NORTH);
        rest.add(error, BorderLayout.CENTER);
        block.add(l, BorderLayout.NORTH);
        block.add(rest, BorderLayout.CENTER);
        return block;
    }

    private static Component wrap(JComponent input) {
        if (input instanceof javax.swing.JTextArea area) {
            area.setMargin(new Insets(6, 8, 6, 8));
            return new JScrollPane(area);   // keeps the standard field outline
        }
        return input;
    }

    private void place(Component c, int x, int width) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = x;
        gc.gridy = row;
        gc.gridwidth = width;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(0, 0, 14, 0);
        body.add(c, gc);
    }

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
}
