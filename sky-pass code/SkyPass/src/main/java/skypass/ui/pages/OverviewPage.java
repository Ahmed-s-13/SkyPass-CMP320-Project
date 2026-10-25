package skypass.ui.pages;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import skypass.dao.ReportDao;
import skypass.dao.ReportDao.Departure;
import skypass.dao.ReportDao.Totals;
import skypass.ui.Async;
import skypass.ui.Page;
import skypass.ui.Theme;
import skypass.ui.Ui;
import skypass.ui.anim.CountUpLabel;
import skypass.ui.anim.FadeLayer;
import skypass.ui.anim.FlapBoard;
import skypass.ui.anim.FlightPathHero;

/**
 * Overview: answers "what flies next, and how is the platform doing?".
 * The next departure is drawn as its route; below it, the departures
 * board and the platform totals.
 */
public class OverviewPage extends JPanel implements Page {

    private static final DateTimeFormatter BOARD_DATE = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
    private static final DateTimeFormatter BOARD_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final JLabel summary = Ui.muted("Loading…");
    private final FlightPathHero hero = new FlightPathHero();
    private final FlapBoard board = new FlapBoard();
    private final CountUpLabel customers = figure();
    private final CountUpLabel flights = figure();
    private final CountUpLabel bookings = figure();
    private final CountUpLabel revenue = figure();

    public OverviewPage(Runnable newBooking) {
        super(new BorderLayout());
        setOpaque(false);
        JPanel page = Ui.page();
        page.setOpaque(false);
        page.add(Ui.pageHeader("Overview", summary, Ui.primary("New booking", e -> newBooking.run())),
                BorderLayout.NORTH);

        JPanel content = new skypass.ui.TrackingPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        content.add(hero, gc);

        // Departures board (left) and platform totals (right)
        FadeLayer boardReveal = FadeLayer.wrap(section("Next departures", board), 24);
        boardReveal.revealWhenVisible(520, 120);
        FadeLayer totalsReveal = FadeLayer.wrap(section("Platform totals", totals()), 24);
        totalsReveal.revealWhenVisible(520, 220);

        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(Theme.S6, 0, 0, Theme.S6);
        content.add(boardReveal.component(), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(Theme.S6, 0, 0, 0);
        content.add(totalsReveal.component(), gc);
        // filler keeps everything at the top
        gc.gridx = 0;
        gc.gridy = 2;
        gc.weighty = 1;
        content.add(new JLabel(), gc);

        page.add(Ui.scroll(content), BorderLayout.CENTER);
        add(page);
    }

    @Override
    public void onShow() {
        Async.run(this, () -> new Object[]{ReportDao.nextDepartures(6), ReportDao.totals()}, result -> {
            @SuppressWarnings("unchecked")
            List<Departure> next = (List<Departure>) result[0];
            Totals totals = (Totals) result[1];
            showDepartures(next);
            customers.countTo(totals.customers(), Ui::whole);
            flights.countTo(totals.flights(), Ui::whole);
            bookings.countTo(totals.bookings(), Ui::whole);
            revenue.countTo(totals.revenue().doubleValue(), v -> Ui.money(BigDecimal.valueOf(v)));
        });
    }

    private void showDepartures(List<Departure> next) {
        if (next.isEmpty()) {
            summary.setText("No upcoming departures. Add a flight on the Flights page.");
            hero.setEmpty("No upcoming departures");
            board.setRows(List.of(), "No upcoming departures");
            return;
        }
        Departure d = next.get(0);
        summary.setText("Next departure: " + d.flightNo() + " " + d.fromCity() + " to " + d.toCity()
                + " on " + Ui.dateTime(d.departs()) + "  ·  " + d.sold() + " of " + d.capacity() + " seats sold");
        hero.setRoute(d.fromCode(), d.fromCity(), d.toCode(), d.toCity(),
                d.flightNo() + "  ·  " + d.airline() + "  ·  departs " + Ui.dateTime(d.departs())
                        + "  ·  " + Ui.nice(d.status()),
                Ui.duration(Duration.between(d.departs(), d.arrives()).toMinutes()));

        List<String[]> rows = new ArrayList<>();
        for (Departure dep : next) {
            rows.add(new String[]{BOARD_DATE.format(dep.departs()), BOARD_TIME.format(dep.departs()),
                    dep.flightNo(), dep.toCity(), remark(dep.status())});
        }
        board.setRows(rows, "");
    }

    private static String remark(String status) {
        return switch (status) {
            case "SCHEDULED" -> "On time";
            case "DELAYED" -> "Delayed";
            case "CANCELLED" -> "Cancelled";
            default -> status;
        };
    }

    private JComponent totals() {
        JPanel grid = new JPanel(new GridLayout(4, 1, 0, Theme.S4));
        grid.setOpaque(false);
        grid.add(stat(customers, "Customers"));
        grid.add(stat(flights, "Flights on the schedule"));
        grid.add(stat(bookings, "Active bookings"));
        grid.add(stat(revenue, "Revenue (AED)"));
        return grid;
    }

    private static JComponent stat(CountUpLabel value, String label) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(value);
        p.add(Ui.label(label, Theme.small(), Theme.INK_3));
        return p;
    }

    private static CountUpLabel figure() {
        CountUpLabel l = new CountUpLabel();
        l.setFont(Theme.figure(26));
        l.setForeground(Theme.INK);
        return l;
    }

    private static JPanel section(String heading, JComponent body) {
        JPanel p = new JPanel(new BorderLayout(0, Theme.S3));
        p.setOpaque(false);
        p.add(Ui.heading(heading), BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder());
        return p;
    }
}
