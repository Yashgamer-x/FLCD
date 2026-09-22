package com.yashgamerx.flcd.flcd.algorithm;

import com.yashgamerx.flcd.common.NodeRole;
import com.yashgamerx.flcd.flcd.engine.FLCDNodeEngine;
import com.yashgamerx.flcd.flcd.model.FLCDNode;

public class PlanarGridAlgorithm implements TreeLayoutAlgorithm {

    private final FLCDNodeEngine engine = new FLCDNodeEngine();

    @Override
    public void calculate(FLCDNode root, double originX, double originY) {
        if (root == null) return;

        root.setRole(NodeRole.ROOT);

        engine.precompute(root);
        root.setLayoutX(originX);
        root.setLayoutY(originY);

        engine.compute(root);
    }
}
