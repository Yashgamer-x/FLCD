package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.RightSecondChildCompute;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;
import com.yashgamerx.flcd.service.compute.factory.ComputableFactory;
import com.yashgamerx.flcd.service.compute.factory.ComputableOption;

public class RightSecondChildComputeInjector implements ComputeInjectable {
    private final ComputableFactory computableFactory;

    public RightSecondChildComputeInjector(ComputableFactory computableFactory) {
        this.computableFactory = computableFactory;
    }

    @Override
    public void inject(AbstractNode abstractNode) {
        var precomputable = abstractNode.getComputable();
        if ((!(precomputable instanceof RightSecondChildCompute || precomputable instanceof RootifiedCompute))) {
            abstractNode.setComputable(computableFactory.getComputable(ComputableOption.RIGHT_SECOND_CHILD));
        }

        if (precomputable instanceof RootifiedCompute) {
            abstractNode.setComputable(computableFactory.getComputable(ComputableOption.RIGHT_SECOND_ROOTIFIED_CHILD));
        }
    }
}
