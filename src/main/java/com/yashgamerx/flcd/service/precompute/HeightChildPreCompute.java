package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;

public class HeightChildPreCompute implements Precomputable {
    @Override
    public void precompute(AbstractNode heightNode) {
        if (isLeafNode(heightNode)) {
            initializeLeafDimensions(heightNode);
            return;
        }

        //TODO: Implement
    }

    /// Checks if the node has no children
    private boolean isLeafNode(AbstractNode node) {
        return node == null || node.getChildren().isEmpty();
    }

    /// Initializes leaf node dimensions
    private void initializeLeafDimensions(AbstractNode node) {
        node.setSubtreeWidth(10.0);
        node.setSubtreeHeight(10.0);
    }
}
