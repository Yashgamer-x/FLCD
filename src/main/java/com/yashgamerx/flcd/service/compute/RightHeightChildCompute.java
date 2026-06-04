package com.yashgamerx.flcd.service.compute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.inject.ComputeInjectable;
import com.yashgamerx.flcd.service.compute.inject.RightWidthChildComputeInjector;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;

import static com.yashgamerx.flcd.model.AbstractNode.*;

public class RightHeightChildCompute implements Computable {
    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();
    private final ComputeInjectable computeInjector = new RightWidthChildComputeInjector();

    @Override
    public void compute(AbstractNode heightChild) {
        if (emptyListChecker.isEmpty(heightChild.getChildren())) return;

        double myAngle = heightChild.getLocalRadianAngle(); // Direction pointing into this node

        // Turn right by -90 degrees (-π/2 radians) relative to parent's angle vector orientation
        double childAngleTrajectory = myAngle - (Math.PI / 2.0);

        // Clearance offset length: (Parent Radius: 5.0) + HEIGHT_SPACER + (Child Radius: 5.0)
        double forwardStepLength = NODE_RADIUS + HEIGHT_SPACER + NODE_RADIUS;

        // Establish structural anchor base coordinates right at the physical edge of this node
        // Inverting vertical vector directions (subtraction) to map to standard UI display systems
        double anchorX = heightChild.getGridX() + (forwardStepLength * Math.cos(myAngle));
        double anchorY = heightChild.getGridY() - (forwardStepLength * Math.sin(myAngle));

        childrenComputationBasedOnAnchorAndTrajectory(heightChild, anchorX, anchorY, childAngleTrajectory);
    }

    private void childrenComputationBasedOnAnchorAndTrajectory(AbstractNode heightChild, double anchorX, double anchorY, double childAngleTrajectory) {
        // Clearance offset Width: (Parent Radius: 5.0) + WIDTH_SPACER
        double generalOffset = NODE_RADIUS + WIDTH_SPACER;

        for (var widthChild : heightChild.getChildren()) {
            if (!(widthChild.getComputable() instanceof RootifiedCompute)) {
                generalOffset = projectChildAlongTheVector(widthChild, anchorX, anchorY, childAngleTrajectory, generalOffset);
            } else {
                generalOffset = projectRootifiedChildAlongTheVector(widthChild, anchorX, anchorY, childAngleTrajectory, generalOffset);
            }
        }
    }

    /// Returns updated Offset
    private double projectChildAlongTheVector(AbstractNode widthChild, double anchorX, double anchorY,
                                              double childAngleTrajectory, double generalOffset) {
        // Map polar placement vectors into Cartesian screen coordinate tracking states

        // Offset is based on the previous spacings + NODE_RADIUS
        // to get to the enter point of the node.
        var centerPoint = generalOffset + NODE_RADIUS;

        double childX = anchorX + (centerPoint * Math.cos(childAngleTrajectory));
        double childY = anchorY - (centerPoint * Math.sin(childAngleTrajectory));

        widthChild.setGridX(childX);
        widthChild.setGridY(childY);

        // Forward the calculated absolute orientation down-chain so descendants can follow the vector
        widthChild.setLocalRadianAngle(childAngleTrajectory);

        computeInjector.inject(widthChild);

        // Execute recursive cascading calls down to children to expand layout structures
        widthChild.compute();

        // Whatever the previous offset was + the entire height of the subtree + the height spacer
        // The spacer is given so that the next one does not need to add its own spacer.
        return generalOffset + widthChild.getSubtreeWidth() + WIDTH_SPACER;
    }

    /// Returns updated Offset
    private double projectRootifiedChildAlongTheVector(AbstractNode widthChild, double anchorX, double anchorY,
                                                       double childAngleTrajectory, double generalOffset) {
        // Map polar placement vectors into Cartesian screen coordinate tracking states

        var centerPoint = generalOffset + (widthChild.getSubtreeWidth() / 2);

        double childX = anchorX + (centerPoint * Math.cos(childAngleTrajectory));
        double childY = anchorY - (centerPoint * Math.sin(childAngleTrajectory));

        widthChild.setGridX(childX);
        widthChild.setGridY(childY);

        // Forward the calculated absolute orientation down-chain so descendants can follow the vector
        widthChild.setLocalRadianAngle(childAngleTrajectory + (Math.PI / 2));

        computeInjector.inject(widthChild);

        // Execute recursive cascading calls down to children to expand layout structures
        widthChild.compute();

        return centerPoint + (widthChild.getSubtreeWidth() / 2) + WIDTH_SPACER;
    }
}
