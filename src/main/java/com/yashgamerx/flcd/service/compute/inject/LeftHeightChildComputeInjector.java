package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.FirstChildRootifiedCompute;
import com.yashgamerx.flcd.service.compute.LeftHeightChildCompute;

public class LeftHeightChildComputeInjector implements ComputeInjectable {
    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getComputable();
        if ((!(precomputable instanceof LeftHeightChildCompute || precomputable instanceof FirstChildRootifiedCompute))) {
            abstractNode.setComputable(new LeftHeightChildCompute());
        }
    }
}
