package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.FirstChildRootifiedCompute;
import com.yashgamerx.flcd.service.compute.LeftSecondChildCompute;

public class LeftSecondChildComputeInjector implements ComputeInjectable {
    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getComputable();
        if ((!(precomputable instanceof LeftSecondChildCompute || precomputable instanceof FirstChildRootifiedCompute))) {
            abstractNode.setComputable(new LeftSecondChildCompute());
        }
    }
}
