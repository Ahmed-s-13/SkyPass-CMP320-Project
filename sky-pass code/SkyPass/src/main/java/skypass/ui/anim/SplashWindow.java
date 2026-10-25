package skypass.ui.anim;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import skypass.db.Db;
import skypass.db.OracleErrors;
import skypass.ui.Theme;
import skypass.ui.Ui;

/**
 * The opening screen. A plane crosses a night sky along a dashed route
 * while the app connects to Oracle. If the connection fails, the reason
 * is shown with a Retry button instead of the app crashing.
 */
public class SplashWindow extends JWindow {

    private static final int W = 620, H = 360;

    private final Sky sky = new Sky();
    private final JLabel status = Ui.label("Connecting to Oracle…", Theme.small(), Theme.SKY_INK_2);
    private final JButton retry;
    private final JButton quit;
    private Animator flight;
    private double t;
    private double wordmark;   // 0..1 fade of the name

    public SplashWindow() {
        setSize(W, H);
        setLocationRelativeTo(null);
        try {
            setShape(new RoundRectangle2D.Double(0, 0, W, H, 18, 18));
        } catch (UnsupportedOperationException ignored) {
            // square corners on systems without shaped windows
        }
        sky.setLayout(null);
        setContentPane(sky);

        status.setBounds(40, H - 54, W - 260, 36);
        status.setVerticalAlignment(JLabel.TOP);
        sky.add(status);

        retry = Ui.primary("Retry", e -> connectAgain());
        quit = Ui.secondary("Quit", e -> System.exit(0));
        retry.setBounds(W - 196, H - 58, 80, 34);
        quit.setBounds(W - 108, H - 58, 72, 34);
        retry.setVisible(false);
        quit.setVisible(false);
        sky.add(retry);
        sky.add(quit);
    }

    private Runnable onConnected;

    /** Shows the splash, connects in the background, then runs onConnected on the Swing thread. */
    public void connect(Runnable onConnected) {
        this.onConnected = onConnected;
        setVisible(true);
        Animator.play(700, Animator::easeOutCubic, p -> {
            wordmark = p;
            sky.repaint();
        }, null);
        flight = Animator.loop(2600, p -> {
            t = p;
            sky.repaint();
        });
        attempt();
    }

    private void connectAgain() {
        retry.setVisible(false);
        quit.setVisible(false);
        status.setForeground(Theme.SKY_INK_2);
        status.setText("Connecting to Oracle…");
        attempt();
    }

    private void attempt() {
        long started = System.currentTimeMillis();
        Thread worker = new Thread(() -> {
            String error = null;
            try {
                Connection c = Db.get();
                // Also proves the SkyPass tables exist (ORA-00942 if the scripts were not run).
                try (Statement s = c.createStatement()) {
                    s.executeQuery("SELECT COUNT(*) FROM CUSTOMER").close();
                }
            } catch (SQLException e) {
                error = OracleErrors.friendly(e);
                Db.close();
            }
            long wait = 1800 - (System.currentTimeMillis() - started);   // let the plane finish a pass
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            String finalError = error;
            SwingUtilities.invokeLater(() -> {
                if (finalError == null) {
                    flight.stop();
                    onConnected.run();
                } else {
                    status.setForeground(new Color(0xFFB4A8));
                    status.setText("<html>" + finalError + "</html>");
                    retry.setVisible(true);
                    quit.setVisible(true);
                }
            });
        }, "skypass-connect");
        worker.setDaemon(true);
        worker.start();
    }

    private final class Sky extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Theme.SKY);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // One pass of the plane: ease along the arc, fading at both ends.
            double eased = Animator.easeInOutCubic(t);
            double alpha = Math.min(1, Math.min(t / 0.12, (1 - t) / 0.12));
            RouteArc arc = new RouteArc(64, 168, W - 64, 150, 120);
            arc.paint(g2, eased, new Color(255, 255, 255, 70), new Color(255, 255, 255, 170),
                    new Color(0xF2703A), 30, alpha);

            float a = (float) wordmark;
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, a));
            g2.translate(0, (1 - wordmark) * 10);
            g2.setColor(Color.WHITE);
            g2.setFont(Theme.figure(44));
            g2.drawString("SkyPass", 40, 250);
            g2.setFont(Theme.body());
            g2.setColor(Theme.SKY_INK_2);
            g2.drawString("Flight booking database  ·  CMP 320", 42, 276);
            g2.dispose();
        }
    }

    @Override
    public void dispose() {
        if (flight != null) {
            flight.stop();
        }
        super.dispose();
    }
}
