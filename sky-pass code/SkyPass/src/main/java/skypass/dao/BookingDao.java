package skypass.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import skypass.db.Db;

/** BOOKING with its TICKETs (ternary relationship) and its PAYMENT (1:1). */
public final class BookingDao {

    public static final String[] STATUSES = {"PENDING", "CONFIRMED", "CANCELLED"};
    public static final String[] CABINS = {"ECONOMY", "PREMIUM_ECONOMY", "BUSINESS", "FIRST"};
    public static final String[] METHODS = {"CARD", "WALLET", "BANK_TRANSFER"};

    /** A row of V_BOOKING_SUMMARY. TotalPrice is derived from the tickets. */
    public record Booking(String ref, int customerId, String customer, LocalDate bookedOn, String status,
                          int tickets, BigDecimal total, String method, BigDecimal paid) {
    }

    public record Ticket(String traveler, String flightNo, String route, LocalDateTime departs,
                         String seat, String cabin, BigDecimal fare) {
    }

    /** A ticket line being assembled in the New booking form. */
    public record NewTicket(int travelerNo, String travelerName, int flightId, String flightLabel,
                            String seat, String cabin, BigDecimal fare) {
    }

    private BookingDao() {
    }

    /** BR12: fare = base fare x cabin multiplier, recorded when the ticket is issued. */
    public static BigDecimal fare(BigDecimal baseFare, String cabin) {
        BigDecimal multiplier = switch (cabin) {
            case "PREMIUM_ECONOMY" -> new BigDecimal("1.5");
            case "BUSINESS" -> new BigDecimal("2.5");
            case "FIRST" -> new BigDecimal("4.0");
            default -> BigDecimal.ONE;
        };
        return baseFare.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    public static List<Booking> list(String search) throws SQLException {
        String sql = "SELECT BookingRef, CustomerID, CustomerName, BookingDate, Status, TicketCount, "
                + "       TotalPrice, PaymentMethod, AmountPaid "
                + "FROM V_BOOKING_SUMMARY "
                + "WHERE UPPER(BookingRef || ' ' || CustomerName || ' ' || Email) LIKE ? "
                + "ORDER BY BookingDate DESC, BookingRef DESC";
        List<Booking> list = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, CustomerDao.like(search));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Booking(rs.getString(1), rs.getInt(2), rs.getString(3),
                            rs.getDate(4).toLocalDate(), rs.getString(5), rs.getInt(6),
                            rs.getBigDecimal(7), rs.getString(8), rs.getBigDecimal(9)));
                }
            }
        }
        return list;
    }

    /** Every ticket on one booking: traveler, flight and seat (a 4-table join). */
    public static List<Ticket> tickets(String ref) throws SQLException {
        String sql = "SELECT tr.FullName, f.FlightNo, f.OriginCode || ' → ' || f.DestCode, f.DepTime, "
                + "       t.SeatNo, t.CabinClass, t.Fare "
                + "FROM TICKET t "
                + "JOIN TRAVELER tr ON tr.CustomerID = t.CustomerID AND tr.TravelerNo = t.TravelerNo "
                + "JOIN FLIGHT f    ON f.FlightID = t.FlightID "
                + "WHERE t.BookingRef = ? "
                + "ORDER BY f.DepTime, tr.FullName";
        List<Ticket> list = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, ref);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Ticket(rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getTimestamp(4).toLocalDateTime(), rs.getString(5), rs.getString(6),
                            rs.getBigDecimal(7)));
                }
            }
        }
        return list;
    }

    /**
     * Creates the booking, its tickets and its payment in ONE transaction.
     * If any trigger or constraint rejects a ticket (seat taken, flight
     * full, ...) nothing is saved.
     *
     * @return the new booking reference, e.g. SP1000
     */
    public static String create(int customerId, List<NewTicket> tickets, String method) throws SQLException {
        return Db.tx(con -> {
            String ref;
            try (PreparedStatement ps = con.prepareStatement("SELECT 'SP' || BOOKING_SEQ.NEXTVAL FROM DUAL");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                ref = rs.getString(1);
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO BOOKING (BookingRef, CustomerID, Status) VALUES (?, ?, 'CONFIRMED')")) {
                ps.setString(1, ref);
                ps.setInt(2, customerId);
                ps.executeUpdate();
            }
            BigDecimal total = BigDecimal.ZERO;
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO TICKET (BookingRef, CustomerID, TravelerNo, FlightID, SeatNo, CabinClass, Fare) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                for (NewTicket t : tickets) {
                    ps.setString(1, ref);
                    ps.setInt(2, customerId);
                    ps.setInt(3, t.travelerNo());
                    ps.setInt(4, t.flightId());
                    ps.setString(5, t.seat());
                    ps.setString(6, t.cabin());
                    ps.setBigDecimal(7, t.fare());
                    ps.executeUpdate();
                    total = total.add(t.fare());
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO PAYMENT (PaymentID, BookingRef, Method, Amount) "
                    + "VALUES (PAYMENT_SEQ.NEXTVAL, ?, ?, ?)")) {
                ps.setString(1, ref);
                ps.setString(2, method);
                ps.setBigDecimal(3, total);
                ps.executeUpdate();
            }
            return ref;
        });
    }

    /** Changes the booking status (e.g. cancel it) and how it was paid. */
    public static void update(String ref, String status, String method) throws SQLException {
        Db.tx(con -> {
            try (PreparedStatement ps = con.prepareStatement("UPDATE BOOKING SET Status = ? WHERE BookingRef = ?")) {
                ps.setString(1, status);
                ps.setString(2, ref);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE PAYMENT SET Method = ? WHERE BookingRef = ?")) {
                ps.setString(1, method);
                ps.setString(2, ref);
                ps.executeUpdate();
            }
            return null;
        });
    }

    /** PAYMENT -> BOOKING is RESTRICT, so the payment goes first; tickets cascade with the booking. */
    public static void delete(String ref) throws SQLException {
        Db.tx(con -> {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM PAYMENT WHERE BookingRef = ?")) {
                ps.setString(1, ref);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM BOOKING WHERE BookingRef = ?")) {
                ps.setString(1, ref);
                ps.executeUpdate();
            }
            return null;
        });
    }
}
