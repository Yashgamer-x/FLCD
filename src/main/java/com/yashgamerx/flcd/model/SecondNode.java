package com.yashgamerx.flcd.model;

import java.util.ArrayList;

public class SecondNode extends AbstractNode {

    public SecondNode(int identifier) {
        super(identifier);
    }

    @Override
    public void precompute() {
        if (isLeafNode()) {
            initializeLeafDimensions();
            return;
        }

        mutateChildrenToHeightNodes();
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

    private void mutateChildrenToHeightNodes() {
        var processedChildren = new ArrayList<AbstractNode>();

        for (var child : this.children) {
            processedChildren.add(convertToHeightNode(child));
        }

        this.children = processedChildren;
    }

    /// Converts the node to HeightNode
    private HeightNode convertToHeightNode(AbstractNode rawNode) {
        // If the rawNode is an instance of HeightNode then return the heightNode directly.
        if (rawNode instanceof HeightNode heightNode) return heightNode;

        // ----- ELSE -----
        var concreteNode = new HeightNode(rawNode.getIdentifier());
        for (var grandChild : rawNode.getChildren()) {
            concreteNode.addChild(grandChild);
        }
        concreteNode.setParent(this);
        return concreteNode;
    }

    private void preComputeChildren() {
        for (var child : this.children) {
            child.precompute();
        }
    }

    private void calculateStackedSubtreeDimensions() {
        // Calculate the maximum child width using a stream
        double maxChildWidth = this.children.stream()
                .mapToDouble(AbstractNode::getSubtreeWidth)
                .max()
                .orElse(0.0);

        // Calculate the combined raw height of all children using a stream
        double rawChildrenHeight = this.children.stream()
                .mapToDouble(AbstractNode::getSubtreeHeight)
                .sum();

        // Apply the spacer adjustments using your exact formulas
        int totalChildren = this.children.size();
        double totalChildrenHeight = rawChildrenHeight + (HEIGHT_SPACER * (totalChildren - 1));

        // Maximum Child Width + Width Spacing + Parent Width (10.0)
        this.subtreeWidth = Math.max(10.0, maxChildWidth) + WIDTH_SPACER + 10.0;

        // Children Height + Height Spacing + Parent Height (10.0)
        this.subtreeHeight = totalChildrenHeight + HEIGHT_SPACER + 10.0;
    }

    @Override
    public void compute() {
        throw new UnsupportedOperationException("SecondChild#compute is not supported.");
    }

    @Override
    public void readjust() {
        throw new UnsupportedOperationException("SecondChild#readjust is not supported.");
    }

    @Override
    public void rootify() {
        throw new UnsupportedOperationException("SecondChild#rootify is not supported.");
    }
}
