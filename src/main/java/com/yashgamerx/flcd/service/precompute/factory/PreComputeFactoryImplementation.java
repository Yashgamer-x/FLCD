package com.yashgamerx.flcd.service.precompute.factory;

import com.yashgamerx.flcd.service.precompute.Precomputable;

import java.util.HashMap;
import java.util.Map;

public class PreComputeFactoryImplementation implements PreComputeFactory {

    private Map<Class<? extends Precomputable>, Precomputable> precomputeCache = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Precomputable> T getPreComputable(Class<T> type) {
        return (T) precomputeCache.computeIfAbsent(type, k -> {
            try {
                return k.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Cannot instantiate " + k, e);
            }
        });
    }
}
