package com.yashgamerx.flcd.service.precompute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.precompute.RootifiedPreCompute;
import com.yashgamerx.flcd.service.precompute.SecondChildPreCompute;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableFactory;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableOption;

public class SecondChildPreComputeInjector implements PrecomputeInjectable {

    private final PreComputableFactory factory;

    public SecondChildPreComputeInjector(PreComputableFactory preComputableFactory) {
        this.factory = preComputableFactory;
    }

    @Override
    public void inject(AbstractNode secondChildNode) {
        var preComputable = secondChildNode.getPrecomputable();
        if ((!(preComputable instanceof SecondChildPreCompute || preComputable instanceof RootifiedPreCompute))) {
            secondChildNode.setPrecomputable(factory.getPreComputable(PreComputableOption.SECOND_CHILD));
        }
    }
}
