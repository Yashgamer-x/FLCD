package com.yashgamerx.flcd.common.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Owned by a single visualization view instance. Every time that view
/// redraws the tree (add node, rootify, readjust, initial load, ...) it
/// appends one [RunRecord] here, so the "Completion Time Graph" button has
/// a running series to plot rather than only ever showing the latest run.
public class MetricsHistory {

    private final List<RunRecord> runs = new ArrayList<>();
    private int nextRunIndex = 1;

    public RunRecord record(int nodeCount, double calculationMillis, double calculationPlusDrawMillis) {
        var run = new RunRecord(nextRunIndex++, nodeCount, calculationMillis, calculationPlusDrawMillis);
        runs.add(run);
        return run;
    }

    public List<RunRecord> getRuns() {
        return Collections.unmodifiableList(runs);
    }

    public boolean isEmpty() {
        return runs.isEmpty();
    }

    public RunRecord latest() {
        return runs.isEmpty() ? null : runs.get(runs.size() - 1);
    }
}
