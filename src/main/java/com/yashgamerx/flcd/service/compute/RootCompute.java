package com.yashgamerx.flcd.service.compute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.angular.Angle360Calculator;
import com.yashgamerx.flcd.service.angular.AngularCalculator;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;
import com.yashgamerx.flcd.service.precompute.inject.FirstChildPrecomputeInjector;
import lombok.extern.java.Log;
import static com.yashgamerx.flcd.model.AbstractNode.NODE_RADIUS;

@Log
public class RootCompute implements Computable {

    private final AngularCalculator angularCalculator = new Angle360Calculator();
    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();
    private final FirstChildPrecomputeInjector injector = new FirstChildPrecomputeInjector();

    @Override
    public void compute(AbstractNode rootNode) {
        if (emptyListChecker.isEmpty(rootNode.getChildren())) return;

        var children = rootNode.getChildren();
        var totalChildren = children.size();
        var angularStep = angularCalculator.calculate(totalChildren);

        for (int i = 0; i < totalChildren; i++) {
            var firstChild = children.get(i);
            double currentAngle = i * angularStep;

            configureChildLayoutState(firstChild, currentAngle, angularStep);
            projectAndAssignCoordinates(rootNode, firstChild, currentAngle);

            injector.inject(firstChild);
            firstChild.compute();
        }
    }

    /// Calculates and assigns the safe radial distance boundary and angle for the child.
    private void configureChildLayoutState(AbstractNode firstChild, double targetAngle, double angularStep) {
        // Clearance calculation updated to reflect the 10.0 baseline node diameter
        // tan(45) = width / clearance height
        // clearance height = width / (tan(45) * 2.0)
        // 2.0 is to divide the entirely equally width in half since the 45 angle is between the center to the end of 1 side.
        // and first child has two sides. Left and right side for its children.
        double clearanceHeight = firstChild.getSubtreeWidth() / (2.0 * Math.tan(angularStep / 2.0));
        double safeScalarOffset = clearanceHeight + firstChild.getSubtreeHeight();

        firstChild.setNodeOffset(safeScalarOffset);
        firstChild.setLocalRadianAngle(targetAngle);
    }

    ///
    private void projectAndAssignCoordinates(AbstractNode rootNode, AbstractNode firstChild, double targetAngle) {
        double safeScalarOffset = firstChild.getNodeOffset();
        double nodeCenter = safeScalarOffset - NODE_RADIUS;

        double childX = rootNode.getGridX() + (nodeCenter * Math.cos(targetAngle));
        double childY = rootNode.getGridY() - (nodeCenter * Math.sin(targetAngle));

        firstChild.setGridX(childX);
        firstChild.setGridY(childY);
    }
}
