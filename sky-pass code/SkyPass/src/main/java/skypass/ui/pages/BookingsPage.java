package skypass.ui.pages;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import skypass.dao.BookingDao;
import skypass.dao.BookingDao.Booking;
import skypass.dao.BookingDao.NewTicket;
import skypass.dao.BookingDao.Ticket;
import skypass.dao.Item;
import skypass.dao.LookupDao;
import skypass.dao.LookupDao.BookableFlight;
import skypass.ui.Async;
import skypass.ui.DataTable;
import skypass.ui.FormCard;
import skypass.ui.Page;
import skypass.ui.Theme;
import skypass.ui.Toast;
import skypass.ui.Ui;
import skypass.ui.Validation;
import skypass.ui.anim.BoardingPassDialog;
import skypass.ui.anim.PlaneSpinner;

/**
 * Bookings: BOOKING, its TICKETs (the ternary relationship between
 * booking, traveler and flight) and its PAYMENT. A new booking is saved
 * with all its tickets and its payment in one transaction.
 */
public class BookingsPage extends JPanel implements Page {

    private final JLabel summary = Ui.muted("Loading…");
    private final PlaneSpinner spinner = new PlaneSpinner();
    private final JTextField search = Ui.search("Search reference or customer", this::reload);
    private final DataTable<Booking> table = new DataTable<>(
            new String[]{"Ref", "Customer", "Booked", "Status", "Total (AED)"},
            b -> new Object[]{b.ref(), b.customer(), DataTable.Sorted.of(Ui.date(b.bookedOn()), b.bookedOn()),
                    b.status(), b.total()});
    private final JLabel ticketsHeading = Ui.heading("Tickets");
    private final DataTable<Ticket> tickets = new DataTable<>(
            new String[]{"Traveler", "Flight", "Departs", "Seat", "Cabin", "Fare (AED)"},
            t -> new Object[]{t.traveler(), t.flightNo() + "  " + t.route(),
                    DataTable.Sorted.of(Ui.COMPACT.format(t.departs()), t.departs()), t.seat(), Ui.nice(t.cabin()), t.fare()});

    // ---- Right side: one card to edit a booking, one to create a new one ----
    private final CardLayout sideCards = new CardLayout();
    private final JPanel side = new JPanel(sideCards);

    private final FormCard editForm = new FormCard("Booking");
    private final JComboBox<String> status = Ui.combo();
    private final JComboBox<String> method = Ui.combo();
    private final JLabel paidNote = Ui.label(" ", Theme.body(), Theme.INK_2);
    private Booking editing;

    private final FormCard newForm = new FormCard("New booking");
    private final JComboBox<Item> customer = Ui.combo();
    private final JComboBox<Item> traveler = Ui.combo();
    private final JComboBox<BookableFlight> flight = Ui.combo();
    private final JComboBox<String> cabin = Ui.combo();
    private final JTextField seat = Ui.field("12A");
    private final JLabel fareNote = Ui.label(" ", Theme.small(), Theme.INK_2);
    private final DataTable<NewTicket> lines = new DataTable<>(
            new String[]{"Traveler", "Flight", "Seat", "Fare (AED)"},
            t -> new Object[]{t.travelerName(), t.flightLabel().split("  ·  ")[0], t.seat(), t.fare()});
    private final JComboBox<String> newMethod = Ui.combo();
    private final JLabel total = Ui.label("AED 0.00", Theme.figure(22), Theme.INK);
    private final List<NewTicket> pending = new ArrayList<>();
    private boolean repopulating;

    public BookingsPage() {
        super(new BorderLayout());
        setOpaque(false);
        JPanel page = Ui.page();
        page.setOpaque(false);
        page.add(Ui.pageHeader("Bookings", summary, spinner, search,
                Ui.secondary("New booking", e -> startNewBooking())), BorderLayout.NORTH);

        table.widths(70, 130, 100, 104, 90);
        table.minWidth(0, 66);
        table.minWidth(3, 100);
        table.renderer(3, DataTable.status());
        table.setEmptyText("No bookings match your search.");
        table.onSelect(this::edit);
        tickets.widths(110, 120, 104, 48, 80, 80);
        tickets.minWidth(3, 46);
        tickets.minWidth(5, 76);
        tickets.setEmptyText("Select a booking to see its tickets.");

        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.gridy = 0;
        gc.weighty = 0.6;
        left.add(table, gc);
        gc.gridy = 1;
        gc.weighty = 0;
        ticketsHeading.setBorder(BorderFactory.createEmptyBorder(Theme.S5, 0, Theme.S2, 0));
        left.add(ticketsHeading, gc);
        gc.gridy = 2;
        gc.weighty = 0.4;
        left.add(tickets, gc);

        buildEditForm();
        buildNewForm();
        side.setOpaque(false);
        side.add(editForm, "edit");
        side.add(newForm, "new");
        page.add(Ui.workspace(left, side), BorderLayout.CENTER);
        add(page);
        sideCards.show(side, "new");
    }

    private void buildEditForm() {
        for (String s : BookingDao.STATUSES) {
            status.addItem(s);
        }
        for (String m : BookingDao.METHODS) {
            method.addItem(m);
            newMethod.addItem(m);
        }
        status.setRenderer(FlightsPage.niceRenderer());
        method.setRenderer(FlightsPage.niceRenderer());
        newMethod.setRenderer(FlightsPage.niceRenderer());
        editForm.field("Status", status);
        editForm.field("Payment method", method);
        editForm.extra(paidNote);
        editForm.end();
        editForm.buttons(Ui.primary("Save changes", e -> saveEdit()), Ui.danger("Delete booking", e -> delete()));
    }

    private void buildNewForm() {
        for (String c : BookingDao.CABINS) {
            cabin.addItem(c);
        }
        cabin.setRenderer(FlightsPage.niceRenderer());
        customer.addActionListener(e -> customerChanged());
        flight.addActionListener(e -> updateFare());
        cabin.addActionListener(e -> updateFare());

        newForm.field("Customer", customer);
        JLabel step = Ui.label("Add tickets", Theme.strong(), Theme.INK);
        step.setBorder(BorderFactory.createEmptyBorder(Theme.S2, 0, Theme.S2, 0));
        newForm.extra(step);
        newForm.field("Traveler", traveler);
        newForm.field("Flight", flight);
        newForm.pair("Cabin", cabin, "Seat", seat);
        JPanel addRow = new JPanel(new BorderLayout());
        addRow.setOpaque(false);
        addRow.add(fareNote, BorderLayout.WEST);
        addRow.add(Ui.secondary("Add ticket", e -> addTicket()), BorderLayout.EAST);
        newForm.extra(addRow);

        lines.setPreferredSize(new Dimension(300, 140));
        lines.setMinimumSize(new Dimension(100, 140));
        lines.widths(110, 60, 46, 90);
        lines.minWidth(3, 84);
        lines.setEmptyText("No tickets added yet.");
        JPanel linesBlock = new JPanel(new BorderLayout(0, Theme.S2));
        linesBlock.setOpaque(false);
        linesBlock.setBorder(BorderFactory.createEmptyBorder(Theme.S3, 0, Theme.S3, 0));
        linesBlock.add(lines, BorderLayout.CENTER);
        JPanel under = new JPanel(new BorderLayout());
        under.setOpaque(false);
        under.add(Ui.danger("Remove ticket", e -> removeTicket()), BorderLayout.WEST);
        under.add(total, BorderLayout.EAST);
        linesBlock.add(under, BorderLayout.SOUTH);
        newForm.extra(linesBlock);
        newForm.field("Payment method", newMethod);
        newForm.end();
        newForm.buttons(Ui.primary("Confirm and pay", e -> create()), Ui.secondary("Clear", e -> resetNew()));
        newForm.setHeading("New booking", "Tickets and payment are saved together.");
    }

    @Override
    public void onShow() {
        reload();
        Async.run(this, () -> new Object[]{LookupDao.customers(), LookupDao.bookableFlights()}, r -> {
            @SuppressWarnings("unchecked")
            List<Item> customers = (List<Item>) r[0];
            @SuppressWarnings("unchecked")
            List<BookableFlight> flights = (List<BookableFlight>) r[1];
            // Refresh the dropdowns without losing a booking that is being put together
            Item keepCustomer = (Item) customer.getSelectedItem();
            BookableFlight keepFlight = (BookableFlight) flight.getSelectedItem();
            repopulating = true;
            customer.removeAllItems();
            customers.forEach(customer::addItem);
            customer.setSelectedIndex(-1);
            if (keepCustomer != null) {
                customers.stream().filter(keepCustomer::equals).findFirst().ifPresent(customer::setSelectedItem);
            }
            flight.removeAllItems();
            flights.forEach(flight::addItem);
            flight.setSelectedIndex(-1);
            if (keepFlight != null) {
                flights.stream().filter(f -> f.id() == keepFlight.id()).findFirst().ifPresent(flight::setSelectedItem);
            }
            repopulating = false;
            if (keepCustomer != null && customer.getSelectedItem() == null) {
                customerChanged();   // that customer no longer exists
            }
        });
    }

    private void reload() {
        spinner.start();
        Async.run(this, () -> BookingDao.list(search.getText()), list -> {
            table.setItems(list);
            BigDecimal sum = list.stream().filter(b -> !"CANCELLED".equals(b.status()))
                    .map(Booking::total).reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setText(list.size() + (list.size() == 1 ? " booking" : " bookings")
                    + "  ·  AED " + Ui.money(sum) + " in active bookings");
        }, spinner::stop);
    }

    // ---------------- Editing an existing booking ----------------

    private void edit(Booking b) {
        if (b == null) {
            return;
        }
        editing = b;
        editForm.setHeading(b.ref(), b.customer() + "  ·  booked " + Ui.date(b.bookedOn()));
        status.setSelectedItem(b.status());
        method.setSelectedItem(b.method());
        paidNote.setText("<html>Total AED " + Ui.money(b.total()) + " for " + b.tickets()
                + (b.tickets() == 1 ? " ticket" : " tickets") + ".<br>Paid AED " + Ui.money(b.paid())
                + " by " + Ui.nice(b.method()).toLowerCase() + ".</html>");
        sideCards.show(side, "edit");
        ticketsHeading.setText("Tickets on " + b.ref());
        Async.run(this, () -> BookingDao.tickets(b.ref()), tickets::setItems);
    }

    private void saveEdit() {
        if (editing == null) {
            return;
        }
        Booking b = editing;
        String newStatus = (String) status.getSelectedItem();
        String newMethodValue = (String) method.getSelectedItem();
        Async.run(this, () -> {
            BookingDao.update(b.ref(), newStatus, newMethodValue);
            return null;
        }, x -> {
            Toast.success(this, b.ref() + " is now " + Ui.nice(newStatus).toLowerCase() + ".");
            reload();
        });
    }

    private void delete() {
        if (editing == null) {
            return;
        }
        Booking b = editing;
        int answer = JOptionPane.showConfirmDialog(this, "Delete booking " + b.ref() + " with its "
                        + b.tickets() + " ticket(s) and its payment record?", "Delete booking",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        Async.run(this, () -> {
            BookingDao.delete(b.ref());
            return null;
        }, x -> {
            Toast.success(this, "Booking " + b.ref() + " was deleted.");
            startNewBooking();
            reload();
        });
    }

    // ---------------- Creating a new booking ----------------

    /** Opens the empty New booking form (also used by the Overview's button). */
    public void startNewBooking() {
        editing = null;
        table.clearSelection();
        tickets.setItems(List.of());
        ticketsHeading.setText("Tickets");
        resetNew();
        sideCards.show(side, "new");
    }

    private void resetNew() {
        newForm.clearErrors();
        pending.clear();
        lines.setItems(List.of());
        seat.setText("");
        customer.setSelectedIndex(-1);
        flight.setSelectedIndex(-1);
        cabin.setSelectedItem("ECONOMY");
        newMethod.setSelectedItem("CARD");
        updateTotal();
    }

    private void customerChanged() {
        if (repopulating) {
            return;
        }
        Item c = (Item) customer.getSelectedItem();
        traveler.removeAllItems();
        if (!pending.isEmpty()) {
            pending.clear();
            lines.setItems(List.of());
            updateTotal();
            Toast.info(this, "Tickets were cleared: travelers must belong to the booking's customer.");
        }
        if (c == null) {
            return;
        }
        Async.run(this, () -> LookupDao.travelers(c.id()), list -> {
            if (c.equals(customer.getSelectedItem())) {
                traveler.removeAllItems();
                list.forEach(traveler::addItem);
            }
        });
    }

    private void updateFare() {
        BookableFlight f = (BookableFlight) flight.getSelectedItem();
        String c = (String) cabin.getSelectedItem();
        fareNote.setText(f == null || c == null ? " "
                : "Fare AED " + Ui.money(BookingDao.fare(f.baseFare(), c)) + "  (" + Ui.nice(c).toLowerCase() + ")");
    }

    private void addTicket() {
        newForm.clearErrors();
        Item t = (Item) traveler.getSelectedItem();
        BookableFlight f = (BookableFlight) flight.getSelectedItem();
        String c = (String) cabin.getSelectedItem();
        String s = seat.getText().trim().toUpperCase();

        if (customer.getSelectedItem() == null) newForm.error(customer, "Choose who is booking.");
        if (t == null) newForm.error(traveler, "Choose a traveler saved on this customer's account.");
        if (f == null) newForm.error(flight, "Choose an upcoming flight.");
        if (!Validation.SEAT.matcher(s).matches()) newForm.error(seat, "Like 12A.");
        if (newForm.ok()) {
            for (NewTicket p : pending) {
                if (p.flightId() == f.id() && p.seat().equals(s)) {
                    newForm.error(seat, "Already in this booking.");
                }
                if (p.flightId() == f.id() && p.travelerNo() == t.id()) {
                    newForm.error(traveler, "Already on this flight in this booking.");
                }
            }
        }
        if (!newForm.ok()) {
            return;
        }
        pending.add(new NewTicket(t.id(), t.label().replaceAll(" \\(.*\\)$", ""), f.id(), f.label(), s, c,
                BookingDao.fare(f.baseFare(), c)));
        lines.setItems(new ArrayList<>(pending));
        seat.setText("");
        updateTotal();
    }

    private void removeTicket() {
        NewTicket t = lines.selected();
        if (t == null) {
            Toast.info(this, "Select a ticket in the list first.");
            return;
        }
        pending.remove(t);
        lines.setItems(new ArrayList<>(pending));
        updateTotal();
    }

    private void updateTotal() {
        BigDecimal sum = pending.stream().map(NewTicket::fare).reduce(BigDecimal.ZERO, BigDecimal::add);
        total.setText("AED " + Ui.money(sum));
    }

    private void create() {
        newForm.clearErrors();
        Item c = (Item) customer.getSelectedItem();
        if (c == null) newForm.error(customer, "Choose who is booking.");
        if (pending.isEmpty() && c != null) newForm.error(flight, "Add at least one ticket.");
        if (!newForm.ok()) {
            return;
        }
        List<NewTicket> toSave = new ArrayList<>(pending);
        String pay = (String) newMethod.getSelectedItem();
        BigDecimal sum = toSave.stream().map(NewTicket::fare).reduce(BigDecimal.ZERO, BigDecimal::add);
        Async.run(this, () -> BookingDao.create(c.id(), toSave, pay), ref -> {
            List<String> flightLines = new ArrayList<>();
            for (NewTicket t : toSave) {
                flightLines.add(t.travelerName() + "  ·  " + t.flightLabel() + "  ·  seat " + t.seat());
            }
            long travelers = toSave.stream().mapToInt(NewTicket::travelerNo).distinct().count();
            String name = c.label().replaceAll("  ·  #\\d+$", "");
            resetNew();
            reload();
            new BoardingPassDialog(SwingUtilities.getWindowAncestor(this), ref, name, flightLines,
                    (int) travelers, Ui.money(sum)).setVisible(true);
        });
    }
}
