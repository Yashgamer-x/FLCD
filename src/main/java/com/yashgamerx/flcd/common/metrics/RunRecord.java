package com.yashgamerx.flcd.common.metrics;

/// One recorded layout run, timed at two granularities:
///  - `calculationMillis`  — pure algorithmic layout time (the
///    `layoutAlgorithm.calculate(...)` call only).
///  - `calculationPlusDrawMillis` — calculation time plus the time to
///    render (build/attach) all JavaFX nodes for the tree.
///
/// Both are kept side by side (rather than just a single "completion time")
/// so the two costs — pure layout math vs. layout + rendering — can be
/// compared and each plotted on its own series in the completion-time graph.
public record RunRecord(int runIndex, int nodeCount,
                        double calculationMillis, double calculationPlusDrawMillis) {
}
