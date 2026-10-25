package skypass.ui.anim;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.Timer;
import skypass.ui.Theme;

/**
 * An airport departures board. Every character sits on its own flap;
 * when rows arrive the flaps spin through letters and settle one by
 * one, row after row, like the boards in a terminal.
 */
public class FlapBoard extends JComponent {

    /** Column titles and their widths in characters. */
    private static final String[] HEADERS = {"Date", "Time", "Flight", "To", "Remarks"};
    private static final int[] WIDTHS = {6, 5, 6, 10, 9};
    private static final String SPIN = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789:";

    private static final int CW = 13, CH = 24, GAP = 2, COL_GAP = 12, ROW_GAP = 8, PAD = 20;
    private static final int HEADER_H = 34;
    private static final int ROWS = 6;

    private final List<String[]> rows = new ArrayList<>();
    private final Random random = new Random();
    private long startedAt;
    private Timer ticker;
    private String emptyText = "";

    public FlapBoard() {
        int chars = 0;
        for (int w : WIDTHS) {
            chars += w;
        }
        int width = PAD * 2 + chars * (CW + GAP) + COL_GAP * (WIDTHS.length - 1);
        int height = PAD + HEADER_H + ROWS * (CH + ROW_GAP) + PAD - ROW_GAP;
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(getPreferredSize());
        getAccessibleContext().setAccessibleName("Next departures board");
    }

    /** Each row: date, time, flight, destination, remarks. Up to six rows are shown. */
    public void setRows(List<String[]> newRows, String emptyMessage) {
        rows.clear();
        for (int i = 0; i < Math.min(ROWS, newRows.size()); i++) {
            String[] r = newRows.get(i);
            String[] padded = new String[WIDTHS.length];
            for (int c = 0; c < WIDTHS.length; c++) {
                String v = (r[c] == null ? "" : r[c]).toUpperCase();
                padded[c] = String.format("%-" + WIDTHS[c] + "s", v.length() > WIDTHS[c] ? v.substring(0, WIDTHS[c]) : v);
            }
            rows.add(padded);
        }
        emptyText = emptyMessage;
        flip();
    }

    /** Replays the flip (used each time the Overview is shown). */
    public void flip() {
        startedAt = System.currentTimeMillis();
        if (ticker != null) {
            ticker.stop();
        }
        ticker = new Timer(40, e -> {
            repaint();
            if (System.currentTimeMillis() - startedAt > settleTime(ROWS - 1, 40) + 100) {
                ((Timer) e.getSource()).stop();
            }
        });
        ticker.start();
    }

    private static long settleTime(int row, int charIndex) {
        return 250 + row * 130L + charIndex * 22L;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Theme.BOARD);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.CARD_RADIUS, Theme.CARD_RADIUS);

        // Column headings
        g2.setFont(Theme.small());
        g2.setColor(new Color(0xA7B0C0));
        int x = PAD;
        for (int c = 0; c < HEADERS.length; c++) {
            g2.drawString(HEADERS[c], x, PAD + 14);
            x += WIDTHS[c] * (CW + GAP) + COL_GAP;
        }

        if (rows.isEmpty()) {
            g2.setFont(Theme.body());
            g2.drawString(emptyText, PAD, PAD + HEADER_H + 20);
            g2.dispose();
            return;
        }

        long elapsed = System.currentTimeMillis() - startedAt;
        g2.setFont(Theme.board(14));
        FontMetrics fm = g2.getFontMetrics();
        for (int r = 0; r < ROWS; r++) {
            int y = PAD + HEADER_H + r * (CH + ROW_GAP);
            String[] row = r < rows.size() ? rows.get(r) : null;
            x = PAD;
            int charIndex = 0;
            for (int c = 0; c < WIDTHS.length; c++) {
                Color ink = Theme.BOARD_TEXT;
                if (row != null && c == 4) {
                    String remark = row[4].trim();
                    if (remark.startsWith("DELAY")) {
                        ink = Theme.BOARD_AMBER;
                    } else if (remark.startsWith("CANCEL")) {
                        ink = new Color(0xFF7A7A);
                    }
                }
                for (int k = 0; k < WIDTHS[c]; k++) {
                    int cx = x + k * (CW + GAP);
                    g2.setColor(Theme.BOARD_TILE);
                    g2.fillRoundRect(cx, y, CW, CH, 4, 4);
                    if (row != null) {
                        char target = row[c].charAt(k);
                        char shown = elapsed >= settleTime(r, charIndex) ? target
                                : (target == ' ' && elapsed < settleTime(r, charIndex) - 200 ? ' '
                                : SPIN.charAt(random.nextInt(SPIN.length())));
                        if (shown != ' ') {
                            g2.setColor(ink);
                            String s = String.valueOf(shown);
                            g2.drawString(s, cx + (CW - fm.stringWidth(s)) / 2, y + (CH + fm.getAscent() - fm.getDescent()) / 2);
                        }
                    }
                    // the split between the two halves of a flap
                    g2.setColor(Theme.BOARD);
                    g2.fillRect(cx, y + CH / 2, CW, 1);
                    charIndex++;
                }
                x += WIDTHS[c] * (CW + GAP) + COL_GAP;
            }
        }
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
