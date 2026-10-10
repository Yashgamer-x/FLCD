package com.yashgamerx.flcd.common.visual;

import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/// Shapes a tree node can be drawn as. Every shape is built to sit inside the
/// circle of radius `r` that the layout algorithms reserve for a node, so
/// reshaping a node never makes it overlap its neighbours or changes any
/// measured bounding box. CIRCLE is the default and doubles as the reset.
public enum NodeShape {
    CIRCLE("Circle (default)"),
    RECTANGLE("Rectangle"),
    TRIANGLE("Triangle"),
    DIAMOND("Diamond"),
    PENTAGON("Pentagon"),
    HEXAGON("Hexagon"),
    STAR("Star"),
    CROSS("Cross");

    /// Rectangle is 4:3, so its corners land exactly on the node's circle.
    private static final double RECT_WIDTH_RATIO = 1.6;
    private static final double RECT_HEIGHT_RATIO = 1.2;
    /// Inner radius of the star, as a fraction of the outer radius.
    private static final double STAR_INNER_RATIO = 0.4;
    /// Half the thickness and half the length of the cross arms, as fractions
    /// of the radius. sqrt(0.95^2 + 0.3^2) is just under 1, so the cross
    /// corners stay inside the circle.
    private static final double CROSS_HALF_THICKNESS_RATIO = 0.3;
    private static final double CROSS_HALF_LENGTH_RATIO = 0.95;

    private final String label;

    NodeShape(String label) {
        this.label = label;
    }

    /// Regular n-gon with one vertex pointing straight up.
    private static Polygon regularPolygon(double cx, double cy, double r, int sides) {
        double[] points = new double[sides * 2];
        for (int i = 0; i < sides; i++) {
            double angle = -Math.PI / 2.0 + (2.0 * Math.PI * i) / sides;
            points[2 * i] = cx + r * Math.cos(angle);
            points[2 * i + 1] = cy + r * Math.sin(angle);
        }
        return new Polygon(points);
    }

    /// Five-pointed star: ten vertices alternating between the outer and inner radius.
    private static Polygon star(double cx, double cy, double r) {
        int vertices = 10;
        double[] points = new double[vertices * 2];
        for (int i = 0; i < vertices; i++) {
            double radius = (i % 2 == 0) ? r : r * STAR_INNER_RATIO;
            double angle = -Math.PI / 2.0 + (2.0 * Math.PI * i) / vertices;
            points[2 * i] = cx + radius * Math.cos(angle);
            points[2 * i + 1] = cy + radius * Math.sin(angle);
        }
        return new Polygon(points);
    }

    /// Plus sign made of a vertical and a horizontal arm.
    private static Polygon cross(double cx, double cy, double r) {
        double t = r * CROSS_HALF_THICKNESS_RATIO;
        double l = r * CROSS_HALF_LENGTH_RATIO;
        return new Polygon(
                cx - t, cy - l,
                cx + t, cy - l,
                cx + t, cy - t,
                cx + l, cy - t,
                cx + l, cy + t,
                cx + t, cy + t,
                cx + t, cy + l,
                cx - t, cy + l,
                cx - t, cy + t,
                cx - l, cy + t,
                cx - l, cy - t,
                cx - t, cy - t);
    }

    /// Builds this shape centered on (cx, cy), fitted inside a circle of
    /// radius `r`. Fill, stroke and click handling are left to the caller.
    public Shape create(double cx, double cy, double r) {
        return switch (this) {
            case CIRCLE -> new Circle(cx, cy, r);
            case RECTANGLE -> {
                double w = r * RECT_WIDTH_RATIO;
                double h = r * RECT_HEIGHT_RATIO;
                yield new Rectangle(cx - w / 2.0, cy - h / 2.0, w, h);
            }
            case TRIANGLE -> {
                double halfBase = r * Math.sqrt(3.0) / 2.0;
                yield new Polygon(
                        cx, cy - r,
                        cx + halfBase, cy + r / 2.0,
                        cx - halfBase, cy + r / 2.0);
            }
            case DIAMOND -> new Polygon(
                    cx, cy - r,
                    cx + r, cy,
                    cx, cy + r,
                    cx - r, cy);
            case PENTAGON -> regularPolygon(cx, cy, r, 5);
            case HEXAGON -> regularPolygon(cx, cy, r, 6);
            case STAR -> star(cx, cy, r);
            case CROSS -> cross(cx, cy, r);
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
