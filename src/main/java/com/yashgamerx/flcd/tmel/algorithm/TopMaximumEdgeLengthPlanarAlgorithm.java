package com.yashgamerx.flcd.tmel.algorithm;

import com.yashgamerx.flcd.common.LayoutAlgorithm;
import com.yashgamerx.flcd.common.NodeRole;
import com.yashgamerx.flcd.tmel.engine.TMELPlanarNodeEngine;
import com.yashgamerx.flcd.tmel.model.TMELNode;

public class TopMaximumEdgeLengthPlanarAlgorithm implements LayoutAlgorithm<TMELNode> {
    private final TMELPlanarNodeEngine engine = new TMELPlanarNodeEngine();

    public void calculate(TMELNode root, double originX, double originY) {
        if (root == null) return;

        root.setRole(NodeRole.ROOT);

        engine.precompute(root);
        root.setLayoutX(originX);
        root.setLayoutY(originY);
        engine.compute(root);
    }
}
