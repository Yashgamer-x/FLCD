package com.yashgamerx.flcd.service.compute.factory;

import com.yashgamerx.flcd.service.compute.inject.ComputeInjectable;

public class ComputableFactoryImplementation implements ComputableFactory {
    @Override
    public <T extends ComputeInjectable> T getComputable(ComputableOption option) {
        return null;
    }
}
