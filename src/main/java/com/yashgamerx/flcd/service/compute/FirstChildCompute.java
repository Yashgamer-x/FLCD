package com.yashgamerx.flcd.service.compute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;

import static com.yashgamerx.flcd.model.AbstractNode.*;

public class FirstChildCompute implements Computable {

    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();

    @Override
    public void compute(AbstractNode firstChild) {
        // If is leaf node then do nothing
        if (emptyListChecker.isEmpty(firstChild.getChildren())) return;

        double parentAngle = firstChild.getLocalRadianAngle();

        // Perpendicular wing baseline vectors [cite: 86, 88]
        double rightAngle = parentAngle - (Math.PI / 2.0); // -90 degrees relative
        double leftAngle = parentAngle + (Math.PI / 2.0);  // +90 degrees relative

        // Clearance offset length pushing children downstream from parent perimeter:
        // (Parent Radius: 5.0) + HEIGHT_SPACER + (Child Radius: 5.0)
        double forwardStepLength = NODE_RADIUS + HEIGHT_SPACER + NODE_RADIUS;

        // Establish the baseline anchor point directly in front of the parent node
        // Correcting for screen coordinate Y-inversion globally (subtraction for positive Y step)
        double anchorX = firstChild.getGridX() - (forwardStepLength * Math.cos(parentAngle));
        double anchorY = firstChild.getGridY() + (forwardStepLength * Math.sin(parentAngle));

        // Track sequential slide displacements along the wings
        double accumulatedRightDistance = 0.0;
        double accumulatedLeftDistance = 0.0;

        for (var child : firstChild.getChildren()) {
            var computable = child.getComputable();
            if (computable instanceof RightSecondChildCompute) {
                accumulatedRightDistance = projectChildAlongVector(child, anchorX, anchorY, rightAngle, accumulatedRightDistance);
            } else if (computable instanceof LeftSecondChildCompute) {
                accumulatedLeftDistance = projectChildAlongVector(child, anchorX, anchorY, leftAngle, accumulatedLeftDistance);
            }
        }
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

    private double projectRootifiedChildAlongVector(AbstractNode child, double anchorX, double anchorY, double baselineAngle, double currentDistance) {

    }
}
