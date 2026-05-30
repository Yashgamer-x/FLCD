package com.yashgamerx.flcd.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FirstChildNode extends AbstractNode {

    public FirstChildNode(int identifier) {
        super(identifier);
    }

    @Override
    public void preCompute() {
        if (isLeafNode()) {
            initializeLeafDimensions();
            return;
        }

        mutateChildrenToSecondNodes();
        preComputeChildren();

        var leftSide = new ArrayList<AbstractNode>();
        var rightSide = new ArrayList<AbstractNode>();
        partitionAndMutateChildrenGreedily(leftSide, rightSide);

        updateChildrenList(leftSide, rightSide);
        calculateBalancedSubtreeDimensions(leftSide, rightSide);
    }

    private void mutateChildrenToSecondNodes() {
        var processedChildren = new ArrayList<AbstractNode>();

        for (var child : this.children) {
            if (child instanceof HeightNode heightNode) {
                processedChildren.add(heightNode);
            } else {
                processedChildren.add(convertToSecondNode(child));
            }
        }
        this.children = processedChildren;
    }

    private SecondChildNode convertToSecondNode(AbstractNode rawNode) {
        var concreteNode = new SecondChildNode(rawNode.getIdentifier());
        transferNodeStructure(rawNode, concreteNode);
        return concreteNode;
    }

    private boolean isLeafNode() {
        return this.children == null || this.children.isEmpty();
    }

    private void initializeLeafDimensions() {
        this.subtreeWidth = 10.0;
        this.subtreeHeight = 10.0;
    }

    /// Invokes preCompute recursively on the newly mutated, safe children
    private void preComputeChildren() {
        for (var child : this.children) {
            child.preCompute();
        }
    }

    /// Partitions raw children greedily by estimating weight via total nested children count
    private void partitionAndMutateChildrenGreedily(List<AbstractNode> leftSide, List<AbstractNode> rightSide) {
        List<AbstractNode> sortedChildren = new ArrayList<>(this.children);
        sortedChildren.sort(Comparator.comparingInt((AbstractNode node) -> node.getChildren().size()).reversed());

        int leftAccumulatedCount = 0;
        int rightAccumulatedCount = 0;

        for (var child : sortedChildren) {
            if (leftAccumulatedCount <= rightAccumulatedCount) {
                AbstractNode leftNode = convertToLeft(child);
                leftSide.add(leftNode);
                leftAccumulatedCount += leftNode.getChildren().size() + 1;
            } else {
                AbstractNode rightNode = convertToRight(child);
                rightSide.add(rightNode);
                rightAccumulatedCount += rightNode.getChildren().size() + 1;
            }
        }
    }

    private SecondLeftNode convertToLeft(AbstractNode node) {
        SecondLeftNode leftNode = new SecondLeftNode(node.getIdentifier());
        transferNodeStructure(node, leftNode);
        return leftNode;
    }

    private SecondRightNode convertToRight(AbstractNode node) {
        SecondRightNode rightNode = new SecondRightNode(node.getIdentifier());
        transferNodeStructure(node, rightNode);
        return rightNode;
    }

    /// Moves the children and links references without touching dimension variables
    private void transferNodeStructure(AbstractNode source, AbstractNode target) {
        // Transfer children down the line so they aren't lost
        for (var grandChild : source.getChildren()) {
            target.addChild(grandChild);
        }
        target.setParent(this);
    }

    private void updateChildrenList(List<AbstractNode> left, List<AbstractNode> right) {
        var merged = new ArrayList<AbstractNode>();
        merged.addAll(left);
        merged.addAll(right);
        this.children = merged;
    }

    private void calculateBalancedSubtreeDimensions(List<AbstractNode> left, List<AbstractNode> right) {
        double leftWidth = calculateSideWidth(left);
        double rightWidth = calculateSideWidth(right);

        double maxSideWidth = Math.max(leftWidth, rightWidth);
        double maxLeftHeight = calculateMaxHeight(left);
        double maxRightHeight = calculateMaxHeight(right);

        // FIXED: Changed 1.0 to 10.0 to match the true baseline diameter size of the parent node
        double totalWidth = (maxSideWidth * 2) + 10.0;

        if (leftWidth > 0 || rightWidth > 0) {
            totalWidth += (WIDTH_SPACER * 2);
        }

        this.subtreeWidth = Math.max(10.0, totalWidth);

        // Height calculation remains the same: this node + vertical gap + tallest child
        this.subtreeHeight = 10.0 + HEIGHT_SPACER + Math.max(maxLeftHeight, maxRightHeight);
    }

    /// Calculates the total sideWidth based on subTree's width
    private double calculateSideWidth(List<AbstractNode> sideNodes) {
        double width = 0.0;
        for (int i = 0; i < sideNodes.size(); i++) {
            width += sideNodes.get(i).getSubtreeWidth();
            if (i < sideNodes.size() - 1) {
                width += WIDTH_SPACER;
            }
        }
        return width;
    }

    /// Finds the maximum height based on subTree's height
    private double calculateMaxHeight(List<AbstractNode> sideNodes) {
        return sideNodes.stream()
                .mapToDouble(AbstractNode::getSubtreeHeight)
                .max()
                .orElse(0.0);
    }

    /// Computes absolute screen coordinates and projects left/right children lines perpendicularly.
    @Override
    public void compute() {
        if (isLeafNode()) {
            return;
        }

        double parentAngle = this.getLocalRadianAngle(); // Trajectory angle inherited from RootNode [cite: 80, 138]

        // Perpendicular wing baseline vectors [cite: 86, 88]
        double rightAngle = parentAngle - (Math.PI / 2.0); // -90 degrees relative
        double leftAngle = parentAngle + (Math.PI / 2.0);  // +90 degrees relative

        // Clearance offset length pushing children downstream from parent perimeter:
        // (Parent Radius: 5.0) + HEIGHT_SPACER + (Child Radius: 5.0)
        double forwardStepLength = 5.0 + HEIGHT_SPACER + 5.0;

        // Establish the baseline anchor point directly in front of the parent node
        // Correcting for screen coordinate Y-inversion globally (subtraction for positive Y step)
        double anchorX = this.getGridX() - (forwardStepLength * Math.cos(parentAngle));
        double anchorY = this.getGridY() + (forwardStepLength * Math.sin(parentAngle));

        // Track sequential slide displacements along the wings
        double accumulatedRightDistance = 0.0;
        double accumulatedLeftDistance = 0.0;

        for (var child : this.children) {
            if (child instanceof SecondRightNode) {
                accumulatedRightDistance = projectChildAlongVector(child, anchorX, anchorY, rightAngle, accumulatedRightDistance);
            } else if (child instanceof SecondLeftNode) {
                accumulatedLeftDistance = projectChildAlongVector(child, anchorX, anchorY, leftAngle, accumulatedLeftDistance);
            }
        }
    }

    /// Positions a child sequentially along a wing baseline and cascades the true angle down-chain.
    private double projectChildAlongVector(AbstractNode child, double anchorX, double anchorY, double baselineAngle, double currentDistance) {
        double halfWidth = child.getSubtreeWidth();

        // Move to the central plotting coordinate of the current node's physical boundary box
        if (currentDistance > 0.0) {
            currentDistance += WIDTH_SPACER;
        }
        currentDistance += halfWidth;

        // Polar layout projection mapped to Cartesian grid space
        // Corrected Y calculation to handle screen inversion seamlessly
        double childX = anchorX + (currentDistance * Math.cos(baselineAngle));
        double childY = anchorY - (currentDistance * Math.sin(baselineAngle));

        child.setGridX(childX);
        child.setGridY(childY);

        // Assign the true baseline angle to the child so its nested sub-elements
        // know which vector they are traveling along when they process their own compute passes!
        child.setLocalRadianAngle(baselineAngle);

        // Process nested layout branches recursively
        child.compute();

        // Return current tail boundary for the next sibling layout spacing step
        return currentDistance + halfWidth;
    }

    @Override
    public void readjust() {
    }

    @Override
    public void rootify() {
    }
}