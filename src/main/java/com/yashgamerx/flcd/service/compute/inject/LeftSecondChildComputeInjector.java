package com.yashgamerx.flcd.service.compute.inject;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.compute.LeftSecondChildCompute;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;
import com.yashgamerx.flcd.service.compute.factory.ComputableFactory;
import com.yashgamerx.flcd.service.compute.factory.ComputableOption;

public class LeftSecondChildComputeInjector implements ComputeInjectable {
    private final ComputableFactory computableFactory;

    public LeftSecondChildComputeInjector(ComputableFactory computableFactory) {
        this.computableFactory = computableFactory;
    }

    @Override
    public void inject(AbstractNode abstractNode) {
        var computable = abstractNode.getComputable();
        if ((!(computable instanceof LeftSecondChildCompute || computable instanceof RootifiedCompute))) {
            abstractNode.setComputable(new LeftSecondChildCompute());
        }

        if (computable instanceof RootifiedCompute) {
            abstractNode.setComputable(computableFactory.getComputable(ComputableOption.LEFT_SECOND_CHILD));
        }
    }
}
