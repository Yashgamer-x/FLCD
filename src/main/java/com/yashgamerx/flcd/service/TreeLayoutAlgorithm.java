package com.yashgamerx.flcd.service;

import com.yashgamerx.flcd.model.AbstractNode;

public interface TreeLayoutAlgorithm {
    /**
     * Executes the specific math to position nodes on the grid.
     * Implementation will handle the circular distribution of level 1 nodes.
     */
    void calculate(AbstractNode root, double originX, double originY);
}
