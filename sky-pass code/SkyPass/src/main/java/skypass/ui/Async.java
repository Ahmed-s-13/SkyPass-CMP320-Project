package skypass.ui;

import java.awt.Component;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import skypass.db.OracleErrors;

/**
 * Runs database work off the Swing thread so the window never freezes.
 * All work goes through one background thread, so statements on the
 * shared connection never overlap.
 */
public final class Async {

    private static final ExecutorService DB = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "skypass-db");
        t.setDaemon(true);
        return t;
    });

    public interface Job<T> {
        T call() throws Exception;
    }

    private Async() {
    }

    /** Runs job in the background; done gets the result on the Swing thread, errors become a toast. */
    public static <T> void run(Component owner, Job<T> job, Consumer<T> done) {
        run(owner, job, done, null);
    }

    /** As above, with a callback that always runs afterwards (e.g. to stop a spinner). */
    public static <T> void run(Component owner, Job<T> job, Consumer<T> done, Runnable always) {
        DB.submit(() -> {
            try {
                T result = job.call();
                SwingUtilities.invokeLater(() -> {
                    if (always != null) {
                        always.run();
                    }
                    done.accept(result);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    if (always != null) {
                        always.run();
                    }
                    Toast.error(owner, OracleErrors.friendly(e));
                });
            }
        });
    }
}
