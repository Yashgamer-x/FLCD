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

        // Injects SecondChildPreCompute dependency and Precomputes all the children
        firstChild.getChildren().forEach(this::injectAndPrecompute);

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
        //TODO: Implement
    }

    private void injectRightSecondChildComputable(AbstractNode secondChildNode) {
        //TODO: Implement
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
