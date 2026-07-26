package com.yashgamerx.flcd.service.algorithm;

import com.yashgamerx.flcd.model.MaximumEdgeLengthNode;

import static com.yashgamerx.flcd.model.MaximumEdgeLengthNode.NODE_DIAMETER;

/// Standalone layout algorithm for [MaximumEdgeLengthNode] trees.
///
/// Intentionally does not implement [TreeLayoutAlgorithm] — that interface
/// is FLCD's contract over [com.yashgamerx.flcd.model.FLCDNode], and this
/// algorithm operates on a different node type with a different
/// placement strategy entirely.
public class MaximumEdgeLengthAlgorithm {

    private static final double LEVEL_SPACING = 60.0;
    private static final double SIBLING_SPACING = 20.0;

    /// PLACEHOLDER: a simple level-order layout so the view has something
    /// to render before the real maximum-edge-length placement math is
    /// written. Replace this body once that algorithm is implemented.
    public void calculate(MaximumEdgeLengthNode root, double originX, double originY) {
        if (root == null) return;

        double[] nextX = {originX};
        placeLevelOrder(root, 0, originY, nextX);
    }

    private void placeLevelOrder(MaximumEdgeLengthNode node, int depth, double originY, double[] nextX) {
        node.setDepth(depth);

        if (node.getChildren().isEmpty()) {
            node.setGridX(nextX[0]);
            node.setGridY(originY + depth * LEVEL_SPACING);
            nextX[0] += NODE_DIAMETER + SIBLING_SPACING;
            return;
        }

        double firstChildX = nextX[0];
        for (var child : node.getChildren()) {
            placeLevelOrder(child, depth + 1, originY, nextX);
        }
        double lastChildX = nextX[0] - (NODE_DIAMETER + SIBLING_SPACING);

        node.setGridX((firstChildX + lastChildX) / 2.0);
        node.setGridY(originY + depth * LEVEL_SPACING);
    }
}
