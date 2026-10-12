package com.yashgamerx.flcd.common.visual;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.transform.Scale;

/// Small floating label showing only a node's name, used by the "Show Name
/// Only" mode of every tree visualization view.
///
/// The panel lives inside the zoomed drawing canvas, so on its own it would
/// shrink with the tree and become unreadable when zoomed out. It therefore
/// carries a counter-scale (the inverse of the canvas scale, pivoting on its
/// top-left corner so it stays attached to its node) which keeps it at the
/// same on-screen size at every zoom level. Views must call
/// [#compensateForZoom(double)] whenever the canvas scale changes.
public final class NodeNamePanel extends HBox {

    private final Scale counterScale = new Scale(1.0, 1.0, 0.0, 0.0);

    public NodeNamePanel(String name, double layoutX, double layoutY, double canvasScale) {
        var nameLabel = new Label((name == null || name.isBlank()) ? "—" : name);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

        getChildren().add(nameLabel);
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: white; "
                + "-fx-border-color: #555; "
                + "-fx-border-width: 1; "
                + "-fx-border-radius: 5; "
                + "-fx-background-radius: 5; "
                + "-fx-padding: 4 8 4 8;");
        setLayoutX(layoutX);
        setLayoutY(layoutY);
        getTransforms().add(counterScale);
        compensateForZoom(canvasScale);

        // Clicks inside the panel must not bubble up to the canvas drag handler
        setOnMouseClicked(javafx.event.Event::consume);
        setOnMousePressed(javafx.event.Event::consume);
    }

    /// Keeps the panel the same on-screen size for the given canvas scale.
    public void compensateForZoom(double canvasScale) {
        double inverse = 1.0 / canvasScale;
        counterScale.setX(inverse);
        counterScale.setY(inverse);
    }
}
