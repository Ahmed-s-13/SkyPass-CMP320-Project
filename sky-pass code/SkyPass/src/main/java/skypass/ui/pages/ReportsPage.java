package skypass.ui.pages;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.TableCellRenderer;
import skypass.dao.ReportDao;
import skypass.dao.ReportDao.AirlineLine;
import skypass.dao.ReportDao.BookingLine;
import skypass.dao.ReportDao.LoadLine;
import skypass.ui.Async;
import skypass.ui.DataTable;
import skypass.ui.Page;
import skypass.ui.Theme;
import skypass.ui.Ui;
import skypass.ui.anim.FadeLayer;
import skypass.ui.anim.PlaneSpinner;

/**
 * Reports: three multi-table reports. Each opens with one sentence that
 * states what the data shows, then the table behind it.
 */
public class ReportsPage extends JPanel implements Page {

    private final JLabel summary = Ui.muted("Loading…");
    private final PlaneSpinner spinner = new PlaneSpinner();
    private final JPanel holder = new JPanel(new BorderLayout());
    private final FadeLayer fade = FadeLayer.wrap(holder, 18);
    private int current = 0;

    private final DataTable<BookingLine> bookings = new DataTable<>(
            new String[]{"Ref", "Customer", "Booked", "Status", "Tickets", "Total (AED)", "Paid by", "Paid (AED)"},
            b -> new Object[]{b.ref(), b.customer(), DataTable.Sorted.of(Ui.date(b.bookedOn()), b.bookedOn()),
                    b.status(), b.tickets(), b.total(), Ui.nice(b.method()), b.paid()});
    private final DataTable<AirlineLine> airlines = new DataTable<>(
            new String[]{"Airline", "Code", "Fleet", "Flights", "Tickets sold", "Revenue (AED)", "Avg rating", "Reviews"},
            a -> new Object[]{a.airline(), a.code(), a.fleet(), a.flights(), a.ticketsSold(), a.revenue(),
                    a.avgRating(), a.reviews()});
    private final DataTable<LoadLine> load = new DataTable<>(
            new String[]{"Flight", "Airline", "Route", "Departs", "Aircraft", "Seats", "Sold", "Load"},
            l -> new Object[]{l.flightNo(), l.airline(), l.route(),
                    DataTable.Sorted.of(Ui.dateTime(l.departs()), l.departs()), l.aircraft(), l.capacity(),
                    l.sold(), l.loadPercent()});

    public ReportsPage() {
        super(new BorderLayout());
        setOpaque(false);
        JPanel page = Ui.page();
        page.setOpaque(false);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.GROUND);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        tabs.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        String[] names = {"Bookings", "Airlines", "Flight load"};
        for (int i = 0; i < names.length; i++) {
            int index = i;
            JToggleButton b = new JToggleButton(names[i], i == 0);
            b.setFont(Theme.body());
            b.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE,
                    "background: #F2F4F7; selectedBackground: #FFFFFF; selectedForeground: #101828;"
                    + "foreground: #475467; borderWidth: 0; focusWidth: 0; arc: 10; margin: 6,14,6,14;"
                    + "hoverBackground: #E6E9EF");
            b.addActionListener(e -> show(index));
            group.add(b);
            tabs.add(b);
        }
        page.add(Ui.pageHeader("Reports", summary, spinner, tabs), BorderLayout.NORTH);

        bookings.renderer(3, DataTable.status());
        bookings.minWidth(3, 100);
        bookings.widths(80, 170, 110, 110, 70, 110, 110, 110);
        airlines.widths(170, 60, 60, 60, 100, 120, 90, 70);
        airlines.renderer(6, ratingRenderer());
        load.widths(70, 140, 100, 150, 140, 60, 60, 170);
        load.renderer(7, new LoadBar());

        holder.setOpaque(false);
        page.add(fade.component(), BorderLayout.CENTER);
        add(page);
    }

    @Override
    public void onShow() {
        show(current);
    }

    private void show(int index) {
        current = index;
        spinner.start();
        switch (index) {
            case 0 -> Async.run(this, ReportDao::bookingSummary, list -> {
                bookings.setItems(list);
                BigDecimal sum = list.stream().filter(b -> !"CANCELLED".equals(b.status()))
                        .map(BookingLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
                summary.setText(list.isEmpty() ? "No bookings yet."
                        : list.size() + " bookings worth AED " + Ui.money(sum) + ". The largest is "
                        + list.get(0).ref() + " (" + list.get(0).customer() + ") at AED " + Ui.money(list.get(0).total()) + ".");
                swap(bookings);
            }, spinner::stop);
            case 1 -> Async.run(this, ReportDao::airlinePerformance, list -> {
                airlines.setItems(list);
                AirlineLine top = list.isEmpty() ? null : list.get(0);
                summary.setText(top == null || top.revenue().signum() == 0 ? "No airline has sold a ticket yet."
                        : top.airline() + " earns the most: AED " + Ui.money(top.revenue()) + " from "
                        + top.ticketsSold() + " tickets" + (top.avgRating() == null ? "." : ", rated "
                        + top.avgRating().setScale(1, java.math.RoundingMode.HALF_UP) + " out of 5."));
                swap(airlines);
            }, spinner::stop);
            default -> Async.run(this, ReportDao::flightLoad, list -> {
                load.setItems(list);
                LoadLine top = list.stream().max(Comparator.comparingDouble(LoadLine::loadPercent)).orElse(null);
                long empty = list.stream().filter(l -> l.sold() == 0).count();
                summary.setText(top == null ? "No flights yet."
                        : "Fullest flight: " + top.flightNo() + " with " + top.sold() + " of " + top.capacity()
                        + " seats sold. " + empty + " of " + list.size() + " flights have no tickets yet.");
                swap(load);
            }, spinner::stop);
        }
    }

    private void swap(DataTable<?> table) {
        holder.removeAll();
        holder.add(table, BorderLayout.CENTER);
        holder.revalidate();
        holder.repaint();
        fade.play(360, 0);
    }

    private static TableCellRenderer ratingRenderer() {
        return new DataTable.CellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int col) {
                Object shown = value instanceof BigDecimal b ? b.setScale(2, java.math.RoundingMode.HALF_UP) + " / 5" : null;
                Component c = super.getTableCellRendererComponent(t, shown, selected, focus, row, col);
                ((JLabel) c).setHorizontalAlignment(JLabel.TRAILING);
                return c;
            }
        };
    }

    /** Seats sold as a share of capacity: a bar in the accent colour, with the percentage. */
    private static final class LoadBar extends DataTable.CellRenderer {
        private double percent;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            percent = value instanceof Number n ? n.doubleValue() : 0;
            Component c = super.getTableCellRendererComponent(t, String.format("%.1f%%", percent),
                    selected, focus, row, col);
            ((JLabel) c).setHorizontalAlignment(JLabel.TRAILING);
            return c;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int track = Math.max(40, getWidth() - 80);
            int y = getHeight() / 2 - 3;
            g2.setColor(Theme.PILL);
            g2.fillRoundRect(12, y, track, 6, 6, 6);
            g2.setColor(Theme.ACCENT);
            int w = (int) Math.round(track * Math.min(100, percent) / 100.0);
            if (percent > 0) {
                g2.fillRoundRect(12, y, Math.max(3, w), 6, 6, 6);
            }
            g2.dispose();
        }
    }
}
