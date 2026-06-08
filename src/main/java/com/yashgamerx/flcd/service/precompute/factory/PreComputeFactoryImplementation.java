package com.yashgamerx.flcd.service.precompute.factory;

import com.yashgamerx.flcd.service.precompute.*;

import java.util.HashMap;
import java.util.Map;

public class PreComputeFactoryImplementation implements PreComputeFactory {

    private Map<Class<? extends Precomputable>, Precomputable> precomputeCache = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Precomputable> T getPreComputable(PreComputableOption option) {

        return (T) switch (option) {
            case ROOT -> getRootPreComputable();
            case FIRST_CHILD -> getFirstChildPreCompute();
            case SECOND_CHILD -> getSecondChildPreCompute();
            case HEIGHT_CHILD -> getHeightChildPreCompute();
            case WIDTH_CHILD -> getWidthChildPreCompute();
            case ROOTIFIED_CHILD -> getRootifiedPreCompute();
        };
    }

    private RootifiedPreCompute getRootPreComputable() {
        return (RootifiedPreCompute) precomputeCache.computeIfAbsent(RootPreCompute.class, _ -> new RootifiedPreCompute());
    }

    private FirstChildPreCompute getFirstChildPreCompute() {
        return (FirstChildPreCompute) precomputeCache.computeIfAbsent(FirstChildPreCompute.class, _ -> new FirstChildPreCompute());
    }

    private SecondChildPreCompute getSecondChildPreCompute() {
        return (SecondChildPreCompute) precomputeCache.computeIfAbsent(SecondChildPreCompute.class, _ -> new SecondChildPreCompute());
    }

    private HeightChildPreCompute getHeightChildPreCompute() {
        return (HeightChildPreCompute) precomputeCache.computeIfAbsent(HeightChildPreCompute.class, _ -> new HeightChildPreCompute());
    }

    private WidthChildPreCompute getWidthChildPreCompute() {
        return (WidthChildPreCompute) precomputeCache.computeIfAbsent(WidthChildPreCompute.class, _ -> new WidthChildPreCompute());
    }

    private RootifiedPreCompute getRootifiedPreCompute() {
        return (RootifiedPreCompute) precomputeCache.computeIfAbsent(RootifiedPreCompute.class, _ -> new RootifiedPreCompute());
    }
}
