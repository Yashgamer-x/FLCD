package com.yashgamerx.flcd.model;

import java.util.ArrayList;

public class RightHeightNode extends AbstractNode {

    public RightHeightNode(int identifier) {
        super(identifier);
    }

    @Override
    public void preCompute() {
        if (isLeafNode()) {
            initializeLeafDimensions();
            return;
        }

        mutateChildrenToWidthNodes();
        preComputeChildren();
        calculatehHorizontalSubtreeDimensions();
    }

    private boolean isLeafNode() {
        return this.children == null || this.children.isEmpty();
    }

    private void initializeLeafDimensions() {
        this.subtreeWidth = 10.0;
        this.subtreeHeight = 10.0;
    }

    /// Toggles the alternation: Converts children to RightWidthNode
    private void mutateChildrenToWidthNodes() {
        var processedChildren = new ArrayList<AbstractNode>();

        for (var child : this.children) {
            if (child instanceof RightWidthNode rightWidthNode) {
                processedChildren.add(rightWidthNode);
            } else {
                var concreteNode = new RightWidthNode(child.getIdentifier());
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

    /// Computes dimensions where children are stacked Horizontally
    private void calculatehHorizontalSubtreeDimensions() {
        // Calculate the maximum child height using a stream
        double maxChildHeight = this.children.stream()
                .mapToDouble(AbstractNode::getSubtreeHeight)
                .max()
                .orElse(0.0);

        // Calculate the combined raw width of all children using a stream
        double rawCombinedWidth = this.children.stream()
                .mapToDouble(AbstractNode::getSubtreeWidth)
                .sum();

        // Apply the spacer adjustments using your exact formulas
        int totalChildren = this.children.size();
        double totalChildrenWidth = rawCombinedWidth + (WIDTH_SPACER * (totalChildren - 1));

        // Total Children Width + Width Spacing + Parent Width (10.0)
        this.subtreeWidth = totalChildrenWidth + WIDTH_SPACER + 10.0;

        // Maximum Child Height + Height Spacing + Parent Height (10.0)
        this.subtreeHeight = Math.max(10.0, maxChildHeight) + HEIGHT_SPACER + 10.0;
    }

    /// Computes spatial placement coordinates for RightHeightNode children line segments.
    @Override
    public void compute() {
        if (isLeafNode()) return;

        double myAngle = this.getLocalRadianAngle(); // Direction pointing into this node

        // Turn right by -90 degrees (-π/2 radians) relative to parent's angle vector orientation
        double childAngleTrajectory = myAngle - (Math.PI / 2.0);

        // Clearance offset length: (Parent Radius: 5.0) + HEIGHT_SPACER + (Child Radius: 5.0)
        double forwardStepLength = 5.0 + HEIGHT_SPACER + 5.0;

        // Establish structural anchor base coordinates right at the physical edge of this node
        // Inverting vertical vector directions (subtraction) to map to standard UI display systems
        double anchorX = this.getGridX() + (forwardStepLength * Math.cos(myAngle));
        double anchorY = this.getGridY() - (forwardStepLength * Math.sin(myAngle));

        childrenComputationBasedOnAnchorAndTrajectory(anchorX, anchorY, childAngleTrajectory);
    }

    private void childrenComputationBasedOnAnchorAndTrajectory(double anchorX, double anchorY, double childAngleTrajectory) {
        // Clearance offset Height: (Parent Radius: 5.0) + HEIGHT_SPACER + (Child Radius: 5.0)
        double runningStackedWidthDistance = 5.0 + WIDTH_SPACER + 5.0;

        for (var child : this.children) {
            // Map polar placement vectors into Cartesian screen coordinate tracking states
            double childX = anchorX + (runningStackedWidthDistance * Math.cos(childAngleTrajectory));
            double childY = anchorY - (runningStackedWidthDistance * Math.sin(childAngleTrajectory));

            child.setGridX(childX);
            child.setGridY(childY);

            // Forward the calculated absolute orientation down-chain so descendants can follow the vector
            child.setLocalRadianAngle(childAngleTrajectory);

            // Execute recursive cascading calls down to children to expand layout structures
            child.compute();

            // runningStackedHeightDistance  = runningStackedHeightDistance + child.subtreeHeight + HEIGHT_SPACER + (Child Radius: 5.0)
            runningStackedWidthDistance += child.getSubtreeWidth() + WIDTH_SPACER;
        }
    }

    @Override
    public void readjust() {
    }

    @Override
    public void rootify() {
    }
}