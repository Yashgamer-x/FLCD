package com.yashgamerx.flcd.model;

import java.util.ArrayList;

public class RootNode extends AbstractNode {

    public RootNode(int identifier) {
        super(identifier);
    }

    /// Precomputes the node and its children.
    /// Converts any {@link UnknownNode} children to {@link FirstChildNode}
    /// and transfers their children before precomputing them.
    @Override
    public void preCompute() {
        var processedChildren = new ArrayList<AbstractNode>();

        for (var child : this.children) {
            AbstractNode processedChild = processAndPreComputeChild(child);
            processedChildren.add(processedChild);
        }

        this.children = processedChildren;
    }

    /// Identifies the node type, applies conversions if necessary, and triggers its pre-computation.
    private AbstractNode processAndPreComputeChild(AbstractNode child) {
        if (child instanceof UnknownNode unknownNode) {
            child = convertToFirstChildNode(unknownNode);
        }

        child.preCompute();
        return child;
    }

    /// Converts an UnknownNode into a FirstChildNode and transfers all its dependencies.
    private FirstChildNode convertToFirstChildNode(UnknownNode unknownNode) {
        var firstChild = new FirstChildNode(unknownNode.getIdentifier());

        // Transfer children from UnknownNode to FirstChildNode
        for (var grandChild : unknownNode.children) {
            firstChild.addChild(grandChild);
        }

        firstChild.setParent(this);
        return firstChild;
    }

    @Override
    public void compute() {
        //TODO
    }

    @Override
    public void readjust() {
        //TODO
    }

    @Override
    public void rootify() {
        //TODO
    }
}