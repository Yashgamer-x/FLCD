package com.yashgamerx.flcd.common.metrics;

/// Geometric measurement of a laid-out tree's bounding box, computed from
/// node centers plus `NODE_RADIUS` so the box encloses full node circles —
/// never from rendered pixel counts (area must stay analytically derived
/// from layout geometry to meet IEEE reviewer expectations).
///
/// `aspectRatio` (the "visibility ratio") is `width / height`; a value near
/// 1.0 means the drawing is roughly square, values far from 1.0 mean a very
/// wide or very tall layout, which can hurt on-screen/print visibility.
public record BoundingBoxMetrics(
        double minX, double maxX, double minY, double maxY,
        double width, double height, double area, double aspectRatio,
        int leftMostId, int rightMostId, int topMostId, int bottomMostId
) {
    public static double computeAspectRatio(double width, double height) {
        if (height == 0.0) return Double.POSITIVE_INFINITY;
        return width / height;
    }
}
