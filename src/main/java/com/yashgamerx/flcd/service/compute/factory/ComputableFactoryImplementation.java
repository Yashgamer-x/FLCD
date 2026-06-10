package com.yashgamerx.flcd.service.compute.factory;

import com.yashgamerx.flcd.service.compute.*;
import com.yashgamerx.flcd.service.compute.inject.factory.ComputeInjectorFactory;
import com.yashgamerx.flcd.service.compute.inject.factory.ComputeInjectorFactoryImplementation;

import java.util.EnumMap;
import java.util.Map;

public class ComputableFactoryImplementation implements ComputableFactory {
    private final Map<ComputableOption, Computable> computableCache = new EnumMap<>(ComputableOption.class);
    private final ComputeInjectorFactory computeInjectorFactory = new ComputeInjectorFactoryImplementation(this);

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Computable> T getComputable(ComputableOption option) {
        if (option == null) throw new IllegalArgumentException("Option cannot be null");

        return (T) computableCache.computeIfAbsent(option, key -> switch (key) {
            case ROOT -> new RootCompute(computeInjectorFactory);
            case FIRST_CHILD -> new FirstChildCompute();
            case LEFT_SECOND_CHILD -> new LeftSecondChildCompute(computeInjectorFactory);
            case RIGHT_SECOND_CHILD -> new RightSecondChildCompute();
            case LEFT_HEIGHT_CHILD -> new LeftHeightChildCompute();
            case RIGHT_HEIGHT_CHILD -> new RightHeightChildCompute();
            case LEFT_WIDTH_CHILD -> new LeftWidthChildCompute();
            case RIGHT_WIDTH_CHILD -> new RightWidthChildCompute();
            case ROOTIFIED_CHILD -> new RootifiedCompute(computeInjectorFactory);
            case LEFT_SECOND_ROOTIFIED_CHILD -> new LeftSecondRootifiedCompute(computeInjectorFactory);
            case RIGHT_SECOND_ROOTIFIED_CHILD -> new RightSecondRootifiedCompute(computeInjectorFactory);
        });
    }
}
