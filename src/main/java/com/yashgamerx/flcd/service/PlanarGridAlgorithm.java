package com.yashgamerx.flcd.service;

import com.yashgamerx.flcd.model.TreeNode;

import java.util.List;

public class PlanarGridAlgorithm implements TreeLayoutAlgorithm {
    private static final double UNIT_X = 70.0;
    private static final double UNIT_Y = 100.0;

    @Override
    public void calculate(TreeNode root, double originX, double originY) {
        if (root == null) return;
        // Pass 1: Aggregate space needs [cite: 105, 138]
        measureSubtree(root, true);

        // Pass 2: Set root and first level [cite: 80, 118]
        root.setGridX(originX);
        root.setGridY(originY);
        layoutFirstLevel(root);
    }

    private void measureSubtree(TreeNode node, boolean isVertical) {
        node.getChildren().forEach(child -> measureSubtree(child, !isVertical));
        if (node.getChildren().isEmpty()) {
            node.setSubtreeWidth(1);
            node.setSubtreeHeight(1);
            return;
        }
        if (isVertical) {
            node.setSubtreeHeight(1 + node.getChildren().stream().mapToDouble(TreeNode::getSubtreeHeight).sum());
            node.setSubtreeWidth(1 + node.getChildren().stream().mapToDouble(TreeNode::getSubtreeWidth).max().orElse(1.0));
            if (node.getChildren().size() == 1)
                node.setSubtreeHeight(Math.sqrt(node.getSubtreeHeight())); // [cite: 112]
        } else {
            node.setSubtreeWidth(1 + node.getChildren().stream().mapToDouble(TreeNode::getSubtreeWidth).sum());
            node.setSubtreeHeight(1 + node.getChildren().stream().mapToDouble(TreeNode::getSubtreeHeight).max().orElse(1.0));
        }
    }

    private void layoutFirstLevel(TreeNode root) {
        List<TreeNode> children = root.getChildren();
        if (children.isEmpty()) return;
        double angleStep = 360.0 / children.size(); // [cite: 123]
        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);
            double theta = i * angleStep;
            child.setCurrentAbsoluteAngle(theta); // [cite: 183]

            // Hypotenuse Rule
            double radius = Math.sqrt(Math.pow(child.getSubtreeWidth() * UNIT_X, 2) + Math.pow(child.getSubtreeHeight() * UNIT_Y, 2));
            child.setGridX(root.getGridX() + radius * Math.cos(Math.toRadians(theta)));
            child.setGridY(root.getGridY() + radius * Math.sin(Math.toRadians(theta)));

            layoutInward(child, theta); // [cite: 126, 137]
        }
    }

    private void layoutInward(TreeNode parent, double angle) {
        double leftAcc = 0, rightAcc = 0;
        for (TreeNode child : parent.getChildren()) {
            boolean isLeft = leftAcc <= rightAcc; // [cite: 128, 129]
            double halfPi = isLeft ? (Math.PI / 2) : (-Math.PI / 2); // [cite: 130]
            double rad = Math.toRadians(angle);

            // Inward Planar Formulas [cite: 132, 133, 134]
            child.setGridX(parent.getGridX() - Math.cos(rad) * UNIT_X + (1 + leftAcc) * Math.cos(rad + halfPi) * UNIT_X);
            child.setGridY(parent.getGridY() - Math.sin(rad) * UNIT_Y + (1 + leftAcc) * Math.sin(rad + halfPi) * UNIT_Y);
            child.setCurrentAbsoluteAngle(angle);

            layoutInward(child, angle);
            if (isLeft) leftAcc += child.getSubtreeWidth();
            else rightAcc += child.getSubtreeWidth();
        }
    }
}