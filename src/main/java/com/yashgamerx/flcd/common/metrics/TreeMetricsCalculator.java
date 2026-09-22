package com.yashgamerx.flcd.common.metrics;

import com.yashgamerx.flcd.common.AlgorithmicNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/// Geometric measurement pass shared by every algorithm family's view
/// (FLCD, TMEL, CMEL, RT, Rings). Operates purely against the
/// [AlgorithmicNode] contract (`layoutX`/`layoutY`/`children`) plus an
/// identifier extractor, so it needs no knowledge of any family-specific
/// node subclass.
///
/// All measurements are analytical — derived from node center positions
/// and `NODE_RADIUS` — never from rendered pixel/widget sizes, matching the
/// project's established area-measurement methodology.
public final class TreeMetricsCalculator {

    private TreeMetricsCalculator() {
    }

    /// Bounding box (and derived area / aspect ratio) over every node's
    /// full circle (center +/- `nodeRadius`), not just its center point.
    public static <T extends AlgorithmicNode<T>> BoundingBoxMetrics computeBoundingBox(
            Collection<T> nodes, double nodeRadius, ToIntFunction<T> idOf) {

        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        int leftMostId = -1, rightMostId = -1, topMostId = -1, bottomMostId = -1;

        for (var node : nodes) {
            double x = node.getLayoutX();
            double y = node.getLayoutY();

            if (x - nodeRadius < minX) {
                minX = x - nodeRadius;
                leftMostId = idOf.applyAsInt(node);
            }
            if (x + nodeRadius > maxX) {
                maxX = x + nodeRadius;
                rightMostId = idOf.applyAsInt(node);
            }
            if (y - nodeRadius < minY) {
                minY = y - nodeRadius;
                topMostId = idOf.applyAsInt(node);
            }
            if (y + nodeRadius > maxY) {
                maxY = y + nodeRadius;
                bottomMostId = idOf.applyAsInt(node);
            }
        }

        double width = maxX - minX;
        double height = maxY - minY;
        double area = width * height;
        double aspectRatio = BoundingBoxMetrics.computeAspectRatio(width, height);

        return new BoundingBoxMetrics(minX, maxX, minY, maxY, width, height, area, aspectRatio,
                leftMostId, rightMostId, topMostId, bottomMostId);
    }

    /// Center-to-center straight-line distance AND along-edge path length
    /// from `root` to every leaf (a node with no children) reachable from
    /// it, via a depth-first walk. `depth` is the number of edges from
    /// root to that leaf.
    public static <T extends AlgorithmicNode<T>> List<RootLeafDistance> computeRootToLeafDistances(
            T root, ToIntFunction<T> idOf) {

        List<RootLeafDistance> results = new ArrayList<>();
        if (root == null) return results;

        double rootX = root.getLayoutX();
        double rootY = root.getLayoutY();
        walk(root, root, rootX, rootY, 0.0, 0, idOf, results);
        return results;
    }

    private static <T extends AlgorithmicNode<T>> void walk(
            T root, T current, double rootX, double rootY,
            double pathLengthSoFar, int depth, ToIntFunction<T> idOf, List<RootLeafDistance> results) {

        if (current.getChildren().isEmpty()) {
            double dx = current.getLayoutX() - rootX;
            double dy = current.getLayoutY() - rootY;
            double straightLine = Math.sqrt(dx * dx + dy * dy);
            results.add(new RootLeafDistance(idOf.applyAsInt(current), depth, straightLine, pathLengthSoFar));
            return;
        }

        for (var child : current.getChildren()) {
            double edx = child.getLayoutX() - current.getLayoutX();
            double edy = child.getLayoutY() - current.getLayoutY();
            double edgeLength = Math.sqrt(edx * edx + edy * edy);
            walk(root, child, rootX, rootY, pathLengthSoFar + edgeLength, depth + 1, idOf, results);
        }
    }

    /// Leaf-to-root distance metric, walking each leaf UP via `parentOf`
    /// rather than descending from a single fixed root. This is not just
    /// the reverse of [#computeRootToLeafDistances] — it changes which
    /// anchor a leaf is measured against. A leaf's walk stops as soon as
    /// it reaches a node the algorithm itself treats as fixed for that
    /// subtree: either a manually-rootified node (`isRootAnchor` true,
    /// checked nearest-ancestor-first) or the tree's real root
    /// (`parentOf` returns null). A leaf under a rootified subtree is
    /// therefore measured against its own rootified anchor, not the
    /// tree's true root, matching what the layout algorithm actually
    /// holds fixed.
    ///
    /// Use this only for node types where "root" is not a single fixed
    /// point for every leaf (currently FLCD, via its manual-rootify
    /// feature). Families without that concept should keep using
    /// [#computeRootToLeafDistances].
    public static <T extends AlgorithmicNode<T>> List<RootLeafDistance> computeLeafToRootDistances(
            Collection<T> allNodes, Function<T, T> parentOf, Predicate<T> isRootAnchor, ToIntFunction<T> idOf) {

        List<RootLeafDistance> results = new ArrayList<>();

        for (T leaf : allNodes) {
            if (!leaf.getChildren().isEmpty()) continue;

            double pathLength = 0.0;
            int depth = 0;
            T current = leaf;
            T anchor = leaf;

            while (true) {
                if (isRootAnchor.test(current)) {
                    anchor = current;
                    break;
                }
                T parent = parentOf.apply(current);
                if (parent == null) {
                    anchor = current;
                    break;
                }
                double edx = current.getLayoutX() - parent.getLayoutX();
                double edy = current.getLayoutY() - parent.getLayoutY();
                pathLength += Math.sqrt(edx * edx + edy * edy);
                depth++;
                current = parent;
            }

            double dx = leaf.getLayoutX() - anchor.getLayoutX();
            double dy = leaf.getLayoutY() - anchor.getLayoutY();
            double straightLine = Math.sqrt(dx * dx + dy * dy);
            results.add(new RootLeafDistance(idOf.applyAsInt(leaf), depth, straightLine, pathLength));
        }

        return results;
    }
}
