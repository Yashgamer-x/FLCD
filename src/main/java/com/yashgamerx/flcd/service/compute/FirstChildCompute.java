package com.yashgamerx.flcd.service.compute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.model.NodeStatus;
import com.yashgamerx.flcd.service.angular.Angle360Calculator;
import com.yashgamerx.flcd.service.angular.AngularCalculator;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;
import com.yashgamerx.flcd.service.precompute.FirstChildPreCompute;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.yashgamerx.flcd.model.AbstractNode.*;

@Log
public class FirstChildCompute implements Computable {

    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();
    private final AngularCalculator angularCalculator = new Angle360Calculator();

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
                .sorted(Comparator.comparingInt(AbstractNode::getDepth))
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
                        if (isReadjustable(child)) {
                            log.info("Readjusted Node: " + child.getIdentifier());
                            readjustNode(child, true);
                        }
                    } else if (computable instanceof LeftSecondChildCompute) {
                        accumulatedDistances[1] = projectChildAlongVector(
                                child, anchorX, anchorY, leftAngle, accumulatedDistances[1]
                        );
                        if (isReadjustable(child)) {
                            log.info("Readjusted Node: " + child.getIdentifier());
                            readjustNode(child, false);
                        }
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

    private boolean isReadjustable(AbstractNode node) {
        return node.getChildren().isEmpty() && node.getStatus() == NodeStatus.READJUSTED;
    }

    private void readjustNode(AbstractNode readjustableNode, boolean isRightNode) {

        // A = readjustableNode
        double ax = readjustableNode.getGridX();
        double ay = readjustableNode.getGridY();

        // Find C (first child)
        AbstractNode firstChild = readjustableNode.getParent();
        while (!(firstChild.getPrecomputable() instanceof FirstChildPreCompute)) {
            firstChild = firstChild.getParent();
        }

        double cx = firstChild.getGridX();
        double cy = firstChild.getGridY();

        // Find B (root/rootified node)
        AbstractNode rootNode = firstChild.getParent();

        double bx = rootNode.getGridX();
        double by = rootNode.getGridY();

        int totalChildren = rootNode.getChildren().size();
        double angularStep = angularCalculator.calculate(totalChildren);

        // --------------------------------------------------
        // Compute perpendicular distance from A to line BC
        // --------------------------------------------------

        double bcx = cx - bx;
        double bcy = cy - by;

        double bcLength = Math.hypot(bcx, bcy);

        double ux = bcx / bcLength;
        double uy = bcy / bcLength;

        double bax = ax - bx;
        double bay = ay - by;

        double projection = bax * ux + bay * uy;

        double px = bx + projection * ux;
        double py = by + projection * uy;

        double opposite = Math.hypot(ax - px, ay - py);

        // --------------------------------------------------
        // Compute new angle
        // --------------------------------------------------

        var negation = isRightNode ? -1.0 : 1.0;

        double halfAngularStep = (angularStep / 2.0);
        double newAngle = firstChild.getLocalRadianAngle() + negation * halfAngularStep;

        // --------------------------------------------------
        // Compute new radius from root
        // --------------------------------------------------

        double radius = opposite / Math.sin(halfAngularStep);

        // --------------------------------------------------
        // Compute new coordinates for A
        // --------------------------------------------------

        double newAx = bx + radius * Math.cos(newAngle);
        double newAy = by - radius * Math.sin(newAngle);

        double newNodeRadius = NODE_RADIUS / Math.sin(halfAngularStep);
        double readjustedAx = newAx + newNodeRadius * Math.cos(firstChild.getLocalRadianAngle());
        double readjustedAy = newAy - newNodeRadius * Math.sin(firstChild.getLocalRadianAngle());

        readjustableNode.setGridX(readjustedAx);
        readjustableNode.setGridY(readjustedAy);
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
