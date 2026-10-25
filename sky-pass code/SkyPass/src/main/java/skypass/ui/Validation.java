package skypass.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Input checks done in Java before anything is sent to Oracle. */
public final class Validation {

    public static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    public static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ]{7,19}$");
    public static final Pattern PASSPORT = Pattern.compile("^[A-Z0-9]{6,15}$");
    public static final Pattern FLIGHT_NO = Pattern.compile("^[A-Z0-9]{2}[0-9]{1,4}[A-Z]?$");
    public static final Pattern SEAT = Pattern.compile("^[1-9][0-9]?[A-K]$");

    private Validation() {
    }

    /** Parses yyyy-MM-dd, or returns null if the text is not a real date. */
    public static LocalDate date(String text) {
        try {
            return LocalDate.parse(text.trim(), Ui.INPUT_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Parses yyyy-MM-dd HH:mm, or returns null. */
    public static LocalDateTime dateTime(String text) {
        try {
            return LocalDateTime.parse(text.trim().replaceAll("\\s+", " "), Ui.INPUT_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Parses a positive amount with at most 2 decimals, or returns null. */
    public static BigDecimal money(String text) {
        try {
            BigDecimal value = new BigDecimal(text.trim().replace(",", ""));
            if (value.signum() <= 0 || value.scale() > 2) {
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Splits "050 123, +97143..." into clean phone numbers; returns null if any is invalid. */
    public static List<String> phones(String text) {
        List<String> result = new ArrayList<>();
        for (String part : text.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            if (!PHONE.matcher(p).matches()) {
                return null;
            }
            String clean = p.replace(" ", "");
            if (!result.contains(clean)) {
                result.add(clean);
            }
        }
        return result;
    }

    public static boolean blank(String text) {
        return text == null || text.trim().isEmpty();
    }
}
