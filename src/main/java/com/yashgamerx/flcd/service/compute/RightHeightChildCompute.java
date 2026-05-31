package com.yashgamerx.flcd.service.compute;

import com.yashgamerx.flcd.model.AbstractNode;

import static com.yashgamerx.flcd.model.AbstractNode.HEIGHT_SPACER;
import static com.yashgamerx.flcd.model.AbstractNode.WIDTH_SPACER;

public class RightHeightChildCompute implements Computable {
    @Override
    public void compute(AbstractNode heightChild) {
        if (isLeafNode(heightChild)) return;

        double myAngle = heightChild.getLocalRadianAngle(); // Direction pointing into this node

        // Turn right by -90 degrees (-π/2 radians) relative to parent's angle vector orientation
        double childAngleTrajectory = myAngle - (Math.PI / 2.0);

        // Clearance offset length: (Parent Radius: 5.0) + HEIGHT_SPACER + (Child Radius: 5.0)
        double forwardStepLength = 5.0 + HEIGHT_SPACER + 5.0;

        // Establish structural anchor base coordinates right at the physical edge of this node
        // Inverting vertical vector directions (subtraction) to map to standard UI display systems
        double anchorX = heightChild.getGridX() + (forwardStepLength * Math.cos(myAngle));
        double anchorY = heightChild.getGridY() - (forwardStepLength * Math.sin(myAngle));

        childrenComputationBasedOnAnchorAndTrajectory(heightChild, anchorX, anchorY, childAngleTrajectory);
    }

    private boolean isLeafNode(AbstractNode heightChild) {
        return heightChild.getChildren() == null || heightChild.getChildren().isEmpty();
    }

    private void injectRightWidthChildCompute(AbstractNode widthChild) {
        widthChild.setComputable(new RightWidthChildCompute());
    }

    private void childrenComputationBasedOnAnchorAndTrajectory(AbstractNode heightChild, double anchorX, double anchorY, double childAngleTrajectory) {
        // Clearance offset Height: (Parent Radius: 5.0) + HEIGHT_SPACER + (Child Radius: 5.0)
        double runningStackedWidthDistance = 5.0 + WIDTH_SPACER + 5.0;

        for (var widthChild : heightChild.getChildren()) {
            // Map polar placement vectors into Cartesian screen coordinate tracking states
            double childX = anchorX + (runningStackedWidthDistance * Math.cos(childAngleTrajectory));
            double childY = anchorY - (runningStackedWidthDistance * Math.sin(childAngleTrajectory));

            widthChild.setGridX(childX);
            widthChild.setGridY(childY);

            // Forward the calculated absolute orientation down-chain so descendants can follow the vector
            widthChild.setLocalRadianAngle(childAngleTrajectory);

            injectRightWidthChildCompute(widthChild);

            // Execute recursive cascading calls down to children to expand layout structures
            widthChild.compute();

            // runningStackedHeightDistance  = runningStackedHeightDistance + rightWidthNode.subtreeHeight + HEIGHT_SPACER + (Child Radius: 5.0)
            runningStackedWidthDistance += widthChild.getSubtreeWidth() + WIDTH_SPACER;
        }
    }
}
