package com.yashgamerx.flcd.service.precompute.factory;

import com.yashgamerx.flcd.service.precompute.*;

import java.util.HashMap;
import java.util.Map;

public class PreComputeFactoryImplementation implements PreComputeFactory {

    private Map<Class<? extends Precomputable>, Precomputable> precomputeCache = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public Precomputable getPreComputable(PreComputableOption option) {

        return switch (option) {
            case ROOT -> getRootPreComputable();
            case FIRST_CHILD -> getFirstChildPreCompute();
            case SECOND_CHILD -> getSecondChildPreCompute();
            case HEIGHT_CHILD -> getHeightChildPreCompute();
            case WIDTH_CHILD -> getWidthChildPreCompute();
            case ROOTIFIED_CHILD -> getRootifiedPreCompute();
        };
    }

    private Precomputable getRootPreComputable() {
        return precomputeCache.computeIfAbsent(RootPreCompute.class, _ -> new RootifiedPreCompute());
    }

    private Precomputable getFirstChildPreCompute() {
        return precomputeCache.computeIfAbsent(FirstChildPreCompute.class, _ -> new FirstChildPreCompute());
    }

    private Precomputable getSecondChildPreCompute() {
        return precomputeCache.computeIfAbsent(SecondChildPreCompute.class, _ -> new SecondChildPreCompute());
    }

    private Precomputable getHeightChildPreCompute() {
        return precomputeCache.computeIfAbsent(HeightChildPreCompute.class, _ -> new HeightChildPreCompute());
    }

    private Precomputable getWidthChildPreCompute() {
        return precomputeCache.computeIfAbsent(WidthChildPreCompute.class, _ -> new WidthChildPreCompute());
    }

    private Precomputable getRootifiedPreCompute() {
        return precomputeCache.computeIfAbsent(RootifiedPreCompute.class, _ -> new RootifiedPreCompute());
    }
}
