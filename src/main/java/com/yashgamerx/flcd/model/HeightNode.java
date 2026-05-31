package com.yashgamerx.flcd.model;

import java.util.ArrayList;

public class HeightNode extends AbstractNode {
    public HeightNode(int identifier) {
        super(identifier);
    }

    @Override
    public void precompute() {
        if (isLeafNode()) {
            initializeLeafDimensions();
            return;
        }

        mutateChildrenToWidthNodes();
        preComputeChildren();
        calculateHorizontalSubtreeDimensions();
    }

    private boolean isLeafNode() {
        return this.children == null || this.children.isEmpty();
    }

    private void initializeLeafDimensions() {
        this.subtreeWidth = 10.0;
        this.subtreeHeight = 10.0;
    }

    /// Toggles the alternation: Converts children to LeftWidthNode
    private void mutateChildrenToWidthNodes() {
        var processedChildren = new ArrayList<AbstractNode>();
        for (var child : this.children) {
            mutateChildToWidthNode(child, processedChildren);
        }
        this.children = processedChildren;
    }

    private void mutateChildToWidthNode(AbstractNode child, ArrayList<AbstractNode> processedChildren) {
        var concreteNode = new WidthNode(child.getIdentifier());
        for (var grandChild : child.getChildren()) {
            concreteNode.addChild(grandChild);
        }
        concreteNode.setParent(this);
        processedChildren.add(concreteNode);
    }

    private void preComputeChildren() {
        for (var child : this.children) {
            child.precompute();
        }
    }

    /// Computes dimensions where children are stacked Horizontally
    private void calculateHorizontalSubtreeDimensions() {
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

    @Override
    public void compute() {
        throw new UnsupportedOperationException("HeightNode#compute is not supported.");
    }

    @Override
    public void readjust() {
        throw new UnsupportedOperationException("HeightNode#readjust is not supported.");
    }

    @Override
    public void rootify() {
        throw new UnsupportedOperationException("HeightNode#rootify is not supported.");
    }
}
