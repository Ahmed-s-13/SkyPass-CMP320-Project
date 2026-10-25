package skypass.dao;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import skypass.db.Db;
import skypass.ui.Ui;

/** Data for the dropdowns used by the forms. */
public final class LookupDao {

    private LookupDao() {
    }

    public static List<Item> airlines() throws SQLException {
        return items("SELECT AirlineID, Name || ' (' || IATACode || ')' FROM AIRLINE ORDER BY Name");
    }

    public static List<Item> airports() throws SQLException {
        return items("SELECT AirportCode, AirportCode || '  ·  ' || City FROM AIRPORT ORDER BY AirportCode");
    }

    /** Only aircraft owned by the given airline, so a flight can never get someone else's plane. */
    public static List<Item> aircraft(int airlineId) throws SQLException {
        String sql = "SELECT TailNo, TailNo || '  ·  ' || Model || ', ' || Capacity || ' seats' "
                + "FROM AIRCRAFT WHERE AirlineID = ? ORDER BY TailNo";
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setInt(1, airlineId);
            return read(ps);
        }
    }

    public static List<Item> customers() throws SQLException {
        return items("SELECT CustomerID, FirstName || ' ' || LastName || '  ·  #' || CustomerID "
                + "FROM CUSTOMER ORDER BY FirstName, LastName");
    }

    /** The travelers saved on one customer's account (key = TravelerNo). */
    public static List<Item> travelers(int customerId) throws SQLException {
        String sql = "SELECT TravelerNo, FullName || ' (' || Relationship || ')' "
                + "FROM TRAVELER WHERE CustomerID = ? ORDER BY TravelerNo";
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setInt(1, customerId);
            return read(ps);
        }
    }

    /** A flight that can still be booked, with its base fare for pricing tickets. */
    public record BookableFlight(int id, String label, BigDecimal baseFare) {
        @Override
        public String toString() {
            return label;
        }
    }

    public static List<BookableFlight> bookableFlights() throws SQLException {
        String sql = "SELECT FlightID, FlightNo, OriginCode, DestCode, DepTime, BaseFare FROM FLIGHT "
                + "WHERE DepTime > SYSDATE AND Status IN ('SCHEDULED', 'DELAYED') ORDER BY DepTime";
        List<BookableFlight> list = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String label = rs.getString(2) + "  ·  " + rs.getString(3).trim() + " → " + rs.getString(4).trim()
                        + "  ·  " + Ui.SHORT.format(rs.getTimestamp(5).toLocalDateTime());
                list.add(new BookableFlight(rs.getInt(1), label, rs.getBigDecimal(6)));
            }
        }
        return list;
    }

    private static List<Item> items(String sql) throws SQLException {
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            return read(ps);
        }
    }

    private static List<Item> read(PreparedStatement ps) throws SQLException {
        List<Item> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object key = rs.getObject(1);
                if (key instanceof String s) {
                    key = s.trim();          // CHAR columns come back padded
                } else if (key instanceof Number n) {
                    key = n.intValue();
                }
                list.add(new Item(key, rs.getString(2)));
            }
        }
        return list;
    }
}
