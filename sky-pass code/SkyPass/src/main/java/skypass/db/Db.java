package skypass.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * The single place that knows how to reach Oracle.
 *
 * Your Oracle username and password are read from
 * src/main/resources/db.properties. That file is listed in .gitignore, so
 * it stays on your computer and is never pushed to GitHub. To set it up,
 * copy db.properties.example (same folder) to db.properties and fill in
 * your account (the same one you use in SQL Developer).
 */
public final class Db {

    public static final String DBURL = "jdbc:oracle:thin:@coeoracle.aus.edu:1521:orcl";
    public static final String DBUSER;
    public static final String DBPASS;

    static {
        Properties account = new Properties();
        try (InputStream in = Db.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                account.load(in);
            }
        } catch (IOException ignored) {
            // no readable file: the placeholders below make Oracle reject the login with a clear message
        }
        DBUSER = account.getProperty("db.user", "username").trim();
        DBPASS = account.getProperty("db.password", "password").trim();
    }

    private static Connection connection;

    private Db() {
    }

    /** Returns the shared connection, opening (or reopening) it when needed. */
    public static synchronized Connection get() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DBURL, DBUSER, DBPASS);
            connection.setAutoCommit(true);
        }
        return connection;
    }

    /** A unit of work that runs inside one transaction. */
    public interface Work<T> {
        T run(Connection c) throws SQLException;
    }

    /**
     * Runs several statements as one transaction: everything is committed
     * together, or everything is rolled back if any statement fails
     * (for example when a trigger rejects a ticket).
     */
    public static synchronized <T> T tx(Work<T> work) throws SQLException {
        Connection c = get();
        c.setAutoCommit(false);
        try {
            T result = work.run(c);
            c.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    public static synchronized void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // closing on exit; nothing useful to do
        }
        connection = null;
    }
}
