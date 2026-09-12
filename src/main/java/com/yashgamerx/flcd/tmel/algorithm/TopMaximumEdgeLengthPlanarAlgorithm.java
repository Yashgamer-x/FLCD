package com.yashgamerx.flcd.tmel.algorithm;

import com.yashgamerx.flcd.common.NodeRole;
import com.yashgamerx.flcd.tmel.engine.TMELPlanarNodeEngine;
import com.yashgamerx.flcd.tmel.model.TMELNode;

public class TopMaximumEdgeLengthPlanarAlgorithm {
    private final TMELPlanarNodeEngine engine = new TMELPlanarNodeEngine();

    public void calculate(TMELNode root, double originX, double originY) {
        if (root == null) return;

        root.setRole(NodeRole.ROOT);

        engine.precompute(root);
        root.setGridX(originX);
        root.setGridY(originY);
        engine.compute(root);
    }
}
