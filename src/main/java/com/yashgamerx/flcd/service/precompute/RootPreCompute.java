package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;

public class RootPreCompute implements Precomputable {
    @Override
    public void precompute(AbstractNode root) {
        root.getChildren().forEach(this::injectAndPrecompute);

        calculateSubtree(root);
    }

    private void injectAndPrecompute(AbstractNode firstChild) {
        injectFirstChildPrecomputation(firstChild);
        firstChild.precompute();
    }

    private void injectFirstChildPrecomputation(AbstractNode node) {
        node.setPrecomputable(new FirstChildPreCompute());
    }

    /// Slices the 360-degree space (2 * PI radians) evenly based on child count.
    private double calculateAngularStep(int totalChildren) {
        return (2.0 * Math.PI) / totalChildren;
    }

    private double calculateScalarOffset(AbstractNode firstChild) {
        int totalChildren = firstChild.getParent().getChildren().size();
        double angularStep = calculateAngularStep(totalChildren);
        // Clearance calculation updated to reflect the 10.0 baseline node diameter
        // tan(45) = width / clearance height
        // clearance height = width / (tan(45) * 2.0)
        // 2.0 is to divide the entirely equally width in half since the 45 angle is between the center to the end of 1 side.
        // and first child has two sides. Left and right side for its children.
        double clearanceHeight = firstChild.getSubtreeWidth() / (2.0 * Math.tan(angularStep / 2.0));
        return clearanceHeight + firstChild.getSubtreeHeight();
    }

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
        root.setSubtreeWidth(maxOffset);
        root.setSubtreeHeight(maxOffset);
    }
}
