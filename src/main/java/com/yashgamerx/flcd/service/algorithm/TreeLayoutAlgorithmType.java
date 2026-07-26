package com.yashgamerx.flcd.service.algorithm;

import java.util.function.Supplier;

/// Selectable algorithms for the FLCD (`FLCDNode`-based) visualization
/// flow only. Maximum Edge Length has its own node type and its own
/// standalone view/entry point — see `MaximumEdgeLengthVisualizationView`
/// — rather than living behind this shared combo box.
public enum TreeLayoutAlgorithmType {
    PLANAR_GRID("Planar Grid", PlanarGridAlgorithm::new);

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
