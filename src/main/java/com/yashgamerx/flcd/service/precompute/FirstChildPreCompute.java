package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;

import java.util.Comparator;

public class FirstChildPreCompute implements Precomputable {
    @Override
    public void precompute(AbstractNode firstChild) {
        if (isLeafNode(firstChild)) {
            initializeLeafDimensions(firstChild);
            return;
        }

        // Injects SecondChildPreCompute dependency for all children of node
        firstChild.getChildren().forEach(this::injectSecondChildPrecomputation);

        // Invokes precompute recursively on all children
        firstChild.getChildren().forEach(AbstractNode::precompute);

        calculateBalancedSubTreeDimensions(firstChild);
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
    private void injectSecondChildPrecomputation(AbstractNode secondChildNode) {
        secondChildNode.setPrecomputable(new SecondChildPreCompute());
    }

    private void injectLeftSecondChildComputable(AbstractNode secondChildNode) {
        //TODO: Implement
    }

    private void injectRightSecondChildComputable(AbstractNode secondChildNode) {
        //TODO: Implement
    }

    private double calculateArea(AbstractNode node) {
        return node.getSubtreeHeight() * node.getSubtreeWidth();
    }

    private void calculateBalancedSubTreeDimensions(AbstractNode firstChild) {
        var areaComparator = Comparator.comparingDouble((AbstractNode node) -> (node.getSubtreeWidth()) * (node.getSubtreeHeight()));
        firstChild.getChildren().sort(areaComparator);

        double leftAccumulatedArea = 0;
        double rightAccumulatedArea = 0;

        for (var child : firstChild.getChildren()) {
            if (leftAccumulatedArea <= rightAccumulatedArea) {
                injectLeftSecondChildComputable(child);
                leftAccumulatedArea += calculateArea(child);
            } else {
                injectRightSecondChildComputable(child);
                rightAccumulatedArea += calculateArea(child);
            }
        }
    }
}
