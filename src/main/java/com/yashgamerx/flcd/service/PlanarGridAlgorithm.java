package com.yashgamerx.flcd.service;

import com.yashgamerx.flcd.model.TreeNode;
import lombok.extern.java.Log;

@Log
public class PlanarGridAlgorithm implements TreeLayoutAlgorithm {

    // The distance between the root and level 1 [cite: 119, 120]
    private static final double INITIAL_RADIUS = 200.0;

    @Override
    public void calculateLayout(TreeNode root, double originX, double originY) {
        if (root == null) return;

        // Step 1: Root is the center of the grid [cite: 80, 81]
        root.setGridX(originX);
        root.setGridY(originY);
        root.setCurrentAbsoluteAngle(0);

        var children = root.getChildren();
        if (children.isEmpty()) return;

        // Step 2: Circular placement of Level 1 [cite: 118, 123]
        double angleStep = 360.0 / children.size();

        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);
            double assignedAngle = i * angleStep;

            // Store the angle in the node for future rootification/readjustment
            child.setCurrentAbsoluteAngle(assignedAngle);

            // Convert to Cartesian for the initial grid placement
            double radians = Math.toRadians(assignedAngle);
            child.setGridX(originX + INITIAL_RADIUS * Math.cos(radians));
            child.setGridY(originY + INITIAL_RADIUS * Math.sin(radians));

            log.info("Node " + child.getIdentifier() + " positioned at angle " + assignedAngle);

            // We stop here for Step 2. Recursive calls for Step 3 will go here later.
        }
    }
}
