package com.yashgamerx.flcd.service.dimension;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.precompute.RootifiedPreCompute;

public class VerticalSubtreeDimensionCalculator implements NodeDimensionCalculator {
    @Override
    public void calculate(AbstractNode node) {
        double maxChildWidth = 0;
        double rawChildrenHeight = 0;

        for (AbstractNode child : node.getChildren()) {
            maxChildWidth = maxWidth(node, maxChildWidth);
        }
    }

    private double maxWidth(AbstractNode node, double maxWidth) {
        if (node.getPrecomputable() instanceof RootifiedPreCompute) {
            return Math.max(maxWidth, node.getSubtreeHeight());
        }

        return Math.max(maxWidth, node.getSubtreeWidth());
    }
}
