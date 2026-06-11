package com.yashgamerx.flcd.service.precompute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.precompute.RootifiedPreCompute;
import com.yashgamerx.flcd.service.precompute.WidthChildPreCompute;

/// Injects [WidthChildPreCompute] dependency and then precomputes that child.
public class WidthChildPreComputeInjector implements PrecomputeInjectable {
    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getPrecomputable();
        if ((!(precomputable instanceof WidthChildPreCompute || precomputable instanceof RootifiedPreCompute))) {
            abstractNode.setPrecomputable(new WidthChildPreCompute());
        }
    }
}
