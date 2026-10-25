package skypass.ui.anim;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * A top-down airliner silhouette drawn in code, used as the product mark
 * and by every plane animation. Defined in a 24x24 box with the nose
 * pointing right (+x), so a rotation angle of 0 means "flying east".
 */
public final class PlaneGlyph {

    private static final Shape PLANE = build();

    private PlaneGlyph() {
    }

    private static Shape build() {
        Area plane = new Area(new RoundRectangle2D.Double(2.5, 10.7, 21, 2.6, 2.6, 2.6));
        plane.add(new Area(polygon(13.8, 11, 8.6, 2.2, 6.6, 2.2, 9.6, 11)));
        plane.add(new Area(polygon(13.8, 13, 8.6, 21.8, 6.6, 21.8, 9.6, 13)));
        plane.add(new Area(polygon(5.2, 11, 2.6, 7.0, 1.2, 7.0, 2.6, 11)));
        plane.add(new Area(polygon(5.2, 13, 2.6, 17.0, 1.2, 17.0, 2.6, 13)));
        return plane;
    }

    private static Path2D polygon(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        p.closePath();
        return p;
    }

    /**
     * Paints the plane centred on (cx, cy).
     *
     * @param size  width of the plane in pixels
     * @param angle heading in radians (0 = right)
     */
    public static void paint(Graphics2D g, double cx, double cy, double size, double angle, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform at = new AffineTransform();
        at.translate(cx, cy);
        at.rotate(angle);
        at.scale(size / 24.0, size / 24.0);
        at.translate(-12, -12);
        g2.setColor(color);
        g2.fill(at.createTransformedShape(PLANE));
        g2.dispose();
    }
}
