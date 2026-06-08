package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.angular.Angle360Calculator;
import com.yashgamerx.flcd.service.angular.AngularCalculator;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableFactoryImplementation;
import com.yashgamerx.flcd.service.precompute.inject.FirstChildPreComputeInjector;
import com.yashgamerx.flcd.service.precompute.inject.PrecomputeInjectable;

import static com.yashgamerx.flcd.model.AbstractNode.NODE_DIAMETER;

public class RootPreCompute implements Precomputable {

    private final PrecomputeInjectable preComputeInjector;
    private final AngularCalculator angularCalculator = new Angle360Calculator();

    public RootPreCompute(PreComputableFactoryImplementation preComputableFactoryImplementation) {
        this.preComputeInjector = new FirstChildPreComputeInjector(preComputableFactoryImplementation);
    }

    @Override
    public void precompute(AbstractNode root) {
        root.getChildren().forEach(this::injectAndPrecompute);

        calculateSubtree(root);
    }

    /// Injects FirstChildPreCompute dependency and Precomputes the child
    private void injectAndPrecompute(AbstractNode firstChild) {
        preComputeInjector.inject(firstChild);
        firstChild.precompute();
    }

    /// Calculates the offset of the firstChild from the root node.
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
        return clearanceHeight + firstChild.getSubtreeHeight();
    }

    /// Calculates the Subtree area requirement based on the requirements firstChildren.
    private void calculateSubtree(AbstractNode root) {
        var firstChildren = root.getChildren();
        double maxOffset = 0;

        for (var firstChild : firstChildren) {
            double safeScalarOffset = calculateScalarOffset(firstChild);
            maxOffset = Math.max(maxOffset, safeScalarOffset);
        }

        // Since the children are placed in a circle,
        // whatever their offset is will be the height and width of the entire root tree;
        // all the nodes need to fit in this giant offset circle.

        // Offset of the entire subtree + diameter of the node
        root.setSubtreeWidth(maxOffset + maxOffset + NODE_DIAMETER);
        root.setSubtreeHeight(maxOffset + maxOffset + NODE_DIAMETER);
    }
}
