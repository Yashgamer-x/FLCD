package com.yashgamerx.flcd.service.precompute.inject.factory;

import com.yashgamerx.flcd.service.precompute.inject.PrecomputeInjectable;

public interface PreComputeInjectorFactory {
    <T extends PrecomputeInjectable> T getInjector(PreComputeInjectorOption option);
}
