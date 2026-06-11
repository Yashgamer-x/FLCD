package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.LeftHeightChildCompute;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;

public class LeftHeightChildComputeInjector implements ComputeInjectable {
    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getComputable();
        if ((!(precomputable instanceof LeftHeightChildCompute || precomputable instanceof RootifiedCompute))) {
            abstractNode.setComputable(new LeftHeightChildCompute());
        }
    }
}
