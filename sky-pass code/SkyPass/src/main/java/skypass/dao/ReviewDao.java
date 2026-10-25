package skypass.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import skypass.db.Db;

/** REVIEW: the M:N relationship between CUSTOMER and AIRLINE. */
public final class ReviewDao {

    public record Review(int customerId, String customer, int airlineId, String airline,
                         int rating, String comment, LocalDate date) {
    }

    /** Derived Airline.AvgRating, read from V_AIRLINE_STATS. */
    public record AirlineRating(double average, int count) {
    }

    private ReviewDao() {
    }

    public static List<Review> list(String search) throws SQLException {
        String sql = "SELECT r.CustomerID, c.FirstName || ' ' || c.LastName, r.AirlineID, a.Name, "
                + "       r.Rating, r.ReviewComment, r.ReviewDate "
                + "FROM REVIEW r "
                + "JOIN CUSTOMER c ON c.CustomerID = r.CustomerID "
                + "JOIN AIRLINE a  ON a.AirlineID = r.AirlineID "
                + "WHERE UPPER(c.FirstName || ' ' || c.LastName || ' ' || a.Name || ' ' || r.ReviewComment) LIKE ? "
                + "ORDER BY r.ReviewDate DESC";
        List<Review> list = new ArrayList<>();
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, CustomerDao.like(search));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Review(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getString(4),
                            rs.getInt(5), rs.getString(6), rs.getDate(7).toLocalDate()));
                }
            }
        }
        return list;
    }

    /** TRG_REVIEW_FLOWN rejects reviews of airlines the customer never flew. */
    public static void insert(Review r) throws SQLException {
        String sql = "INSERT INTO REVIEW (CustomerID, AirlineID, Rating, ReviewComment, ReviewDate) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setInt(1, r.customerId());
            ps.setInt(2, r.airlineId());
            ps.setInt(3, r.rating());
            setComment(ps, 4, r.comment());
            ps.setDate(5, Date.valueOf(r.date()));
            ps.executeUpdate();
        }
    }

    public static void update(Review r) throws SQLException {
        String sql = "UPDATE REVIEW SET Rating = ?, ReviewComment = ?, ReviewDate = ? "
                + "WHERE CustomerID = ? AND AirlineID = ?";
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setInt(1, r.rating());
            setComment(ps, 2, r.comment());
            ps.setDate(3, Date.valueOf(r.date()));
            ps.setInt(4, r.customerId());
            ps.setInt(5, r.airlineId());
            ps.executeUpdate();
        }
    }

    public static void delete(int customerId, int airlineId) throws SQLException {
        try (PreparedStatement ps = Db.get().prepareStatement(
                "DELETE FROM REVIEW WHERE CustomerID = ? AND AirlineID = ?")) {
            ps.setInt(1, customerId);
            ps.setInt(2, airlineId);
            ps.executeUpdate();
        }
    }

    public static AirlineRating rating(int airlineId) throws SQLException {
        try (PreparedStatement ps = Db.get().prepareStatement(
                "SELECT AvgRating, ReviewCount FROM V_AIRLINE_STATS WHERE AirlineID = ?")) {
            ps.setInt(1, airlineId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AirlineRating(rs.getDouble(1), rs.getInt(2));
                }
            }
        }
        return new AirlineRating(0, 0);
    }

    private static void setComment(PreparedStatement ps, int index, String comment) throws SQLException {
        if (comment == null || comment.isBlank()) {
            ps.setNull(index, Types.VARCHAR);   // the comment is optional
        } else {
            ps.setString(index, comment.trim());
        }
    }
}
