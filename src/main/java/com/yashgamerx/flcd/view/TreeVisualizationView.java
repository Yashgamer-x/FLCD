package com.yashgamerx.flcd.view;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.model.RootNode;
import com.yashgamerx.flcd.service.algorithm.TreeLayoutAlgorithm;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
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
import javafx.scene.text.TextAlignment;
import lombok.extern.java.Log;

@Log
public class TreeVisualizationView extends BorderPane {

    private final RootNode rootNode;
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

    public TreeVisualizationView(final RootNode rootNode, final TreeLayoutAlgorithm algorithm) {
        this.rootNode = rootNode;
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

        Platform.runLater(this::handleReadjustAction);
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

        var text = new Text(String.valueOf(node.getIdentifier()));
        text.setStyle("-fx-font-weight: bold; -fx-font-size: 4px;");

        // Set the anchor point of the text right in its center
        text.setTextOrigin(VPos.CENTER);
        text.setTextAlignment(TextAlignment.CENTER);

        // Position the center anchor exactly at the circle's (x, y)
        text.setX(x);
        text.setY(y);

        // If needed, minor manual adjustment for perfect centering
        // because JavaFX font metrics can sometimes render slightly high:
        // text.setY(y + 0.05);

        drawingCanvas.getChildren().addAll(circle, text);
    }

    private void drawConnectionEdge(double x1, double y1, double x2, double y2) {
        var line = new Line(x1, y1, x2, y2);
        line.setStroke(Color.GRAY);
        line.setStrokeWidth(1); // Scale line thickness down to match coordinates

        // Adds edges to back of layout stack
        drawingCanvas.getChildren().addFirst(line);
    }

    private void handleReadjustAction() {
        drawingCanvas.setTranslateX(0);
        drawingCanvas.setTranslateY(0);
        drawingCanvas.setScaleX(1.0);
        drawingCanvas.setScaleY(1.0);
        renderTreeStructure();

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