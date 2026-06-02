package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.RightHeightChildCompute;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;

public class RightHeightChildComputeInjector implements ComputeInjectable {
    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getComputable();
        if ((!(precomputable instanceof RightHeightChildCompute || precomputable instanceof RootifiedCompute))) {
            abstractNode.setComputable(new RightHeightChildCompute());
        }
    }
}
