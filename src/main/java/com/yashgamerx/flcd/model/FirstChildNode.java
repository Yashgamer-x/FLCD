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

        // 1. First Pass: Partition and mutate types while they are still raw
        var leftSide = new ArrayList<AbstractNode>();
        var rightSide = new ArrayList<AbstractNode>();
        partitionAndMutateChildrenGreedily(leftSide, rightSide);

        // 2. Commit the mutated nodes to the main children list
        updateChildrenList(leftSide, rightSide);

        // 3. Second Pass: Now that they are typed nodes, it is safe to measure them bottom-up
        preComputeChildren();

        // 4. Calculate final parent dimensions based on the calculated sizes
        calculateBalancedSubtreeDimensions(leftSide, rightSide);
    }

    private boolean isLeafNode() {
        return this.children == null || this.children.isEmpty();
    }

    private void initializeLeafDimensions() {
        this.subtreeWidth = 1.0;
        this.subtreeHeight = 1.0;
    }

    /// Invokes preCompute recursively on the newly mutated, safe children
    private void preComputeChildren() {
        for (var child : this.children) {
            child.preCompute();
        }
    }

    /// Partitions raw children greedily by estimating weight via total nested children count
    private void partitionAndMutateChildrenGreedily(List<AbstractNode> leftSide, List<AbstractNode> rightSide) {
        // Sort original children descending by their nested child count as a proxy for weight
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
        // TODO
    }
}