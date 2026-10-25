package skypass.ui.anim;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import skypass.ui.Theme;
import skypass.ui.Ui;

/**
 * Shown after a booking is saved: the booking as a boarding pass that
 * rises into place. The stub on the right carries the total and a
 * barcode generated from the booking reference.
 */
public class BoardingPassDialog extends JDialog {

    private static final int W = 680, H = 300, STUB = 200;

    public BoardingPassDialog(Window owner, String ref, String customer, List<String> flights,
                              int travelers, String total) {
        super(owner, "Booking confirmed", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        Pass pass = new Pass(ref, customer, flights, travelers, total);
        pass.setLayout(null);
        JButton done = Ui.primary("Done", e -> dispose());
        done.setBounds(W - STUB + 28, H - 64, STUB - 56, 36);
        pass.add(done);

        FadeLayer fade = FadeLayer.wrap(pass, 40);
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.add(fade.component());
        setContentPane(root);
        setSize(W + 24, H + 24);
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(done);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        fade.play(460, 60);
    }

    private static final class Pass extends JComponent {
        private final String ref, customer, total;
        private final List<String> flights;
        private final int travelers;

        Pass(String ref, String customer, List<String> flights, int travelers, String total) {
            this.ref = ref;
            this.customer = customer;
            this.flights = flights;
            this.travelers = travelers;
            this.total = total;
            setPreferredSize(new Dimension(W + 24, H + 24));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Ticket outline with the two perforation notches
            int perfX = W - STUB;
            Area ticket = new Area(new RoundRectangle2D.Double(0, 0, W, H, 18, 18));
            ticket.subtract(new Area(new Ellipse2D.Double(perfX - 12, -12, 24, 24)));
            ticket.subtract(new Area(new Ellipse2D.Double(perfX - 12, H - 12, 24, 24)));

            // It floats over the window, so it gets a soft shadow
            for (int i = 0; i < 10; i++) {
                g2.setColor(new Color(16, 24, 40, 8));
                g2.translate(0, 1.2);
                g2.fill(ticket);
            }
            g2.translate(0, -12);
            g2.setColor(Color.WHITE);
            g2.fill(ticket);

            // Stub tint and perforation
            Area stub = new Area(ticket);
            stub.intersect(new Area(new java.awt.Rectangle(perfX, 0, STUB, H)));
            g2.setColor(new Color(0xFFF4EE));
            g2.fill(stub);
            g2.setColor(new Color(0xD0D5DD));
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3f, 6f}, 0f));
            g2.drawLine(perfX, 18, perfX, H - 18);

            // Main part
            int x = 32;
            g2.setColor(Theme.OK);
            g2.setFont(Theme.strong());
            g2.drawString("Booking confirmed", x, 44);
            g2.setColor(Theme.INK);
            g2.setFont(Theme.figure(38));
            g2.drawString(ref, x, 92);
            g2.setFont(Theme.body());
            g2.setColor(Theme.INK_2);
            g2.drawString(customer + "  ·  " + travelers + (travelers == 1 ? " traveler" : " travelers"), x, 118);

            int y = 158;
            g2.setFont(Theme.body());
            for (int i = 0; i < flights.size() && i < 4; i++) {
                PlaneGlyph.paint(g2, x + 7, y - 5, 13, 0, Theme.ACCENT);
                g2.setColor(Theme.INK);
                g2.drawString(flights.get(i), x + 24, y);
                y += 26;
            }
            if (flights.size() > 4) {
                g2.setColor(Theme.INK_3);
                g2.drawString("and " + (flights.size() - 4) + " more", x + 24, y);
            }

            // Stub: total + barcode from the reference
            int sx = perfX + 28;
            g2.setColor(Theme.INK_3);
            g2.setFont(Theme.small());
            g2.drawString("Total paid (AED)", sx, 44);
            g2.setColor(Theme.INK);
            g2.setFont(Theme.figure(22));
            g2.drawString(total, sx, 74);
            int bx = sx;
            long seed = ref.hashCode();
            java.util.Random r = new java.util.Random(seed);
            while (bx < W - 28) {
                int bar = 1 + r.nextInt(3);
                g2.fillRect(bx, 104, bar, 92);
                bx += bar + 1 + r.nextInt(3);
            }
            g2.dispose();
        }
    }
}
