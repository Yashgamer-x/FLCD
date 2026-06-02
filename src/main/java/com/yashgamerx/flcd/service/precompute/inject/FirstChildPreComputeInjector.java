package com.yashgamerx.flcd.service.precompute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.FirstChildCompute;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;

/// Injects [FirstChildCompute] to the AbstractNode
public class FirstChildPreComputeInjector implements PrecomputeInjectable {
    @Override
    public void inject(AbstractNode firstChild) {
        var computable = firstChild.getComputable();
        if ((!(computable instanceof FirstChildCompute || computable instanceof RootifiedCompute))) {
            firstChild.setComputable(new FirstChildCompute());
        }
    }
}
