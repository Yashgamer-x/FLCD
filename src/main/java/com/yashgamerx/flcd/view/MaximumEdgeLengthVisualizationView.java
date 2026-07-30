package com.yashgamerx.flcd.view;

import com.yashgamerx.flcd.model.MaximumEdgeLengthNode;
import com.yashgamerx.flcd.service.algorithm.MaximumEdgeLengthAlgorithm;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.yashgamerx.flcd.model.MaximumEdgeLengthNode.NODE_DIAMETER;
import static com.yashgamerx.flcd.model.MaximumEdgeLengthNode.NODE_RADIUS;

/// Read-only visualization for [MaximumEdgeLengthNode] trees.
///
/// Unlike [FLCDTreeVisualizationView], there is no toolbar and no
/// interaction modes — no Rootify, Readjust, or Calculate Area. This view
/// only draws the computed layout and lets the user click a node to see
/// its info panel. It is intentionally its own class rather than a
/// stripped-down mode of the FLCD view, since it renders a different node
/// type produced by a different algorithm.
@Log
public class MaximumEdgeLengthVisualizationView extends BorderPane {

    private final Map<Integer, MaximumEdgeLengthNode> nodeMap;
    private final MaximumEdgeLengthAlgorithm layoutAlgorithm;

    private final Pane drawingCanvas;
    private final ScrollPane scrollPaneContainer;

    private final Map<Integer, VBox> openInfoPanels = new HashMap<>();

    private double mouseDragAnchorX;
    private double mouseDragAnchorY;
    private static final double VIRTUAL_CANVAS_SIZE = 8000.0;
    private static final double ZOOM_INTENSITY = 0.1;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;

    public MaximumEdgeLengthVisualizationView(final Map<Integer, MaximumEdgeLengthNode> nodeMap,
                                              final MaximumEdgeLengthAlgorithm algorithm) {
        this.nodeMap = nodeMap;
        this.layoutAlgorithm = algorithm;
        this.drawingCanvas = new Pane();
        this.drawingCanvas.setPrefSize(VIRTUAL_CANVAS_SIZE, VIRTUAL_CANVAS_SIZE);
        this.drawingCanvas.setStyle("-fx-background-color: white;");

        var contentWrapper = new StackPane(drawingCanvas);
        contentWrapper.setAlignment(Pos.TOP_LEFT);
        this.scrollPaneContainer = new ScrollPane(contentWrapper);
        this.scrollPaneContainer.setPannable(false);
        this.scrollPaneContainer.setStyle("-fx-background-color:transparent; -fx-padding: 0; -fx-background: white;");

        this.setCenter(scrollPaneContainer);

        attachMouseGestureListeners();
        attachZoomListeners();
        attachKeyboardZoomListeners();

        Platform.runLater(this::renderTreeStructure);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rendering
    // ─────────────────────────────────────────────────────────────────────────

    private void renderTreeStructure() {
        drawingCanvas.setTranslateX(0);
        drawingCanvas.setTranslateY(0);
        drawingCanvas.setScaleX(1.0);
        drawingCanvas.setScaleY(1.0);

        drawingCanvas.getChildren().clear();
        openInfoPanels.clear();

        var rootNode = nodeMap.get(1);
        if (rootNode != null) {
            layoutAlgorithm.calculate(rootNode, VIRTUAL_CANVAS_SIZE / 2, VIRTUAL_CANVAS_SIZE / 2);
            drawCalculatedTree(rootNode);
        }

        scrollPaneContainer.setHvalue(0.5);
        scrollPaneContainer.setVvalue(0.5);
    }

    private void drawCalculatedTree(MaximumEdgeLengthNode node) {
        for (var child : node.getChildren()) {
            drawConnectionEdge(node.getGridX(), node.getGridY(), child.getGridX(), child.getGridY());
            drawCalculatedTree(child);
        }
        renderNodeVisuals(node, node.getGridX(), node.getGridY());
    }

    private void drawConnectionEdge(double x1, double y1, double x2, double y2) {
        var line = new Line(x1, y1, x2, y2);
        line.setStroke(Color.GRAY);
        line.setStrokeWidth(1);
        drawingCanvas.getChildren().addFirst(line);
    }

    private void renderNodeVisuals(MaximumEdgeLengthNode node, double x, double y) {
        var circle = new Circle(x, y, NODE_RADIUS, Color.AZURE);
        circle.setStroke(Color.DARKSLATEGRAY);
        circle.setStrokeWidth(1);
        circle.setCursor(Cursor.HAND);

        var text = new Label(String.valueOf(node.getIdentifier()));
        text.setStyle("-fx-font-weight: bold; -fx-font-size: 4px;");
        text.setAlignment(Pos.CENTER);
        text.setLayoutX(x - NODE_RADIUS);
        text.setLayoutY(y - NODE_RADIUS);
        text.setPrefSize(NODE_DIAMETER, NODE_DIAMETER);
        text.setMouseTransparent(true);

        // Only interaction here is inspecting the node — no modes to dispatch on.
        circle.setOnMouseClicked(e -> {
            showNodeInfoPanel(node, x, y);
            e.consume();
        });

        drawingCanvas.getChildren().addAll(circle, text);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Node info panel
    // ─────────────────────────────────────────────────────────────────────────

    private void showNodeInfoPanel(MaximumEdgeLengthNode node, double nodeX, double nodeY) {
        if (openInfoPanels.containsKey(node.getIdentifier())) return;

        var titleLabel = new Label("Node #" + node.getIdentifier());
        titleLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        var closeBtn = new javafx.scene.control.Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent; "
                        + "-fx-text-fill: #888; -fx-font-size: 9px; -fx-cursor: hand; "
                        + "-fx-padding: 0 2 0 2; -fx-min-width: 14; -fx-min-height: 14;");

        var header = new HBox(4, titleLabel, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 6, 0));
        header.setStyle("-fx-border-color: transparent transparent #ddd transparent; -fx-border-width: 1;");

        var grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(3);
        grid.setPadding(new Insets(6, 0, 0, 0));

        int row = 0;
        addInfoRow(grid, row++, "Name",
                (node.getName() == null || node.getName().isBlank()) ? "—" : node.getName());
        addInfoRow(grid, row++, "Parent",
                node.getParent() != null ? "#" + node.getParent().getIdentifier() : "none (root)");

        grid.add(makeSeparator(), 0, row++, 2, 1);

        addInfoRow(grid, row++, "Grid X", String.format("%.2f", node.getGridX()));
        addInfoRow(grid, row++, "Grid Y", String.format("%.2f", node.getGridY()));
        addInfoRow(grid, row++, "Depth", String.valueOf(node.getDepth()));

        grid.add(makeSeparator(), 0, row++, 2, 1);

        String childrenStr = node.getChildren().isEmpty()
                ? "none"
                : node.getChildren().stream()
                .map(c -> "#" + c.getIdentifier())
                .collect(Collectors.joining(", "));
        addInfoRow(grid, row, "Children (" + node.getChildren().size() + ")", childrenStr);

        var panel = new VBox(4, header, grid);
        panel.setStyle(
                "-fx-background-color: white; "
                        + "-fx-border-color: #bbb; "
                        + "-fx-border-width: 0.8; "
                        + "-fx-border-radius: 5; "
                        + "-fx-background-radius: 5; "
                        + "-fx-padding: 8;");
        panel.setPrefWidth(185);

        panel.setLayoutX(nodeX + NODE_RADIUS + 4);
        panel.setLayoutY(nodeY - NODE_RADIUS);

        panel.setOnMouseClicked(javafx.event.Event::consume);
        panel.setOnMousePressed(javafx.event.Event::consume);

        closeBtn.setOnAction(_ -> dismissInfoPanel(node.getIdentifier()));

        openInfoPanels.put(node.getIdentifier(), panel);
        drawingCanvas.getChildren().add(panel);
    }

    private void dismissInfoPanel(int nodeIdentifier) {
        var panel = openInfoPanels.remove(nodeIdentifier);
        if (panel != null) {
            drawingCanvas.getChildren().remove(panel);
        }
    }

    private void addInfoRow(GridPane grid, int row, String label, String value) {
        var lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #666; -fx-font-size: 9px;");

        var val = new Label(value);
        val.setStyle("-fx-font-size: 9px; -fx-font-family: monospace; -fx-text-fill: #1a1a1a;");
        val.setWrapText(true);
        val.setMaxWidth(105);

        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    private Separator makeSeparator() {
        var sep = new Separator();
        sep.setMaxWidth(Double.MAX_VALUE);
        GridPane.setColumnSpan(sep, 2);
        sep.setPadding(new Insets(2, 0, 2, 0));
        return sep;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pan / zoom (same behavior as FLCDTreeVisualizationView)
    // ─────────────────────────────────────────────────────────────────────────

    private void attachZoomListeners() {
        drawingCanvas.setOnScroll(e -> {
            double zoomFactor = (e.getDeltaY() > 0) ? (1 + ZOOM_INTENSITY) : (1 - ZOOM_INTENSITY);
            zoomBy(zoomFactor);
            e.consume();
        });
    }

    /// Multiplies the current scale by `zoomFactor`, clamped to
    /// `[MIN_SCALE, MAX_SCALE]`. Shared by scroll-wheel zoom and the
    /// Ctrl+=/Ctrl+- keyboard shortcuts so both paths behave identically.
    private void zoomBy(double zoomFactor) {
        double newScale = drawingCanvas.getScaleX() * zoomFactor;
        if (newScale >= MIN_SCALE && newScale <= MAX_SCALE) {
            drawingCanvas.setScaleX(newScale);
            drawingCanvas.setScaleY(newScale);
        }
    }

    /// Keyboard-driven zoom for anyone who'd rather not rely on a mouse
    /// wheel/trackpad: Ctrl+= (or Ctrl+Plus) zooms in, Ctrl+- (or
    /// Ctrl+Minus) zooms out. Attached at the Scene level (once the view
    /// is actually part of a Scene) rather than on drawingCanvas directly,
    /// since key events need a focus owner and this view isn't guaranteed
    /// to hold focus itself.
    private void attachKeyboardZoomListeners() {
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyboardZoom);
            }
        });
    }

    private void handleKeyboardZoom(KeyEvent e) {
        if (!e.isControlDown()) return;

        var code = e.getCode();
        if (code == KeyCode.EQUALS || code == KeyCode.ADD || code == KeyCode.PLUS) {
            zoomBy(1 + ZOOM_INTENSITY);
            e.consume();
        } else if (code == KeyCode.MINUS || code == KeyCode.SUBTRACT) {
            zoomBy(1 - ZOOM_INTENSITY);
            e.consume();
        }
    }

    private void attachMouseGestureListeners() {
        drawingCanvas.setOnMousePressed(e -> {
            mouseDragAnchorX = e.getSceneX();
            mouseDragAnchorY = e.getSceneY();
        });
        drawingCanvas.setOnMouseDragged(e -> {
            drawingCanvas.setTranslateX(drawingCanvas.getTranslateX() + (e.getSceneX() - mouseDragAnchorX));
            drawingCanvas.setTranslateY(drawingCanvas.getTranslateY() + (e.getSceneY() - mouseDragAnchorY));
            mouseDragAnchorX = e.getSceneX();
            mouseDragAnchorY = e.getSceneY();
        });
    }
}
