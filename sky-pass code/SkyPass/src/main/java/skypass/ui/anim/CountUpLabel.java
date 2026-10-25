package skypass.ui.anim;

import java.util.function.DoubleFunction;
import javax.swing.JLabel;

/** A number that counts up to its value when it is shown. */
public class CountUpLabel extends JLabel {

    private Animator running;

    public CountUpLabel() {
        super("–");
    }

    public void countTo(double target, DoubleFunction<String> format) {
        if (running != null) {
            running.stop();
        }
        getAccessibleContext().setAccessibleName(format.apply(target));
        running = Animator.play(1100, Animator::easeOutExpo, p -> setText(format.apply(target * p)),
                () -> setText(format.apply(target)));
    }
}
