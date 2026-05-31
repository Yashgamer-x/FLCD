package com.yashgamerx.flcd.service.compute;

import com.yashgamerx.flcd.model.AbstractNode;

import static com.yashgamerx.flcd.model.AbstractNode.*;

public class LeftSecondChildCompute implements Computable {
    @Override
    public void compute(AbstractNode secondChild) {
        if (isLeafNode(secondChild)) return;

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

    private boolean isLeafNode(AbstractNode secondChild) {
        return secondChild.getChildren() == null || secondChild.getChildren().isEmpty();
    }

    private void injectLeftHeightChildCompute(AbstractNode heightChild) {
        heightChild.setComputable(new LeftHeightChildCompute());
    }

    private void childrenComputationBasedOnAnchorAndTrajectory(AbstractNode secondChild, double anchorX, double anchorY, double childAngleTrajectory) {
        // Clearance offset Height: (Parent Radius: 5.0) + HEIGHT_SPACER + (Child Radius: 5.0)
        double runningStackedHeightDistance = NODE_RADIUS + HEIGHT_SPACER + NODE_RADIUS;

        for (var heightChild : secondChild.getChildren()) {
            // Map polar placement vectors into Cartesian screen coordinate tracking states
            double childX = anchorX + (runningStackedHeightDistance * Math.cos(childAngleTrajectory));
            double childY = anchorY - (runningStackedHeightDistance * Math.sin(childAngleTrajectory));

            heightChild.setGridX(childX);
            heightChild.setGridY(childY);

            // Forward the calculated absolute orientation down-chain so descendants can follow the vector
            heightChild.setLocalRadianAngle(childAngleTrajectory);

            injectLeftHeightChildCompute(heightChild);

            // Execute recursive cascading calls down to children to expand layout structures
            heightChild.compute();

            // runningStackedHeightDistance  = runningStackedHeightDistance + child.subtreeHeight + HEIGHT_SPACER + (Child Radius: 5.0)
            runningStackedHeightDistance += heightChild.getSubtreeHeight() + HEIGHT_SPACER;
        }
    }
}
