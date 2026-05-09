package com.yashgamerx.flcd.view;

import com.yashgamerx.flcd.model.TreeNode;
import com.yashgamerx.flcd.service.TreeLayoutAlgorithm;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import lombok.extern.java.Log;

@Log
public class TreeVisualizationView extends BorderPane {

    private final TreeNode rootNode;
    private final Pane drawingCanvas;
    private final ScrollPane scrollPaneContainer;

    private static final double NODE_RADIUS = 22.0;

    private double mouseDragAnchorX;
    private double mouseDragAnchorY;
    private static final double VIRTUAL_CANVAS_SIZE = 8000.0;
    private static final double ZOOM_INTENSITY = 0.1;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;
    // SOLID: Dependency Inversion - Depend on Abstraction [cite: 63]
    private final TreeLayoutAlgorithm layoutAlgorithm;

    public TreeVisualizationView(final TreeNode rootNode, final TreeLayoutAlgorithm algorithm) {
        this.rootNode = rootNode;
        this.layoutAlgorithm = algorithm; // Algorithm is injected [cite: 63]
        this.drawingCanvas = new Pane();
        this.drawingCanvas.setPrefSize(VIRTUAL_CANVAS_SIZE, VIRTUAL_CANVAS_SIZE);
        this.drawingCanvas.setStyle("-fx-background-color: white;");

        var contentWrapper = new StackPane(drawingCanvas);
        contentWrapper.setAlignment(Pos.TOP_LEFT);
        this.scrollPaneContainer = new ScrollPane(contentWrapper);

        initializeComponentLayout();
        attachMouseGestureListeners();
        attachZoomListeners();

        Platform.runLater(this::handleReadjustAction);
    }

    private void initializeComponentLayout() {
        var actionToolbar = createActionToolbar();
        scrollPaneContainer.setPannable(false);
        scrollPaneContainer.setStyle("-fx-background-color:transparent; -fx-padding: 0; -fx-background: white;");
        this.setTop(actionToolbar);
        this.setCenter(scrollPaneContainer);
    }

    /**
     * Executes the rendering pass after the algorithm has computed the grid[cite: 31, 32].
     */
    private void renderTreeStructure() {
        drawingCanvas.getChildren().clear();
        if (rootNode != null) {
            // PASS 1: The algorithm computes coordinates using divide and conquer [cite: 78, 79]
            layoutAlgorithm.calculate(rootNode, VIRTUAL_CANVAS_SIZE / 2, VIRTUAL_CANVAS_SIZE / 2); // [cite: 80]

            // PASS 2: The view renders based on computed coordinates
            drawCalculatedTree(rootNode);
        }
    }

    private void drawCalculatedTree(TreeNode node) {
        // Use the coordinates assigned by the layout engine [cite: 32]
        renderNodeVisuals(node, node.getGridX(), node.getGridY());

        for (TreeNode child : node.getChildren()) {
            drawConnectionEdge(node.getGridX(), node.getGridY(), child.getGridX(), child.getGridY());
            drawCalculatedTree(child);
        }
    }

    private void renderNodeVisuals(TreeNode node, double x, double y) {
        var circle = new Circle(x, y, NODE_RADIUS, Color.AZURE);
        circle.setStroke(Color.DARKSLATEGRAY);
        circle.setStrokeWidth(2.0);

        var label = new Text(String.valueOf(node.getIdentifier()));
        label.setX(x - (label.getLayoutBounds().getWidth() / 2));
        label.setY(y + (label.getLayoutBounds().getHeight() / 4));
        label.setStyle("-fx-font-weight: bold;");

        drawingCanvas.getChildren().addAll(circle, label);
    }

    private void drawConnectionEdge(double x1, double y1, double x2, double y2) {
        var line = new Line(x1, y1, x2, y2);
        line.setStroke(Color.GRAY);
        drawingCanvas.getChildren().addFirst(line);
    }

    private void handleReadjustAction() {
        drawingCanvas.setTranslateX(0);
        drawingCanvas.setTranslateY(0);
        drawingCanvas.setScaleX(1.0);
        drawingCanvas.setScaleY(1.0);
        renderTreeStructure();

        // Centering is crucial for visual organization [cite: 81]
        scrollPaneContainer.setHvalue(0.5);
        scrollPaneContainer.setVvalue(0.5);
    }

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
    }

    private HBox createActionToolbar() {
        var btnAdd = new Button("Add Node");
        var btnRoot = new Button("Rootify");
        var btnReset = new Button("Readjust");
        var toolbar = new HBox(15, btnAdd, btnRoot, btnReset);
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setStyle("-fx-padding: 10; -fx-background-color: #f4f4f4; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");
        btnReset.setOnAction(_ -> handleReadjustAction());
        return toolbar;
    }
}