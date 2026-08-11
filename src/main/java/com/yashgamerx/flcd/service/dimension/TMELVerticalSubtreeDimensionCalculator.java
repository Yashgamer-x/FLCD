package com.yashgamerx.flcd.service.dimension;

import com.yashgamerx.flcd.model.TMELNode;

import static com.yashgamerx.flcd.model.FLCDNode.*;

public class TMELVerticalSubtreeDimensionCalculator {
    public void calculate(TMELNode node) {
        double maxChildWidth = 0;
        double rawChildrenHeight = 0;

        for (TMELNode child : node.getChildren()) {
            maxChildWidth = Math.max(maxChildWidth, child.getSubtreeWidth());
            rawChildrenHeight += child.getSubtreeHeight();
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
}
