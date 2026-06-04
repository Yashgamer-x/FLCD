package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.LeftSecondChildCompute;
import com.yashgamerx.flcd.service.compute.LeftSecondRootifiedCompute;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;

public class LeftSecondChildComputeInjector implements ComputeInjectable {
    @Override
    public void inject(AbstractNode abstractNode) {
        var computable = abstractNode.getComputable();
        if ((!(computable instanceof LeftSecondChildCompute || computable instanceof RootifiedCompute))) {
            abstractNode.setComputable(new LeftSecondChildCompute());
        }

        if (computable instanceof RootifiedCompute) {
            abstractNode.setComputable(new LeftSecondRootifiedCompute());
        }
    }
}
