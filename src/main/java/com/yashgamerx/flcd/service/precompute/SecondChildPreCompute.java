package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;

import static com.yashgamerx.flcd.model.AbstractNode.*;

public class SecondChildPreCompute implements Precomputable {
    @Override
    public void precompute(AbstractNode secondChild) {
        if (isLeafNode(secondChild)) {
            initializeLeafDimensions(secondChild);
            return;
        }

        // Inject and Precompute
        secondChild.getChildren().forEach(this::injectAndPrecomputeChildren);

        calculateStackedSubtreeDimensions(secondChild);
    }

    /// Checks if the node has no children
    private boolean isLeafNode(AbstractNode node) {
        return node.getChildren() == null || node.getChildren().isEmpty();
    }

    /// Initializes leaf node dimensions
    private void initializeLeafDimensions(AbstractNode node) {
        node.setSubtreeWidth(NODE_DIAMETER);
        node.setSubtreeHeight(NODE_DIAMETER);
    }

    /// Injects {@link HeightChildPreCompute} dependency and Precomputes the child
    private void injectAndPrecomputeChildren(AbstractNode heightChild) {
        injectHeightChildPrecomputation(heightChild);
        heightChild.precompute();
    }

    /// Injects {@link HeightChildPreCompute} dependency to the node
    private void injectHeightChildPrecomputation(AbstractNode heightChild) {
        heightChild.setPrecomputable(new HeightChildPreCompute());
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
        var subtreeWidth = Math.max(NODE_DIAMETER, maxChildWidth) + WIDTH_SPACER + NODE_DIAMETER;

        // Children Height + Height Spacing + Parent Height (10.0)
        var subtreeHeight = totalChildrenHeight + HEIGHT_SPACER + NODE_DIAMETER;

        // Sets the dimensions
        secondChild.setSubtreeWidth(subtreeWidth);
        secondChild.setSubtreeHeight(subtreeHeight);
    }
}
