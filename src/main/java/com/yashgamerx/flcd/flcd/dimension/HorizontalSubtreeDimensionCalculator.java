package com.yashgamerx.flcd.flcd.dimension;

import com.yashgamerx.flcd.flcd.model.FLCDNode;

import static com.yashgamerx.flcd.flcd.model.FLCDNode.*;

/// Used by `HEIGHT_CHILD`-role nodes to size themselves against their
/// `WIDTH_CHILD` children, laid out side-by-side.
///
/// Note: the rootified/non-rootified formulas here are identical (this
/// mirrors the original `HorizontalSubtreeDimensionCalculator`, whose
/// rootified branch was dead code — both branches computed the same
/// value), so no status check is needed.
public class HorizontalSubtreeDimensionCalculator implements NodeDimensionCalculator {
    @Override
    public void calculate(FLCDNode node) {
        double maxChildHeight = 0;
        double rawChildrenWidth = 0;

        for (FLCDNode child : node.getChildren()) {
            maxChildHeight = Math.max(maxChildHeight, child.getSubtreeHeight());
            rawChildrenWidth += child.getSubtreeWidth();
        }

        int totalChildren = node.getChildren().size();
        double totalChildrenWidth = rawChildrenWidth + (WIDTH_SPACER * (totalChildren - 1));

        // Children Width + Width Spacing + Parent Width (10.0)
        var subtreeWidth = totalChildrenWidth + WIDTH_SPACER + NODE_DIAMETER;

        // Maximum Child Height + Height Spacing + Parent Height (10.0)
        var subtreeHeight = Math.max(NODE_DIAMETER, maxChildHeight) + HEIGHT_SPACER + NODE_DIAMETER;

        node.setSubtreeWidth(subtreeWidth);
        node.setSubtreeHeight(subtreeHeight);
    }
}
