package com.yashgamerx.flcd.model;

public class Node extends AbstractNode {

    public Node(int identifier) {
        super(identifier);
    }

    @Override
    public void precompute() {
        precomputable.precompute(this);
    }

    @Override
    public void compute() {
        computable.compute(this);
    }

    @Override
    public void readjust() {
        var parent = getParent();
        if (parent == null) throw new IllegalStateException("Cannot readjust a root node");
        if (parent.getStatus() == NodeStatus.ROOTIFIED) return;


        parent.readjust();
    }

    @Override
    public void rootify() {

    }

}
