package com.yashgamerx.flcd.service.precompute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.precompute.HeightChildPreCompute;
import com.yashgamerx.flcd.service.precompute.RootifiedPreCompute;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableFactory;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableOption;

/// Injects [HeightChildPreCompute] dependency to the node
public class HeightChildPreComputeInjector implements PrecomputeInjectable {

    private final PreComputableFactory factory;

    public HeightChildPreComputeInjector(PreComputableFactory factory) {
        this.factory = factory;
    }

    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getPrecomputable();
        if ((!(precomputable instanceof HeightChildPreCompute || precomputable instanceof RootifiedPreCompute))) {
            abstractNode.setPrecomputable(factory.getPreComputable(PreComputableOption.HEIGHT_CHILD));
        }
    }
}
