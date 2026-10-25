package skypass.ui.anim;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.HierarchyEvent;
import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.plaf.LayerUI;

/**
 * Wraps a component so it can fade in while rising a few pixels.
 * Used for page changes and for cards that reveal as they scroll into
 * view. JLayer routes every child repaint through here, so nothing
 * flickers in at full strength during the transition.
 */
public final class FadeLayer extends LayerUI<JComponent> {

    private final int rise;
    private double progress = 1;
    private JLayer<JComponent> layer;
    private Animator running;

    private FadeLayer(int rise) {
        this.rise = rise;
    }

    public static FadeLayer wrap(JComponent content, int rise) {
        FadeLayer ui = new FadeLayer(rise);
        ui.layer = new JLayer<>(content, ui);
        ui.layer.setOpaque(false);
        return ui;
    }

    public JLayer<JComponent> component() {
        return layer;
    }

    /** Fade and rise in over durationMs, after delayMs. */
    public void play(int durationMs, int delayMs) {
        if (running != null) {
            running.stop();
        }
        progress = 0;
        layer.repaint();
        running = Animator.play(durationMs, delayMs, Animator::easeOutCubic, p -> {
            progress = p;
            layer.repaint();
        }, null);
    }

    /**
     * Plays once, the first time the component is actually on screen
     * (shown, and scrolled into its viewport).
     */
    public void revealWhenVisible(int durationMs, int delayMs) {
        progress = 0;
        Runnable check = new Runnable() {
            boolean done;

            public void run() {
                if (done || !layer.isShowing()) {
                    return;
                }
                if (layer.getVisibleRect().height > 24) {
                    done = true;
                    play(durationMs, delayMs);
                }
            }
        };
        layer.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                SwingUtilities.invokeLater(check);
                JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, layer);
                if (viewport != null) {
                    viewport.addChangeListener(ev -> check.run());
                }
            }
        });
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        if (progress >= 1) {
            super.paint(g, c);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.max(0, progress)));
        g2.translate(0, (1 - progress) * rise);
        super.paint(g2, c);
        g2.dispose();
    }
}
