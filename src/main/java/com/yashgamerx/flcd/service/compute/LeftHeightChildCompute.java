package com.yashgamerx.flcd.service.compute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.inject.ComputeInjectable;
import com.yashgamerx.flcd.service.compute.inject.LeftWidthChildComputeInjector;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;

import static com.yashgamerx.flcd.model.AbstractNode.*;

public class LeftHeightChildCompute implements Computable {
    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();
    private final ComputeInjectable computeInjector = new LeftWidthChildComputeInjector();

    @Override
    public void compute(AbstractNode heightChild) {
        if (emptyListChecker.isEmpty(heightChild.getChildren())) return;

        double myAngle = heightChild.getLocalRadianAngle(); // Direction pointing into this node

        // Turn right by -90 degrees (-π/2 radians) relative to parent's angle vector orientation
        double childAngleTrajectory = myAngle + (Math.PI / 2.0);

        // Clearance offset length: (Parent Radius: 5.0) + HEIGHT_SPACER + (Child Radius: 5.0)
        double forwardStepLength = NODE_RADIUS + HEIGHT_SPACER + NODE_RADIUS;

        // Establish structural anchor base coordinates right at the physical edge of this node
        // Inverting vertical vector directions (subtraction) to map to standard UI display systems
        double anchorX = heightChild.getGridX() + (forwardStepLength * Math.cos(myAngle));
        double anchorY = heightChild.getGridY() - (forwardStepLength * Math.sin(myAngle));

        childrenComputationBasedOnAnchorAndTrajectory(heightChild, anchorX, anchorY, childAngleTrajectory);
    }

    private void childrenComputationBasedOnAnchorAndTrajectory(AbstractNode heightChild, double anchorX, double anchorY, double childAngleTrajectory) {
        // Clearance offset Height: (Parent Radius: 5.0) + HEIGHT_SPACER + (Child Radius: 5.0)
        double runningStackedWidthDistance = NODE_RADIUS + WIDTH_SPACER + NODE_RADIUS;

        for (var widthChild : heightChild.getChildren()) {
            // Map polar placement vectors into Cartesian screen coordinate tracking states
            double childX = anchorX + (runningStackedWidthDistance * Math.cos(childAngleTrajectory));
            double childY = anchorY - (runningStackedWidthDistance * Math.sin(childAngleTrajectory));

            widthChild.setGridX(childX);
            widthChild.setGridY(childY);

            // Forward the calculated absolute orientation down-chain so descendants can follow the vector
            widthChild.setLocalRadianAngle(childAngleTrajectory);

            computeInjector.inject(widthChild);

            // Execute recursive cascading calls down to children to expand layout structures
            widthChild.compute();

            // runningStackedHeightDistance  = runningStackedHeightDistance + rightWidthNode.subtreeHeight + HEIGHT_SPACER + (Child Radius: 5.0)
            runningStackedWidthDistance += widthChild.getSubtreeWidth() + WIDTH_SPACER;
        }
    }
}
