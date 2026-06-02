package com.yashgamerx.flcd.service.precompute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.precompute.HeightChildPreCompute;
import com.yashgamerx.flcd.service.precompute.RootifiedPreCompute;

/// Injects [HeightChildPreCompute] dependency to the node
public class HeightChildPreComputeInjector implements PrecomputeInjectable {
    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getPrecomputable();
        if ((!(precomputable instanceof HeightChildPreCompute || precomputable instanceof RootifiedPreCompute))) {
            abstractNode.setPrecomputable(new HeightChildPreCompute());
        }
    }
}
