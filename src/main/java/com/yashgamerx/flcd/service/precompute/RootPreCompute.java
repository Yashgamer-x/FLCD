package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;

public class RootPreCompute implements Precomputable {
    @Override
    public void precompute(AbstractNode root) {
        // Inject First Child Precomputation dependency for all children of root
        root.getChildren().forEach(this::injectFirstChildPrecomputation);

        // Invokes precompute recursively on all children
        root.getChildren().forEach(AbstractNode::precompute);
    }

    private void injectFirstChildPrecomputation(AbstractNode node) {
        node.setPrecomputable(new FirstChildPreCompute());
    }
}
