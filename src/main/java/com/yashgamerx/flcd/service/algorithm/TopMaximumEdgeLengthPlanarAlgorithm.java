package com.yashgamerx.flcd.service.algorithm;

import com.yashgamerx.flcd.model.FLCDNode;
import com.yashgamerx.flcd.model.NodeRole;
import com.yashgamerx.flcd.service.engine.TMELPlanarNodeEngine;

public class TopMaximumEdgeLengthPlanarAlgorithm implements TreeLayoutAlgorithm {
    private final TMELPlanarNodeEngine engine = new TMELPlanarNodeEngine();

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
