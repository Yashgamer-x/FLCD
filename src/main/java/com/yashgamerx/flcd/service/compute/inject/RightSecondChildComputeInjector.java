package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.RightSecondChildCompute;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;

public class RightSecondChildComputeInjector implements ComputeInjectable {
    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getComputable();
        if ((!(precomputable instanceof RightSecondChildCompute || precomputable instanceof RootifiedCompute))) {
            abstractNode.setComputable(new RightSecondChildCompute());
        }
    }
}
