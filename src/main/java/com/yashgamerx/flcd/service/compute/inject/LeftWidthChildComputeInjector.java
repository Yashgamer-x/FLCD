package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.LeftWidthChildCompute;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;
import com.yashgamerx.flcd.service.compute.factory.ComputableFactory;
import com.yashgamerx.flcd.service.compute.factory.ComputableOption;

public class LeftWidthChildComputeInjector implements ComputeInjectable {
    private final ComputableFactory computableFactory;

    public LeftWidthChildComputeInjector(ComputableFactory computableFactory) {
        this.computableFactory = computableFactory;
    }

    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getComputable();
        if ((!(precomputable instanceof LeftWidthChildCompute || precomputable instanceof RootifiedCompute))) {
            abstractNode.setComputable(computableFactory.getComputable(ComputableOption.LEFT_WIDTH_CHILD));
        }
    }
}
