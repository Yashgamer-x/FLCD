package com.yashgamerx.flcd.service.algorithm;

import com.yashgamerx.flcd.model.AbstractNode;

public class PlanarGridAlgorithm implements TreeLayoutAlgorithm {
    private static final double UNIT_X = 70.0;
    private static final double UNIT_Y = 100.0;

    @Override
    public void calculate(AbstractNode root, double originX, double originY) {
        if (root == null) return;
        root.preCompute();
        root.compute();

        root.setGridX(originX);
        root.setGridY(originY);
    }

    private void measureSubtree(AbstractNode node, boolean isVertical) {
        node.getChildren().forEach(child -> measureSubtree(child, !isVertical));
        if (node.getChildren().isEmpty()) {
            node.setSubtreeWidth(1);
            node.setSubtreeHeight(1);
            return;
        }
        if (isVertical) {
            node.setSubtreeHeight(1 + node.getChildren().stream().mapToDouble(AbstractNode::getSubtreeHeight).sum());
            node.setSubtreeWidth(1 + node.getChildren().stream().mapToDouble(AbstractNode::getSubtreeWidth).max().orElse(1.0));
            if (node.getChildren().size() == 1)
                node.setSubtreeHeight(Math.sqrt(node.getSubtreeHeight()));
        } else {
            node.setSubtreeWidth(1 + node.getChildren().stream().mapToDouble(AbstractNode::getSubtreeWidth).sum());
            node.setSubtreeHeight(1 + node.getChildren().stream().mapToDouble(AbstractNode::getSubtreeHeight).max().orElse(1.0));
        }
    }
}