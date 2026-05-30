package com.yashgamerx.flcd.service.algorithm;

import com.yashgamerx.flcd.model.AbstractNode;

public class PlanarGridAlgorithm implements TreeLayoutAlgorithm {

    @Override
    public void calculate(AbstractNode root, double originX, double originY) {
        if (root == null) return;
        root.preCompute();

        root.setGridX(originX);
        root.setGridY(originY);
        root.compute();
    }
}