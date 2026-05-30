package com.yashgamerx.flcd.model;

import java.util.ArrayList;

public class RootNode extends AbstractNode {

    public RootNode(int identifier) {
        super(identifier);
    }

    /// Precomputes the node and its children.
    /// Converts any {@link UnknownNode} children to {@link FirstChildNode}
    /// and transfers their children before precomputing them.
    @Override
    public void preCompute() {
        var processedChildren = new ArrayList<AbstractNode>();

        for (var child : this.children) {
            AbstractNode processedChild = processAndPreComputeChild(child);
            processedChildren.add(processedChild);
        }

        this.children = processedChildren;
    }

    /// Identifies the node type, applies conversions if necessary, and triggers its pre-computation.
    private AbstractNode processAndPreComputeChild(AbstractNode child) {
        if (child instanceof UnknownNode unknownNode) {
            child = convertToFirstChildNode(unknownNode);
        }

        child.preCompute();
        return child;
    }

    /// Converts an UnknownNode into a FirstChildNode and transfers all its dependencies.
    private FirstChildNode convertToFirstChildNode(UnknownNode unknownNode) {
        var firstChild = new FirstChildNode(unknownNode.getIdentifier());

        for (var grandChild : unknownNode.children) {
            firstChild.addChild(grandChild);
        }

        firstChild.setParent(this);
        return firstChild;
    }

    /// Entry point for radial coordinate distribution.
    /// Spreads all first-level children uniformly across a 360-degree radius.
    @Override
    public void compute() {
        if (isChildrenListEmpty()) {
            return;
        }

        int totalChildren = this.children.size();
        double angularStep = calculateAngularStep(totalChildren);

        for (int i = 0; i < totalChildren; i++) {
            AbstractNode child = this.children.get(i);
            double currentAngle = i * angularStep;

            configureChildLayoutState(child, currentAngle);
            projectAndAssignCoordinates(child, currentAngle);

            // Cascade down computation recursively
            child.compute();
        }
    }

    private boolean isChildrenListEmpty() {
        return this.children == null || this.children.isEmpty();
    }

    /// Slices the 360-degree space (2 * PI radians) evenly based on child count.
    private double calculateAngularStep(int totalChildren) {
        return (2.0 * Math.PI) / totalChildren;
    }

    /// Calculates and assigns the safe radial distance boundary and angle for the child.
    private void configureChildLayoutState(AbstractNode child, double targetAngle) {
        // Clearance calculation updated to reflect the 10.0 baseline node diameter
        double clearanceHeight = child.getSubtreeWidth() / 2.0;
        double safeScalarOffset = clearanceHeight + child.getSubtreeHeight();

        child.setNodeOffset(safeScalarOffset);
        child.setLocalRadianAngle(targetAngle);
    }

    private void projectAndAssignCoordinates(AbstractNode child, double targetAngle) {
        double safeScalarOffset = child.getNodeOffset();

        double childX = this.getGridX() + (safeScalarOffset * Math.cos(targetAngle));
        double childY = this.getGridY() + (safeScalarOffset * Math.sin(targetAngle));

        child.setGridX(childX);
        child.setGridY(childY);
    }

    @Override
    public void readjust() {
    }

    @Override
    public void rootify() {
    }
}