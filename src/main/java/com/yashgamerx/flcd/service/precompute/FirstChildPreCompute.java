package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.LeftSecondChildCompute;
import com.yashgamerx.flcd.service.compute.RightSecondChildCompute;

import java.util.Comparator;

import static com.yashgamerx.flcd.model.AbstractNode.*;

public class FirstChildPreCompute implements Precomputable {
    @Override
    public void precompute(AbstractNode firstChild) {
        if (isLeafNode(firstChild)) {
            initializeLeafDimensions(firstChild);
            return;
        }

        // Injects SecondChildPreCompute dependency and Precomputes all the children
        firstChild.getChildren().forEach(this::injectAndPrecompute);

        calculateBalancedSubTreeDimensions(firstChild);
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

    /// Injects SecondChildPreCompute dependency and Precomputes the child
    private void injectAndPrecompute(AbstractNode secondChildNode) {
        injectSecondChildPrecomputation(secondChildNode);
        secondChildNode.precompute();
    }

    /// Injects SecondChildPreCompute dependency to the node
    private void injectSecondChildPrecomputation(AbstractNode secondChildNode) {
        secondChildNode.setPrecomputable(new SecondChildPreCompute());
    }

    private void injectLeftSecondChildComputable(AbstractNode secondChildNode) {
        secondChildNode.setComputable(new LeftSecondChildCompute());
    }

    private void injectRightSecondChildComputable(AbstractNode secondChildNode) {
        secondChildNode.setComputable(new RightSecondChildCompute());
    }

    /// Calculates and returns the area of the node
    private double calculateArea(AbstractNode node) {
        return node.getSubtreeHeight() * node.getSubtreeWidth();
    }

    /// Sorts the children in descending order based on their area.
    /// Injects the Computable based on the balance
    private void calculateBalancedSubTreeDimensions(AbstractNode firstChild) {
        var areaComparator = Comparator.comparingDouble((AbstractNode node) -> (node.getSubtreeWidth()) * (node.getSubtreeHeight()));
        firstChild.getChildren().sort(areaComparator.reversed());

        // Area
        double leftAccumulatedArea = 0;
        double rightAccumulatedArea = 0;

        // Width
        double rightWidth = 0;
        double leftWidth = 0;

        // Height
        double maxLeftHeight = 0;
        double maxRightHeight = 0;

        for (var secondChild : firstChild.getChildren()) {
            if (leftAccumulatedArea <= rightAccumulatedArea) {
                injectLeftSecondChildComputable(secondChild);
                leftAccumulatedArea += calculateArea(secondChild);
                leftWidth += secondChild.getSubtreeWidth() + WIDTH_SPACER;
                maxLeftHeight = Math.max(maxLeftHeight, secondChild.getSubtreeHeight());
            } else {
                injectRightSecondChildComputable(secondChild);
                rightAccumulatedArea += calculateArea(secondChild);
                rightWidth += secondChild.getSubtreeWidth() + WIDTH_SPACER;
                maxRightHeight = Math.max(maxRightHeight, secondChild.getSubtreeHeight());
            }
        }

        double maxSideWidth = Math.max(leftWidth, rightWidth);

        // Max Width * 2 + (Node Diameter: 10.0)
        double totalWidth = (maxSideWidth * 2) + NODE_DIAMETER;
        firstChild.setSubtreeWidth(Math.max(NODE_DIAMETER, totalWidth));

        // Height calculation remains the same: this node + vertical gap + tallest child + SPACER (between Root and First Child)
        firstChild.setSubtreeHeight(NODE_DIAMETER + HEIGHT_SPACER + Math.max(maxLeftHeight, maxRightHeight));
    }
}
