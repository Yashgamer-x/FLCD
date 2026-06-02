package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.FirstChildCompute;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;

public class FirstChildComputeInjector implements ComputeInjectable {
    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getComputable();
        if ((!(precomputable instanceof FirstChildCompute || precomputable instanceof RootifiedCompute))) {
            abstractNode.setComputable(new FirstChildCompute());
        }
    }
}
