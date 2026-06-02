package com.yashgamerx.flcd.service.precompute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.precompute.RootifiedPreCompute;
import com.yashgamerx.flcd.service.precompute.SecondChildPreCompute;

public class SecondChildPreComputeInjector implements PrecomputeInjectable {
    @Override
    public void inject(AbstractNode secondChildNode) {
        var preComputable = secondChildNode.getPrecomputable();
        if ((!(preComputable instanceof SecondChildPreCompute || preComputable instanceof RootifiedPreCompute))) {
            secondChildNode.setPrecomputable(new SecondChildPreCompute());
        }
    }
}
