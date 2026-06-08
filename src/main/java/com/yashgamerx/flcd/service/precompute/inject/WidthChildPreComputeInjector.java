package com.yashgamerx.flcd.service.precompute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.precompute.RootifiedPreCompute;
import com.yashgamerx.flcd.service.precompute.WidthChildPreCompute;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableFactory;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableOption;

/// Injects [WidthChildPreCompute] dependency and then precomputes that child.
public class WidthChildPreComputeInjector implements PrecomputeInjectable {

    private final PreComputableFactory factory;

    public WidthChildPreComputeInjector(PreComputableFactory factory) {
        this.factory = factory;
    }

    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getPrecomputable();
        if ((!(precomputable instanceof WidthChildPreCompute || precomputable instanceof RootifiedPreCompute))) {
            abstractNode.setPrecomputable(factory.getPreComputable(PreComputableOption.WIDTH_CHILD));
        }
    }
}
