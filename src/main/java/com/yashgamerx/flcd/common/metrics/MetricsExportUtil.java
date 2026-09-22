package com.yashgamerx.flcd.common.metrics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/// Appends one CSV row per layout run to a comparison-study data file: which
/// algorithm produced the run, which source `.txt` file the tree was parsed
/// from, its size, timing, area/aspect-ratio geometry, and root-to-leaf
/// distance summary stats (min/max/avg only — the per-leaf breakdown shown
/// in [MetricsDialogUtil#showLeafDistancesDialog] is intentionally left out
/// here, since it doesn't belong in a row-per-run comparison table).
///
/// The file is created with a header row the first time it's written to;
/// every call after that appends a single data row, so the same file can
/// accumulate rows across algorithms and runs for the baseline comparison
/// study (Reingold–Tilford / radial / force-directed vs. FLCD/TMEL/CMEL).
public final class MetricsExportUtil {

    private static final String HEADER = String.join(",",
            "algorithm", "sourceFile", "nodeCount",
            "calculationMillis", "calculationPlusDrawMillis", "drawOnlyMillis",
            "width", "height", "area", "aspectRatio",
            "leafCount",
            "straightLineMin", "straightLineMax", "straightLineAvg",
            "pathLengthMin", "pathLengthMax", "pathLengthAvg");

    private MetricsExportUtil() {
    }

    /// Appends one row summarizing `run`/`box`/`leafDistances` for
    /// `algorithmName` — run against `sourceFileName` — to `file`, writing
    /// the header first if the file is new or empty. Throws [IOException]
    /// on write failure so the caller can surface it (e.g. via an error
    /// alert) rather than fail silently.
    public static void appendRecord(java.io.File file, String algorithmName, String sourceFileName, RunRecord run,
                                    BoundingBoxMetrics box, List<RootLeafDistance> leafDistances) throws IOException {
        boolean needsHeader = !file.exists() || file.length() == 0L;

        var stats = summarizeLeafDistances(leafDistances);

        try (var writer = new PrintWriter(new FileWriter(file, true))) {
            if (needsHeader) {
                writer.println(HEADER);
            }
            writer.println(String.join(",",
                    csv(algorithmName),
                    csv(sourceFileName),
                    String.valueOf(run.nodeCount()),
                    fmt(run.calculationMillis()),
                    fmt(run.calculationPlusDrawMillis()),
                    fmt(run.calculationPlusDrawMillis() - run.calculationMillis()),
                    fmt(box.width()),
                    fmt(box.height()),
                    fmt(box.area()),
                    fmt(box.aspectRatio()),
                    String.valueOf(leafDistances.size()),
                    fmt(stats.straightMin()), fmt(stats.straightMax()), fmt(stats.straightAvg()),
                    fmt(stats.pathMin()), fmt(stats.pathMax()), fmt(stats.pathAvg())
            ));
        }
    }

    private static LeafDistanceStats summarizeLeafDistances(List<RootLeafDistance> leafDistances) {
        if (leafDistances.isEmpty()) {
            return new LeafDistanceStats(0, 0, 0, 0, 0, 0);
        }
        double minStraight = Double.POSITIVE_INFINITY, maxStraight = Double.NEGATIVE_INFINITY, sumStraight = 0;
        double minPath = Double.POSITIVE_INFINITY, maxPath = Double.NEGATIVE_INFINITY, sumPath = 0;
        for (var d : leafDistances) {
            minStraight = Math.min(minStraight, d.straightLineDistance());
            maxStraight = Math.max(maxStraight, d.straightLineDistance());
            sumStraight += d.straightLineDistance();
            minPath = Math.min(minPath, d.pathLength());
            maxPath = Math.max(maxPath, d.pathLength());
            sumPath += d.pathLength();
        }
        int n = leafDistances.size();
        return new LeafDistanceStats(minStraight, maxStraight, sumStraight / n, minPath, maxPath, sumPath / n);
    }

    private static String fmt(double value) {
        return String.format("%.4f", value);
    }

    /// Minimal CSV escaping for the one free-text field (algorithm name).
    private static String csv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private record LeafDistanceStats(double straightMin, double straightMax, double straightAvg,
                                     double pathMin, double pathMax, double pathAvg) {
    }
}
