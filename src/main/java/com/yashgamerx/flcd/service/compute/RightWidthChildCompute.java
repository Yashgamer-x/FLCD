package com.yashgamerx.flcd.service.compute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.inject.ComputeInjectable;
import com.yashgamerx.flcd.service.compute.inject.RightHeightChildComputeInjector;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;

import static com.yashgamerx.flcd.model.AbstractNode.*;

public class RightWidthChildCompute implements Computable {
    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();
    private final ComputeInjectable computeInjector = new RightHeightChildComputeInjector();

    @Override
    public void compute(AbstractNode widthNode) {
        if (emptyListChecker.isEmpty(widthNode.getChildren())) return;

        double myAngle = widthNode.getLocalRadianAngle(); // Direction pointing into this node

        // Turn right by -90 degrees (-π/2 radians) relative to parent's angle vector orientation
        double childAngleTrajectory = myAngle + (Math.PI / 2.0);

        // Clearance offset length: (Parent Radius: 5.0) + WIDTH_SPACER + (Child Radius: 5.0)
        double forwardStepLength = NODE_RADIUS + WIDTH_SPACER + NODE_RADIUS;

        // Establish structural anchor base coordinates right at the physical edge of this node
        // Inverting vertical vector directions (subtraction) to map to standard UI display systems
        double anchorX = widthNode.getGridX() + (forwardStepLength * Math.cos(myAngle));
        double anchorY = widthNode.getGridY() - (forwardStepLength * Math.sin(myAngle));

        childrenComputationBasedOnAnchorAndTrajectory(widthNode, anchorX, anchorY, childAngleTrajectory);
    }

    private void childrenComputationBasedOnAnchorAndTrajectory(AbstractNode widthNode, double anchorX,
                                                               double anchorY, double childAngleTrajectory) {

        // Clearance offset Width: (Parent Radius: 5.0) + HEIGHT_SPACER
        double generalOffset = NODE_RADIUS + HEIGHT_SPACER;

        for (var heightChild : widthNode.getChildren()) {
            if (!(heightChild.getComputable() instanceof RootifiedCompute)) {
                generalOffset = projectChildAlongTheVector(heightChild, anchorX, anchorY, childAngleTrajectory, generalOffset);
            } else {
                generalOffset = projectRootifiedChildAlongTheVector(heightChild, anchorX, anchorY, childAngleTrajectory, generalOffset);
            }
        }
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
        return generalOffset + heightChild.getSubtreeWidth() + WIDTH_SPACER;
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
