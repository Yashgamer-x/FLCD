package com.yashgamerx.flcd.service.compute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.model.NodeStatus;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;

import java.util.Arrays;
import java.util.stream.Stream;

import static com.yashgamerx.flcd.model.AbstractNode.*;

public class FirstChildCompute implements Computable {

    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();

    @Override
    public void compute(AbstractNode firstChild) {
        // Structural check: Early exit if there are no children to position
        if (emptyListChecker.isEmpty(firstChild.getChildren())) return;

        double parentAngle = firstChild.getLocalRadianAngle();

        // Perpendicular wing baseline vectors
        double rightAngle = parentAngle - (Math.PI / 2.0); // -90 degrees relative
        double leftAngle = parentAngle + (Math.PI / 2.0);  // +90 degrees relative
        double additionalAngle = Math.PI / 2.0;

        // Clearance offset length pushing children downstream from parent perimeter
        double forwardStepLength = NODE_RADIUS + HEIGHT_SPACER + NODE_RADIUS;

        // Establish the baseline anchor point directly in front of the parent node
        // Correcting for screen coordinate Y-inversion globally (subtraction for positive Y step)
        double anchorX = firstChild.getGridX() - (forwardStepLength * Math.cos(parentAngle));
        double anchorY = firstChild.getGridY() + (forwardStepLength * Math.sin(parentAngle));

        // Primitive wrapper array to allow safe state tracking mutations inside the Stream lambda body:
        // Index [0] = accumulatedRightDistance, Index [1] = accumulatedLeftDistance
        double[] accumulatedDistances = {0.0, 0.0};

        // Separate into status buckets from the sorted list
        AbstractNode[] normalNodes = firstChild.getChildren().stream()
                .filter(node -> node.getStatus() == NodeStatus.NORMAL)
                .toArray(AbstractNode[]::new);

        AbstractNode[] rootifiedNodes = firstChild.getChildren().stream()
                .filter(node -> node.getStatus() == NodeStatus.ROOTIFIED)
                .toArray(AbstractNode[]::new);

        AbstractNode[] readjustedNodes = firstChild.getChildren().stream()
                .filter(node -> node.getStatus() == NodeStatus.READJUSTED)
                .toArray(AbstractNode[]::new);

        // Recombine and execute sequentially inside a clean forEach pipeline
        Stream.of(readjustedNodes, normalNodes, rootifiedNodes)
                .flatMap(Arrays::stream)
                .forEach(child -> {
                    var computable = child.getComputable();

                    if (computable instanceof RightSecondChildCompute) {
                        accumulatedDistances[0] = projectChildAlongVector(
                                child, anchorX, anchorY, rightAngle, accumulatedDistances[0]
                        );
                    } else if (computable instanceof LeftSecondChildCompute) {
                        accumulatedDistances[1] = projectChildAlongVector(
                                child, anchorX, anchorY, leftAngle, accumulatedDistances[1]
                        );
                    } else if (computable instanceof LeftSecondRootifiedCompute) {
                        accumulatedDistances[1] = projectRootifiedChildAlongVector(
                                child, anchorX, anchorY, leftAngle, additionalAngle, accumulatedDistances[1]
                        );
                    } else if (computable instanceof RightSecondRootifiedCompute) {
                        accumulatedDistances[0] = projectRootifiedChildAlongVector(
                                child, anchorX, anchorY, rightAngle, -additionalAngle, accumulatedDistances[0]
                        );
                    }
                });
    }


    /// Positions a child sequentially along a wing baseline and cascades the true angle down-chain.
    private double projectChildAlongVector(AbstractNode child, double anchorX, double anchorY,
                                           double baselineAngle, double currentDistance) {

        // Entire subtree - (Radius of Circle: 5.0) to get the exact point of
        double newDistance = currentDistance + WIDTH_SPACER + child.getSubtreeWidth();

        double centerPoint = newDistance - NODE_RADIUS;

        // Polar layout projection mapped to Cartesian grid space
        // Corrected Y calculation to handle screen inversion seamlessly
        double childX = anchorX + (centerPoint * Math.cos(baselineAngle));
        double childY = anchorY - (centerPoint * Math.sin(baselineAngle));

        child.setGridX(childX);
        child.setGridY(childY);

        // Assign the true baseline angle to the child so its nested sub-elements
        // know which vector they are traveling along when they process their own compute passes!
        child.setLocalRadianAngle(baselineAngle);

        // Process nested layout branches recursively
        child.compute();

        // Return current tail boundary for the next sibling layout spacing step
        return newDistance;
    }

    private double projectRootifiedChildAlongVector(AbstractNode child, double anchorX, double anchorY,
                                                    double baselineAngle, double additionalAngle, double currentDistance) {
        // Entire subtree - (Radius of Circle: 5.0) to get the exact point of
        double centerPoint = currentDistance + WIDTH_SPACER + child.getSubtreeWidth() / 2;

        // Polar layout projection mapped to Cartesian grid space
        // Corrected Y calculation to handle screen inversion seamlessly
        double childX = anchorX + (centerPoint * Math.cos(baselineAngle));
        double childY = anchorY - (centerPoint * Math.sin(baselineAngle));

        child.setGridX(childX);
        child.setGridY(childY);

        // Assign the true baseline angle to the child so its nested sub-elements
        // know which vector they are traveling along when they process their own compute passes!
        child.setLocalRadianAngle(baselineAngle + additionalAngle);

        // Process nested layout branches recursively
        child.compute();

        // Return current tail boundary for the next sibling layout spacing step
        return centerPoint + child.getSubtreeWidth() / 2;
    }
}
