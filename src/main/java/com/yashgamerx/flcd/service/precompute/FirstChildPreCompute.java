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
import java.util.stream.Stream;

import static com.yashgamerx.flcd.model.AbstractNode.*;

@Log
public class FirstChildPreCompute implements Precomputable {

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
        Stream.concat(firstChild.getChildren().stream(), firstChild.getRootifiedChildren().stream())
                .forEach(this::injectAndPrecompute);

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
        // 1. Sort collections using a descriptive reusable comparator
        var areaDescending = Comparator.comparingDouble(this::calculateArea).reversed();
        firstChild.getChildren().sort(areaDescending);
        firstChild.getRootifiedChildren().sort(areaDescending);

        // 2. Initialize tracking states for the greedy wing-balancing heuristic
        SideState left = new SideState();
        SideState right = new SideState();

        // 3. Process all target children through a unified processing stream
        Stream.concat(firstChild.getChildren().stream(), firstChild.getRootifiedChildren().stream())
                .forEach(secondChild -> {
                    // Balance dynamically on whichever side currently contains less area
                    if (left.accumulatedArea <= right.accumulatedArea) {
                        processChildPlacement(secondChild, left, leftComputeInjector);
                    } else {
                        processChildPlacement(secondChild, right, rightComputeInjector);
                    }
                });

        // 4. Resolve final composite boundaries
        double maxSideWidth = Math.max(left.width, right.width);
        double totalWidth = (maxSideWidth * 2) + NODE_DIAMETER;

        firstChild.setSubtreeWidth(Math.max(NODE_DIAMETER, totalWidth));

        double maxChildHeight = Math.max(left.maxHeight, right.maxHeight);
        firstChild.setSubtreeHeight(NODE_DIAMETER + HEIGHT_SPACER + maxChildHeight);
    }

    /**
     * Helper method to handle injection and boundary state accumulation for a given child node.
     */
    private void processChildPlacement(AbstractNode child, SideState side, ComputeInjectable injector) {
        injector.inject(child);
        side.accumulatedArea += calculateArea(child);
        side.width += child.getSubtreeWidth() + WIDTH_SPACER;
        side.maxHeight = Math.max(side.maxHeight, child.getSubtreeHeight());
    }

    /**
     * Light mutable structural container tracking a layout wing's current dimensions.
     */
    private static class SideState {
        double accumulatedArea = 0;
        double width = 0;
        double maxHeight = 0;
    }
}
