package com.yashgamerx.flcd.flcd.algorithm;

import com.yashgamerx.flcd.flcd.model.FLCDNode;

public interface TreeLayoutAlgorithm {
    /**
     * Executes the specific math to position nodes on the grid.
     * Implementation will handle the circular distribution of level 1 nodes.
     */
    void calculate(FLCDNode root, double originX, double originY);
}
