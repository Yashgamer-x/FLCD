package com.yashgamerx.flcd.service.compute.inject.factory;

import com.yashgamerx.flcd.service.compute.inject.ComputeInjectable;

public class ComputeInjectorFactoryImplementation implements ComputeInjectorFactory {
    @Override
    public <T extends ComputeInjectable> T getInjector(ComputeInjectorOption option) {
        return null;
    }
}
