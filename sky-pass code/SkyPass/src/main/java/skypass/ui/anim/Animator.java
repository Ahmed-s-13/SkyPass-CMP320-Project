package skypass.ui.anim;

import java.util.function.DoubleConsumer;
import javax.swing.Timer;

/**
 * One tiny tween engine for every animation in the app. It runs on the
 * Swing event thread at ~60 fps, so it never blocks input and never
 * needs locking.
 */
public final class Animator {

    /** Easing curves: progress 0..1 in, eased 0..1 out. */
    public static double easeOutCubic(double t) {
        double u = 1 - t;
        return 1 - u * u * u;
    }

    public static double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    public static double easeOutExpo(double t) {
        return t >= 1 ? 1 : 1 - Math.pow(2, -10 * t);
    }

    public interface Ease {
        double apply(double t);
    }

    private final Timer timer;
    private long start;

    private Animator(int durationMs, int delayMs, Ease ease, DoubleConsumer onFrame, Runnable onDone, boolean loop) {
        timer = new Timer(16, null);
        timer.addActionListener(e -> {
            long now = System.currentTimeMillis();
            double elapsed = now - start - delayMs;
            if (elapsed < 0) {
                return;
            }
            double t = Math.min(1, elapsed / durationMs);
            if (loop) {
                onFrame.accept((elapsed % durationMs) / durationMs);
                return;
            }
            onFrame.accept(ease.apply(t));
            if (t >= 1) {
                timer.stop();
                if (onDone != null) {
                    onDone.run();
                }
            }
        });
    }

    /** Plays once: onFrame receives the eased progress from 0 to 1. */
    public static Animator play(int durationMs, Ease ease, DoubleConsumer onFrame, Runnable onDone) {
        return play(durationMs, 0, ease, onFrame, onDone);
    }

    public static Animator play(int durationMs, int delayMs, Ease ease, DoubleConsumer onFrame, Runnable onDone) {
        Animator a = new Animator(durationMs, delayMs, ease, onFrame, onDone, false);
        a.start();
        return a;
    }

    /** Repeats forever: onFrame receives linear progress 0..1 per cycle. Stop it when hidden. */
    public static Animator loop(int periodMs, DoubleConsumer onFrame) {
        Animator a = new Animator(periodMs, 0, t -> t, onFrame, null, true);
        a.start();
        return a;
    }

    public void start() {
        start = System.currentTimeMillis();
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }
}
