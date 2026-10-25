package skypass.db;

import java.sql.SQLException;
import java.util.Map;

/** Turns Oracle error codes into sentences a user can act on. */
public final class OracleErrors {

    // ORA-00001: unique constraint violated
    private static final Map<String, String> UNIQUE = Map.of(
            "UQ_CUSTOMER_EMAIL", "That email is already registered to another customer.",
            "PK_CUSTOMER_PHONE", "The same phone number is listed twice.",
            "UQ_TRAVELER_PASSPORT", "This customer already has a traveler with that passport number.",
            "UQ_FLIGHT_NO_DEP", "A flight with this number already departs at that time.",
            "UQ_TICKET_SEAT", "That seat is already taken on this flight. Pick another seat.",
            "UQ_TICKET_TRAVELER", "That traveler already has a ticket on this flight.",
            "PK_TICKET", "That traveler is already on this flight in this booking.",
            "PK_REVIEW", "This customer has already reviewed that airline. Edit the existing review instead.");

    // ORA-02292: child record found (delete blocked)
    private static final Map<String, String> CHILD = Map.of(
            "FK_BOOKING_CUSTOMER", "This customer has bookings, so they can't be deleted. Delete their bookings first.",
            "FK_TICKET_FLIGHT", "This flight has tickets booked on it, so it can't be deleted. Change its status to Cancelled instead.",
            "FK_TICKET_TRAVELER", "This person is on a booked ticket, so they can't be removed. Delete their bookings first.",
            "FK_PAYMENT_BOOKING", "This booking still has a payment record.",
            "FK_REVIEW_AIRLINE", "This airline has reviews, so it can't be deleted.");

    // ORA-02290: check constraint violated
    private static final Map<String, String> CHECK = Map.of(
            "CK_CUSTOMER_EMAIL", "Enter a valid email address, like name@example.com.",
            "CK_TRAVELER_PASSPORT", "Passport numbers are 6 to 15 capital letters or digits.",
            "CK_FLIGHT_ROUTE", "Origin and destination must be different airports.",
            "CK_FLIGHT_TIMES", "Arrival must be after departure.",
            "CK_FLIGHT_FARE", "The base fare must be more than zero.",
            "CK_TICKET_SEAT", "Seats look like 12A: a row number and a letter from A to K.",
            "CK_REVIEW_RATING", "Ratings go from 1 to 5.");

    private OracleErrors() {
    }

    public static String friendly(Throwable error) {
        if (!(error instanceof SQLException e)) {
            String msg = error.getMessage();
            return msg == null ? "Something went wrong: " + error.getClass().getSimpleName() : msg;
        }
        String text = e.getMessage() == null ? "" : e.getMessage();
        int code = e.getErrorCode();

        // Our own trigger messages (RAISE_APPLICATION_ERROR -20001..-20999)
        if (code >= 20000 && code <= 20999) {
            return firstLine(text.replaceFirst("^ORA-2\\d{4}:\\s*", ""));
        }
        switch (code) {
            case 1:
                return lookup(UNIQUE, text, "That record already exists.");
            case 2292:
                return lookup(CHILD, text, "This record is still used elsewhere, so it can't be deleted.");
            case 2291:
                if (text.toUpperCase().contains("FK_FLIGHT_AIRCRAFT")) {
                    return "That aircraft doesn't belong to the chosen airline.";
                }
                return "A linked record no longer exists. Refresh the page and try again.";
            case 2290:
                return lookup(CHECK, text, "One of the values is outside what the database allows.");
            case 1400:
                return "A required field is empty.";
            case 12899:
                return "One of the values is too long for its field.";
            case 1017:
                return "Oracle rejected the username or password. Check db.user and db.password in src/main/resources/db.properties.";
            case 942:
                return "A SkyPass table is missing. Run the SQL scripts 01 to 06 in SQL Developer first.";
            case 17002:
            case 17800:
                return "Can't reach the Oracle server. Make sure you're on the AUS network or VPN.";
            default:
                break;
        }
        if (text.contains("IO Error") || text.contains("Network Adapter")) {
            return "Can't reach the Oracle server. Make sure you're on the AUS network or VPN.";
        }
        return firstLine(text);
    }

    private static String lookup(Map<String, String> messages, String text, String fallback) {
        String upper = text.toUpperCase();
        for (Map.Entry<String, String> entry : messages.entrySet()) {
            if (upper.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return fallback;
    }

    private static String firstLine(String text) {
        int newline = text.indexOf('\n');
        return (newline >= 0 ? text.substring(0, newline) : text).trim();
    }
}
