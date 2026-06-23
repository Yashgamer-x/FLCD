package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.model.NodeStatus;
import com.yashgamerx.flcd.service.compute.inject.ComputeInjectable;
import com.yashgamerx.flcd.service.compute.inject.LeftSecondChildComputeInjector;
import com.yashgamerx.flcd.service.compute.inject.RightSecondChildComputeInjector;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;
import com.yashgamerx.flcd.service.precompute.inject.PrecomputeInjectable;
import com.yashgamerx.flcd.service.precompute.inject.SecondChildPreComputeInjector;
import lombok.extern.java.Log;

import java.util.Arrays;
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
        firstChild.incrementDepth();
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
        var areaComparator = Comparator.comparingDouble(this::calculateArea);
        // Sort the original collection before filtering down into specific status buckets
        firstChild.getChildren().sort(areaComparator.reversed());

        // Local state tracking context layout:
        // [0] leftAccumulatedArea, [1] rightAccumulatedArea
        // [2] leftWidth,           [3] rightWidth
        // [4] maxLeftHeight,       [5] maxRightHeight
        double[] metrics = new double[6];

        // Separate the nodes dynamically by status arrays
        AbstractNode[] normalNodes = firstChild.getChildren().stream()
                .filter(node -> node.getStatus() == NodeStatus.NORMAL)
                .toArray(AbstractNode[]::new);

        AbstractNode[] rootifiedNodes = firstChild.getChildren().stream()
                .filter(node -> node.getStatus() == NodeStatus.ROOTIFIED)
                .toArray(AbstractNode[]::new);

        AbstractNode[] readjustedNodes = firstChild.getChildren().stream()
                .filter(node -> node.getStatus() == NodeStatus.READJUSTED)
                .toArray(AbstractNode[]::new);

        // Process combined elements sequentially: Normal first, then Rootified
        Stream.of(readjustedNodes, normalNodes, rootifiedNodes)
                .flatMap(Arrays::stream)
                .forEach(secondChild -> {
                    double nodeArea = calculateArea(secondChild);

                    // Balanced greedy layout heuristic allocation step
                    if (metrics[0] <= metrics[1]) { // leftAccumulatedArea <= rightAccumulatedArea
                        leftComputeInjector.inject(secondChild);
                        metrics[0] += nodeArea;                                                // leftAccumulatedArea
                        metrics[2] += secondChild.getSubtreeWidth() + WIDTH_SPACER;            // leftWidth
                        metrics[4] = Math.max(metrics[4], secondChild.getSubtreeHeight());     // maxLeftHeight
                    } else {
                        rightComputeInjector.inject(secondChild);
                        metrics[1] += nodeArea;                                                // rightAccumulatedArea
                        metrics[3] += secondChild.getSubtreeWidth() + WIDTH_SPACER;            // rightWidth
                        metrics[5] = Math.max(metrics[5], secondChild.getSubtreeHeight());     // maxRightHeight
                    }
                });

        // 3. Extract finalized measurements for layout assignment
        double leftWidth = metrics[2];
        double rightWidth = metrics[3];
        double maxLeftHeight = metrics[4];
        double maxRightHeight = metrics[5];

        double maxSideWidth = Math.max(leftWidth, rightWidth);

        // Max Width * 2 + (Node Diameter: 10.0)
        double totalWidth = (maxSideWidth * 2) + NODE_DIAMETER;
        firstChild.setSubtreeWidth(Math.max(NODE_DIAMETER, totalWidth));

        // Height calculation remains the same: this node + vertical gap + tallest child + SPACER (between Root and First Child)
        firstChild.setSubtreeHeight(NODE_DIAMETER + HEIGHT_SPACER + Math.max(maxLeftHeight, maxRightHeight));
    }
}
