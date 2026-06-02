package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;

public class RootifiedPreCompute implements Precomputable {
    @Override
    public void precompute(AbstractNode node) {
        node.getChildren().forEach(this::injectAndPrecompute);

        calculateSubtree(node);
    }

    private void injectAndPrecompute(AbstractNode abstractNode) {
        injectFirstChildPreCompute(abstractNode);
        abstractNode.precompute();
    }

    private void injectFirstChildPreCompute(AbstractNode abstractNode) {
        abstractNode.setPrecomputable(new FirstChildPreCompute());
    }

    /// Slices the 180-degree space (PI radians) evenly based on child count.
    private double calculateAngularStep(int totalChildren) {
        return (Math.PI) / totalChildren;
    }

    /// Calculates the offset of the firstChild from the root node.
    private double calculateScalarOffset(AbstractNode firstChild) {
        int totalChildren = firstChild.getParent().getChildren().size();
        double angularStep = calculateAngularStep(totalChildren);
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
        root.setSubtreeWidth(maxOffset);
        root.setSubtreeHeight(maxOffset);
    }
}
