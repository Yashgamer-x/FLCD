package com.yashgamerx.flcd.tmel.view;

import com.yashgamerx.flcd.tmel.algorithm.TopMaximumEdgeLengthPlanarAlgorithm;
import com.yashgamerx.flcd.tmel.model.TMELNode;
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

import static com.yashgamerx.flcd.tmel.model.TMELNode.NODE_DIAMETER;

/// Runs the exact same `TMELNode` / `TopMaximumEdgeLengthPlanarAlgorithm` pipeline as
/// [com.yashgamerx.flcd.flcd.view.FLCDTreeVisualizationView], but deliberately strips out the Rootify
/// and Readjust toolbar actions and their node-click dispatch — this
/// variant of the algorithm family never allows a node to be rootified
/// or readjusted. Node clicks always just open the info panel.
///
/// Kept as a parallel class (rather than a flag on `FLCDTreeVisualizationView`)
/// so the "no rootify/readjust" behavior is structurally guaranteed instead
/// of being one more conditional to keep in sync.
@Log
public class TMELTreeVisualizationView extends BorderPane {

    /// Matches the diameter of 5.0 established in the preCompute phase.
    private static final double NODE_RADIUS = TMELNode.NODE_RADIUS;
    private static final double VIRTUAL_CANVAS_SIZE = 8000.0;
    private static final double ZOOM_INTENSITY = 0.1;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;
    private final Pane drawingCanvas;
    private final ScrollPane scrollPaneContainer;
    private final Map<Integer, TMELNode> nodeMap;
    private final TopMaximumEdgeLengthPlanarAlgorithm layoutAlgorithm;
    /// Tracks all currently visible node info panels, keyed by node identifier,
    /// so any number of them can stay open at once. A panel is only ever
    /// removed when its own ✕ button is clicked, or when the tree is redrawn.
    private final Map<Integer, VBox> openInfoPanels = new HashMap<>();
    /// Circle references keyed by node identifier, so edge-length selection
    /// can highlight/reset a node's circle without re-scanning the canvas.
    private final Map<Integer, Circle> nodeCircles = new HashMap<>();
    /// Nodes picked so far for the "Calculate Edge Length" click-to-select flow.
    /// Holds 0, 1 (waiting on second pick), or transiently 2 nodes before the
    /// result is shown and the selection is reset.
    private final List<TMELNode> edgeLengthSelection = new ArrayList<>();
    private boolean edgeLengthSelectionMode = false;
    private Button btnCalculateEdgeLength;
    private Label edgeLengthHintLabel;
    private double mouseDragAnchorX;
    private double mouseDragAnchorY;

    public TMELTreeVisualizationView(final Map<Integer, TMELNode> nodeMap, final TopMaximumEdgeLengthPlanarAlgorithm algorithm) {
        this.nodeMap = nodeMap;
        this.layoutAlgorithm = algorithm;
        this.drawingCanvas = new Pane();
        this.drawingCanvas.setPrefSize(VIRTUAL_CANVAS_SIZE, VIRTUAL_CANVAS_SIZE);
        this.drawingCanvas.setStyle("-fx-background-color: white;");

        var contentWrapper = new StackPane(drawingCanvas);
        contentWrapper.setAlignment(Pos.TOP_LEFT);
        this.scrollPaneContainer = new ScrollPane(contentWrapper);

        initializeComponentLayout();
        attachMouseGestureListeners();
        attachZoomListeners();
        attachKeyboardZoomListeners();

        Platform.runLater(this::handlePanelAction);
    }

    private void initializeComponentLayout() {
        var actionToolbar = createActionToolbar();
        scrollPaneContainer.setPannable(false);
        scrollPaneContainer.setStyle("-fx-background-color:transparent; -fx-padding: 0; -fx-background: white;");
        this.setTop(actionToolbar);
        this.setCenter(scrollPaneContainer);
    }

    private void renderNodeVisuals(TMELNode node, double x, double y) {
        var circle = new Circle(x, y, NODE_RADIUS, Color.AZURE);
        circle.setStroke(Color.DARKSLATEGRAY);
        circle.setStrokeWidth(1);
        circle.setCursor(Cursor.HAND);

        var text = new Label(String.valueOf(node.getIdentifier()));
        text.setStyle("-fx-font-weight: bold; -fx-font-size: 4px;");

        // Set the anchor point of the text right in its center
        text.setAlignment(Pos.CENTER);

        // Position the center anchor exactly at the circle's (x, y)
        text.setLayoutX(x - NODE_RADIUS);
        text.setLayoutY(y - NODE_RADIUS);
        text.setPrefSize(NODE_DIAMETER, NODE_DIAMETER);
        text.setMouseTransparent(true);

        // Normal clicks open the info panel; while edge-length selection mode
        // is active, clicks instead pick nodes for the edge-length measurement.
        circle.setOnMouseClicked(e -> {
            if (edgeLengthSelectionMode) {
                handleNodeSelectedForEdgeLength(node, circle);
            } else {
                showNodeInfoPanel(node, x, y);
            }
            e.consume();
        });

        nodeCircles.put(node.getIdentifier(), circle);
        drawingCanvas.getChildren().addAll(circle, text);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rendering
    // ─────────────────────────────────────────────────────────────────────────

    /// Executes the rendering pass after the algorithm has computed the grid
    private void renderTreeStructure() {
        drawingCanvas.getChildren().clear();
        openInfoPanels.clear(); // Panel references are cleared with the canvas
        nodeCircles.clear();
        resetEdgeLengthSelection();
        var rootNode = nodeMap.get(1);
        if (rootNode != null) {
            layoutAlgorithm.calculate(rootNode, VIRTUAL_CANVAS_SIZE / 2, VIRTUAL_CANVAS_SIZE / 2);
            drawCalculatedTree(rootNode);
        }
    }

    private void drawCalculatedTree(TMELNode node) {
        // Render edges first so they sit visually behind the circles
        for (var child : node.getChildren()) {
            drawConnectionEdge(node.getGridX(), node.getGridY(), child.getGridX(), child.getGridY());
            drawCalculatedTree(child);
        }

        // Render node visuals on top
        renderNodeVisuals(node, node.getGridX(), node.getGridY());
    }

    private void drawConnectionEdge(double x1, double y1, double x2, double y2) {
        var line = new Line(x1, y1, x2, y2);
        line.setStroke(Color.GRAY);
        line.setStrokeWidth(1);

        // Adds edges to back of layout stack
        drawingCanvas.getChildren().addFirst(line);
    }

    private void handlePanelAction() {
        drawingCanvas.setTranslateX(0);
        drawingCanvas.setTranslateY(0);
        drawingCanvas.setScaleX(1.0);
        drawingCanvas.setScaleY(1.0);
        renderTreeStructure();

        scrollPaneContainer.setHvalue(0.5);
        scrollPaneContainer.setVvalue(0.5);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Toolbar — deliberately has no Rootify/Readjust toggles or hint label.
    // ─────────────────────────────────────────────────────────────────────────

    private HBox createActionToolbar() {
        var btnAdd = new Button("Add Node");

        var btnCalculateArea = new Button("Calculate Area");
        btnCalculateArea.setOnAction(_ -> handleCalculateArea());

        btnCalculateEdgeLength = new Button("Calculate Edge Length");
        btnCalculateEdgeLength.setOnAction(_ -> handleCalculateEdgeLength());

        edgeLengthHintLabel = new Label();
        edgeLengthHintLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 10px; -fx-font-style: italic;");

        var toolbar = new HBox(15, btnAdd, btnCalculateArea, btnCalculateEdgeLength, edgeLengthHintLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-padding: 10; -fx-background-color: #f4f4f4; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");
        return toolbar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Node info panel
    // ─────────────────────────────────────────────────────────────────────────

    /// Builds and positions a floating info panel on the canvas anchored to the
    /// clicked node. Any previously shown panel is removed first.
    private void showNodeInfoPanel(TMELNode node, double nodeX, double nodeY) {
        // If this node's panel is already open, leave it as-is instead of
        // duplicating it — panels only close via their own ✕ button.
        if (openInfoPanels.containsKey(node.getIdentifier())) return;

        // ── Header row (title + close button) ────────────────────────────────
        var titleLabel = new Label("Node #" + node.getIdentifier());
        titleLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        var closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-border-color: transparent; "
                        + "-fx-text-fill: #888; -fx-font-size: 9px; -fx-cursor: hand; "
                        + "-fx-padding: 0 2 0 2; -fx-min-width: 14; -fx-min-height: 14;");

        var header = new HBox(4, titleLabel, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 6, 0));
        header.setStyle("-fx-border-color: transparent transparent #ddd transparent; -fx-border-width: 1;");

        // ── Data grid ────────────────────────────────────────────────────────
        var grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(3);
        grid.setPadding(new Insets(6, 0, 0, 0));

        int row = 0;

        // Identity
        addInfoRow(grid, row++, "Name",
                (node.getName() == null || node.getName().isBlank()) ? "—" : node.getName());
        addInfoRow(grid, row++, "Parent",
                node.getParent() != null ? "#" + node.getParent().getIdentifier() : "none (root)");

        grid.add(makeSeparator(), 0, row++, 2, 1);

        // Screen coordinates
        addInfoRow(grid, row++, "Grid X", String.format("%.2f", node.getGridX()));
        addInfoRow(grid, row++, "Grid Y", String.format("%.2f", node.getGridY()));

        grid.add(makeSeparator(), 0, row++, 2, 1);

        // Subtree dimensions
        addInfoRow(grid, row++, "Subtree W", String.format("%.2f", node.getSubtreeWidth()));
        addInfoRow(grid, row++, "Subtree H", String.format("%.2f", node.getSubtreeHeight()));

        grid.add(makeSeparator(), 0, row++, 2, 1);

        // Algorithmic state
        addInfoRow(grid, row++, "Offset", String.format("%.4f", node.getNodeOffset()));
        addInfoRow(grid, row++, "Local angle", String.format("%.4f rad", node.getLocalRadianAngle()));
        addInfoRow(grid, row++, "Global angle", String.format("%.4f rad", node.getGlobalRadianAngle()));

        grid.add(makeSeparator(), 0, row++, 2, 1);

        // Children
        String childrenStr = node.getChildren().isEmpty()
                ? "none"
                : node.getChildren().stream()
                .map(c -> "#" + c.getIdentifier())
                .collect(Collectors.joining(", "));
        addInfoRow(grid, row++, "Children (" + node.getChildren().size() + ")", childrenStr);

        grid.add(makeSeparator(), 0, row++, 2, 1);

        // Structural state
        addInfoRow(grid, row++, "Role", node.getRole() != null ? node.getRole().toString() : "none");
        addInfoRow(grid, row++, "Side", node.getSide().toString());

        grid.add(makeSeparator(), 0, row++, 2, 1);

        addInfoRow(grid, row++, "Depth", String.valueOf(node.getDepth()));

        // ── Assemble ─────────────────────────────────────────────────────────
        var panel = new VBox(4, header, grid);
        panel.setStyle(
                "-fx-background-color: white; "
                        + "-fx-border-color: #bbb; "
                        + "-fx-border-width: 0.8; "
                        + "-fx-border-radius: 5; "
                        + "-fx-background-radius: 5; "
                        + "-fx-padding: 8;");
        panel.setPrefWidth(185);

        // Anchor the panel just to the right of the node circle
        panel.setLayoutX(nodeX + NODE_RADIUS + 4);
        panel.setLayoutY(nodeY - NODE_RADIUS);

        // Clicks inside the panel must not bubble up to the canvas drag handler
        panel.setOnMouseClicked(javafx.event.Event::consume);
        panel.setOnMousePressed(javafx.event.Event::consume);

        closeBtn.setOnAction(_ -> dismissInfoPanel(node.getIdentifier()));

        openInfoPanels.put(node.getIdentifier(), panel);
        drawingCanvas.getChildren().add(panel);
    }

    /// Removes a single node's info panel from the canvas, identified by node id.
    /// This is the only path that closes a panel — it's wired to that panel's ✕ button.
    private void dismissInfoPanel(int nodeIdentifier) {
        var panel = openInfoPanels.remove(nodeIdentifier);
        if (panel != null) {
            drawingCanvas.getChildren().remove(panel);
        }
    }

    /// Adds a label/value pair to the info grid at the given row index.
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

    /// Creates a full-width separator for the info grid.
    private Separator makeSeparator() {
        var sep = new Separator();
        sep.setMaxWidth(Double.MAX_VALUE);
        GridPane.setColumnSpan(sep, 2);
        sep.setPadding(new Insets(2, 0, 2, 0));
        return sep;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Interaction listeners
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

        // Clicking on blank canvas space no longer dismisses info panels —
        // panels stay open until their own ✕ button is clicked.
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Calculate Area action
    // ─────────────────────────────────────────────────────────────────────────

    /// Scans all currently rendered nodes to find the bounding box of the tree,
    /// accounting for NODE_RADIUS so the box encloses the full node circles
    /// (not just their center points), then displays the resulting area.
    private void handleCalculateArea() {
        if (nodeMap.isEmpty()) {
            showErrorAlert("Calculate Area Error", "There are no nodes to measure.");
            return;
        }

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        TMELNode leftMost = null, rightMost = null, topMost = null, bottomMost = null;

        for (var node : nodeMap.values()) {
            double x = node.getGridX();
            double y = node.getGridY();

            if (x - NODE_RADIUS < minX) {
                minX = x - NODE_RADIUS;
                leftMost = node;
            }
            if (x + NODE_RADIUS > maxX) {
                maxX = x + NODE_RADIUS;
                rightMost = node;
            }
            // In screen/scene coordinates, smaller Y is "up" (top), larger Y is "down" (bottom).
            if (y - NODE_RADIUS < minY) {
                minY = y - NODE_RADIUS;
                topMost = node;
            }
            if (y + NODE_RADIUS > maxY) {
                maxY = y + NODE_RADIUS;
                bottomMost = node;
            }
        }

        double width = maxX - minX;
        double height = maxY - minY;
        double area = width * height;

        showAreaResultAlert(width, height, area, leftMost, rightMost, topMost, bottomMost);
    }

    /// Displays the calculated bounding-box area in a popup dialog, including
    /// which nodes defined each extreme edge.
    private void showAreaResultAlert(double width, double height, double area,
                                     TMELNode leftMost, TMELNode rightMost,
                                     TMELNode topMost, TMELNode bottomMost) {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Bounding Area");
        alert.setHeaderText("Tree Bounding Box Area");

        String content = String.format(
                "Width:  %.2f%n" +
                        "Height: %.2f%n" +
                        "Area:   %.2f%n%n" +
                        "Leftmost node:   #%d%n" +
                        "Rightmost node:  #%d%n" +
                        "Topmost node:    #%d%n" +
                        "Bottommost node: #%d%n%n" +
                        "(NODE_RADIUS of %.1f included so the box encloses full node circles.)",
                width, height, area,
                leftMost.getIdentifier(), rightMost.getIdentifier(),
                topMost.getIdentifier(), bottomMost.getIdentifier(),
                NODE_RADIUS
        );

        // Use a read-only, selectable TextArea instead of setContentText so the
        // user can click-drag/select and copy (Ctrl+C) the results — Alert's
        // plain contentText label does not support text selection.
        var textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(360);
        textArea.setPrefHeight(220);
        textArea.setStyle("-fx-font-family: monospace;");

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Calculate Edge Length action
    // ─────────────────────────────────────────────────────────────────────────

    /// Arms/disarms click-to-select mode for measuring the distance between
    /// two nodes. While armed, node clicks are intercepted by
    /// [#handleNodeSelectedForEdgeLength] instead of opening the info panel
    /// (see [#renderNodeVisuals]). Clicking the toolbar button again while
    /// already armed cancels the pending selection.
    private void handleCalculateEdgeLength() {
        if (edgeLengthSelectionMode) {
            resetEdgeLengthSelection();
            return;
        }

        if (nodeMap.size() < 2) {
            showErrorAlert("Calculate Edge Length Error", "Need at least two nodes to measure an edge.");
            return;
        }

        edgeLengthSelectionMode = true;
        btnCalculateEdgeLength.setText("Cancel Selection");
        edgeLengthHintLabel.setText("Click a node on the canvas, then click a second node.");
    }

    /// Records a node click made while edge-length selection mode is armed.
    /// The first click highlights that node and waits for a second; the
    /// second click completes the pair, shows the result, and resets state.
    private void handleNodeSelectedForEdgeLength(TMELNode node, Circle circle) {
        if (edgeLengthSelection.contains(node)) return; // ignore re-clicking the same node

        circle.setStroke(Color.ORANGERED);
        circle.setStrokeWidth(2);
        edgeLengthSelection.add(node);

        if (edgeLengthSelection.size() == 1) {
            edgeLengthHintLabel.setText("Node #" + node.getIdentifier() + " selected — click a second node.");
            return;
        }

        var nodeA = edgeLengthSelection.get(0);
        var nodeB = edgeLengthSelection.get(1);
        resetEdgeLengthSelection();
        showEdgeLengthResultAlert(nodeA, nodeB);
    }

    /// Clears any in-progress edge-length selection: un-highlights picked
    /// circles, drops the pending nodes, and restores the toolbar button/hint
    /// to their idle state. Safe to call whether or not a selection is active.
    private void resetEdgeLengthSelection() {
        for (var node : edgeLengthSelection) {
            var circle = nodeCircles.get(node.getIdentifier());
            if (circle != null) {
                circle.setStroke(Color.DARKSLATEGRAY);
                circle.setStrokeWidth(1);
            }
        }
        edgeLengthSelection.clear();
        edgeLengthSelectionMode = false;
        if (btnCalculateEdgeLength != null) {
            btnCalculateEdgeLength.setText("Calculate Edge Length");
        }
        if (edgeLengthHintLabel != null) {
            edgeLengthHintLabel.setText("");
        }
    }

    /// Displays the straight-line (Euclidean) distance between the two
    /// selected nodes' grid centers — i.e. the length of the line
    /// [#drawConnectionEdge] would draw between them, regardless of whether
    /// the two nodes are actually parent/child.
    private void showEdgeLengthResultAlert(TMELNode nodeA, TMELNode nodeB) {
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
}