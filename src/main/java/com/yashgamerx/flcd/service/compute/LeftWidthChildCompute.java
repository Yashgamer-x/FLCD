package com.yashgamerx.flcd.service.compute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.model.NodeStatus;
import com.yashgamerx.flcd.service.angular.Angle180Calculator;
import com.yashgamerx.flcd.service.angular.Angle360Calculator;
import com.yashgamerx.flcd.service.angular.AngularCalculator;
import com.yashgamerx.flcd.service.compute.inject.ComputeInjectable;
import com.yashgamerx.flcd.service.compute.inject.LeftHeightChildComputeInjector;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;
import com.yashgamerx.flcd.service.precompute.FirstChildPreCompute;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.stream.Stream;

import static com.yashgamerx.flcd.model.AbstractNode.*;

@Log
public class LeftWidthChildCompute implements Computable {

    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();
    private final ComputeInjectable computeInjector = new LeftHeightChildComputeInjector();
    private final AngularCalculator angular360Calculator = new Angle360Calculator();
    private final AngularCalculator angular180Calculator = new Angle180Calculator();

    @Override
    public void compute(AbstractNode widthNode) {
        if (emptyListChecker.isEmpty(widthNode.getChildren())) return;

        double myAngle = widthNode.getLocalRadianAngle(); // Direction pointing into this node

        // Turn right by -90 degrees (-π/2 radians) relative to parent's angle vector orientation
        double childAngleTrajectory = myAngle - (Math.PI / 2.0);

        // Clearance offset length: (Parent Radius: 5.0) + WIDTH_SPACER + (Child Radius: 5.0)
        double forwardStepLength = NODE_RADIUS + WIDTH_SPACER + NODE_RADIUS;

        // Establish structural anchor base coordinates right at the physical edge of this node
        // Inverting vertical vector directions (subtraction) to map to standard UI display systems
        double anchorX = widthNode.getGridX() + (forwardStepLength * Math.cos(myAngle));
        double anchorY = widthNode.getGridY() - (forwardStepLength * Math.sin(myAngle));

        childrenComputationBasedOnAnchorAndTrajectory(widthNode, anchorX, anchorY, childAngleTrajectory);
    }

    private void childrenComputationBasedOnAnchorAndTrajectory(AbstractNode secondChild, double anchorX, double anchorY, double childAngleTrajectory) {
        // Initialize tracker as a 1-element primitive wrapper array to support mutations inside lambda
        // Offset Height layout: (Parent Radius: 5.0) + HEIGHT_SPACER
        double[] trackingOffset = {NODE_RADIUS + HEIGHT_SPACER};

        // 2. Isolate into dynamic status collections
        var normalChildren = secondChild.getChildren()
                .stream()
                .filter(child -> child.getStatus() == NodeStatus.NORMAL)
                .toArray(AbstractNode[]::new);

        var rootifiedChildren = secondChild.getChildren()
                .stream()
                .filter(child -> child.getStatus() == NodeStatus.ROOTIFIED)
                .toArray(AbstractNode[]::new);

        var readjustedNodes = secondChild.getChildren().stream()
                .filter(node -> node.getStatus() == NodeStatus.READJUSTED)
                .toArray(AbstractNode[]::new);

        // Concatenate and execute sequentially
        Stream.of(rootifiedChildren, normalChildren, readjustedNodes)
                .flatMap(Arrays::stream)
                .forEach(heightChild -> {
                    if (!(heightChild.getComputable() instanceof RootifiedCompute)) {
                        trackingOffset[0] = projectChildAlongTheVector(
                                heightChild, anchorX, anchorY, childAngleTrajectory, trackingOffset[0]
                        );
                    } else {
                        trackingOffset[0] = projectRootifiedChildAlongTheVector(
                                heightChild, anchorX, anchorY, childAngleTrajectory, trackingOffset[0]
                        );
                    }

                    if (isReadjustable(heightChild)) {
                        log.info("Readjusted Node: " + heightChild.getIdentifier());
                        readjustNode(heightChild);
                    }
                });
    }

    private boolean isReadjustable(AbstractNode node) {
        return node.getChildren().isEmpty() && node.getStatus() == NodeStatus.READJUSTED;
    }

    private boolean isAncestorRootified(AbstractNode rootNode) {
        return rootNode.getStatus() == NodeStatus.ROOTIFIED;
    }

    private void readjustNode(AbstractNode readjustableNode) {

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
        double angularStep = isAncestorRootified(rootNode) ?
                angular180Calculator.calculate(totalChildren) :
                angular360Calculator.calculate(totalChildren);

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

        double halfAngularStep = (angularStep / 2.0);
        double newAngle = firstChild.getLocalRadianAngle() + halfAngularStep;

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

    private double projectChildAlongTheVector(AbstractNode heightChild, double anchorX, double anchorY,
                                              double childAngleTrajectory, double generalOffset) {
        // Map polar placement vectors into Cartesian screen coordinate tracking states

        // Offset is based on the previous spacings + NODE_RADIUS
        // to get to the enter point of the node.
        var centerPoint = generalOffset + NODE_RADIUS;

        double childX = anchorX + (centerPoint * Math.cos(childAngleTrajectory));
        double childY = anchorY - (centerPoint * Math.sin(childAngleTrajectory));

        heightChild.setGridX(childX);
        heightChild.setGridY(childY);

        // Forward the calculated absolute orientation down-chain so descendants can follow the vector
        heightChild.setLocalRadianAngle(childAngleTrajectory);

        computeInjector.inject(heightChild);

        // Execute recursive cascading calls down to children to expand layout structures
        heightChild.compute();

        // Whatever the previous offset was + the entire height of the subtree + the height spacer
        // The spacer is given so that the next one does not need to add its own spacer.
        return generalOffset + heightChild.getSubtreeHeight() + HEIGHT_SPACER;
    }

    private double projectRootifiedChildAlongTheVector(AbstractNode heightChild, double anchorX, double anchorY,
                                                       double childAngleTrajectory, double generalOffset) {
        // Map polar placement vectors into Cartesian screen coordinate tracking states

        var centerPoint = generalOffset + (heightChild.getSubtreeWidth() / 2);

        double childX = anchorX + (centerPoint * Math.cos(childAngleTrajectory));
        double childY = anchorY - (centerPoint * Math.sin(childAngleTrajectory));

        heightChild.setGridX(childX);
        heightChild.setGridY(childY);

        // Forward the calculated absolute orientation down-chain so descendants can follow the vector
        heightChild.setLocalRadianAngle(childAngleTrajectory + (Math.PI / 2));

        computeInjector.inject(heightChild);

        // Execute recursive cascading calls down to children to expand layout structures
        heightChild.compute();

        return centerPoint + (heightChild.getSubtreeWidth() / 2) + WIDTH_SPACER;
    }
}
