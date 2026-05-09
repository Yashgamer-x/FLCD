package com.yashgamerx.flcd.view;

import com.yashgamerx.flcd.model.TreeNode;
import com.yashgamerx.flcd.service.PlanarGridAlgorithm;
import com.yashgamerx.flcd.service.TreeLayoutAlgorithm;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import lombok.extern.java.Log;

/**
 * TreeVisualizationView implements the initial stages of the Planar Straight-line
 * Grid Drawing Algorithm for General Trees. [cite: 30]
 * * CORE PRINCIPLES APPLIED:
 * 1. Step 1: Root is placed at the absolute center of the drawing area. [cite: 80, 81]
 * 2. Step 2: Level 1 nodes are arranged in a circular fashion with quasi-equal
 * spacing. [cite: 118]
 * 3. Angular Persistence: Each node stores its angle to facilitate future
 * readjustment and rootification. [cite: 31, 183]
 */
@Log
public class TreeVisualizationView extends BorderPane {

    // Visual Constants derived from algorithmic requirements
    private static final double NODE_CIRCLE_RADIUS = 22.0;
    private static final double RADIAL_LAYER_DISTANCE = 150.0; // Radius for circular placement
    private static final double VIRTUAL_CANVAS_SIZE = 8000.0;

    // Zoom Constraints for interactive exploration [cite: 14]
    private static final double ZOOM_INTENSITY = 0.1;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;

    private final TreeLayoutAlgorithm currentAlgorithm = new PlanarGridAlgorithm();

    private final TreeNode rootNode;
    private final Pane drawingCanvas;
    private final ScrollPane scrollPaneContainer;

    private double mouseDragAnchorX;
    private double mouseDragAnchorY;

    public TreeVisualizationView(final TreeNode rootNode) {
        this.rootNode = rootNode;
        this.drawingCanvas = new Pane();
        this.drawingCanvas.setPrefSize(VIRTUAL_CANVAS_SIZE, VIRTUAL_CANVAS_SIZE);
        this.drawingCanvas.setStyle("-fx-background-color: white;");

        var contentWrapper = new StackPane(drawingCanvas);
        contentWrapper.setAlignment(Pos.TOP_LEFT);
        this.scrollPaneContainer = new ScrollPane(contentWrapper);

        initializeComponentLayout();
        attachMouseGestureListeners();
        attachZoomListeners();

        // Platform.runLater ensures layout is calculated before centering the viewport
        Platform.runLater(this::handleReadjustAction);
    }

    private void initializeComponentLayout() {
        var actionToolbar = createActionToolbar();

        scrollPaneContainer.setPannable(false);
        scrollPaneContainer.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPaneContainer.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPaneContainer.setStyle("-fx-background-color:transparent; -fx-padding: 0; -fx-background: white;");

        this.setTop(actionToolbar);
        this.setCenter(scrollPaneContainer);
    }

    /**
     * Orchestrates the drawing process based on the Planar Grid Algorithm. [cite: 31]
     */
    private void renderTreeStructure() {
        drawingCanvas.getChildren().clear();

        if (rootNode != null) {
            // STEP 1: Delegate MATH to the Algorithm class [cite: 78]
            double centerX = VIRTUAL_CANVAS_SIZE / 2;
            double centerY = VIRTUAL_CANVAS_SIZE / 2;

            currentAlgorithm.calculateLayout(rootNode, centerX, centerY);

            // STEP 2: Only handle the DRAWING/UI here
            drawTreeFromCalculatedModel(rootNode);
        }
    }

    private void drawTreeFromCalculatedModel(TreeNode node) {
        // Use the gridX and gridY values previously set by the algorithm [cite: 166]
        renderNodeVisuals(node, node.getGridX(), node.getGridY());

        for (TreeNode child : node.getChildren()) {
            drawConnectionEdge(node.getGridX(), node.getGridY(), child.getGridX(), child.getGridY());
            drawTreeFromCalculatedModel(child);
        }
    }

    private void renderNodeVisuals(TreeNode node, double x, double y) {
        var nodeCircle = new Circle(x, y, NODE_CIRCLE_RADIUS, Color.AZURE);
        nodeCircle.setStroke(Color.DARKSLATEGRAY);
        nodeCircle.setStrokeWidth(2.0);

        var label = new Text(String.valueOf(node.getIdentifier()));
        label.setX(x - (label.getLayoutBounds().getWidth() / 2));
        label.setY(y + (label.getLayoutBounds().getHeight() / 4));
        label.setStyle("-fx-font-weight: bold;");

        drawingCanvas.getChildren().addAll(nodeCircle, label);
    }

    private void drawConnectionEdge(double x1, double y1, double x2, double y2) {
        var connector = new Line(x1, y1, x2, y2);
        connector.setStroke(Color.GRAY);
        drawingCanvas.getChildren().addFirst(connector); // Ensure lines are behind nodes
    }

    private void handleReadjustAction() {
        drawingCanvas.setTranslateX(0);
        drawingCanvas.setTranslateY(0);
        drawingCanvas.setScaleX(1.0);
        drawingCanvas.setScaleY(1.0);

        renderTreeStructure();

        // Center viewport on the (4000, 4000) coordinate [cite: 81]
        scrollPaneContainer.setHvalue(0.5);
        scrollPaneContainer.setVvalue(0.5);
    }

    private void attachZoomListeners() {
        drawingCanvas.setOnScroll((ScrollEvent event) -> {
            double zoomFactor = (event.getDeltaY() > 0) ? (1 + ZOOM_INTENSITY) : (1 - ZOOM_INTENSITY);
            double newScale = drawingCanvas.getScaleX() * zoomFactor;

            if (newScale >= MIN_SCALE && newScale <= MAX_SCALE) {
                drawingCanvas.setScaleX(newScale);
                drawingCanvas.setScaleY(newScale);
            }
            event.consume();
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
    }

    private HBox createActionToolbar() {
        var addNodeButton = new Button("Add Node");
        var rootifyNodeButton = new Button("Rootify Node");
        var readjustNodeButton = new Button("Readjust");
        var zoomInButton = new Button("+");
        var zoomOutButton = new Button("-");

        var toolbar = new HBox(15, addNodeButton, rootifyNodeButton, readjustNodeButton, zoomOutButton, zoomInButton);
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setStyle("-fx-padding: 10; -fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");

        readjustNodeButton.setOnAction(_ -> handleReadjustAction());
        zoomInButton.setOnAction(_ -> applyManualZoom(1.1));
        zoomOutButton.setOnAction(_ -> applyManualZoom(0.9));

        return toolbar;
    }

    private void applyManualZoom(double factor) {
        double newScale = drawingCanvas.getScaleX() * factor;
        if (newScale >= MIN_SCALE && newScale <= MAX_SCALE) {
            drawingCanvas.setScaleX(newScale);
            drawingCanvas.setScaleY(newScale);
        }
    }
}