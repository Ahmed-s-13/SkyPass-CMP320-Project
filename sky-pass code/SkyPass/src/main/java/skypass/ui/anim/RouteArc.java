package skypass.ui.anim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;

/**
 * A flight route drawn as a curved arc between two points, the way
 * route maps show great-circle paths. The plane flies along it; the
 * part already flown becomes a solid contrail, the rest stays dashed.
 */
public final class RouteArc {

    private final double x0, y0, cx, cy, x1, y1;

    public RouteArc(double x0, double y0, double x1, double y1, double lift) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.cx = (x0 + x1) / 2;
        this.cy = Math.min(y0, y1) - lift;
    }

    public Point2D point(double t) {
        double u = 1 - t;
        return new Point2D.Double(u * u * x0 + 2 * u * t * cx + t * t * x1,
                                  u * u * y0 + 2 * u * t * cy + t * t * y1);
    }

    /** Heading of the plane at t, in radians. */
    public double angle(double t) {
        double dx = 2 * (1 - t) * (cx - x0) + 2 * t * (x1 - cx);
        double dy = 2 * (1 - t) * (cy - y0) + 2 * t * (y1 - cy);
        return Math.atan2(dy, dx);
    }

    /** Paints the dashed route, the contrail up to t and the plane at t. */
    public void paint(Graphics2D g2, double t, Color dash, Color trail, Color plane, double planeSize, double alpha) {
        g2.setColor(dash);
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f, 7f}, 0f));
        g2.draw(new QuadCurve2D.Double(x0, y0, cx, cy, x1, y1));

        // Contrail: the flown part of the curve, fading in towards the plane
        Path2D flown = new Path2D.Double();
        Point2D start = point(0);
        flown.moveTo(start.getX(), start.getY());
        int steps = 48;
        for (int i = 1; i <= steps; i++) {
            Point2D p = point(t * i / steps);
            flown.lineTo(p.getX(), p.getY());
        }
        g2.setColor(withAlpha(trail, alpha));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(flown);

        // End points
        g2.setColor(trail);
        g2.fillOval((int) x0 - 4, (int) y0 - 4, 8, 8);
        g2.drawOval((int) x1 - 4, (int) y1 - 4, 8, 8);

        Point2D p = point(t);
        PlaneGlyph.paint(g2, p.getX(), p.getY(), planeSize, angle(t), withAlpha(plane, alpha));
    }

    private static Color withAlpha(Color c, double alpha) {
        int a = (int) Math.round(c.getAlpha() * Math.max(0, Math.min(1, alpha)));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
