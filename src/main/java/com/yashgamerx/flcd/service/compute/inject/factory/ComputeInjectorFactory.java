package com.yashgamerx.flcd.service.compute.inject.factory;

import com.yashgamerx.flcd.service.compute.inject.ComputeInjectable;

public interface ComputeInjectorFactory {
    <T extends ComputeInjectable> T getInjector(ComputeInjectorOption option);
}
