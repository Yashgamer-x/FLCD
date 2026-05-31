package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;

public class FirstChildPreCompute implements Precomputable {
    @Override
    public void precompute(AbstractNode node) {
        if (isLeafNode(node)) {
            initializeLeafDimensions(node);
            return;
        }

        // Invokes precompute recursively on all children
        node.getChildren().forEach(this::precompute);
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
