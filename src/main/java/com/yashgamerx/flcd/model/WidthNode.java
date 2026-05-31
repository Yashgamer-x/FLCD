package com.yashgamerx.flcd.model;

import java.util.ArrayList;

public class WidthNode extends AbstractNode {
    public WidthNode(int identifier) {
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
            processedChildren.add(mutateChildToHeightNode(child));
        }
        this.children = processedChildren;
    }

    /// Converts the node to HeightNode
    private HeightNode mutateChildToHeightNode(AbstractNode child) {
        // Creates new HeightNode width given Identifier.
        var concreteNode = new HeightNode(child.getIdentifier());

        // Loops through the children of the Child and adds the children to the newly created node.
        for (var grandChild : child.getChildren()) {
            concreteNode.addChild(grandChild);
        }

        // Sets this as the parent of the newly created node.
        concreteNode.setParent(this);
        return concreteNode;
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

    @Override
    public void compute() {
        throw new UnsupportedOperationException("WidthNode#compute is not supported.");
    }

    @Override
    public void readjust() {
        throw new UnsupportedOperationException("WidthNode#readjust is not supported.");
    }

    @Override
    public void rootify() {
        throw new UnsupportedOperationException("WidthNode#rootify is not supported.");
    }
}
