package com.yashgamerx.flcd.service.algorithm;

import java.util.function.Supplier;

public enum TreeLayoutAlgorithmType {
    PLANAR_GRID("Planar Grid", PlanarGridAlgorithm::new),
    MAXIMUM_EDGE_LENGTH("Maximum Edge Length", MaximumEdgeLengthAlgorithm::new);

    private final String displayName;
    private final Supplier<TreeLayoutAlgorithm> factory;

    TreeLayoutAlgorithmType(String displayName, Supplier<TreeLayoutAlgorithm> factory) {
        this.displayName = displayName;
        this.factory = factory;
    }

    public TreeLayoutAlgorithm create() {
        return factory.get();
    }

    @Override
    public String toString() {
        return displayName;
    }
}