package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;
import com.yashgamerx.flcd.service.precompute.inject.PrecomputeInjectable;
import com.yashgamerx.flcd.service.precompute.inject.WidthChildPreComputeInjector;

import static com.yashgamerx.flcd.model.AbstractNode.*;

public class HeightChildPreCompute implements Precomputable {

    private final PrecomputeInjectable injectable = new WidthChildPreComputeInjector();
    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();

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
        return emptyListChecker.isEmpty(node.getChildren());
    }

    /// Initializes leaf node dimensions
    private void initializeLeafDimensions(AbstractNode node) {
        node.setSubtreeWidth(NODE_DIAMETER);
        node.setSubtreeHeight(NODE_DIAMETER);
    }

    /// Injects and then precomputes the width child
    private void injectAndPrecompute(AbstractNode widthChild) {
        injectable.inject(widthChild);
        widthChild.precompute();
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

        // Total Children Width + Width Spacing + (Parent Diameter: 10.0)
        var subtreeWidth = totalChildrenWidth + WIDTH_SPACER + NODE_DIAMETER;

        // Maximum Child Height + Height Spacing + (Parent Diameter: 10.0)
        var subtreeHeight = Math.max(NODE_DIAMETER, maxChildHeight) + HEIGHT_SPACER + NODE_DIAMETER;

        // Sets the dimensions
        heightNode.setSubtreeWidth(subtreeWidth);
        heightNode.setSubtreeHeight(subtreeHeight);
    }
}
