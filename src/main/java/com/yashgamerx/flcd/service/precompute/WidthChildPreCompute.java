package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;

import static com.yashgamerx.flcd.model.AbstractNode.HEIGHT_SPACER;
import static com.yashgamerx.flcd.model.AbstractNode.WIDTH_SPACER;

public class WidthChildPreCompute implements Precomputable {
    @Override
    public void precompute(AbstractNode widthNode) {
        if (isLeafNode(widthNode)) {
            initializeLeafDimensions(widthNode);
            return;
        }

        widthNode.getChildren().forEach(this::injectAndPrecomputeChildren);

        calculateVerticalSubtreeDimensions(widthNode);
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

    /// Injects the dependency and then precomputes that child.
    private void injectAndPrecomputeChildren(AbstractNode node) {
        injectHeightChildPrecomputation(node);
        node.precompute();
    }

    /// Injects [HeightChildPreCompute] dependency to the heightChild
    private void injectHeightChildPrecomputation(AbstractNode heightChild) {
        heightChild.setPrecomputable(new HeightChildPreCompute());
    }

    /// Computes dimensions where children are stacked vertically
    private void calculateVerticalSubtreeDimensions(AbstractNode widthNode) {
        // Calculate the maximum child height using a stream
        double maxChildWidth = widthNode.getChildren().stream()
                .mapToDouble(AbstractNode::getSubtreeWidth)
                .max()
                .orElse(0.0);

        // Calculate the combined raw width of all children using a stream
        double rawCombinedHeight = widthNode.getChildren().stream()
                .mapToDouble(AbstractNode::getSubtreeHeight)
                .sum();

        // Apply the spacer adjustments using your exact formulas
        int totalChildren = widthNode.getChildren().size();
        double totalChildrenHeight = rawCombinedHeight + (HEIGHT_SPACER * (totalChildren - 1));

        // Maximum Child Width + Width Spacing + Parent Width (10.0)
        var subtreeWidth = Math.max(10.0, maxChildWidth) + WIDTH_SPACER + 10.0;

        // Total Children Height + Height Spacing + Parent Height (10.0)
        var subtreeHeight = totalChildrenHeight + HEIGHT_SPACER + 10.0;

        // Sets the dimensions
        widthNode.setSubtreeWidth(subtreeWidth);
        widthNode.setSubtreeHeight(subtreeHeight);
    }
}
