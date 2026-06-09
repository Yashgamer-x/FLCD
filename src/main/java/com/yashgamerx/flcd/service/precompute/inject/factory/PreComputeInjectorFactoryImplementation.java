package com.yashgamerx.flcd.service.precompute.inject.factory;

import com.yashgamerx.flcd.service.precompute.*;
import com.yashgamerx.flcd.service.precompute.inject.PrecomputeInjectable;

import java.util.EnumMap;
import java.util.Map;

public class PreComputeInjectorFactoryImplementation implements PreComputeInjectorFactory {
    private final Map<PreComputeInjectorOption, PrecomputeInjectable> precomputeCache = new EnumMap<>(PreComputeInjectorOption.class);

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PrecomputeInjectable> T getInjector(PreComputeInjectorOption option) {
        if (option == null) throw new IllegalArgumentException("Option cannot be null");

        // The cache breaks circular dependencies because an instance can be registered
        // in the cache BEFORE its recursive pipeline steps execute!
        return (T) precomputeCache.computeIfAbsent(option, opt -> switch (opt) {
            case ROOT -> new RootPreCompute(this);
            case FIRST_CHILD -> new FirstChildPreCompute(this);
            case SECOND_CHILD -> new SecondChildPreCompute(this);
            case HEIGHT_CHILD -> new HeightChildPreCompute(this);
            case WIDTH_CHILD -> new WidthChildPreCompute(this);
            case ROOTIFIED_CHILD -> new RootifiedPreCompute(this);
        });
    }
}
