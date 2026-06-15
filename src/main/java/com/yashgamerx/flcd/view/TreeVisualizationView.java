package com.yashgamerx.flcd.view;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.model.NodeStatus;
import com.yashgamerx.flcd.service.algorithm.TreeLayoutAlgorithm;
import com.yashgamerx.flcd.service.compute.RootifiedCompute;
import com.yashgamerx.flcd.service.precompute.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import lombok.extern.java.Log;

import java.util.Map;
import java.util.stream.Collectors;

import static com.yashgamerx.flcd.model.AbstractNode.NODE_DIAMETER;

@Log
public class TreeVisualizationView extends BorderPane {

    private final Map<Integer, AbstractNode> abstractNodeMap;
    private final Pane drawingCanvas;
    private final ScrollPane scrollPaneContainer;

    /// Matches the diameter of 5.0 established in the preCompute phase.
    private static final double NODE_RADIUS = 5;

    private double mouseDragAnchorX;
    private double mouseDragAnchorY;
    private static final double VIRTUAL_CANVAS_SIZE = 8000.0;
    private static final double ZOOM_INTENSITY = 0.1;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;

    private final TreeLayoutAlgorithm layoutAlgorithm;

    /// Tracks the currently visible node info panel so it can be removed when needed.
    private VBox currentInfoPanel = null;

    public TreeVisualizationView(final Map<Integer, AbstractNode> abstractNodeMap, final TreeLayoutAlgorithm algorithm) {
        this.abstractNodeMap = abstractNodeMap;
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

        Platform.runLater(this::handlePanelAction);
    }

    private void initializeComponentLayout() {
        var actionToolbar = createActionToolbar();
        scrollPaneContainer.setPannable(false);
        scrollPaneContainer.setStyle("-fx-background-color:transparent; -fx-padding: 0; -fx-background: white;");
        this.setTop(actionToolbar);
        this.setCenter(scrollPaneContainer);
    }

    /// Executes the rendering pass after the algorithm has computed the grid
    private void renderTreeStructure() {
        drawingCanvas.getChildren().clear();
        currentInfoPanel = null; // Panel references are cleared with the canvas
        var rootNode = abstractNodeMap.get(1);
        if (rootNode != null) {
            layoutAlgorithm.calculate(rootNode, VIRTUAL_CANVAS_SIZE / 2, VIRTUAL_CANVAS_SIZE / 2);
            drawCalculatedTree(rootNode);
        }
    }

    private void drawCalculatedTree(AbstractNode node) {
        // Render edges first so they sit visually behind the circles
        for (var child : node.getChildren()) {
            drawConnectionEdge(node.getGridX(), node.getGridY(), child.getGridX(), child.getGridY());
            drawCalculatedTree(child);
        }

        // Render node visuals on top
        renderNodeVisuals(node, node.getGridX(), node.getGridY());
    }

    private void renderNodeVisuals(AbstractNode node, double x, double y) {
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

        // Show the info panel when a node circle is clicked.
        // e.consume() prevents the canvas drag handler from also firing.
        circle.setOnMouseClicked(e -> {
            showNodeInfoPanel(node, x, y);
            e.consume();
        });

        drawingCanvas.getChildren().addAll(circle, text);
    }

    private void drawConnectionEdge(double x1, double y1, double x2, double y2) {
        var line = new Line(x1, y1, x2, y2);
        line.setStroke(Color.GRAY);
        line.setStrokeWidth(1);

        // Adds edges to back of layout stack
        drawingCanvas.getChildren().addFirst(line);
    }

    private void handlePanelAction() {
        // Dismiss any open info panel before resetting the view
        dismissInfoPanel();

        drawingCanvas.setTranslateX(0);
        drawingCanvas.setTranslateY(0);
        drawingCanvas.setScaleX(1.0);
        drawingCanvas.setScaleY(1.0);
        renderTreeStructure();

        scrollPaneContainer.setHvalue(0.5);
        scrollPaneContainer.setVvalue(0.5);
    }

    private void handleReadjustAction() {
        var dialog = new TextInputDialog();
        dialog.setTitle("Readjust Node");
        dialog.setHeaderText("Enter Node ID");
        dialog.setContentText("Please enter an integer ID:");

        var result = dialog.showAndWait();

        result.ifPresent(input -> {
            try {
                int nodeId = Integer.parseInt(input.trim());
                var node = abstractNodeMap.get(nodeId);

                // Changes the status of the node to readjusted.
                if (node.getStatus() == NodeStatus.READJUSTED)
                    throw new IllegalStateException("Node is already readjusted.");
                if (!node.getChildren().isEmpty())
                    throw new IllegalStateException("Node cannot be readjusted because it has children.");
                if (isNotReadjustable(node.getPrecomputable()))
                    throw new IllegalStateException("Node cannot be readjusted because it is not readjustable.");

                node.setStatus(NodeStatus.READJUSTED);
                node.readjust();

                renderTreeStructure();
            } catch (NumberFormatException e) {
                showErrorAlert("Invalid Input", "The value '" + input + "' is not a valid integer.");
            } catch (IllegalStateException e) {
                log.warning(e.getMessage());
                showErrorAlert("Readjust Error", e.getMessage());
            }
        });
    }

    private boolean isNotReadjustable(Precomputable precomputable) {
        return precomputable instanceof RootPreCompute ||
                precomputable instanceof FirstChildPreCompute ||
                precomputable instanceof SecondChildPreCompute ||
                precomputable instanceof RootifiedPreCompute;

    }

    // ─────────────────────────────────────────────────────────────────────────
    // Node info panel
    // ─────────────────────────────────────────────────────────────────────────

    /// Builds and positions a floating info panel on the canvas anchored to the
    /// clicked node. Any previously shown panel is removed first.
    private void showNodeInfoPanel(AbstractNode node, double nodeX, double nodeY) {
        dismissInfoPanel();

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

        // Services — show the simple class name, fall back to "none" if null
        String precomputeName = node.getPrecomputable() != null
                ? node.getPrecomputable().getClass().getSimpleName()
                : "none";
        String computeName = node.getComputable() != null
                ? node.getComputable().getClass().getSimpleName()
                : "none";
        addInfoRow(grid, row++, "Precomputable", precomputeName);
        addInfoRow(grid, row++, "Computable", computeName);

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

        closeBtn.setOnAction(_ -> dismissInfoPanel());

        currentInfoPanel = panel;
        drawingCanvas.getChildren().add(panel);
    }

    /// Removes the info panel from the canvas if one is currently shown.
    private void dismissInfoPanel() {
        if (currentInfoPanel != null) {
            drawingCanvas.getChildren().remove(currentInfoPanel);
            currentInfoPanel = null;
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
            double newScale = drawingCanvas.getScaleX() * zoomFactor;
            if (newScale >= MIN_SCALE && newScale <= MAX_SCALE) {
                drawingCanvas.setScaleX(newScale);
                drawingCanvas.setScaleY(newScale);
            }
            e.consume();
        });
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

        // Clicking on blank canvas space dismisses the info panel
        drawingCanvas.setOnMouseClicked(e -> dismissInfoPanel());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Toolbar
    // ─────────────────────────────────────────────────────────────────────────

    private HBox createActionToolbar() {
        var btnAdd = new Button("Add Node");
        var btnRoot = new Button("Rootify");
        btnRoot.setOnAction(_ -> handleRootifyAction());
        var btnReset = new Button("Readjust");
        btnReset.setOnAction(_ -> handleReadjustAction());
        var toolbar = new HBox(15, btnAdd, btnRoot, btnReset);
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setStyle("-fx-padding: 10; -fx-background-color: #f4f4f4; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");
        return toolbar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rootify action
    // ─────────────────────────────────────────────────────────────────────────

    private void handleRootifyAction() {
        var dialog = new TextInputDialog();
        dialog.setTitle("Rootify Node");
        dialog.setHeaderText("Enter Node ID");
        dialog.setContentText("Please enter an integer ID:");

        var result = dialog.showAndWait();

        result.ifPresent(input -> {
            try {
                int nodeId = Integer.parseInt(input.trim());
                var node = abstractNodeMap.get(nodeId);
                rootifyNode(node);
                renderTreeStructure();
            } catch (NumberFormatException e) {
                showErrorAlert("Invalid Input", "The value '" + input + "' is not a valid integer.");
            }
        });
    }

    private void rootifyNode(AbstractNode node) {

        node.setStatus(NodeStatus.ROOTIFIED);

        node.setPrecomputable(new RootifiedPreCompute());
        node.setComputable(new RootifiedCompute());
    }

    private void showErrorAlert(String header, String content) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}