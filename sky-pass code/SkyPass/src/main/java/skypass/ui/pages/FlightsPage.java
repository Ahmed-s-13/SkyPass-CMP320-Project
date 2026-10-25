package skypass.ui.pages;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import skypass.dao.FlightDao;
import skypass.dao.FlightDao.Flight;
import skypass.dao.Item;
import skypass.dao.LookupDao;
import skypass.ui.Async;
import skypass.ui.DataTable;
import skypass.ui.FormCard;
import skypass.ui.Page;
import skypass.ui.Theme;
import skypass.ui.Toast;
import skypass.ui.Ui;
import skypass.ui.Validation;
import skypass.ui.anim.PlaneSpinner;

/** Flights: FLIGHT with its amenities (multi-valued). Duration is derived, never stored. */
public class FlightsPage extends JPanel implements Page {

    private final JLabel summary = Ui.muted("Loading…");
    private final PlaneSpinner spinner = new PlaneSpinner();
    private final JTextField search = Ui.search("Search flight, airport or airline", this::reload);
    private final DataTable<Flight> table = new DataTable<>(
            new String[]{"Flight", "Route", "Departs", "Sold", "Status"},
            f -> new Object[]{f.flightNo(), f.origin() + " → " + f.destination(),
                    DataTable.Sorted.of(Ui.COMPACT.format(f.departs()), f.departs()),
                    DataTable.Sorted.of(f.sold() + " / " + f.capacity(), f.sold()), f.status()});

    private final FormCard form = new FormCard("New flight");
    private final JTextField flightNo = Ui.field("EK001");
    private final JComboBox<String> status = Ui.combo();
    private final JComboBox<Item> airline = Ui.combo();
    private final JComboBox<Item> aircraft = Ui.combo();
    private final JComboBox<Item> origin = Ui.combo();
    private final JComboBox<Item> destination = Ui.combo();
    private final JTextField departs = Ui.field("yyyy-mm-dd hh:mm");
    private final JTextField arrives = Ui.field("yyyy-mm-dd hh:mm");
    private final JTextField fare = Ui.field("2150.00");
    private final JLabel duration = Ui.label(" ", Theme.small(), Theme.INK_2);
    private final List<JCheckBox> amenities = new ArrayList<>();
    private final JButton save = Ui.primary("Add flight", e -> save());
    private final JButton delete = Ui.danger("Delete", e -> delete());

    private Flight editing;
    private boolean lookupsLoaded;
    private String wantedTail;   // aircraft to select once the airline's fleet has loaded

    public FlightsPage() {
        super(new BorderLayout());
        setOpaque(false);
        JPanel page = Ui.page();
        page.setOpaque(false);
        page.add(Ui.pageHeader("Flights", summary, spinner, search,
                Ui.secondary("New flight", e -> startNew())), BorderLayout.NORTH);

        table.widths(70, 100, 130, 80, 104);
        table.minWidth(0, 64);
        table.minWidth(1, 92);
        table.renderer(4, DataTable.status());
        table.minWidth(4, 100);
        table.setEmptyText("No flights match your search.");
        table.onSelect(this::edit);

        for (String s : FlightDao.STATUSES) {
            status.addItem(s);
        }
        status.setRenderer(niceRenderer());
        airline.addActionListener(e -> loadAircraft());

        form.pair("Flight number", flightNo, "Status", status);
        form.field("Airline", airline);
        form.field("Aircraft", aircraft);
        form.pair("From", origin, "To", destination);
        form.pair("Departs (UAE time)", departs, "Arrives (UAE time)", arrives);
        form.pair("Base fare (AED)", fare, "Duration", duration);
        JPanel boxes = new JPanel(new GridLayout(0, 2, Theme.S2, Theme.S1));
        boxes.setOpaque(false);
        for (String a : FlightDao.AMENITIES) {
            JCheckBox box = new JCheckBox(a);
            box.setFont(Theme.body());
            box.setOpaque(false);
            amenities.add(box);
            boxes.add(box);
        }
        JPanel amenityBlock = new JPanel(new BorderLayout(0, 6));
        amenityBlock.setOpaque(false);
        amenityBlock.add(Ui.label("Amenities", Theme.small(), Theme.INK_2), BorderLayout.NORTH);
        amenityBlock.add(boxes, BorderLayout.CENTER);
        form.extra(amenityBlock);
        form.end();
        form.buttons(save, delete, Ui.secondary("Clear", e -> startNew()));

        // Duration is derived live from the two times
        DocumentListener times = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateDuration(); }
            public void removeUpdate(DocumentEvent e) { updateDuration(); }
            public void changedUpdate(DocumentEvent e) { updateDuration(); }
        };
        departs.getDocument().addDocumentListener(times);
        arrives.getDocument().addDocumentListener(times);

        page.add(Ui.workspace(table, form), BorderLayout.CENTER);
        add(page);
        startNew();
    }

    @Override
    public void onShow() {
        if (!lookupsLoaded) {
            Async.run(this, () -> new Object[]{LookupDao.airlines(), LookupDao.airports()}, r -> {
                @SuppressWarnings("unchecked")
                List<Item> airlines = (List<Item>) r[0];
                @SuppressWarnings("unchecked")
                List<Item> airports = (List<Item>) r[1];
                airlines.forEach(airline::addItem);
                for (Item a : airports) {
                    origin.addItem(a);
                    destination.addItem(a);
                }
                origin.setSelectedIndex(-1);
                destination.setSelectedIndex(-1);
                lookupsLoaded = true;
                if (editing != null) {
                    edit(editing);
                } else {
                    airline.setSelectedIndex(-1);
                }
            });
        }
        reload();
    }

    private void reload() {
        spinner.start();
        Async.run(this, () -> FlightDao.list(search.getText()), list -> {
            table.setItems(list);
            long upcoming = list.stream().filter(f -> f.departs().isAfter(LocalDateTime.now())).count();
            summary.setText(list.size() + " flights  ·  " + upcoming + " still to depart");
        }, spinner::stop);
    }

    private void loadAircraft() {
        Item a = (Item) airline.getSelectedItem();
        aircraft.removeAllItems();
        if (a == null) {
            return;
        }
        Async.run(this, () -> LookupDao.aircraft(a.id()), list -> {
            if (!Objects.equals(airline.getSelectedItem(), a)) {
                return;   // the user picked another airline meanwhile
            }
            aircraft.removeAllItems();
            list.forEach(aircraft::addItem);
            selectByKey(aircraft, wantedTail);
            if (wantedTail == null) {
                aircraft.setSelectedIndex(list.isEmpty() ? -1 : 0);
            }
            wantedTail = null;
        });
    }

    private void updateDuration() {
        LocalDateTime d = Validation.dateTime(departs.getText());
        LocalDateTime a = Validation.dateTime(arrives.getText());
        if (d != null && a != null && a.isAfter(d)) {
            duration.setText(Ui.duration(Duration.between(d, a).toMinutes()));
        } else {
            duration.setText("–");
        }
    }

    // ---------------- Modes ----------------

    private void startNew() {
        editing = null;
        table.clearSelection();
        form.clearErrors();
        form.setHeading("New flight", "Aircraft are limited to the chosen airline's fleet.");
        flightNo.setText("");
        status.setSelectedItem("SCHEDULED");
        airline.setSelectedIndex(-1);
        origin.setSelectedIndex(-1);
        destination.setSelectedIndex(-1);
        departs.setText("");
        arrives.setText("");
        fare.setText("");
        amenities.forEach(b -> b.setSelected(false));
        save.setText("Add flight");
        delete.setVisible(false);
    }

    private void edit(Flight f) {
        if (f == null) {
            return;
        }
        editing = f;
        form.clearErrors();
        form.setHeading(f.flightNo() + "  " + f.origin() + " → " + f.destination(),
                f.airline() + "  ·  " + f.sold() + " of " + f.capacity() + " seats sold");
        flightNo.setText(f.flightNo());
        status.setSelectedItem(f.status());
        wantedTail = f.tailNo();
        if (!selectByKey(airline, f.airlineId())) {
            wantedTail = null;
        } else if (aircraft.getItemCount() > 0) {
            selectByKey(aircraft, f.tailNo());
        }
        selectByKey(origin, f.origin());
        selectByKey(destination, f.destination());
        departs.setText(Ui.INPUT_DATE_TIME.format(f.departs()));
        arrives.setText(Ui.INPUT_DATE_TIME.format(f.arrives()));
        fare.setText(f.baseFare().toPlainString());
        for (JCheckBox b : amenities) {
            b.setSelected(f.amenities().contains(b.getText()));
        }
        save.setText("Save changes");
        delete.setVisible(true);
    }

    // ---------------- Actions ----------------

    private void save() {
        form.clearErrors();
        String no = flightNo.getText().trim().toUpperCase();
        Item al = (Item) airline.getSelectedItem();
        Item ac = (Item) aircraft.getSelectedItem();
        Item from = (Item) origin.getSelectedItem();
        Item to = (Item) destination.getSelectedItem();
        LocalDateTime dep = Validation.dateTime(departs.getText());
        LocalDateTime arr = Validation.dateTime(arrives.getText());
        BigDecimal base = Validation.money(fare.getText());

        if (!Validation.FLIGHT_NO.matcher(no).matches()) form.error(flightNo, "Like EK001.");
        if (al == null) form.error(airline, "Choose the operating airline.");
        if (ac == null) form.error(aircraft, al == null ? "Choose an airline first." : "This airline has no aircraft.");
        if (from == null) form.error(origin, "Choose where it departs from.");
        if (to == null) form.error(destination, "Choose where it lands.");
        else if (from != null && from.code().equals(to.code())) form.error(destination, "Same as the origin.");
        if (dep == null) form.error(departs, "Like 2026-11-08 07:45");
        if (arr == null) form.error(arrives, "Like 2026-11-08 15:35");
        else if (dep != null && !arr.isAfter(dep)) form.error(arrives, "Must be after departure.");
        if (base == null) form.error(fare, "A positive amount.");
        else if (base.compareTo(new BigDecimal("999999.99")) > 0) form.error(fare, "Too large.");
        if (!form.ok()) {
            return;
        }

        List<String> chosen = new ArrayList<>();
        amenities.stream().filter(JCheckBox::isSelected).forEach(b -> chosen.add(b.getText()));
        Flight f = new Flight(editing == null ? 0 : editing.id(), no, al.id(), al.label(), ac.code(),
                from.code(), to.code(), dep, arr, base, (String) status.getSelectedItem(), 0, 0, chosen);
        boolean isNew = editing == null;
        Async.run(this, () -> {
            if (isNew) {
                return FlightDao.insert(f);
            }
            FlightDao.update(f);
            return f.id();
        }, id -> {
            Toast.success(this, isNew ? "Flight " + no + " was added to the schedule." : "Saved changes to " + no + ".");
            spinner.start();
            Async.run(this, () -> FlightDao.list(search.getText()), list -> {
                table.setItems(list);
                list.stream().filter(x -> x.id() == id).findFirst().ifPresent(table::select);
            }, spinner::stop);
        });
    }

    private void delete() {
        if (editing == null) {
            return;
        }
        Flight f = editing;
        int answer = JOptionPane.showConfirmDialog(this, "Delete flight " + f.flightNo() + " on "
                        + Ui.dateTime(f.departs()) + "?", "Delete flight",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        Async.run(this, () -> {
            FlightDao.delete(f.id());
            return null;
        }, x -> {
            Toast.success(this, "Flight " + f.flightNo() + " was deleted.");
            startNew();
            reload();
        });
    }

    // ---------------- Helpers ----------------

    private static boolean selectByKey(JComboBox<Item> combo, Object key) {
        if (key == null) {
            return false;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).key().equals(key)) {
                combo.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    static javax.swing.ListCellRenderer<Object> niceRenderer() {
        javax.swing.DefaultListCellRenderer r = new javax.swing.DefaultListCellRenderer();
        return (list, value, index, selected, focus) ->
                r.getListCellRendererComponent(list, value == null ? "" : Ui.nice(value.toString()), index, selected, focus);
    }
}
