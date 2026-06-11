package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.inject.ComputeInjectable;
import com.yashgamerx.flcd.service.compute.inject.LeftSecondChildComputeInjector;
import com.yashgamerx.flcd.service.compute.inject.RightSecondChildComputeInjector;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;
import com.yashgamerx.flcd.service.precompute.inject.PrecomputeInjectable;
import com.yashgamerx.flcd.service.precompute.inject.SecondChildPreComputeInjector;
import lombok.extern.java.Log;

import java.util.Comparator;

import static com.yashgamerx.flcd.model.AbstractNode.*;

@Log
public class FirstChildWithReadjustmentModificationPreCompute implements Precomputable {

    private final PrecomputeInjectable injectable = new SecondChildPreComputeInjector();
    private final ComputeInjectable rightComputeInjector = new RightSecondChildComputeInjector();
    private final ComputeInjectable leftComputeInjector = new LeftSecondChildComputeInjector();
    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();

    @Override
    public void precompute(AbstractNode firstChild) {
        // Checks if the node has no children
        if (emptyListChecker.isEmpty(firstChild.getChildren())) {
            initializeLeafDimensions(firstChild);
            return;
        }

        // Injects SecondChildPreCompute dependency and Precomputes all the children
        firstChild.getChildren().forEach(this::injectAndPrecompute);

        calculateBalancedSubTreeDimensions(firstChild);
    }

    /// Initializes leaf node dimensions
    private void initializeLeafDimensions(AbstractNode node) {
        node.setSubtreeWidth(NODE_DIAMETER);
        node.setSubtreeHeight(NODE_DIAMETER);
    }

    /// Injects SecondChildPreCompute dependency and Precomputes the child
    private void injectAndPrecompute(AbstractNode secondChildNode) {
        injectable.inject(secondChildNode);
        secondChildNode.precompute();
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
                leftComputeInjector.inject(secondChild);
                leftAccumulatedArea += calculateArea(secondChild);
                leftWidth += secondChild.getSubtreeWidth() + WIDTH_SPACER;
                maxLeftHeight = Math.max(maxLeftHeight, secondChild.getSubtreeHeight());
            } else {
                rightComputeInjector.inject(secondChild);
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