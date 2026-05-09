package com.yashgamerx.flcd.service;

import com.yashgamerx.flcd.model.TreeNode;

public interface TreeLayoutAlgorithm {
    /**
     * Executes the specific math to position nodes on the grid.
     * Implementation will handle the circular distribution of level 1 nodes[cite: 80, 118].
     */
    void calculateLayout(TreeNode root, double originX, double originY);
}
