package com.yashgamerx.flcd.model;

public class Node extends AbstractNode {

    public Node(int identifier) {
        super(identifier);
    }

    @Override
    public void preCompute() {
        precomputable.precompute(this);
    }

    @Override
    public void compute() {
        computable.compute(this);
    }

    @Override
    public void readjust() {

    }

    @Override
    public void rootify() {

    }

}
