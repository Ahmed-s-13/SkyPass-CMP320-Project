package skypass.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import skypass.db.Db;

/** FLIGHT and its amenities (multi-valued attribute). */
public final class FlightDao {

    public static final String[] STATUSES = {"SCHEDULED", "DELAYED", "DEPARTED", "ARRIVED", "CANCELLED"};
    public static final String[] AMENITIES = {"Wi-Fi", "Meals", "Power outlets", "Entertainment", "Extra legroom"};

    public record Flight(int id, String flightNo, int airlineId, String airline, String tailNo,
                         String origin, String destination, LocalDateTime departs, LocalDateTime arrives,
                         BigDecimal baseFare, String status, int sold, int capacity, List<String> amenities) {

        /** Derived attribute: ArrTime - DepTime. */
        public long durationMinutes() {
            return Duration.between(departs, arrives).toMinutes();
        }
    }

    private FlightDao() {
    }

    public static List<Flight> list(String search) throws SQLException {
        String sql = "SELECT f.FlightID, f.FlightNo, f.AirlineID, a.Name, f.TailNo, f.OriginCode, f.DestCode, "
                + "       f.DepTime, f.ArrTime, f.BaseFare, f.Status, "
                + "       (SELECT COUNT(*) FROM TICKET t WHERE t.FlightID = f.FlightID) AS Sold, "
                + "       ac.Capacity, "
                + "       (SELECT LISTAGG(fa.Amenity, ',') WITHIN GROUP (ORDER BY fa.Amenity) "
                + "          FROM FLIGHT_AMENITY fa WHERE fa.FlightID = f.FlightID) AS Amenities "
                + "FROM FLIGHT f "
                + "JOIN AIRLINE a   ON a.AirlineID = f.AirlineID "
                + "JOIN AIRCRAFT ac ON ac.TailNo = f.TailNo "
                + "WHERE UPPER(f.FlightNo || ' ' || f.OriginCode || ' ' || f.DestCode || ' ' || a.Name) LIKE ? "
                + "ORDER BY f.DepTime DESC";
        List<Flight> list = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, CustomerDao.like(search));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String amenities = rs.getString(14);
                    list.add(new Flight(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4),
                            rs.getString(5), rs.getString(6).trim(), rs.getString(7).trim(),
                            rs.getTimestamp(8).toLocalDateTime(), rs.getTimestamp(9).toLocalDateTime(),
                            rs.getBigDecimal(10), rs.getString(11), rs.getInt(12), rs.getInt(13),
                            amenities == null ? List.of() : Arrays.asList(amenities.split(","))));
                }
            }
        }
        return list;
    }

    public static int insert(Flight f) throws SQLException {
        return Db.tx(con -> {
            int id;
            try (PreparedStatement ps = con.prepareStatement("SELECT FLIGHT_SEQ.NEXTVAL FROM DUAL");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                id = rs.getInt(1);
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO FLIGHT (FlightID, FlightNo, AirlineID, TailNo, OriginCode, DestCode, "
                    + "DepTime, ArrTime, BaseFare, Status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                ps.setInt(1, id);
                bind(ps, f, 2);
                ps.executeUpdate();
            }
            insertAmenities(con, id, f.amenities());
            return id;
        });
    }

    public static void update(Flight f) throws SQLException {
        Db.tx(con -> {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE FLIGHT SET FlightNo = ?, AirlineID = ?, TailNo = ?, OriginCode = ?, DestCode = ?, "
                    + "DepTime = ?, ArrTime = ?, BaseFare = ?, Status = ? WHERE FlightID = ?")) {
                bind(ps, f, 1);
                ps.setInt(10, f.id());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM FLIGHT_AMENITY WHERE FlightID = ?")) {
                ps.setInt(1, f.id());
                ps.executeUpdate();
            }
            insertAmenities(con, f.id(), f.amenities());
            return null;
        });
    }

    /** Amenities cascade; a flight with tickets is protected by FK_TICKET_FLIGHT. */
    public static void delete(int flightId) throws SQLException {
        try (PreparedStatement ps = Db.get().prepareStatement("DELETE FROM FLIGHT WHERE FlightID = ?")) {
            ps.setInt(1, flightId);
            ps.executeUpdate();
        }
    }

    private static void bind(PreparedStatement ps, Flight f, int from) throws SQLException {
        ps.setString(from, f.flightNo());
        ps.setInt(from + 1, f.airlineId());
        ps.setString(from + 2, f.tailNo());
        ps.setString(from + 3, f.origin());
        ps.setString(from + 4, f.destination());
        ps.setTimestamp(from + 5, Timestamp.valueOf(f.departs()));
        ps.setTimestamp(from + 6, Timestamp.valueOf(f.arrives()));
        ps.setBigDecimal(from + 7, f.baseFare());
        ps.setString(from + 8, f.status());
    }

    private static void insertAmenities(Connection con, int flightId, List<String> amenities) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO FLIGHT_AMENITY (FlightID, Amenity) VALUES (?, ?)")) {
            for (String a : amenities) {
                ps.setInt(1, flightId);
                ps.setString(2, a);
                ps.executeUpdate();
            }
        }
    }
}
