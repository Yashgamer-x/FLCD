package com.yashgamerx.flcd.flcd.dimension;

import com.yashgamerx.flcd.flcd.model.FLCDNode;
import com.yashgamerx.flcd.flcd.model.NodeStatus;

import static com.yashgamerx.flcd.flcd.model.FLCDNode.*;

/// Used by `SECOND_CHILD`/`WIDTH_CHILD`-role nodes to size themselves
/// against their `HEIGHT_CHILD` children, stacked vertically.
///
/// A manually-rootified child measures differently (its own width stands
/// in for what would normally be its height contribution, since a
/// rootified subtree spreads outward rather than downward) — this is a
/// genuine behavioral difference from the original code, preserved here
/// via a status check rather than a role/type check.
public class VerticalSubtreeDimensionCalculator implements NodeDimensionCalculator {
    @Override
    public void calculate(FLCDNode node) {
        double maxChildWidth = 0;
        double rawChildrenHeight = 0;

        for (FLCDNode child : node.getChildren()) {
            maxChildWidth = Math.max(maxChildWidth, rootifiedAwareWidth(child));
            rawChildrenHeight += rootifiedAwareHeight(child);
        }

        int totalChildren = node.getChildren().size();
        double totalChildrenHeight = rawChildrenHeight + (HEIGHT_SPACER * (totalChildren - 1));

        // Maximum Child Width + Width Spacing + Parent Width (10.0)
        var subtreeWidth = Math.max(NODE_DIAMETER, maxChildWidth) + WIDTH_SPACER + NODE_DIAMETER;

        // Children Height + Height Spacing + Parent Height (10.0)
        var subtreeHeight = totalChildrenHeight + HEIGHT_SPACER + NODE_DIAMETER;

        node.setSubtreeWidth(subtreeWidth);
        node.setSubtreeHeight(subtreeHeight);
    }

    private double rootifiedAwareWidth(FLCDNode child) {
        return child.getStatus() == NodeStatus.ROOTIFIED ? child.getSubtreeHeight() : child.getSubtreeWidth();
    }

    private double rootifiedAwareHeight(FLCDNode child) {
        return child.getStatus() == NodeStatus.ROOTIFIED ? child.getSubtreeWidth() : child.getSubtreeHeight();
    }
}
