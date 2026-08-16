package com.yashgamerx.flcd.cmel.view;

import com.yashgamerx.flcd.cmel.algorithm.CircleMaximumEdgeLengthAlgorithm;
import com.yashgamerx.flcd.cmel.model.CircleMaximumEdgeLengthNode;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.yashgamerx.flcd.cmel.model.CircleMaximumEdgeLengthNode.NODE_DIAMETER;
import static com.yashgamerx.flcd.cmel.model.CircleMaximumEdgeLengthNode.NODE_RADIUS;

/// Visualization for [CircleMaximumEdgeLengthNode] trees.
///
/// Unlike [com.yashgamerx.flcd.flcd.view.FLCDTreeVisualizationView], there is no Rootify, Readjust, or
/// Calculate Area toolbar action — the only interaction mode is
/// Calculate Edge Length (click two nodes to measure the distance between
/// them). This view only draws the computed layout and otherwise lets the
/// user click a node to see its info panel. It is intentionally its own
/// class rather than a stripped-down mode of the FLCD view, since it
/// renders a different node type produced by a different algorithm.
@Log
public class CircleMaximumEdgeLengthVisualizationView extends BorderPane {

    private static final double VIRTUAL_CANVAS_SIZE = 8000.0;
    private static final double ZOOM_INTENSITY = 0.1;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;
    private final Map<Integer, CircleMaximumEdgeLengthNode> nodeMap;
    private final CircleMaximumEdgeLengthAlgorithm layoutAlgorithm;
    private final Pane drawingCanvas;
    private final ScrollPane scrollPaneContainer;
    private final Map<Integer, VBox> openInfoPanels = new HashMap<>();
    /// Circle references keyed by node identifier, so edge-length selection
    /// can highlight/reset a node's circle without re-scanning the canvas.
    private final Map<Integer, Circle> nodeCircles = new HashMap<>();
    /// Nodes picked so far for the EDGE_LENGTH click-to-select flow. Holds
    /// 0 or 1 nodes between clicks; a second click completes the pair,
    /// shows the result, and the list is cleared for the next pair.
    private final List<CircleMaximumEdgeLengthNode> edgeLengthSelection = new ArrayList<>();
    /// Current interaction mode — mirrors FLCDTreeVisualizationView's pattern,
    /// even though EDGE_LENGTH is the only mode here (no Rootify/Readjust/
    /// Calculate Area in this view — see class javadoc).
    private ActiveMode activeMode = ActiveMode.NONE;
    /// Reference to the active toggle button so it can be deselected after an action.
    private ToggleButton activeModeButton = null;
    private Label hintLabel;
    private double mouseDragAnchorX;
    private double mouseDragAnchorY;

    public CircleMaximumEdgeLengthVisualizationView(final Map<Integer, CircleMaximumEdgeLengthNode> nodeMap,
                                                    final CircleMaximumEdgeLengthAlgorithm algorithm) {
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

        this.setTop(createActionToolbar());
        this.setCenter(scrollPaneContainer);

        attachMouseGestureListeners();
        attachZoomListeners();
        attachKeyboardZoomListeners();

        Platform.runLater(this::renderTreeStructure);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mode management
    // ─────────────────────────────────────────────────────────────────────────

    /// Sets the active mode when a toolbar toggle button is pressed.
    /// Pressing the same button again returns to NONE. Clears any
    /// in-progress edge-length selection on every mode transition.
    private void setMode(ActiveMode mode, ToggleButton source) {
        clearEdgeLengthSelectionHighlights();
        if (activeMode == mode) {
            // Same button toggled off — return to idle
            activeMode = ActiveMode.NONE;
            activeModeButton = null;
        } else {
            activeMode = mode;
            activeModeButton = source;
        }
    }

    /// Resets mode to NONE and deselects the toolbar toggle button.
    private void clearMode() {
        clearEdgeLengthSelectionHighlights();
        activeMode = ActiveMode.NONE;
        if (activeModeButton != null) {
            activeModeButton.setSelected(false);
            activeModeButton = null;
        }
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
        nodeCircles.clear();
        edgeLengthSelection.clear(); // Underlying circles are gone; no need to un-highlight them

        var rootNode = nodeMap.get(1);
        if (rootNode != null) {
            layoutAlgorithm.calculate(rootNode, VIRTUAL_CANVAS_SIZE / 2, VIRTUAL_CANVAS_SIZE / 2);
            drawCalculatedTree(rootNode);
        }

        scrollPaneContainer.setHvalue(0.5);
        scrollPaneContainer.setVvalue(0.5);
    }

    private void drawCalculatedTree(CircleMaximumEdgeLengthNode node) {
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

    private void renderNodeVisuals(CircleMaximumEdgeLengthNode node, double x, double y) {
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

        // Normal clicks open the info panel; in EDGE_LENGTH mode, clicks
        // instead pick nodes for the edge-length measurement.
        circle.setOnMouseClicked(e -> {
            switch (activeMode) {
                case EDGE_LENGTH -> handleNodeSelectedForEdgeLength(node, circle);
                default -> showNodeInfoPanel(node, x, y);
            }
            e.consume();
        });

        nodeCircles.put(node.getIdentifier(), circle);
        drawingCanvas.getChildren().addAll(circle, text);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Node info panel
    // ─────────────────────────────────────────────────────────────────────────

    private void showNodeInfoPanel(CircleMaximumEdgeLengthNode node, double nodeX, double nodeY) {
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
    // Toolbar — EDGE_LENGTH is the only toggle mode in this view (see class javadoc).
    // ─────────────────────────────────────────────────────────────────────────

    private HBox createActionToolbar() {
        var modeGroup = new ToggleGroup();

        var btnEdgeLength = new ToggleButton("Calculate Edge Length");
        btnEdgeLength.setToggleGroup(modeGroup);
        btnEdgeLength.setOnAction(_ -> {
            if (activeMode != ActiveMode.EDGE_LENGTH && nodeMap.size() < 2) {
                btnEdgeLength.setSelected(false);
                showErrorAlert("Calculate Edge Length Error", "Need at least two nodes to measure an edge.");
                return;
            }
            setMode(ActiveMode.EDGE_LENGTH, btnEdgeLength);
        });

        hintLabel = new Label();
        hintLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 10px; -fx-font-style: italic;");

        modeGroup.selectedToggleProperty().addListener((_, _, newToggle) -> {
            if (newToggle == btnEdgeLength)
                hintLabel.setText("Click a node, then click a second node to measure the edge between them");
            else hintLabel.setText("");
        });

        var toolbar = new HBox(15, btnEdgeLength, hintLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-padding: 10; -fx-background-color: #f4f4f4; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");
        return toolbar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge Length action — triggered by clicking two nodes in EDGE_LENGTH mode
    // ─────────────────────────────────────────────────────────────────────────

    /// Records a node click made while EDGE_LENGTH mode is active. The first
    /// click highlights that node and waits for a second; the second click
    /// completes the pair, shows the result, and clears the pair so the mode
    /// stays armed for measuring another pair.
    private void handleNodeSelectedForEdgeLength(CircleMaximumEdgeLengthNode node, Circle circle) {
        if (edgeLengthSelection.contains(node)) return; // ignore re-clicking the same node

        circle.setStroke(Color.ORANGERED);
        circle.setStrokeWidth(2);
        edgeLengthSelection.add(node);

        if (edgeLengthSelection.size() == 1) {
            hintLabel.setText("Node #" + node.getIdentifier() + " selected — click a second node.");
            return;
        }

        var nodeA = edgeLengthSelection.get(0);
        var nodeB = edgeLengthSelection.get(1);
        clearEdgeLengthSelectionHighlights();
        hintLabel.setText("Click a node, then click a second node to measure the edge between them");
        showEdgeLengthResultAlert(nodeA, nodeB);
    }

    /// Un-highlights any circles picked so far for the edge-length pair and
    /// clears the pending selection. Safe to call whether or not a selection
    /// is in progress — used both mid-flow (after a completed pair) and
    /// whenever the active mode changes away from EDGE_LENGTH.
    private void clearEdgeLengthSelectionHighlights() {
        for (var node : edgeLengthSelection) {
            var circle = nodeCircles.get(node.getIdentifier());
            if (circle != null) {
                circle.setStroke(Color.DARKSLATEGRAY);
                circle.setStrokeWidth(1);
            }
        }
        edgeLengthSelection.clear();
    }

    /// Displays the straight-line (Euclidean) distance between the two
    /// selected nodes' grid centers — i.e. the length of the line
    /// [#drawConnectionEdge] would draw between them, regardless of whether
    /// the two nodes are actually parent/child.
    private void showEdgeLengthResultAlert(CircleMaximumEdgeLengthNode nodeA, CircleMaximumEdgeLengthNode nodeB) {
        double dx = nodeB.getGridX() - nodeA.getGridX();
        double dy = nodeB.getGridY() - nodeA.getGridY();
        double centerDistance = Math.sqrt((dx * dx) + (dy * dy));
        double surfaceDistance = Math.max(0.0, centerDistance - (2 * NODE_RADIUS));
        boolean directlyConnected = nodeA.getParent() == nodeB || nodeB.getParent() == nodeA;

        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Edge Length");
        alert.setHeaderText("Distance Between Node #" + nodeA.getIdentifier() + " and Node #" + nodeB.getIdentifier());

        String content = String.format(
                "Center-to-center distance: %.4f%n" +
                        "Surface-to-surface gap:    %.4f%n" +
                        "\u0394X: %.4f%n" +
                        "\u0394Y: %.4f%n%n" +
                        "Node #%d  (x=%.2f, y=%.2f)%n" +
                        "Node #%d  (x=%.2f, y=%.2f)%n%n" +
                        "Directly connected in tree: %s",
                centerDistance, surfaceDistance, dx, dy,
                nodeA.getIdentifier(), nodeA.getGridX(), nodeA.getGridY(),
                nodeB.getIdentifier(), nodeB.getGridX(), nodeB.getGridY(),
                directlyConnected ? "yes" : "no"
        );

        var textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(360);
        textArea.setPrefHeight(200);
        textArea.setStyle("-fx-font-family: monospace;");

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private void showErrorAlert(String header, String content) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private enum ActiveMode {NONE, EDGE_LENGTH}

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