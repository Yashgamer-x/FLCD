package com.yashgamerx.flcd.service.algorithm;

import com.yashgamerx.flcd.model.MaximumEdgeLengthNode;

import java.util.HashMap;
import java.util.Map;

/// Standalone layout algorithm for [MaximumEdgeLengthNode] trees.
///
/// Intentionally does not implement [TreeLayoutAlgorithm] — that interface
/// is FLCD's contract over [com.yashgamerx.flcd.model.FLCDNode], and this
/// algorithm operates on a different node type with a different
/// placement strategy entirely.
public class MaximumEdgeLengthAlgorithm {

    private static final double RADIUS_STEP = 45.0;
    private static final double TWO_PI = 2.0 * Math.PI;

    /// PLACEHOLDER: a recursive radial layout so the view has something
    /// reasonable to render before the real maximum-edge-length placement
    /// math is written. Replace this body once that algorithm exists.
    ///
    /// Shape: every node at depth `d` sits on the circle of radius
    /// `d * RADIUS_STEP` centered on the root. The root's children split
    /// the full circle (0..2π) between them; every other node's children
    /// split *that node's own inherited angular slice* the same way —
    /// so each subtree keeps growing outward as an arc within the wedge
    /// its parent was given, rather than wrapping around into a sibling's
    /// territory. Slices are sized proportionally to subtree weight (leaf
    /// count) so a bushy branch gets more arc than a single leaf.
    public void calculate(MaximumEdgeLengthNode root, double originX, double originY) {
        if (root == null) return;

        root.setDepth(0);
        root.setGridX(originX);
        root.setGridY(originY);

        var subtreeWeights = new HashMap<MaximumEdgeLengthNode, Integer>();
        computeSubtreeWeight(root, subtreeWeights);

        placeChildrenInArc(root, 0.0, TWO_PI, originX, originY, subtreeWeights);
    }

    /// Post-order pass: weight of a leaf is 1, weight of any other node is
    /// the sum of its children's weights. Used to proportion arc width.
    private int computeSubtreeWeight(MaximumEdgeLengthNode node, Map<MaximumEdgeLengthNode, Integer> weights) {
        if (node.getChildren().isEmpty()) {
            weights.put(node, 1);
            return 1;
        }

        int total = 0;
        for (var child : node.getChildren()) {
            total += computeSubtreeWeight(child, weights);
        }
        weights.put(node, total);
        return total;
    }

    /// Splits `[startAngle, endAngle)` — the arc `node` was itself given by
    /// its own parent (the full circle, for root) — across `node`'s
    /// children proportionally to subtree weight, places each child at the
    /// midpoint of its own sub-slice one radius step further out, then
    /// recurses so that child's children continue subdividing *that*
    /// sub-slice.
    private void placeChildrenInArc(MaximumEdgeLengthNode node, double startAngle, double endAngle,
                                    double originX, double originY,
                                    Map<MaximumEdgeLengthNode, Integer> subtreeWeights) {
        var children = node.getChildren();
        if (children.isEmpty()) return;

        int totalWeight = 0;
        for (var child : children) {
            totalWeight += subtreeWeights.get(child);
        }

        double radius = (node.getDepth() + 1) * RADIUS_STEP;
        double angleCursor = startAngle;

        for (var child : children) {
            double share = (endAngle - startAngle) * subtreeWeights.get(child) / (double) totalWeight;
            double childStart = angleCursor;
            double childEnd = angleCursor + share;
            double childAngle = (childStart + childEnd) / 2.0;

            child.setDepth(node.getDepth() + 1);
            child.setGridX(originX + radius * Math.cos(childAngle));
            child.setGridY(originY - radius * Math.sin(childAngle));

            placeChildrenInArc(child, childStart, childEnd, originX, originY, subtreeWeights);

            angleCursor = childEnd;
        }
    }
}
