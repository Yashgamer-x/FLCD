package com.yashgamerx.flcd.model;

import com.yashgamerx.flcd.service.precompute.factory.PreComputableFactory;

public class Node extends AbstractNode {

    public Node(int identifier, PreComputableFactory preComputableFactory) {
        super(identifier, preComputableFactory);
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

    }

    @Override
    public void rootify() {

    }

}
