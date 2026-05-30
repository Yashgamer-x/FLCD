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

        // 1. Measure everyone bottom-up first
        preComputeChildren();

        // 2. Partition children into Left and Right balances greedily
        var leftSide = new ArrayList<AbstractNode>();
        var rightSide = new ArrayList<AbstractNode>();
        partitionChildrenGreedily(leftSide, rightSide);

        // 3. Calculate dimensions based on the balanced sides
        calculateBalancedSubtreeDimensions(leftSide, rightSide);
    }

    ///  When the children is null or the list is empty, it is considered to be a leaf node
    private boolean isLeafNode() {
        return this.children == null || this.children.isEmpty();
    }

    /// Default initialization to 1.0
    private void initializeLeafDimensions() {
        this.subtreeWidth = 1.0;
        this.subtreeHeight = 1.0;
    }

    /// Invokes [AbstractNode#preCompute()] for children nodes
    private void preComputeChildren() {
        for (var child : this.children) {
            child.preCompute();
        }
    }

    /// Partitions children into left and right lists based on descending subtreeWidth
    private void partitionChildrenGreedily(List<AbstractNode> leftSide, List<AbstractNode> rightSide) {
        // Sort children descending by their precomputed width
        List<AbstractNode> sortedChildren = new ArrayList<>(this.children);
        sortedChildren.sort(Comparator.comparingDouble(AbstractNode::getSubtreeWidth).reversed());

        double leftAccumulatedWidth = 0.0;
        double rightAccumulatedWidth = 0.0;

        for (var child : sortedChildren) {
            // Greedy choice: add to the side that is currently lighter
            if (leftAccumulatedWidth <= rightAccumulatedWidth) {
                leftSide.add(child);
                leftAccumulatedWidth += child.getSubtreeWidth() + WIDTH_SPACER;
            } else {
                rightSide.add(child);
                rightAccumulatedWidth += child.getSubtreeWidth() + WIDTH_SPACER;
            }
        }
    }

    /// Computes layout boundaries assuming the parent sits between the left and right groups
    private void calculateBalancedSubtreeDimensions(List<AbstractNode> left, List<AbstractNode> right) {
        double leftWidth = calculateSideWidth(left);
        double rightWidth = calculateSideWidth(right);

        double maxLeftHeight = calculateMaxHeight(left);
        double maxRightHeight = calculateMaxHeight(right);

        // Subtree width = Left group width + Right group width + 1.0 (this node) + spacing
        double totalWidth = leftWidth + rightWidth + 1.0;
        if (leftWidth > 0) totalWidth += WIDTH_SPACER;
        if (rightWidth > 0) totalWidth += WIDTH_SPACER;

        this.subtreeWidth = Math.max(1.0, totalWidth);

        // Height is this node (1.0) + spacer + the tallest child from either side
        this.subtreeHeight = 1.0 + HEIGHT_SPACER + Math.max(maxLeftHeight, maxRightHeight);
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

    @Override
    public void compute() {
        // TODO
    }

    @Override
    public void readjust() {
        // TODO
    }

    @Override
    public void rootify() {
        //TODO
    }
}