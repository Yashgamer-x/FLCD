package com.yashgamerx.flcd.common.metrics;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;

import java.util.Comparator;
import java.util.List;

/// Builds the read-only, selectable-text `Alert` popups used by every
/// algorithm view for the Metrics / Aspect Ratio / Completion Graph
/// toolbar buttons. Kept here once instead of duplicated per view.
public final class MetricsDialogUtil {

    private MetricsDialogUtil() {
    }

    /// Full metrics dialog: timing (calculation-only vs. calculation+draw),
    /// area consumption, aspect ratio, and root-to-leaf distance summary.
    public static void showMetricsDialog(String algorithmName, RunRecord run,
                                         BoundingBoxMetrics box, List<RootLeafDistance> leafDistances) {
        var sb = new StringBuilder();

        sb.append("Algorithm: ").append(algorithmName).append('\n');
        sb.append("Nodes:     ").append(run.nodeCount()).append("\n\n");

        sb.append("-- Timing --\n");
        sb.append(String.format("Calculation only:        %.3f ms%n", run.calculationMillis()));
        sb.append(String.format("Calculation + drawing:    %.3f ms%n", run.calculationPlusDrawMillis()));
        sb.append(String.format("Drawing only (derived):   %.3f ms%n%n",
                run.calculationPlusDrawMillis() - run.calculationMillis()));

        sb.append("-- Area Consumption --\n");
        sb.append(String.format("Width:   %.2f%n", box.width()));
        sb.append(String.format("Height:  %.2f%n", box.height()));
        sb.append(String.format("Area:    %.2f%n%n", box.area()));

        sb.append("-- Visibility Ratio (Aspect Ratio) --\n");
        sb.append(String.format("Width / Height: %.4f%n%n", box.aspectRatio()));

        sb.append("-- Center-to-Center Distance: Root to Leaves --\n");
        appendLeafDistanceSummary(sb, leafDistances);

        showTextAlert("Metrics — " + algorithmName, "Layout Metrics", sb.toString(), 420, 420);
    }

    /// Standalone Aspect Ratio ("Visibility Ratio") popup — its own toolbar
    /// button per request, separate from the combined Metrics dialog.
    public static void showAspectRatioDialog(BoundingBoxMetrics box) {
        String content = String.format(
                "Width:  %.2f%n" +
                        "Height: %.2f%n" +
                        "Visibility Ratio (Width / Height): %.4f%n%n" +
                        "%s",
                box.width(), box.height(), box.aspectRatio(),
                box.aspectRatio() > 1.0 ? "Layout is wider than it is tall."
                        : box.aspectRatio() < 1.0 ? "Layout is taller than it is wide."
                        : "Layout is perfectly square."
        );
        showTextAlert("Visibility Ratio", "Aspect Ratio (Width / Height)", content, 360, 160);
    }

    /// Detailed per-leaf root distance popup (straight-line + along-edge path length).
    public static void showLeafDistancesDialog(List<RootLeafDistance> leafDistances) {
        var sb = new StringBuilder();
        appendLeafDistanceSummary(sb, leafDistances);
        sb.append("\n-- Per-Leaf Detail --\n");
        for (var d : leafDistances.stream()
                .sorted(Comparator.comparingInt(RootLeafDistance::leafId)).toList()) {
            sb.append(String.format("Leaf #%-4d depth=%-3d straight=%.4f  path=%.4f%n",
                    d.leafId(), d.depth(), d.straightLineDistance(), d.pathLength()));
        }
        showTextAlert("Root-to-Leaf Distances", "Center-to-Center Distance: Root to Leaves", sb.toString(), 420, 420);
    }

    private static void appendLeafDistanceSummary(StringBuilder sb, List<RootLeafDistance> leafDistances) {
        if (leafDistances.isEmpty()) {
            sb.append("No leaves found.\n");
            return;
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
        sb.append(String.format("Leaves: %d%n", n));
        sb.append(String.format("Straight-line — min: %.4f  max: %.4f  avg: %.4f%n",
                minStraight, maxStraight, sumStraight / n));
        sb.append(String.format("Along-edge path — min: %.4f  max: %.4f  avg: %.4f%n",
                minPath, maxPath, sumPath / n));
    }

    private static void showTextAlert(String title, String header, String content, double width, double height) {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);

        var textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(width);
        textArea.setPrefHeight(height);
        textArea.setStyle("-fx-font-family: monospace;");

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }
}
