package com.yashgamerx.flcd.service.algorithm;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.precompute.RootPreCompute;

public class PlanarGridAlgorithm implements TreeLayoutAlgorithm {

    @Override
    public void calculate(AbstractNode root, double originX, double originY) {
        if (root == null) return;

        injectRootPrecompute(root);

        root.precompute();
        root.setGridX(originX);
        root.setGridY(originY);

        root.compute();
    }

    private void injectRootPrecompute(AbstractNode root) {
        root.setPrecomputable(new RootPreCompute());
    }

}