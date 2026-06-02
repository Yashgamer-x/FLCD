package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.FirstChildCompute;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;

/// Injects [FirstChildCompute] to the AbstractNode
public class FirstChildComputeInjector implements ComputeInjectable {
    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getComputable();
        if ((!(precomputable instanceof FirstChildCompute || precomputable instanceof RootifiedCompute))) {
            abstractNode.setComputable(new FirstChildCompute());
        }
    }
}
