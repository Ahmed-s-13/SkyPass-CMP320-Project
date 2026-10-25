package skypass.ui.pages;

import java.awt.BorderLayout;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import skypass.dao.Item;
import skypass.dao.LookupDao;
import skypass.dao.ReviewDao;
import skypass.dao.ReviewDao.Review;
import skypass.ui.Async;
import skypass.ui.DataTable;
import skypass.ui.FormCard;
import skypass.ui.Page;
import skypass.ui.StarRating;
import skypass.ui.Theme;
import skypass.ui.Toast;
import skypass.ui.Ui;
import skypass.ui.Validation;
import skypass.ui.anim.PlaneSpinner;

/** Reviews: the M:N relationship REVIEW between customers and airlines. */
public class ReviewsPage extends JPanel implements Page {

    private final JLabel summary = Ui.muted("Loading…");
    private final PlaneSpinner spinner = new PlaneSpinner();
    private final JTextField search = Ui.search("Search customer, airline or comment", this::reload);
    private final DataTable<Review> table = new DataTable<>(
            new String[]{"Customer", "Airline", "Rating", "Comment", "Date"},
            r -> new Object[]{r.customer(), r.airline(), r.rating(), r.comment(),
                    DataTable.Sorted.of(Ui.date(r.date()), r.date())});

    private final FormCard form = new FormCard("New review");
    private final JComboBox<Item> customer = Ui.combo();
    private final JComboBox<Item> airline = Ui.combo();
    private final JLabel average = Ui.label(" ", Theme.small(), Theme.INK_2);
    private final StarRating rating = new StarRating();
    private final JTextArea comment = new JTextArea(4, 20);
    private final JTextField date = Ui.field("yyyy-mm-dd");
    private final JButton save = Ui.primary("Post review", e -> save());
    private final JButton delete = Ui.danger("Delete", e -> delete());

    private Review editing;
    private boolean lookupsLoaded;

    public ReviewsPage() {
        super(new BorderLayout());
        setOpaque(false);
        JPanel page = Ui.page();
        page.setOpaque(false);
        page.add(Ui.pageHeader("Reviews", summary, spinner, search,
                Ui.secondary("New review", e -> startNew())), BorderLayout.NORTH);

        table.widths(120, 120, 90, 220, 90);
        table.renderer(2, StarRating.renderer());
        table.minWidth(2, 84);
        table.setEmptyText("No reviews match your search.");
        table.onSelect(this::edit);

        comment.setFont(Theme.body());
        comment.setLineWrap(true);
        comment.setWrapStyleWord(true);
        comment.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT, "Optional");
        airline.addActionListener(e -> showAverage());

        form.field("Customer", customer);
        form.field("Airline", airline);
        form.extra(average);
        form.field("Rating", rating);
        form.field("Comment", comment);
        form.field("Review date", date);
        form.end();
        form.buttons(save, delete, Ui.secondary("Clear", e -> startNew()));

        page.add(Ui.workspace(table, form), BorderLayout.CENTER);
        add(page);
        startNew();
    }

    @Override
    public void onShow() {
        Async.run(this, () -> new Object[]{LookupDao.customers(), LookupDao.airlines()}, r -> {
            @SuppressWarnings("unchecked")
            List<Item> customers = (List<Item>) r[0];
            @SuppressWarnings("unchecked")
            List<Item> airlines = (List<Item>) r[1];
            customer.removeAllItems();
            customers.forEach(customer::addItem);
            if (!lookupsLoaded) {
                airlines.forEach(airline::addItem);
                lookupsLoaded = true;
            }
            if (editing != null) {
                edit(editing);
            } else {
                customer.setSelectedIndex(-1);
                airline.setSelectedIndex(-1);
            }
        });
        reload();
    }

    private void reload() {
        spinner.start();
        Async.run(this, () -> ReviewDao.list(search.getText()), list -> {
            table.setItems(list);
            double avg = list.stream().mapToInt(Review::rating).average().orElse(0);
            summary.setText(list.size() + (list.size() == 1 ? " review" : " reviews")
                    + (list.isEmpty() ? "" : String.format("  ·  %.1f out of 5 on average", avg)));
        }, spinner::stop);
    }

    /** Shows the airline's derived average rating from V_AIRLINE_STATS. */
    private void showAverage() {
        Item a = (Item) airline.getSelectedItem();
        if (a == null) {
            average.setText(" ");
            return;
        }
        Async.run(this, () -> ReviewDao.rating(a.id()), r -> {
            if (a.equals(airline.getSelectedItem())) {
                average.setText(r.count() == 0 ? "No reviews for this airline yet."
                        : String.format("Rated %.2f on average from %d review%s.", r.average(), r.count(),
                                r.count() == 1 ? "" : "s"));
            }
        });
    }

    // ---------------- Modes ----------------

    private void startNew() {
        editing = null;
        table.clearSelection();
        form.clearErrors();
        form.setHeading("New review", "Customers can only review airlines they have flown with.");
        customer.setEnabled(true);
        airline.setEnabled(true);
        customer.setSelectedIndex(-1);
        airline.setSelectedIndex(-1);
        rating.setValue(0);
        comment.setText("");
        date.setText(Ui.INPUT_DATE.format(LocalDate.now()));
        save.setText("Post review");
        delete.setVisible(false);
    }

    private void edit(Review r) {
        if (r == null) {
            return;
        }
        editing = r;
        form.clearErrors();
        form.setHeading(r.customer() + " on " + r.airline(), "Customer and airline can't be changed.");
        selectById(customer, r.customerId());
        selectById(airline, r.airlineId());
        customer.setEnabled(false);
        airline.setEnabled(false);
        rating.setValue(r.rating());
        comment.setText(r.comment() == null ? "" : r.comment());
        date.setText(Ui.INPUT_DATE.format(r.date()));
        save.setText("Save changes");
        delete.setVisible(true);
    }

    // ---------------- Actions ----------------

    private void save() {
        form.clearErrors();
        Item c = (Item) customer.getSelectedItem();
        Item a = (Item) airline.getSelectedItem();
        LocalDate d = Validation.date(date.getText());
        if (c == null) form.error(customer, "Choose who wrote the review.");
        if (a == null) form.error(airline, "Choose the airline being reviewed.");
        if (rating.getValue() < 1) form.error(rating, "Pick 1 to 5 stars.");
        if (comment.getText().trim().length() > 500) form.error(comment, "Keep it under 500 characters.");
        if (d == null) form.error(date, "Use the format 2026-10-14.");
        else if (d.isAfter(LocalDate.now())) form.error(date, "Can't be in the future.");
        if (!form.ok()) {
            return;
        }
        Review r = new Review(c.id(), c.label(), a.id(), a.label(), rating.getValue(), comment.getText(), d);
        boolean isNew = editing == null;
        Async.run(this, () -> {
            if (isNew) {
                ReviewDao.insert(r);
            } else {
                ReviewDao.update(r);
            }
            return null;
        }, x -> {
            Toast.success(this, isNew ? "Review posted." : "Review updated.");
            startNew();
            reload();
            showAverage();
        });
    }

    private void delete() {
        if (editing == null) {
            return;
        }
        Review r = editing;
        int answer = JOptionPane.showConfirmDialog(this, "Delete " + r.customer() + "'s review of " + r.airline() + "?",
                "Delete review", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        Async.run(this, () -> {
            ReviewDao.delete(r.customerId(), r.airlineId());
            return null;
        }, x -> {
            Toast.success(this, "Review deleted.");
            startNew();
            reload();
        });
    }

    private static void selectById(JComboBox<Item> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id() == id) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }
}
