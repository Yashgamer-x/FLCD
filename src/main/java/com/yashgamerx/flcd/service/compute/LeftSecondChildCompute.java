package com.yashgamerx.flcd.service.compute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.inject.ComputeInjectable;
import com.yashgamerx.flcd.service.compute.inject.factory.ComputeInjectorFactory;
import com.yashgamerx.flcd.service.compute.inject.factory.ComputeInjectorOption;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;

import static com.yashgamerx.flcd.model.AbstractNode.*;

public class LeftSecondChildCompute implements Computable {

    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();
    private final ComputeInjectable computeInjector;

    public LeftSecondChildCompute(ComputeInjectorFactory computeInjectorFactory) {
        this.computeInjector = computeInjectorFactory.getInjector(ComputeInjectorOption.LEFT_HEIGHT_CHILD);
    }

    @Override
    public void compute(AbstractNode secondChild) {
        if (emptyListChecker.isEmpty(secondChild.getChildren())) return;

        double myAngle = secondChild.getLocalRadianAngle(); // Direction pointing into this node

        // Turn right by -90 degrees (-π/2 radians) relative to parent's angle vector orientation
        double childAngleTrajectory = myAngle + (Math.PI / 2.0);

        // Clearance offset length: (Parent Radius: 5.0) + WIDTH_SPACER + (Child Radius: 5.0)
        double forwardStepLength = NODE_RADIUS + WIDTH_SPACER + NODE_RADIUS;

        // Establish structural anchor base coordinates right at the physical edge of this node
        // Inverting vertical vector directions (subtraction) to map to standard UI display systems
        double anchorX = secondChild.getGridX() - (forwardStepLength * Math.cos(myAngle));
        double anchorY = secondChild.getGridY() + (forwardStepLength * Math.sin(myAngle));

        childrenComputationBasedOnAnchorAndTrajectory(secondChild, anchorX, anchorY, childAngleTrajectory);
    }

    private void childrenComputationBasedOnAnchorAndTrajectory(AbstractNode secondChild, double anchorX, double anchorY, double childAngleTrajectory) {
        // Clearance offset Height: (Parent Radius: 5.0) + HEIGHT_SPACER
        double generalOffset = NODE_RADIUS + HEIGHT_SPACER;

        for (var heightChild : secondChild.getChildren()) {
            if (!(heightChild.getComputable() instanceof RootifiedCompute)) {
                generalOffset = projectChildAlongTheVector(heightChild, anchorX, anchorY, childAngleTrajectory, generalOffset);
            } else {
                generalOffset = projectRootifiedChildAlongTheVector(heightChild, anchorX, anchorY, childAngleTrajectory, generalOffset);
            }
        }
    }

    /// Returns updated Offset
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

    /// Returns updated Offset
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

        return generalOffset + heightChild.getSubtreeWidth() + HEIGHT_SPACER;
    }
}
