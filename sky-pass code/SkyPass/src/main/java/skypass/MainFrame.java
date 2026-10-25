package skypass;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import skypass.db.Db;
import skypass.ui.Page;
import skypass.ui.Theme;
import skypass.ui.anim.PageHost;
import skypass.ui.anim.SidebarNav;
import skypass.ui.pages.BookingsPage;
import skypass.ui.pages.CustomersPage;
import skypass.ui.pages.FlightsPage;
import skypass.ui.pages.OverviewPage;
import skypass.ui.pages.ReportsPage;
import skypass.ui.pages.ReviewsPage;

/** The main window: sidebar on the left, the current page on a white sheet. */
public class MainFrame extends JFrame {

    private static final String[] NAMES = {"Overview", "Customers", "Flights", "Bookings", "Reviews", "Reports"};
    private static final String[] GROUPS = {null, "Manage", null, null, null, "Insight"};
    private static final int BOOKINGS = 3;

    private SidebarNav nav;
    private final PageHost host = new PageHost();
    private final List<JComponent> pages;

    public MainFrame() {
        super("SkyPass");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        java.awt.Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(1180, screen.width), Math.min(720, screen.height)));
        setSize(Math.min(1440, screen.width - 40), Math.min(900, screen.height - 20));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Db.close();
            }
        });

        BookingsPage bookings = new BookingsPage();
        pages = List.of(
                new OverviewPage(() -> {
                    nav.select(BOOKINGS);
                    bookings.startNewBooking();
                }),
                new CustomersPage(),
                new FlightsPage(),
                bookings,
                new ReviewsPage(),
                new ReportsPage());
        for (int i = 0; i < pages.size(); i++) {
            host.add(NAMES[i], pages.get(i));
        }

        nav = new SidebarNav(NAMES, GROUPS, "Oracle  ·  " + Db.DBUSER);
        nav.onSelect(this::open);

        JPanel frame = new JPanel(new BorderLayout());
        frame.setBackground(Theme.GROUND);
        frame.add(nav, BorderLayout.WEST);
        Sheet sheet = new Sheet();
        sheet.add(host.component(), BorderLayout.CENTER);
        JPanel inset = new JPanel(new BorderLayout());
        inset.setOpaque(false);
        inset.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 8));
        inset.add(sheet);
        frame.add(inset, BorderLayout.CENTER);
        setContentPane(frame);

        open(0);
    }

    private void open(int index) {
        host.show(NAMES[index]);
        ((Page) pages.get(index)).onShow();
    }

    /** The white page, inset from the window frame, with rounded corners and a 1px edge. */
    private static final class Sheet extends JPanel {
        Sheet() {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.SHEET);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(Theme.LINE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
        }
    }
}
