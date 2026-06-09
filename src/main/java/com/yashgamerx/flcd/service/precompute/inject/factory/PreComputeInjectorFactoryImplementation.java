package com.yashgamerx.flcd.service.precompute.inject.factory;

import com.yashgamerx.flcd.service.precompute.factory.PreComputableFactory;
import com.yashgamerx.flcd.service.precompute.inject.*;

import java.util.EnumMap;
import java.util.Map;

public class PreComputeInjectorFactoryImplementation implements PreComputeInjectorFactory {
    private final Map<PreComputeInjectorOption, PrecomputeInjectable> precomputeCache = new EnumMap<>(PreComputeInjectorOption.class);
    private final PreComputableFactory preComputableFactory;

    public PreComputeInjectorFactoryImplementation(PreComputableFactory preComputableFactory) {
        this.preComputableFactory = preComputableFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PrecomputeInjectable> T getInjector(PreComputeInjectorOption option) {
        if (option == null) throw new IllegalArgumentException("Option cannot be null");

        // The cache breaks circular dependencies because an instance can be registered
        // in the cache BEFORE its recursive pipeline steps execute!
        return (T) precomputeCache.computeIfAbsent(option, opt -> switch (opt) {
            case FIRST_CHILD -> new FirstChildPreComputeInjector(preComputableFactory);
            case SECOND_CHILD -> new SecondChildPreComputeInjector(preComputableFactory);
            case HEIGHT_CHILD -> new HeightChildPreComputeInjector(preComputableFactory);
            case WIDTH_CHILD -> new WidthChildPreComputeInjector(preComputableFactory);
        });
    }
}
