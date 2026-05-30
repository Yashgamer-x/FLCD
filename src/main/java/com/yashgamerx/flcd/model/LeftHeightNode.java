package com.yashgamerx.flcd.model;

import java.util.ArrayList;

public class LeftHeightNode extends AbstractNode {

    public LeftHeightNode(int identifier) {
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
        calculateVerticalSubtreeDimensions();
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
            if (child instanceof LeftWidthNode leftWidthNode) {
                processedChildren.add(leftWidthNode);
            } else {
                var concreteNode = new LeftWidthNode(child.getIdentifier());
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

    /// Computes dimensions where children are stacked vertically
    private void calculateVerticalSubtreeDimensions() {
        double maxChildWidth = 0.0;
        double totalChildrenHeight = 0.0;

        for (int i = 0; i < this.children.size(); i++) {
            AbstractNode child = this.children.get(i);

            maxChildWidth = Math.max(maxChildWidth, child.getSubtreeWidth());
            totalChildrenHeight += child.getSubtreeHeight();

            if (i < this.children.size() - 1) {
                totalChildrenHeight += HEIGHT_SPACER;
            }
        }

        this.subtreeWidth = Math.max(10.0, maxChildWidth);
        this.subtreeHeight = 10.0 + HEIGHT_SPACER + totalChildrenHeight;
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