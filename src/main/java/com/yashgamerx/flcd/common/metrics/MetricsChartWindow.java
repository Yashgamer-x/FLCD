package com.yashgamerx.flcd.common.metrics;

import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/// Plots the [MetricsHistory] of a view as a line chart — one series for
/// calculation-only time, one for calculation+draw time — both against the
/// run index, so repeated actions (add node, rootify, readjust, ...) show
/// how completion time trends as the tree changes.
public final class MetricsChartWindow {

    private MetricsChartWindow() {
    }

    public static void show(String algorithmName, List<RunRecord> runs) {
        if (runs.isEmpty()) {
            var alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Completion Time Graph");
            alert.setHeaderText(null);
            alert.setContentText("No recorded runs yet — draw or modify the tree first.");
            alert.showAndWait();
            return;
        }

        var xAxis = new NumberAxis();
        xAxis.setLabel("Run #");
        var yAxis = new NumberAxis();
        yAxis.setLabel("Time (ms)");

        var chart = new LineChart<Number, Number>(xAxis, yAxis);
        chart.setTitle("Completion Time — " + algorithmName);
        chart.setAnimated(false);

        var calcSeries = new XYChart.Series<Number, Number>();
        calcSeries.setName("Calculation only");
        var totalSeries = new XYChart.Series<Number, Number>();
        totalSeries.setName("Calculation + drawing");

        for (var run : runs) {
            calcSeries.getData().add(new XYChart.Data<>(run.runIndex(), run.calculationMillis()));
            totalSeries.getData().add(new XYChart.Data<>(run.runIndex(), run.calculationPlusDrawMillis()));
        }

        chart.getData().addAll(calcSeries, totalSeries);

        var caption = new Label("Node count on latest run: " + runs.get(runs.size() - 1).nodeCount());
        caption.setStyle("-fx-font-size: 10px; -fx-text-fill: #666; -fx-padding: 4;");

        var root = new javafx.scene.layout.BorderPane();
        root.setCenter(chart);
        root.setBottom(caption);

        var stage = new Stage();
        stage.setTitle("Completion Time Graph — " + algorithmName);
        stage.setScene(new Scene(root, 640, 460));
        stage.initModality(Modality.NONE);
        stage.show();
    }
}
