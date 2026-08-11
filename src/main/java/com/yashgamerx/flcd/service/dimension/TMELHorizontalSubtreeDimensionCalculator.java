package com.yashgamerx.flcd.service.dimension;

import com.yashgamerx.flcd.model.TMELNode;

import static com.yashgamerx.flcd.model.FLCDNode.*;

public class TMELHorizontalSubtreeDimensionCalculator {
    public void calculate(TMELNode node) {
        double maxChildHeight = 0;
        double rawChildrenWidth = 0;

        for (TMELNode child : node.getChildren()) {
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
