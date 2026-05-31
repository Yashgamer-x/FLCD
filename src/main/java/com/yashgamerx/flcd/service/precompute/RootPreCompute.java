package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;

public class RootPreCompute implements Precomputable {
    @Override
    public void precompute(AbstractNode root) {
        root.getChildren().forEach(this::injectAndPrecompute);
    }

    private void injectAndPrecompute(AbstractNode firstChild) {
        injectFirstChildPrecomputation(firstChild);
        firstChild.precompute();
    }

    private void injectFirstChildPrecomputation(AbstractNode node) {
        node.setPrecomputable(new FirstChildPreCompute());
    }
}
