package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.LeftHeightChildCompute;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;
import com.yashgamerx.flcd.service.compute.factory.ComputableFactory;
import com.yashgamerx.flcd.service.compute.factory.ComputableOption;

public class LeftHeightChildComputeInjector implements ComputeInjectable {
    private final ComputableFactory computableFactory;

    public LeftHeightChildComputeInjector(ComputableFactory computableFactory) {
        this.computableFactory = computableFactory;
    }

    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getComputable();
        if ((!(precomputable instanceof LeftHeightChildCompute || precomputable instanceof RootifiedCompute))) {
            abstractNode.setComputable(computableFactory.getComputable(ComputableOption.LEFT_HEIGHT_CHILD));
        }
    }
}
