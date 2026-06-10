package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.FirstChildCompute;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;
import com.yashgamerx.flcd.service.compute.factory.ComputableFactory;
import com.yashgamerx.flcd.service.compute.factory.ComputableOption;

/// Injects [FirstChildCompute] to the AbstractNode
public class FirstChildComputeInjector implements ComputeInjectable {
    private final ComputableFactory computableFactory;

    public FirstChildComputeInjector(ComputableFactory computableFactory) {
        this.computableFactory = computableFactory;
    }

    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getComputable();
        if ((!(precomputable instanceof FirstChildCompute || precomputable instanceof RootifiedCompute))) {
            abstractNode.setComputable(computableFactory.getComputable(ComputableOption.FIRST_CHILD));
        }
    }
}
