package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.RightWidthChildCompute;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;

public class RightWidthChildComputeInjector implements ComputeInjectable {
    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getComputable();
        if ((!(precomputable instanceof RightWidthChildCompute || precomputable instanceof RootifiedCompute))) {
            abstractNode.setComputable(new RightWidthChildCompute());
        }
    }
}
