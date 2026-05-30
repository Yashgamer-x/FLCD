package com.yashgamerx.flcd.model;

import java.util.ArrayList;

public class LeftWidthNode extends AbstractNode {

    public LeftWidthNode(int identifier) {
        super(identifier);
    }

    @Override
    public void preCompute() {
        if (isLeafNode()) {
            initializeLeafDimensions();
            return;
        }

        mutateChildrenToHeightNodes();
        preComputeChildren();
        calculateVerticalSubtreeDimensions();
    }

    private boolean isLeafNode() {
        return this.children == null || this.children.isEmpty();
    }

    private void initializeLeafDimensions() {
        this.subtreeWidth = 10.0;
        this.subtreeHeight = 10.0;
    }

    /// Toggles the alternation: Converts children to LeftHeightNode
    private void mutateChildrenToHeightNodes() {
        var processedChildren = new ArrayList<AbstractNode>();

        for (var child : this.children) {
            if (child instanceof LeftHeightNode leftHeightNode) {
                processedChildren.add(leftHeightNode);
            } else {
                var concreteNode = new LeftHeightNode(child.getIdentifier());
                for (var grandChild : child.getChildren()) {
                    concreteNode.addChild(grandChild);
                }
                concreteNode.setParent(this);
                processedChildren.add(concreteNode);
            }
        }
        this.children = processedChildren;
    }

    private void preComputeChildren() {
        for (var child : this.children) {
            child.preCompute();
        }
    }

    /// Computes dimensions where children are stacked vertically
    private void calculateVerticalSubtreeDimensions() {
        // Calculate the maximum child height using a stream
        double maxChildWidth = this.children.stream()
                .mapToDouble(AbstractNode::getSubtreeWidth)
                .max()
                .orElse(0.0);

        // Calculate the combined raw width of all children using a stream
        double rawCombinedHeight = this.children.stream()
                .mapToDouble(AbstractNode::getSubtreeHeight)
                .sum();

        // Apply the spacer adjustments using your exact formulas
        int totalChildren = this.children.size();
        double totalChildrenHeight = rawCombinedHeight + (HEIGHT_SPACER * (totalChildren - 1));

        // Maximum Child Width + Width Spacing + Parent Width (10.0)
        this.subtreeWidth = Math.max(10.0, maxChildWidth) + WIDTH_SPACER + 10.0;

        // Total Children Height + Height Spacing + Parent Height (10.0)
        this.subtreeHeight = totalChildrenHeight + HEIGHT_SPACER + 10.0;
    }

    /// Computes spatial placement coordinates for RightHeightNode children line segments.
    @Override
    public void compute() {
        if (isLeafNode()) return;

        double myAngle = this.getLocalRadianAngle(); // Direction pointing into this node

        // Turn right by -90 degrees (-π/2 radians) relative to parent's angle vector orientation
        double childAngleTrajectory = myAngle - (Math.PI / 2.0);

        // Clearance offset length: (Parent Radius: 5.0) + WIDTH_SPACER + (Child Radius: 5.0)
        double forwardStepLength = 5.0 + WIDTH_SPACER + 5.0;

        // Establish structural anchor base coordinates right at the physical edge of this node
        // Inverting vertical vector directions (subtraction) to map to standard UI display systems
        double anchorX = this.getGridX() + (forwardStepLength * Math.cos(myAngle));
        double anchorY = this.getGridY() - (forwardStepLength * Math.sin(myAngle));

        childrenComputationBasedOnAnchorAndTrajectory(anchorX, anchorY, childAngleTrajectory);
    }

    private void childrenComputationBasedOnAnchorAndTrajectory(double anchorX, double anchorY, double childAngleTrajectory) {
        // Clearance offset Height: (Parent Radius: 5.0) + HEIGHT_SPACER + (Child Radius: 5.0)
        double runningStackedHeightDistance = 5.0 + HEIGHT_SPACER + 5.0;

        for (var child : this.children) {
            // Map polar placement vectors into Cartesian screen coordinate tracking states
            double childX = anchorX + (runningStackedHeightDistance * Math.cos(childAngleTrajectory));
            double childY = anchorY - (runningStackedHeightDistance * Math.sin(childAngleTrajectory));

            child.setGridX(childX);
            child.setGridY(childY);

            // Forward the calculated absolute orientation down-chain so descendants can follow the vector
            child.setLocalRadianAngle(childAngleTrajectory);

            // Execute recursive cascading calls down to children to expand layout structures
            child.compute();

            // runningStackedHeightDistance  = runningStackedHeightDistance + child.subtreeHeight + HEIGHT_SPACER + (Child Radius: 5.0)
            runningStackedHeightDistance += child.getSubtreeHeight() + HEIGHT_SPACER;
        }
    }

    @Override
    public void readjust() {
    }

    @Override
    public void rootify() {
    }
}