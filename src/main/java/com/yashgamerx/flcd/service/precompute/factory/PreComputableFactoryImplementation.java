package com.yashgamerx.flcd.service.precompute.factory;

import com.yashgamerx.flcd.service.compute.inject.factory.ComputeInjectorFactory;
import com.yashgamerx.flcd.service.precompute.*;
import com.yashgamerx.flcd.service.precompute.inject.factory.PreComputeInjectorFactory;
import com.yashgamerx.flcd.service.precompute.inject.factory.PreComputeInjectorFactoryImplementation;

import java.util.EnumMap;
import java.util.Map;

public class PreComputableFactoryImplementation implements PreComputableFactory {

    private final Map<PreComputableOption, Precomputable> precomputeCache = new EnumMap<>(PreComputableOption.class);
    private final PreComputeInjectorFactory preComputeInjectorFactory = new PreComputeInjectorFactoryImplementation(this);
    private final ComputeInjectorFactory computeInjectorFactory;

    public PreComputableFactoryImplementation(ComputeInjectorFactory computeInjectorFactory) {
        this.computeInjectorFactory = computeInjectorFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Precomputable> T getPreComputable(PreComputableOption option) {
        if (option == null) throw new IllegalArgumentException("Option cannot be null");

        // The cache breaks circular dependencies because an instance can be registered
        // in the cache BEFORE its recursive pipeline steps execute!
        return (T) precomputeCache.computeIfAbsent(option, opt -> switch (opt) {
            case ROOT -> new RootPreCompute(this);
            case FIRST_CHILD -> new FirstChildPreCompute(preComputeInjectorFactory, computeInjectorFactory);
            case SECOND_CHILD -> new SecondChildPreCompute(preComputeInjectorFactory);
            case HEIGHT_CHILD -> new HeightChildPreCompute(preComputeInjectorFactory);
            case WIDTH_CHILD -> new WidthChildPreCompute(preComputeInjectorFactory);
            case ROOTIFIED_CHILD -> new RootifiedPreCompute(this);
        });
    }
}
