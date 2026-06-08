package com.yashgamerx.flcd.service.algorithm;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.RootCompute;
import com.yashgamerx.flcd.service.precompute.RootPreCompute;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableFactoryImplementation;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableOption;

public class PlanarGridAlgorithm implements TreeLayoutAlgorithm {

    private final PreComputableFactoryImplementation factory;

    public PlanarGridAlgorithm(PreComputableFactoryImplementation preComputableFactoryImplementation) {
        this.factory = preComputableFactoryImplementation;
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
        root.setPrecomputable(factory.getPreComputable(PreComputableOption.ROOT));
    }

    /// Injects [RootCompute]
    private void injectCompute(AbstractNode root) {
        root.setComputable(new RootCompute());
    }

}