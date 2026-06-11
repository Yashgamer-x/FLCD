package com.yashgamerx.flcd.service.algorithm;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.RootCompute;
import com.yashgamerx.flcd.service.precompute.RootPreCompute;

public class PlanarGridAlgorithm implements TreeLayoutAlgorithm {

    @Override
    public void calculate(AbstractNode root, double originX, double originY) {
        if (root == null) return;

        injectRootPrecomputeAndCompute(root);

        root.precompute();
        root.setGridX(originX);
        root.setGridY(originY);

        root.compute();
    }

    private void injectRootPrecomputeAndCompute(AbstractNode root) {
        injectPrecompute(root);
        injectCompute(root);
    }

    /// Injects [RootPreCompute]
    private void injectPrecompute(AbstractNode root) {
        root.setPrecomputable(new RootPreCompute());
    }

    /// Injects [RootCompute]
    private void injectCompute(AbstractNode root) {
        root.setComputable(new RootCompute());
    }

}