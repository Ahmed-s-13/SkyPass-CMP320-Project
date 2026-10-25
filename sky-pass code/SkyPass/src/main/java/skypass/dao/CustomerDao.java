package skypass.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import skypass.db.Db;

/** CUSTOMER, its phones (multi-valued) and its travelers (weak entity). */
public final class CustomerDao {

    public record Customer(int id, String firstName, String lastName, String email, LocalDate dob,
                           String street, String city, String country, String phones,
                           int age, int bookings, String passport) {
        public String name() {
            return firstName + " " + lastName;
        }
    }

    public record Traveler(int customerId, int no, String fullName, LocalDate dob,
                           String relationship, String passport) {
    }

    private CustomerDao() {
    }

    /** Customers whose name, email or city contains the search text. Age is derived. */
    public static List<Customer> list(String search) throws SQLException {
        String sql = "SELECT c.CustomerID, c.FirstName, c.LastName, c.Email, c.DateOfBirth, "
                + "       c.Street, c.City, c.Country, "
                + "       (SELECT LISTAGG(p.Phone, ', ') WITHIN GROUP (ORDER BY p.Phone) "
                + "          FROM CUSTOMER_PHONE p WHERE p.CustomerID = c.CustomerID) AS Phones, "
                + "       TRUNC(MONTHS_BETWEEN(SYSDATE, c.DateOfBirth) / 12) AS Age, "
                + "       (SELECT COUNT(*) FROM BOOKING b WHERE b.CustomerID = c.CustomerID) AS Bookings, "
                + "       (SELECT t.PassportNo FROM TRAVELER t "
                + "         WHERE t.CustomerID = c.CustomerID AND t.Relationship = 'Self' AND ROWNUM = 1) AS Passport "
                + "FROM CUSTOMER c "
                + "WHERE UPPER(c.FirstName || ' ' || c.LastName || ' ' || c.Email || ' ' || c.City) LIKE ? "
                + "ORDER BY c.CustomerID";
        List<Customer> list = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, like(search));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Customer(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getDate(5).toLocalDate(), rs.getString(6), rs.getString(7), rs.getString(8),
                            rs.getString(9) == null ? "" : rs.getString(9), rs.getInt(10), rs.getInt(11),
                            rs.getString(12) == null ? "" : rs.getString(12)));
                }
            }
        }
        return list;
    }

    /**
     * Registers a customer: the CUSTOMER row, each phone, and the customer
     * as their own 'Self' traveler (BR2), all in one transaction.
     */
    public static int insert(Customer c, List<String> phones) throws SQLException {
        return Db.tx(con -> {
            int id;
            try (PreparedStatement ps = con.prepareStatement("SELECT CUSTOMER_SEQ.NEXTVAL FROM DUAL");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getInt(1);
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO CUSTOMER (CustomerID, FirstName, LastName, Email, DateOfBirth, Street, City, Country) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setInt(1, id);
                bindDetails(ps, c, 2);
                ps.executeUpdate();
            }
            insertPhones(con, id, phones);
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO TRAVELER (CustomerID, TravelerNo, FullName, DateOfBirth, Relationship, PassportNo) "
                    + "VALUES (?, 1, ?, ?, 'Self', ?)")) {
                ps.setInt(1, id);
                ps.setString(2, c.name());
                ps.setDate(3, Date.valueOf(c.dob()));
                ps.setString(4, c.passport());
                ps.executeUpdate();
            }
            return id;
        });
    }

    /** Updates the customer, replaces their phones and keeps the Self traveler in sync. */
    public static void update(Customer c, List<String> phones) throws SQLException {
        Db.tx(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE CUSTOMER SET FirstName = ?, LastName = ?, Email = ?, DateOfBirth = ?, "
                    + "Street = ?, City = ?, Country = ? WHERE CustomerID = ?")) {
                bindDetails(ps, c, 1);
                ps.setInt(8, c.id());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM CUSTOMER_PHONE WHERE CustomerID = ?")) {
                ps.setInt(1, c.id());
                ps.executeUpdate();
            }
            insertPhones(con, c.id(), phones);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE TRAVELER SET FullName = ?, DateOfBirth = ?, PassportNo = ? "
                    + "WHERE CustomerID = ? AND Relationship = 'Self'")) {
                ps.setString(1, c.name());
                ps.setDate(2, Date.valueOf(c.dob()));
                ps.setString(3, c.passport());
                ps.setInt(4, c.id());
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** Phones and travelers go with the customer (ON DELETE CASCADE); bookings block it (RESTRICT). */
    public static void delete(int customerId) throws SQLException {
        try (PreparedStatement ps = Db.get().prepareStatement("DELETE FROM CUSTOMER WHERE CustomerID = ?")) {
            ps.setInt(1, customerId);
            ps.executeUpdate();
        }
    }

    // ---------------- Travelers (weak entity) ----------------

    public static List<Traveler> travelers(int customerId) throws SQLException {
        String sql = "SELECT CustomerID, TravelerNo, FullName, DateOfBirth, Relationship, PassportNo "
                + "FROM TRAVELER WHERE CustomerID = ? ORDER BY TravelerNo";
        List<Traveler> list = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Traveler(rs.getInt(1), rs.getInt(2), rs.getString(3),
                            rs.getDate(4).toLocalDate(), rs.getString(5), rs.getString(6)));
                }
            }
        }
        return list;
    }

    /** TravelerNo is the partial key: the next number within this customer only. */
    public static void addTraveler(int customerId, String name, LocalDate dob, String relationship,
                                   String passport) throws SQLException {
        String sql = "INSERT INTO TRAVELER (CustomerID, TravelerNo, FullName, DateOfBirth, Relationship, PassportNo) "
                + "SELECT ?, NVL(MAX(TravelerNo), 0) + 1, ?, ?, ?, ? FROM TRAVELER WHERE CustomerID = ?";
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.setString(2, name);
            ps.setDate(3, Date.valueOf(dob));
            ps.setString(4, relationship);
            ps.setString(5, passport);
            ps.setInt(6, customerId);
            ps.executeUpdate();
        }
    }

    public static void deleteTraveler(int customerId, int travelerNo) throws SQLException {
        try (PreparedStatement ps = Db.get().prepareStatement(
                "DELETE FROM TRAVELER WHERE CustomerID = ? AND TravelerNo = ? AND Relationship <> 'Self'")) {
            ps.setInt(1, customerId);
            ps.setInt(2, travelerNo);
            ps.executeUpdate();
        }
    }

    // ---------------- Helpers ----------------

    private static void bindDetails(PreparedStatement ps, Customer c, int from) throws SQLException {
        ps.setString(from, c.firstName());
        ps.setString(from + 1, c.lastName());
        ps.setString(from + 2, c.email());
        ps.setDate(from + 3, Date.valueOf(c.dob()));
        ps.setString(from + 4, c.street());
        ps.setString(from + 5, c.city());
        ps.setString(from + 6, c.country());
    }

    private static void insertPhones(Connection con, int customerId, List<String> phones) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO CUSTOMER_PHONE (CustomerID, Phone) VALUES (?, ?)")) {
            for (String phone : phones) {
                ps.setInt(1, customerId);
                ps.setString(2, phone);
                ps.executeUpdate();
            }
        }
    }

    static String like(String search) {
        return "%" + (search == null ? "" : search.trim().toUpperCase()) + "%";
    }
}
