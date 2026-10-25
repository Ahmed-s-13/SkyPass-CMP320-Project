package skypass.ui;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.Scrollable;

/**
 * A panel for inside a scroll pane that always matches the viewport's
 * width (so nothing scrolls sideways) and scrolls vertically only when
 * its content is taller than the window.
 */
public class TrackingPanel extends JPanel implements Scrollable {

    public TrackingPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
        return 16;
    }

    public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
        return r.height;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return getParent() != null && getParent().getHeight() > getPreferredSize().height;
    }
}
