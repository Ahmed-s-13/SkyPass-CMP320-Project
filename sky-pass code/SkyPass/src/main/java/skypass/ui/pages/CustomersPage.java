package skypass.ui.pages;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import skypass.dao.CustomerDao;
import skypass.dao.CustomerDao.Customer;
import skypass.dao.CustomerDao.Traveler;
import skypass.ui.Async;
import skypass.ui.DataTable;
import skypass.ui.FormCard;
import skypass.ui.Page;
import skypass.ui.Theme;
import skypass.ui.Toast;
import skypass.ui.Ui;
import skypass.ui.Validation;
import skypass.ui.anim.PlaneSpinner;

/**
 * Customers: CUSTOMER with its phones (multi-valued) and the travelers
 * saved on the account (weak entity TRAVELER).
 */
public class CustomersPage extends JPanel implements Page {

    private static final String[] RELATIONSHIPS = {"Spouse", "Child", "Parent", "Sibling", "Friend", "Other"};

    private final JLabel summary = Ui.muted("Loading…");
    private final PlaneSpinner spinner = new PlaneSpinner();
    private final JTextField search = Ui.search("Search name, email or city", this::reload);
    private final DataTable<Customer> table = new DataTable<>(
            new String[]{"ID", "Name", "Email", "City", "Bookings"},
            c -> new Object[]{c.id(), c.name(), c.email(), c.city(), c.bookings()});
    private final DataTable<Traveler> travelers = new DataTable<>(
            new String[]{"Name", "Relationship", "Born"},
            t -> new Object[]{t.fullName(), t.relationship(), DataTable.Sorted.of(Ui.date(t.dob()), t.dob())});

    private final FormCard form = new FormCard("New customer");
    private final JTextField first = Ui.field("Omar");
    private final JTextField last = Ui.field("Haddad");
    private final JTextField email = Ui.field("name@example.com");
    private final JTextField dob = Ui.field("e.g. 27/04/2001");
    private final JTextField passport = Ui.field("N4821937");
    private final JTextField street = Ui.field("Al Wasl Road, Villa 12");
    private final JTextField city = Ui.field("Dubai");
    private final JTextField country = Ui.field("UAE");
    private final JTextField phones = Ui.field("+971501234567, +97143456789");
    private final JPanel companions = new JPanel(new BorderLayout(0, Theme.S2));
    private final JButton save = Ui.primary("Register customer", e -> save());
    private final JButton delete = Ui.danger("Delete", e -> delete());

    private Customer editing;

    public CustomersPage() {
        super(new BorderLayout());
        setOpaque(false);
        JPanel page = Ui.page();
        page.setOpaque(false);
        page.add(Ui.pageHeader("Customers", summary, spinner, search,
                Ui.secondary("New customer", e -> startNew())), BorderLayout.NORTH);

        table.widths(44, 150, 200, 90, 70);
        table.setEmptyText("No customers match your search.");
        table.onSelect(this::edit);

        buildForm();
        page.add(Ui.workspace(table, form, 380), BorderLayout.CENTER);
        add(page);
        startNew();
    }

    private void buildForm() {
        form.pair("First name", first, "Last name", last);
        form.field("Email", email);
        form.pair("Date of birth", dob, "Passport number", passport);
        form.field("Street", street);
        form.pair("City", city, "Country", country);
        form.field("Phone numbers (comma-separated)", phones);

        // Travelers saved on this account (weak entity)
        travelers.setEmptyText("Save the customer first.");
        travelers.setPreferredSize(new Dimension(300, 150));
        travelers.setMinimumSize(new Dimension(100, 150));
        travelers.widths(130, 92, 100);
        travelers.minWidth(2, 98);
        JPanel actions = Ui.row(Theme.S2,
                Ui.secondary("Add companion", e -> addCompanion()),
                Ui.danger("Remove", e -> removeCompanion()));
        companions.setOpaque(false);
        companions.add(Ui.label("Travelers on this account", Theme.strong(), Theme.INK), BorderLayout.NORTH);
        companions.add(travelers, BorderLayout.CENTER);
        companions.add(actions, BorderLayout.SOUTH);
        companions.setBorder(javax.swing.BorderFactory.createEmptyBorder(Theme.S3, 0, 0, 0));
        form.extra(companions);
        form.end();
        form.buttons(save, delete, Ui.secondary("Clear", e -> startNew()));
    }

    @Override
    public void onShow() {
        reload();
    }

    private void reload() {
        spinner.start();
        Async.run(this, () -> CustomerDao.list(search.getText()), list -> {
            table.setItems(list);
            summary.setText(list.size() + (list.size() == 1 ? " customer" : " customers")
                    + (search.getText().isBlank() ? "" : " match “" + search.getText().trim() + "”"));
        }, spinner::stop);
    }

    // ---------------- Modes ----------------

    private void startNew() {
        editing = null;
        table.clearSelection();
        form.clearErrors();
        form.setHeading("New customer", "Also saved as their own traveler.");
        for (JTextField f : new JTextField[]{first, last, email, dob, passport, street, city, phones}) {
            f.setText("");
        }
        country.setText("UAE");
        save.setText("Register customer");
        delete.setVisible(false);
        companions.setVisible(false);
        travelers.setItems(List.of());
    }

    private void edit(Customer c) {
        if (c == null) {
            return;
        }
        editing = c;
        form.clearErrors();
        form.setHeading(c.name(), "Customer #" + c.id() + "  ·  age " + c.age() + "  ·  "
                + c.bookings() + (c.bookings() == 1 ? " booking" : " bookings"));
        first.setText(c.firstName());
        last.setText(c.lastName());
        email.setText(c.email());
        dob.setText(Ui.INPUT_DATE.format(c.dob()));
        passport.setText(c.passport());
        street.setText(c.street());
        city.setText(c.city());
        country.setText(c.country());
        phones.setText(c.phones());
        save.setText("Save changes");
        delete.setVisible(true);
        companions.setVisible(true);
        loadTravelers(c.id());
    }

    private void loadTravelers(int customerId) {
        Async.run(this, () -> CustomerDao.travelers(customerId), travelers::setItems);
    }

    // ---------------- Actions ----------------

    private void save() {
        form.clearErrors();
        String fn = first.getText().trim(), ln = last.getText().trim(), em = email.getText().trim();
        String pp = passport.getText().trim().toUpperCase();
        LocalDate birth = Validation.date(dob.getText());
        List<String> phoneList = Validation.phones(phones.getText());

        if (fn.isEmpty()) form.error(first, "Enter a first name.");
        else if (fn.length() > 40) form.error(first, "Keep it under 40 characters.");
        if (ln.isEmpty()) form.error(last, "Enter a last name.");
        else if (ln.length() > 40) form.error(last, "Keep it under 40 characters.");
        if (!Validation.EMAIL.matcher(em).matches()) form.error(email, "Enter an email like name@example.com.");
        if (birth == null) form.error(dob, "Like 27/04/2001.");
        else if (birth.isAfter(LocalDate.now())) form.error(dob, "Can't be in the future.");
        else if (birth.isBefore(LocalDate.of(1900, 1, 1))) form.error(dob, "Check the year.");
        if (!Validation.PASSPORT.matcher(pp).matches()) form.error(passport, "6–15 letters or digits.");
        if (street.getText().isBlank()) form.error(street, "Enter a street address.");
        if (city.getText().isBlank()) form.error(city, "Enter a city.");
        if (country.getText().isBlank()) form.error(country, "Enter a country.");
        if (phoneList == null) form.error(phones, "Phone numbers use digits, spaces and an optional +.");
        else if (phoneList.isEmpty()) form.error(phones, "Add at least one phone number.");
        if (!form.ok()) {
            return;
        }

        Customer c = new Customer(editing == null ? 0 : editing.id(), fn, ln, em, birth,
                street.getText().trim(), city.getText().trim(), country.getText().trim(),
                "", 0, 0, pp);
        boolean isNew = editing == null;
        Async.run(this, () -> {
            if (isNew) {
                return CustomerDao.insert(c, phoneList);
            }
            CustomerDao.update(c, phoneList);
            return c.id();
        }, id -> {
            Toast.success(this, isNew ? c.name() + " is registered as customer #" + id + "."
                                      : "Saved changes to " + c.name() + ".");
            reloadAndSelect(id);
        });
    }

    private void reloadAndSelect(int id) {
        Async.run(this, () -> CustomerDao.list(search.getText()), list -> {
            table.setItems(list);
            summary.setText(list.size() + " customers");
            list.stream().filter(x -> x.id() == id).findFirst().ifPresent(table::select);
        });
    }

    private void delete() {
        if (editing == null) {
            return;
        }
        Customer c = editing;
        if (c.bookings() > 0) {
            // Oracle would refuse too (FK_BOOKING_CUSTOMER is RESTRICT); say why up front.
            Toast.info(this, c.name() + " has " + c.bookings() + (c.bookings() == 1 ? " booking" : " bookings")
                    + ", so they can't be deleted. Delete the bookings first.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete " + c.name() + "? Their phone numbers and saved travelers are deleted too.",
                "Delete customer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            return;
        }
        Async.run(this, () -> {
            CustomerDao.delete(c.id());
            return null;
        }, x -> {
            Toast.success(this, c.name() + " was deleted.");
            startNew();
            reload();
        });
    }

    private void addCompanion() {
        if (editing == null) {
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        new CompanionDialog(owner, editing).setVisible(true);
    }

    private void removeCompanion() {
        Traveler t = travelers.selected();
        if (t == null) {
            Toast.info(this, "Select a traveler in the list first.");
            return;
        }
        if ("Self".equals(t.relationship())) {
            Toast.info(this, "The customer's own traveler profile can't be removed.");
            return;
        }
        Async.run(this, () -> {
            CustomerDao.deleteTraveler(t.customerId(), t.no());
            return null;
        }, x -> {
            Toast.success(this, t.fullName() + " was removed.");
            loadTravelers(t.customerId());
        });
    }

    /** Small form for saving a travel companion on the customer's account. */
    private final class CompanionDialog extends JDialog {
        CompanionDialog(Window owner, Customer customer) {
            super(owner, "Add companion", ModalityType.APPLICATION_MODAL);
            FormCard f = new FormCard("Add a companion");
            f.setHeading("Add a companion", "Saved to " + customer.name() + "'s account.");
            JTextField name = Ui.field("Full name");
            JTextField born = Ui.field("e.g. 01/12/2015");
            JComboBox<String> rel = Ui.combo();
            for (String r : RELATIONSHIPS) {
                rel.addItem(r);
            }
            JTextField pass = Ui.field("Passport number");
            f.field("Full name", name);
            f.pair("Date of birth", born, "Relationship", rel);
            f.field("Passport number", pass);
            f.end();
            JButton add = Ui.primary("Add companion", e -> {
                f.clearErrors();
                LocalDate d = Validation.date(born.getText());
                String p = pass.getText().trim().toUpperCase();
                if (name.getText().isBlank()) f.error(name, "Enter their full name.");
                else if (name.getText().trim().length() > 80) f.error(name, "Keep it under 80 characters.");
                if (d == null) f.error(born, "Like 01/12/2015.");
                else if (d.isAfter(LocalDate.now())) f.error(born, "Can't be in the future.");
                if (!Validation.PASSPORT.matcher(p).matches()) f.error(pass, "6–15 letters or digits.");
                String relationship = (String) rel.getSelectedItem();
                if ("Spouse".equals(relationship)
                        && travelers.items().stream().anyMatch(t -> "Spouse".equals(t.relationship()))) {
                    f.error(rel, "Already has a spouse.");
                }
                if (!f.ok()) {
                    return;
                }
                Async.run(CustomersPage.this, () -> {
                    CustomerDao.addTraveler(customer.id(), name.getText().trim(), d, relationship, p);
                    return null;
                }, x -> {
                    Toast.success(CustomersPage.this, name.getText().trim() + " was added.");
                    loadTravelers(customer.id());
                    dispose();
                });
            });
            f.buttons(add, Ui.secondary("Cancel", e -> dispose()));
            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(Theme.SHEET);
            root.setBorder(javax.swing.BorderFactory.createEmptyBorder(Theme.S3, Theme.S3, Theme.S3, Theme.S3));
            root.add(f);
            setContentPane(root);
            getRootPane().setDefaultButton(add);
            setSize(440, 400);
            setLocationRelativeTo(owner);
        }
    }
}
