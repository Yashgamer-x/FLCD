package com.yashgamerx.flcd.service.precompute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.FirstChildCompute;
import com.yashgamerx.flcd.service.precompute.FirstChildPreCompute;
import com.yashgamerx.flcd.service.precompute.RootifiedPreCompute;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableFactory;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableOption;

/// Injects [FirstChildCompute] to the AbstractNode
public class FirstChildPreComputeInjector implements PrecomputeInjectable {

    private final PreComputableFactory factory;

    public FirstChildPreComputeInjector(PreComputableFactory preComputableFactory) {
        this.factory = preComputableFactory;
    }

    @Override
    public void inject(AbstractNode firstChild) {
        var precomputable = firstChild.getPrecomputable();
        if ((!(precomputable instanceof FirstChildPreCompute || precomputable instanceof RootifiedPreCompute))) {
            firstChild.setPrecomputable(factory.getPreComputable(PreComputableOption.FIRST_CHILD));
        }
    }
}
