package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;
import static com.yashgamerx.flcd.model.AbstractNode.HEIGHT_SPACER;
import static com.yashgamerx.flcd.model.AbstractNode.WIDTH_SPACER;

public class SecondChildPreCompute implements Precomputable {
    @Override
    public void precompute(AbstractNode secondChild) {
        if (isLeafNode(secondChild)) {
            initializeLeafDimensions(secondChild);
            return;
        }

        // Inject and Precompute
        secondChild.getChildren().forEach(this::injectHeightChildPrecomputation);
        secondChild.getChildren().forEach(AbstractNode::precompute);

        calculateStackedSubtreeDimensions(secondChild);
    }

    /// Checks if the node has no children
    private boolean isLeafNode(AbstractNode node) {
        return node == null || node.getChildren().isEmpty();
    }

    /// Initializes leaf node dimensions
    private void initializeLeafDimensions(AbstractNode node) {
        node.setSubtreeWidth(10.0);
        node.setSubtreeHeight(10.0);
    }

    /// Injects SecondChildPreCompute dependency to the node
    private void injectHeightChildPrecomputation(AbstractNode secondChildNode) {
        secondChildNode.setPrecomputable(new HeightChildPreCompute());
    }

    private void calculateStackedSubtreeDimensions(AbstractNode secondChild) {
        // Calculate the maximum child width using a stream
        double maxChildWidth = secondChild.getChildren().stream()
                .mapToDouble(AbstractNode::getSubtreeWidth)
                .max()
                .orElse(0.0);

        // Calculate the combined raw height of all children using a stream
        double rawChildrenHeight = secondChild.getChildren().stream()
                .mapToDouble(AbstractNode::getSubtreeHeight)
                .sum();

        // Apply the spacer adjustments using your exact formulas
        int totalChildren = secondChild.getChildren().size();
        double totalChildrenHeight = rawChildrenHeight + (HEIGHT_SPACER * (totalChildren - 1));

        // Maximum Child Width + Width Spacing + Parent Width (10.0)
        var subtreeWidth = Math.max(10.0, maxChildWidth) + WIDTH_SPACER + 10.0;

        // Children Height + Height Spacing + Parent Height (10.0)
        var subtreeHeight = totalChildrenHeight + HEIGHT_SPACER + 10.0;

        // Sets the dimensions
        secondChild.setSubtreeWidth(subtreeWidth);
        secondChild.setSubtreeHeight(subtreeHeight);
    }
}
