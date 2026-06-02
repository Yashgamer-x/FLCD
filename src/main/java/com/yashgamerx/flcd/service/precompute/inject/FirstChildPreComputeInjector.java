package com.yashgamerx.flcd.service.precompute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.FirstChildCompute;
import com.yashgamerx.flcd.service.precompute.FirstChildPreCompute;
import com.yashgamerx.flcd.service.precompute.RootifiedPreCompute;

/// Injects [FirstChildCompute] to the AbstractNode
public class FirstChildPreComputeInjector implements PrecomputeInjectable {
    @Override
    public void inject(AbstractNode firstChild) {
        var precomputable = firstChild.getPrecomputable();
        if ((!(precomputable instanceof FirstChildPreCompute || precomputable instanceof RootifiedPreCompute))) {
            firstChild.setPrecomputable(new FirstChildPreCompute());
        }
    }
}
