package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;

import static com.yashgamerx.flcd.model.AbstractNode.*;

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
        return node.getChildren() == null || node.getChildren().isEmpty();
    }

    /// Initializes leaf node dimensions
    private void initializeLeafDimensions(AbstractNode node) {
        node.setSubtreeWidth(NODE_DIAMETER);
        node.setSubtreeHeight(NODE_DIAMETER);
    }

    /// Injects the dependency and then precomputes that child.
    private void injectAndPrecomputeChildren(AbstractNode heightChild) {
        injectHeightChildPrecomputation(heightChild);
        heightChild.precompute();
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
        var subtreeWidth = Math.max(NODE_DIAMETER, maxChildWidth) + WIDTH_SPACER + NODE_DIAMETER;

        // Total Children Height + Height Spacing + Parent Height (10.0)
        var subtreeHeight = totalChildrenHeight + HEIGHT_SPACER + NODE_DIAMETER;

        // Sets the dimensions
        widthNode.setSubtreeWidth(subtreeWidth);
        widthNode.setSubtreeHeight(subtreeHeight);
    }
}
