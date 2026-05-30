package com.yashgamerx.flcd.model;

import java.util.ArrayList;

public class SecondRightNode extends AbstractNode {

    public SecondRightNode(int identifier) {
        super(identifier);
    }

    @Override
    public void preCompute() {
        if (isLeafNode()) {
            initializeLeafDimensions();
            return;
        }

        mutateChildrenToRightHeightNodes();
        preComputeChildren();
        calculateStackedSubtreeDimensions();
    }

    private boolean isLeafNode() {
        return this.children == null || this.children.isEmpty();
    }

    private void initializeLeafDimensions() {
        this.subtreeWidth = 10.0;
        this.subtreeHeight = 10.0;
    }

    private void mutateChildrenToRightHeightNodes() {
        var processedChildren = new ArrayList<AbstractNode>();

        for (var child : this.children) {
            if (child instanceof RightHeightNode rightHeightNode) {
                processedChildren.add(rightHeightNode);
            } else {
                processedChildren.add(convertToRightHeightNode(child));
            }
        }
        this.children = processedChildren;
    }

    private RightHeightNode convertToRightHeightNode(AbstractNode rawNode) {
        var concreteNode = new RightHeightNode(rawNode.getIdentifier());
        for (var grandChild : rawNode.getChildren()) {
            concreteNode.addChild(grandChild);
        }
        concreteNode.setParent(this);
        return concreteNode;
    }

    private void preComputeChildren() {
        for (var child : this.children) {
            child.preCompute();
        }
    }

    private void calculateStackedSubtreeDimensions() {
        double maxChildWidth = 0.0;
        double totalChildrenHeight = 0.0;

        for (AbstractNode child : this.children) {
            maxChildWidth = Math.max(maxChildWidth, child.getSubtreeWidth());
            totalChildrenHeight += child.getSubtreeHeight();
        }

        // totalChildrenHeight = totalChildrenHeight + (HEIGHT_SPACER* (n-1))
        // where n is the number of children
        // e.g. child---child---child---child
        // 4 children, 3 spacers
        totalChildrenHeight += (HEIGHT_SPACER * (this.children.size() - 1));

        // Maximum Child Width + Width Spacing + Parent Width (10.0)
        this.subtreeWidth = Math.max(10.0, maxChildWidth) + WIDTH_SPACER + 10.0;

        // Children Height + Height Spacing + Parent Height (10.0)
        this.subtreeHeight = totalChildrenHeight + HEIGHT_SPACER + 10.0;
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

        // Track running linear tracking accumulation down the layout vector rail path
        double runningStackedHeightDistance = 0.0;

        for (var child : this.children) {
            double halfHeight = child.getSubtreeHeight() / 2.0;

            // Apply horizontal default gap between structural sibling blocks
            if (runningStackedHeightDistance > 0.0) {
                runningStackedHeightDistance += HEIGHT_SPACER;
            }

            // Advance to the exact mid-point coordinate of the current child block bounding frame
            runningStackedHeightDistance += halfHeight;

            // Map polar placement vectors into Cartesian screen coordinate tracking states
            double childX = anchorX + (runningStackedHeightDistance * Math.cos(childAngleTrajectory));
            double childY = anchorY - (runningStackedHeightDistance * Math.sin(childAngleTrajectory));

            child.setGridX(childX);
            child.setGridY(childY);

            // Forward the calculated absolute orientation down-chain so descendants can follow the vector
            child.setLocalRadianAngle(childAngleTrajectory);

            // Execute recursive cascading calls down to children to expand layout structures
            child.compute();

            // Track remaining spacing clearance so next loop doesn't cause collision overlaps
            runningStackedHeightDistance += halfHeight;
        }
    }

    @Override
    public void readjust() {
    }

    @Override
    public void rootify() {
    }
}