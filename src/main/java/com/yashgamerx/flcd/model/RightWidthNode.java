package com.yashgamerx.flcd.model;

import java.util.ArrayList;

public class RightWidthNode extends AbstractNode {

    public RightWidthNode(int identifier) {
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
        calculateHorizontalSubtreeDimensions();
    }

    private boolean isLeafNode() {
        return this.children == null || this.children.isEmpty();
    }

    private void initializeLeafDimensions() {
        this.subtreeWidth = 10.0;
        this.subtreeHeight = 10.0;
    }

    /// Toggles the alternation: Converts children to RightHeightNode
    private void mutateChildrenToHeightNodes() {
        var processedChildren = new ArrayList<AbstractNode>();

        for (var child : this.children) {
            if (child instanceof RightHeightNode rightHeightNode) {
                processedChildren.add(rightHeightNode);
            } else {
                var concreteNode = new RightHeightNode(child.getIdentifier());
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

    /// Computes dimensions where children are arranged horizontally
    private void calculateHorizontalSubtreeDimensions() {
        double totalChildrenWidth = 0.0;
        double maxChildHeight = 0.0;

        for (int i = 0; i < this.children.size(); i++) {
            AbstractNode child = this.children.get(i);

            totalChildrenWidth += child.getSubtreeWidth();
            maxChildHeight = Math.max(maxChildHeight, child.getSubtreeHeight());

            if (i < this.children.size() - 1) {
                totalChildrenWidth += WIDTH_SPACER;
            }
        }

        // Subtree Width = node's own width (10.0) + space gap + sibling accumulated widths
        this.subtreeWidth = 10.0 + WIDTH_SPACER + totalChildrenWidth;

        // Subtree Height = maximum child depth or its own diameter (10.0), whichever is taller
        this.subtreeHeight = Math.max(10.0, maxChildHeight);
    }

    @Override
    public void compute() {
    }

    @Override
    public void readjust() {
    }

    @Override
    public void rootify() {
    }
}