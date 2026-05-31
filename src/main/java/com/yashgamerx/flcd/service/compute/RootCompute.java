package com.yashgamerx.flcd.service.compute;

import com.yashgamerx.flcd.model.AbstractNode;

public class RootCompute implements Computable {

    @Override
    public void compute(AbstractNode rootNode) {
        if (isChildrenListEmpty(rootNode)) return;

        var children = rootNode.getChildren();
        var totalChildren = children.size();
        var angularStep = calculateAngularStep(totalChildren);

        for (int i = 0; i < totalChildren; i++) {
            var firstChild = children.get(i);
            double currentAngle = i * angularStep;

            configureChildLayoutState(firstChild, currentAngle);
            projectAndAssignCoordinates(rootNode, firstChild, currentAngle);

            injectFirstChildComputable(firstChild);
            firstChild.compute();
        }
    }

    private boolean isChildrenListEmpty(AbstractNode rootNode) {
        return rootNode.getChildren() == null || rootNode.getChildren().isEmpty();
    }

    /// Slices the 360-degree space (2 * PI radians) evenly based on child count.
    private double calculateAngularStep(int totalChildren) {
        return (2.0 * Math.PI) / totalChildren;
    }

    /// Calculates and assigns the safe radial distance boundary and angle for the child.
    private void configureChildLayoutState(AbstractNode child, double targetAngle) {
        // Clearance calculation updated to reflect the 10.0 baseline node diameter
        // tan(45) = width / clearance height
        // clearance height = width / (tan(45) * 2.0)
        // 2.0 is to divide the entirely equally width in half since the 45 angle is between the center to the end of 1 side.
        // and first child has two sides. Left and right side for its children.
        double clearanceHeight = child.getSubtreeWidth();
        double safeScalarOffset = clearanceHeight + child.getSubtreeHeight();

        child.setNodeOffset(safeScalarOffset);
        child.setLocalRadianAngle(targetAngle);
    }

    ///
    private void projectAndAssignCoordinates(AbstractNode rootNode, AbstractNode firstChild, double targetAngle) {
        double safeScalarOffset = firstChild.getNodeOffset();

        double childX = rootNode.getGridX() + (safeScalarOffset * Math.cos(targetAngle));
        double childY = rootNode.getGridY() - (safeScalarOffset * Math.sin(targetAngle));

        firstChild.setGridX(childX);
        firstChild.setGridY(childY);
    }

    /// Injects [FirstChildCompute] to the AbstractNode
    private void injectFirstChildComputable(AbstractNode firstChild) {
        firstChild.setComputable(new FirstChildCompute());
    }
}
