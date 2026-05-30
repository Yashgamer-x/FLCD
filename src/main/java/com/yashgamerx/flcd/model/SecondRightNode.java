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

        for (int i = 0; i < this.children.size(); i++) {
            AbstractNode child = this.children.get(i);

            maxChildWidth = Math.max(maxChildWidth, child.getSubtreeWidth());
            totalChildrenHeight += child.getSubtreeHeight();

            if (currentIndexHasNextSibling(i)) {
                totalChildrenHeight += HEIGHT_SPACER;
            }
        }

        // Changed 1.0 -> 10.0 to match the custom radius node boundary sizes!
        this.subtreeWidth = Math.max(10.0, maxChildWidth);
        this.subtreeHeight = 10.0 + HEIGHT_SPACER + totalChildrenHeight;
    }

    private boolean currentIndexHasNextSibling(int currentIndex) {
        return currentIndex < this.children.size() - 1;
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