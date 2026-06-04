package com.yashgamerx.flcd.service.dimension;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.precompute.RootifiedPreCompute;

import static com.yashgamerx.flcd.model.AbstractNode.*;

public class HorizontalSubtreeDimensionCalculator implements NodeDimensionCalculator {
    @Override
    public void calculate(AbstractNode node) {
        double maxChildHeight = 0;
        double rawChildrenWidth = 0;

        for (AbstractNode child : node.getChildren()) {
            maxChildHeight = maxHeight(child, maxChildHeight);
            rawChildrenWidth = accumulateWidth(child, rawChildrenWidth);
        }

        // Apply the spacer adjustments using your exact formulas
        int totalChildren = node.getChildren().size();
        double totalChildrenWidth = rawChildrenWidth + (WIDTH_SPACER * (totalChildren - 1));

        // Children Width + Width Spacing + Parent Width (10.0)
        var subtreeWidth = totalChildrenWidth + WIDTH_SPACER + NODE_DIAMETER;

        // Maximum Child Height + Height Spacing + Parent Height (10.0)
        var subtreeHeight = Math.max(NODE_DIAMETER, maxChildHeight) + HEIGHT_SPACER + NODE_DIAMETER;

        // Sets the dimensions
        node.setSubtreeWidth(subtreeWidth);
        node.setSubtreeHeight(subtreeHeight);
    }

    private double maxHeight(AbstractNode node, double maxHeight) {
        if (node.getPrecomputable() instanceof RootifiedPreCompute) {
            return Math.max(maxHeight, node.getSubtreeWidth());
        }

        return Math.max(maxHeight, node.getSubtreeHeight());
    }

    private double accumulateWidth(AbstractNode node, double accumulatedWidth) {
        if (node.getPrecomputable() instanceof RootifiedPreCompute) {
            return accumulatedWidth + node.getSubtreeHeight();
        }

        return accumulatedWidth + node.getSubtreeWidth();
    }
}
