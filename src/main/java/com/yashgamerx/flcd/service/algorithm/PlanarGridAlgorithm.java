package com.yashgamerx.flcd.service.algorithm;

import com.yashgamerx.flcd.model.FLCDNode;
import com.yashgamerx.flcd.model.NodeRole;
import com.yashgamerx.flcd.service.engine.FLCDNodeEngine;

public class PlanarGridAlgorithm implements TreeLayoutAlgorithm {

    private final FLCDNodeEngine engine = new FLCDNodeEngine();

    @Override
    public void calculate(FLCDNode root, double originX, double originY) {
        if (root == null) return;

        root.setRole(NodeRole.ROOT);

        engine.precompute(root);
        root.setGridX(originX);
        root.setGridY(originY);

        engine.compute(root);
    }
}
