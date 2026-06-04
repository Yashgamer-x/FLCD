package com.yashgamerx.flcd.service.compute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.angular.Angle180Calculator;
import com.yashgamerx.flcd.service.angular.AngularCalculator;
import com.yashgamerx.flcd.service.compute.inject.ComputeInjectable;
import com.yashgamerx.flcd.service.compute.inject.FirstChildComputeInjector;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;
import lombok.extern.java.Log;
import static com.yashgamerx.flcd.model.AbstractNode.NODE_RADIUS;

@Log
public class RootifiedCompute implements Computable {

    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();
    private final AngularCalculator angularCalculator = new Angle180Calculator();
    private final ComputeInjectable computeInjector = new FirstChildComputeInjector();

    @Override
    public void compute(AbstractNode rootNode) {
        if (emptyListChecker.isEmpty(rootNode.getChildren())) return;

        log.info("Rootified Computation");

        var children = rootNode.getChildren();
        var totalChildren = children.size();
        var angularStep = angularCalculator.calculate(totalChildren);
        var parentAngle = rootNode.getLocalRadianAngle();
        var beginningAngle = parentAngle - Math.PI / 2;

        for (int i = 0; i < totalChildren; i++) {
            var firstChild = children.get(i);
            double currentAngle = beginningAngle + i * angularStep + (angularStep / 2);

            // Core mathematical alignment tracking logic
            double clearanceHeight = firstChild.getSubtreeWidth() / (2.0 * Math.tan(angularStep / 2.0));

            // The node center sits directly at the edge of the clearance boundary plus its own radius
            double nodeCenter = clearanceHeight + NODE_RADIUS;

            // Total outer boundary used is where the center sits plus the remaining subtree height
            double safeScalarOffset = nodeCenter + firstChild.getSubtreeHeight();

            // Set node state properties
            firstChild.setNodeOffset(safeScalarOffset);
            firstChild.setLocalRadianAngle(currentAngle);

            // Project coordinates using the corrected center vector
            projectAndAssignCoordinates(rootNode, firstChild, currentAngle, nodeCenter);

            // Recurse down the layout tree hierarchy
            computeInjector.inject(firstChild);
            firstChild.compute();
        }
    }

    /// Computes spatial cartesian positions based on the exact vector center
    private void projectAndAssignCoordinates(AbstractNode rootNode, AbstractNode firstChild, double targetAngle, double nodeCenter) {
        double childX = rootNode.getGridX() + (nodeCenter * Math.cos(targetAngle));
        double childY = rootNode.getGridY() - (nodeCenter * Math.sin(targetAngle));

        firstChild.setGridX(childX);
        firstChild.setGridY(childY);
    }
}