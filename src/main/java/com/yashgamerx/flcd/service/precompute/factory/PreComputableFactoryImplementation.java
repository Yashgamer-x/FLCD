package com.yashgamerx.flcd.service.precompute.factory;

import com.yashgamerx.flcd.service.precompute.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public class PreComputableFactoryImplementation implements PreComputableFactory {

    // Using highly-optimized EnumMap instead of HashMap for enum keys
    private final Map<PreComputableOption, Precomputable> precomputeCache = new EnumMap<>(PreComputableOption.class);

    // Immutable routing registry mapping options directly to their initialization targets
    private final Map<PreComputableOption, Supplier<Precomputable>> registry = new EnumMap<>(PreComputableOption.class);

    public PreComputableFactoryImplementation() {
        registry.put(PreComputableOption.ROOT, RootifiedPreCompute::new);
        registry.put(PreComputableOption.FIRST_CHILD, FirstChildPreCompute::new);
        registry.put(PreComputableOption.SECOND_CHILD, SecondChildPreCompute::new);
        registry.put(PreComputableOption.HEIGHT_CHILD, HeightChildPreCompute::new);
        registry.put(PreComputableOption.WIDTH_CHILD, WidthChildPreCompute::new);
        registry.put(PreComputableOption.ROOTIFIED_CHILD, RootifiedPreCompute::new);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Precomputable getPreComputable(PreComputableOption option) {
        if (option == null) {
            throw new IllegalArgumentException("PreComputableOption strategy selector cannot be null.");
        }

        return precomputeCache.computeIfAbsent(option, opt -> {
            Supplier<Precomputable> supplier = registry.get(opt);
            if (supplier == null) {
                throw new UnsupportedOperationException("No layout strategy registered for option: " + opt);
            }
            return supplier.get();
        });
    }
}
