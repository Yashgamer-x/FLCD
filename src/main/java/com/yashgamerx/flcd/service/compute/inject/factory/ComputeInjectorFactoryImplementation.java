package com.yashgamerx.flcd.service.compute.inject.factory;

import com.yashgamerx.flcd.service.compute.factory.ComputableFactory;
import com.yashgamerx.flcd.service.compute.inject.*;

import java.util.EnumMap;
import java.util.Map;

public class ComputeInjectorFactoryImplementation implements ComputeInjectorFactory {
    private final Map<ComputeInjectorOption, ComputeInjectable> computeInjectorCache = new EnumMap<>(ComputeInjectorOption.class);
    private final ComputableFactory computableFactory;

    public ComputeInjectorFactoryImplementation(ComputableFactory computableFactory) {
        this.computableFactory = computableFactory;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T extends ComputeInjectable> T getInjector(ComputeInjectorOption option) {
        if (option == null) throw new IllegalArgumentException("Option cannot be null");

        return (T) computeInjectorCache.computeIfAbsent(option, key -> switch (key) {
            case FIRST_CHILD -> new FirstChildComputeInjector(computableFactory);
            case LEFT_SECOND_CHILD -> new LeftSecondChildComputeInjector(computableFactory);
            case RIGHT_SECOND_CHILD -> new RightSecondChildComputeInjector();
            case LEFT_HEIGHT_CHILD -> new LeftHeightChildComputeInjector();
            case RIGHT_HEIGHT_CHILD -> new RightHeightChildComputeInjector();
            case LEFT_WIDTH_CHILD -> new LeftWidthChildComputeInjector();
            case RIGHT_WIDTH_CHILD -> new RightWidthChildComputeInjector();
        });
    }
}
