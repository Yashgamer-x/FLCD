package com.yashgamerx.flcd.model;

import java.util.ArrayList;

public class SecondRightNode extends AbstractNode {

    public SecondRightNode(int identifier) {
        super(identifier);
    }

    /// Bottom-up pre-computation. Converts uncalculated children to RightHeightNode,
    /// triggers their internal measurements, and calculates total stacked boundaries.
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

    /// Structurally transforms generic or unknown nodes to RightHeightNode before measurement.
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

        // Retain nested down-chain hierarchy structure
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

    /// Computes the layout dimensions assuming vertical/linear stacking of the branch children.
    private void calculateStackedSubtreeDimensions() {
        double maxChildWidth = 0.0;
        double totalChildrenHeight = 0.0;

        for (int i = 0; i < this.children.size(); i++) {
            AbstractNode child = this.children.get(i);

            maxChildWidth = Math.max(maxChildWidth, child.getSubtreeWidth());
            totalChildrenHeight += child.getSubtreeHeight();

            if (isNotLastChild(i)) {
                totalChildrenHeight += HEIGHT_SPACER;
            }
        }

        // Subtree Width is determined by the widest child element branch or its own base unit
        this.subtreeWidth = Math.max(1.0, maxChildWidth);

        // Subtree Height is this node's height (1.0) + distance gap + the accumulated height of the chain
        this.subtreeHeight = 1.0 + HEIGHT_SPACER + totalChildrenHeight;
    }

    private boolean isNotLastChild(int currentIndex) {
        return currentIndex < this.children.size() - 1;
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