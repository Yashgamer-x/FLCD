package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.angular.Angle180Calculator;
import com.yashgamerx.flcd.service.angular.AngularCalculator;
import com.yashgamerx.flcd.service.precompute.inject.FirstChildPreComputeInjector;
import com.yashgamerx.flcd.service.precompute.inject.PrecomputeInjectable;

import static com.yashgamerx.flcd.model.AbstractNode.NODE_DIAMETER;

public class RootifiedPreCompute implements Precomputable {

    private final AngularCalculator angularCalculator = new Angle180Calculator();
    private final PrecomputeInjectable injector = new FirstChildPreComputeInjector();

    @Override
    public void precompute(AbstractNode node) {
        node.getChildren().forEach(this::injectAndPrecompute);
        calculateSubtree(node);
    }

    private void injectAndPrecompute(AbstractNode abstractNode) {
        injector.inject(abstractNode);
        abstractNode.precompute();
    }

    /// Calculates the safe boundary offset requirement of a child from its parent node center.
    private double calculateScalarOffset(AbstractNode firstChild) {
        int totalChildren = firstChild.getParent().getChildren().size();
        double angularStep = angularCalculator.calculate(totalChildren);
        // Clearance calculation updated to reflect the 10.0 baseline node diameter
        // clearance height = width / (tan(theta/2) * 2.0)
        // theta/2 is required so that, if the first children are placed at differences of angle 60 degrees;
        // then the angle from root to the edge of the area required by first child will be 30 degree.
        // And the child in total is allocated 60 degrees.
        // 30 degrees to the extreme right side and 30 degrees to the extreme left side.
        double clearanceHeight = firstChild.getSubtreeWidth() / (2.0 * Math.tan(angularStep / 2.0));

        // Total safe boundary required is clearance height plus the child's own subtree height
        return clearanceHeight + firstChild.getSubtreeHeight();
    }

    /// Calculates the Subtree area requirement based on the requirements of firstChildren.
    private void calculateSubtree(AbstractNode node) {
        var firstChildren = node.getChildren();
        double maxOffset = 0;

        for (var firstChild : firstChildren) {
            double safeScalarOffset = calculateScalarOffset(firstChild);
            maxOffset = Math.max(maxOffset, safeScalarOffset);
        }

        // Offset of the entire subtree + diameter of the node
        // We are assuming that the worst case child will be on the edges and requires that much space.
        // Rootified node has right and left side making it maxoffset * 2.
        node.setSubtreeWidth(maxOffset + maxOffset + NODE_DIAMETER);
        node.setSubtreeHeight(maxOffset + NODE_DIAMETER);
    }
}