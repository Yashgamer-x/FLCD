package com.yashgamerx.flcd.rt.view;

import com.yashgamerx.flcd.rt.algorithm.ReingoldTilfordAlgorithm;
import com.yashgamerx.flcd.rt.model.RTNode;
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

import java.util.HashMap;
import java.util.Map;

import static com.yashgamerx.flcd.rt.model.RTNode.NODE_DIAMETER;
import static com.yashgamerx.flcd.rt.model.RTNode.NODE_RADIUS;

/// Visualization canvas for the classic Reingold–Tilford comparison
/// baseline. Deliberately has no Rootify/Readjust/Edge-Length toolbar —
/// those are FLCD-specific interactive operations with no equivalent in
/// the classic algorithm — but keeps the same pan/zoom/inspect/area
/// affordances as [com.yashgamerx.flcd.flcd.view.FLCDTreeVisualizationView]
/// so the two are visually and interactionally comparable side by side.
@Log
public class RTTreeVisualizationView extends BorderPane {

    private static final double VIRTUAL_CANVAS_SIZE = 8000.0;
    private static final double ZOOM_INTENSITY = 0.1;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;

    private final Pane drawingCanvas;
    private final ScrollPane scrollPaneContainer;
    private final Map<Integer, RTNode> nodeMap;
    private final ReingoldTilfordAlgorithm layoutAlgorithm;

    /// Tracks all currently visible node info panels, keyed by node
    /// identifier, so any number of them can stay open at once. A panel is
    /// only ever removed when its own ✕ button is clicked, or the tree is
    /// redrawn.
    private final Map<Integer, VBox> openInfoPanels = new HashMap<>();

    private double mouseDragAnchorX;
    private double mouseDragAnchorY;

    public RTTreeVisualizationView(final Map<Integer, RTNode> nodeMap, final ReingoldTilfordAlgorithm algorithm) {
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

        Platform.runLater(this::handleInitialRender);
    }

    private void initializeComponentLayout() {
        var actionToolbar = createActionToolbar();
        scrollPaneContainer.setPannable(false);
        scrollPaneContainer.setStyle("-fx-background-color:transparent; -fx-padding: 0; -fx-background: white;");
        this.setTop(actionToolbar);
        this.setCenter(scrollPaneContainer);
    }

    private void handleInitialRender() {
        drawingCanvas.setTranslateX(0);
        drawingCanvas.setTranslateY(0);
        drawingCanvas.setScaleX(1.0);
        drawingCanvas.setScaleY(1.0);
        renderTreeStructure();

        scrollPaneContainer.setHvalue(0.5);
        scrollPaneContainer.setVvalue(0);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Toolbar
    // ─────────────────────────────────────────────────────────────────────

    private HBox createActionToolbar() {
        var titleLabel = new Label("Reingold–Tilford (Classic)");
        titleLabel.setStyle("-fx-font-weight: bold;");

        var btnCalculateArea = new Button("Calculate Area");
        btnCalculateArea.setOnAction(_ -> handleCalculateArea());

        var hintLabel = new Label("Click a node to inspect it · scroll or Ctrl +/- to zoom · drag to pan");
        hintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #777; -fx-font-style: italic;");

        var toolbar = new HBox(15, titleLabel, btnCalculateArea, hintLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-padding: 10; -fx-background-color: #f4f4f4; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");
        return toolbar;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Rendering
    // ─────────────────────────────────────────────────────────────────────

    private void renderTreeStructure() {
        drawingCanvas.getChildren().clear();
        openInfoPanels.clear();
        var rootNode = nodeMap.get(1);
        if (rootNode != null) {
            layoutAlgorithm.calculate(rootNode, VIRTUAL_CANVAS_SIZE / 2, 60.0);
            drawCalculatedTree(rootNode);
        }
    }

    private void drawCalculatedTree(RTNode node) {
        // Render edges first so they sit visually behind the circles
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

    private void renderNodeVisuals(RTNode node, double x, double y) {
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

        circle.setOnMouseClicked(e -> {
            showNodeInfoPanel(node, x, y);
            e.consume();
        });

        drawingCanvas.getChildren().addAll(circle, text);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Node info panel
    // ─────────────────────────────────────────────────────────────────────

    private void showNodeInfoPanel(RTNode node, double nodeX, double nodeY) {
        if (openInfoPanels.containsKey(node.getIdentifier())) return;

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

        addInfoRow(grid, row++, "Prelim X", String.format("%.4f", node.getPrelimX()));
        addInfoRow(grid, row++, "Mod", String.format("%.4f", node.getMod()));
        addInfoRow(grid, row++, "Sibling #", String.valueOf(node.getNumber()));

        grid.add(makeSeparator(), 0, row++, 2, 1);

        String childrenStr = node.getChildren().isEmpty()
                ? "none"
                : node.getChildren().stream()
                .map(c -> "#" + c.getIdentifier())
                .reduce((a, b) -> a + ", " + b)
                .orElse("none");
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

    // ─────────────────────────────────────────────────────────────────────
    // Calculate Area
    // ─────────────────────────────────────────────────────────────────────

    /// Scans all currently rendered nodes to find the bounding box of the
    /// tree, accounting for NODE_RADIUS so the box encloses the full node
    /// circles (not just their center points), then displays the resulting
    /// area — same convention as FLCDTreeVisualizationView#handleCalculateArea
    /// so RT's numbers are directly comparable to the other families'.
    private void handleCalculateArea() {
        if (nodeMap.isEmpty()) {
            showErrorAlert("Calculate Area Error", "There are no nodes to measure.");
            return;
        }

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        RTNode leftMost = null, rightMost = null, topMost = null, bottomMost = null;

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

    private void showAreaResultAlert(double width, double height, double area,
                                     RTNode leftMost, RTNode rightMost,
                                     RTNode topMost, RTNode bottomMost) {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Bounding Area");
        alert.setHeaderText("Tree Bounding Box Area (Reingold–Tilford)");

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

        var textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(360);
        textArea.setPrefHeight(220);
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

    // ─────────────────────────────────────────────────────────────────────
    // Pan / zoom interaction listeners
    // ─────────────────────────────────────────────────────────────────────

    private void attachZoomListeners() {
        drawingCanvas.setOnScroll(e -> {
            double zoomFactor = (e.getDeltaY() > 0) ? (1 + ZOOM_INTENSITY) : (1 - ZOOM_INTENSITY);
            zoomBy(zoomFactor);
            e.consume();
        });
    }

    private void zoomBy(double zoomFactor) {
        double newScale = drawingCanvas.getScaleX() * zoomFactor;
        if (newScale >= MIN_SCALE && newScale <= MAX_SCALE) {
            drawingCanvas.setScaleX(newScale);
            drawingCanvas.setScaleY(newScale);
        }
    }

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
