package com.yashgamerx.flcd.model;

import com.yashgamerx.flcd.service.compute.Computable;
import com.yashgamerx.flcd.service.precompute.Precomputable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Node extends AbstractNode {

    // Services
    Precomputable precomputable;
    Computable computable;


    public Node(int identifier) {
        super(identifier);
    }

    @Override
    public void preCompute() {
        precomputable.precompute();
    }

    @Override
    public void compute() {
        computable.compute();
    }

    @Override
    public void readjust() {

    }

    @Override
    public void rootify() {

    }

}
