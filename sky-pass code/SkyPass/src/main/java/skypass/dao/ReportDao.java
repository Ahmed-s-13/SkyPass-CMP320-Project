package skypass.dao;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import skypass.db.Db;

/** Multi-table reports and the figures on the Overview page. */
public final class ReportDao {

    // ---------------- Overview ----------------

    public record Totals(int customers, int flights, int bookings, BigDecimal revenue) {
    }

    public record Departure(String flightNo, String airline, String fromCode, String fromCity,
                            String toCode, String toCity, LocalDateTime departs, LocalDateTime arrives,
                            String status, int sold, int capacity) {
    }

    private ReportDao() {
    }

    /** Revenue counts payments of bookings that are not cancelled. */
    public static Totals totals() throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM CUSTOMER), "
                + "       (SELECT COUNT(*) FROM FLIGHT), "
                + "       (SELECT COUNT(*) FROM BOOKING WHERE Status <> 'CANCELLED'), "
                + "       (SELECT NVL(SUM(p.Amount), 0) FROM PAYMENT p "
                + "          JOIN BOOKING b ON b.BookingRef = p.BookingRef WHERE b.Status <> 'CANCELLED') "
                + "FROM DUAL";
        try (PreparedStatement ps = Db.get().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new Totals(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getBigDecimal(4));
        }
    }

    /** The next departures from now, soonest first (FLIGHT x AIRLINE x AIRPORT x AIRPORT x AIRCRAFT). */
    public static List<Departure> nextDepartures(int limit) throws SQLException {
        String sql = "SELECT * FROM ("
                + "  SELECT f.FlightNo, a.Name AS Airline, f.OriginCode, o.City AS FromCity, f.DestCode, "
                + "         d.City AS ToCity, f.DepTime, f.ArrTime, f.Status, "
                + "         (SELECT COUNT(*) FROM TICKET t WHERE t.FlightID = f.FlightID) AS Sold, ac.Capacity "
                + "  FROM FLIGHT f "
                + "  JOIN AIRLINE a   ON a.AirlineID = f.AirlineID "
                + "  JOIN AIRPORT o   ON o.AirportCode = f.OriginCode "
                + "  JOIN AIRPORT d   ON d.AirportCode = f.DestCode "
                + "  JOIN AIRCRAFT ac ON ac.TailNo = f.TailNo "
                + "  WHERE f.DepTime > SYSDATE AND f.Status <> 'ARRIVED' "
                + "  ORDER BY f.DepTime"
                + ") WHERE ROWNUM <= ?";
        List<Departure> list = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Departure(rs.getString(1), rs.getString(2), rs.getString(3).trim(), rs.getString(4),
                            rs.getString(5).trim(), rs.getString(6), rs.getTimestamp(7).toLocalDateTime(),
                            rs.getTimestamp(8).toLocalDateTime(), rs.getString(9), rs.getInt(10), rs.getInt(11)));
                }
            }
        }
        return list;
    }

    // ---------------- Reports page ----------------

    public record BookingLine(String ref, String customer, LocalDate bookedOn, String status, int tickets,
                              BigDecimal total, String method, BigDecimal paid) {
    }

    public record AirlineLine(String code, String airline, int fleet, int flights, int ticketsSold,
                              BigDecimal revenue, BigDecimal avgRating, int reviews) {
    }

    public record LoadLine(String flightNo, String airline, String route, LocalDateTime departs,
                           String aircraft, int capacity, int sold, double loadPercent) {
    }

    /** Report 1: every booking with its derived total and payment (view V_BOOKING_SUMMARY). */
    public static List<BookingLine> bookingSummary() throws SQLException {
        String sql = "SELECT BookingRef, CustomerName, BookingDate, Status, TicketCount, TotalPrice, "
                + "PaymentMethod, AmountPaid FROM V_BOOKING_SUMMARY ORDER BY TotalPrice DESC";
        List<BookingLine> list = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new BookingLine(rs.getString(1), rs.getString(2), rs.getDate(3).toLocalDate(),
                        rs.getString(4), rs.getInt(5), rs.getBigDecimal(6), rs.getString(7), rs.getBigDecimal(8)));
            }
        }
        return list;
    }

    /** Report 2: airline performance with derived AvgRating (view V_AIRLINE_STATS). */
    public static List<AirlineLine> airlinePerformance() throws SQLException {
        String sql = "SELECT IATACode, Name, FleetSize, Flights, TicketsSold, Revenue, AvgRating, ReviewCount "
                + "FROM V_AIRLINE_STATS ORDER BY Revenue DESC, Name";
        List<AirlineLine> list = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new AirlineLine(rs.getString(1).trim(), rs.getString(2), rs.getInt(3), rs.getInt(4),
                        rs.getInt(5), rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getInt(8)));
            }
        }
        return list;
    }

    /** Report 3: seats sold against aircraft capacity for every flight (outer join keeps empty flights). */
    public static List<LoadLine> flightLoad() throws SQLException {
        String sql = "SELECT f.FlightNo, a.Name, f.OriginCode || ' → ' || f.DestCode, f.DepTime, ac.Model, "
                + "       ac.Capacity, COUNT(t.FlightID) AS Sold, "
                + "       ROUND(COUNT(t.FlightID) * 100 / ac.Capacity, 1) AS LoadPct "
                + "FROM FLIGHT f "
                + "JOIN AIRLINE a   ON a.AirlineID = f.AirlineID "
                + "JOIN AIRCRAFT ac ON ac.TailNo = f.TailNo "
                + "LEFT JOIN TICKET t ON t.FlightID = f.FlightID "
                + "GROUP BY f.FlightID, f.FlightNo, a.Name, f.OriginCode, f.DestCode, f.DepTime, ac.Model, ac.Capacity "
                + "ORDER BY LoadPct DESC, f.DepTime";
        List<LoadLine> list = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new LoadLine(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getTimestamp(4).toLocalDateTime(), rs.getString(5), rs.getInt(6), rs.getInt(7),
                        rs.getDouble(8)));
            }
        }
        return list;
    }
}
