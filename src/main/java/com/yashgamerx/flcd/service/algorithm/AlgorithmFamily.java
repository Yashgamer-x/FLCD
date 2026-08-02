package com.yashgamerx.flcd.service.algorithm;

/// Top-level choice presented to the user: which layout algorithm (and,
/// by extension, which node type — `FLCDNode` vs `MaximumEdgeLengthNode`
/// — and which view) to run the selected file through.
///
/// These families are intentionally not unified behind a common
/// `calculate(...)` interface — they operate on different node types with
/// different placement strategies, so [com.yashgamerx.flcd.view.AlgorithmSelectorView]
/// switches on this enum directly rather than going through a shared
/// abstraction that would only fit one side comfortably.
public enum AlgorithmFamily {
    FLCD("FLCD (Planar Grid)"),
    FLCD_MAXIMUM_EDGE_LENGTH("FLCD Maximum Edge Length"),
    CIRCLE_MAXIMUM_EDGE_LENGTH("Circle Maximum Edge Length");

    private final String displayName;

    AlgorithmFamily(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}