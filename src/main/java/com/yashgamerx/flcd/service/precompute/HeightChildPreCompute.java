package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;

import static com.yashgamerx.flcd.model.AbstractNode.HEIGHT_SPACER;
import static com.yashgamerx.flcd.model.AbstractNode.WIDTH_SPACER;

public class HeightChildPreCompute implements Precomputable {
    @Override
    public void precompute(AbstractNode heightNode) {
        if (isLeafNode(heightNode)) {
            initializeLeafDimensions(heightNode);
            return;
        }

        heightNode.getChildren().forEach(this::injectAndPrecompute);

        calculateHorizontalSubtreeDimensions(heightNode);
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

    /// Injects and then precomputes the width child
    private void injectAndPrecompute(AbstractNode widthChild) {
        injectWidthChildPrecomputation(widthChild);
        widthChild.precompute();
    }

    /// Injects [WidthChildPreCompute] dependency and then precomputes that child.
    private void injectWidthChildPrecomputation(AbstractNode widthChild) {
        widthChild.setPrecomputable(new WidthChildPreCompute());
    }

    /// Computes dimensions where children are stacked Horizontally
    private void calculateHorizontalSubtreeDimensions(AbstractNode heightNode) {
        // Calculate the maximum child height using a stream
        double maxChildHeight = heightNode.getChildren().stream()
                .mapToDouble(AbstractNode::getSubtreeHeight)
                .max()
                .orElse(0.0);

        // Calculate the combined raw width of all children using a stream
        double rawCombinedWidth = heightNode.getChildren().stream()
                .mapToDouble(AbstractNode::getSubtreeWidth)
                .sum();

        // Apply the spacer adjustments using your exact formulas
        int totalChildren = heightNode.getChildren().size();
        double totalChildrenWidth = rawCombinedWidth + (WIDTH_SPACER * (totalChildren - 1));

        // Total Children Width + Width Spacing + Parent Width (10.0)
        var subtreeWidth = totalChildrenWidth + WIDTH_SPACER + 10.0;

        // Maximum Child Height + Height Spacing + Parent Height (10.0)
        var subtreeHeight = Math.max(10.0, maxChildHeight) + HEIGHT_SPACER + 10.0;

        // Sets the dimensions
        heightNode.setSubtreeWidth(subtreeWidth);
        heightNode.setSubtreeHeight(subtreeHeight);
    }
}
