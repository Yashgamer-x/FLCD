package com.yashgamerx.flcd.service.algorithm;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.RootCompute;
import com.yashgamerx.flcd.service.compute.factory.ComputableFactory;
import com.yashgamerx.flcd.service.compute.factory.ComputableOption;
import com.yashgamerx.flcd.service.precompute.RootPreCompute;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableFactory;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableOption;

public class PlanarGridAlgorithm implements TreeLayoutAlgorithm {

    private final PreComputableFactory preComputableFactory;
    private final ComputableFactory computableFactory;

    public PlanarGridAlgorithm(PreComputableFactory preComputableFactory, ComputableFactory computableFactory) {
        this.preComputableFactory = preComputableFactory;
        this.computableFactory = computableFactory;
    }

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
        root.setPrecomputable(preComputableFactory.getPreComputable(PreComputableOption.ROOT));
    }

    /// Injects [RootCompute]
    private void injectCompute(AbstractNode root) {
        root.setComputable(computableFactory.getComputable(ComputableOption.ROOT));
    }

}