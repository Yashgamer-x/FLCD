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
        calculateHorizontalSubtreeDimensions();
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

        this.subtreeWidth = 10.0 + WIDTH_SPACER + totalChildrenWidth;
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