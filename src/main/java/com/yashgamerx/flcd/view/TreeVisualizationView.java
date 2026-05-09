package com.yashgamerx.flcd.view;

import com.yashgamerx.flcd.model.TreeNode;
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

@Log
public class TreeVisualizationView extends BorderPane {

    // Visual Configuration Constants
    private static final double NODE_CIRCLE_RADIUS = 22.0;
    private static final double VERTICAL_LEVEL_OFFSET = 100.0;
    private static final double HORIZONTAL_SIBLING_OFFSET = 70.0;
    private static final double VIRTUAL_CANVAS_SIZE = 8000.0;
    // Zoom Constants
    private static final double ZOOM_INTENSITY = 0.1;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;
    private final TreeNode rootNode;
    private final Pane drawingCanvas;
    private double mouseDragAnchorX;
    private double mouseDragAnchorY;

    public TreeVisualizationView(final TreeNode rootNode) {
        this.rootNode = rootNode;
        this.drawingCanvas = new Pane();

        this.drawingCanvas.setPrefSize(VIRTUAL_CANVAS_SIZE, VIRTUAL_CANVAS_SIZE);
        this.drawingCanvas.setStyle("-fx-background-color: white;");

        initializeComponentLayout();
        attachMouseGestureListeners();
        attachZoomListeners();
        renderTreeStructure();
    }

    private void initializeComponentLayout() {
        var actionToolbar = createActionToolbar();

        // FIX: Wrap drawingCanvas in a StackPane to force alignment
        var contentWrapper = new StackPane(drawingCanvas);
        contentWrapper.setAlignment(Pos.TOP_LEFT); // Force canvas to the top

        var scrollPaneContainer = new ScrollPane(contentWrapper);
        scrollPaneContainer.setPannable(false);
        scrollPaneContainer.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPaneContainer.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Remove default ScrollPane border/padding which creates "empty space"
        scrollPaneContainer.setStyle("-fx-background-color:transparent; -fx-padding: 0; -fx-background: white;");

        // Center horizontally on the virtual canvas, but start at the very top (vValue 0)
        scrollPaneContainer.setHvalue(0.5);
        scrollPaneContainer.setVvalue(0.0);

        this.setTop(actionToolbar);
        this.setCenter(scrollPaneContainer);
    }

    private void attachZoomListeners() {
        drawingCanvas.setOnScroll((ScrollEvent event) -> {
            double zoomFactor = (event.getDeltaY() > 0) ? (1 + ZOOM_INTENSITY) : (1 - ZOOM_INTENSITY);
            double currentScaleX = drawingCanvas.getScaleX();
            double newScale = currentScaleX * zoomFactor;

            if (newScale >= MIN_SCALE && newScale <= MAX_SCALE) {
                drawingCanvas.setScaleX(newScale);
                drawingCanvas.setScaleY(newScale);
            }
            event.consume();
        });
    }

    private void attachMouseGestureListeners() {
        drawingCanvas.setOnMousePressed(mouseEvent -> {
            mouseDragAnchorX = mouseEvent.getSceneX();
            mouseDragAnchorY = mouseEvent.getSceneY();
        });

        drawingCanvas.setOnMouseDragged(mouseEvent -> {
            var deltaX = mouseEvent.getSceneX() - mouseDragAnchorX;
            var deltaY = mouseEvent.getSceneY() - mouseDragAnchorY;

            drawingCanvas.setTranslateX(drawingCanvas.getTranslateX() + deltaX);
            drawingCanvas.setTranslateY(drawingCanvas.getTranslateY() + deltaY);

            mouseDragAnchorX = mouseEvent.getSceneX();
            mouseDragAnchorY = mouseEvent.getSceneY();
        });
    }

    private void renderTreeStructure() {
        drawingCanvas.getChildren().clear();

        // Start drawing at the top of the canvas
        var startXPosition = VIRTUAL_CANVAS_SIZE / 2;
        var startYPosition = 50.0; // Reduced from 150 to remove gap

        if (rootNode != null) {
            executeRecursiveDraw(rootNode, startXPosition, startYPosition);
        }
    }

    private void executeRecursiveDraw(TreeNode currentNode, double xCoordinate, double yCoordinate) {
        var nodeCircle = new Circle(xCoordinate, yCoordinate, NODE_CIRCLE_RADIUS, Color.AZURE);
        nodeCircle.setStroke(Color.DARKSLATEGRAY);
        nodeCircle.setStrokeWidth(2.0);

        var nodeLabel = new Text(String.valueOf(currentNode.getIdentifier()));
        nodeLabel.setX(xCoordinate - (nodeLabel.getLayoutBounds().getWidth() / 2));
        nodeLabel.setY(yCoordinate + (nodeLabel.getLayoutBounds().getHeight() / 4));
        nodeLabel.setStyle("-fx-font-weight: bold;");

        var childrenList = currentNode.getChildren();
        if (!childrenList.isEmpty()) {
            var totalWidthForChildren = (childrenList.size() - 1) * HORIZONTAL_SIBLING_OFFSET;
            var childStartingX = xCoordinate - (totalWidthForChildren / 2);

            for (int i = 0; i < childrenList.size(); i++) {
                var childX = childStartingX + (i * HORIZONTAL_SIBLING_OFFSET);
                var childY = yCoordinate + VERTICAL_LEVEL_OFFSET;

                var connectorLine = new Line(xCoordinate, yCoordinate, childX, childY);
                connectorLine.setStroke(Color.GRAY);
                drawingCanvas.getChildren().addFirst(connectorLine);

                executeRecursiveDraw(childrenList.get(i), childX, childY);
            }
        }
        drawingCanvas.getChildren().addAll(nodeCircle, nodeLabel);
    }

    private HBox createActionToolbar() {
        var addNodeButton = new Button("Add Node");
        var rootifyNodeButton = new Button("Rootify Node");
        var readjustNodeButton = new Button("Readjust Node");
        var zoomInButton = new Button("+");
        var zoomOutButton = new Button("-");

        var toolbarContainer = new HBox(15, addNodeButton, rootifyNodeButton, readjustNodeButton, zoomOutButton, zoomInButton);
        toolbarContainer.setAlignment(Pos.CENTER);
        toolbarContainer.setStyle("-fx-padding: 10; -fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");

        readjustNodeButton.setOnAction(_ -> handleReadjustAction());
        zoomInButton.setOnAction(_ -> applyManualZoom(1.1));
        zoomOutButton.setOnAction(_ -> applyManualZoom(0.9));

        return toolbarContainer;
    }

    private void applyManualZoom(double factor) {
        double newScale = drawingCanvas.getScaleX() * factor;
        if (newScale >= MIN_SCALE && newScale <= MAX_SCALE) {
            drawingCanvas.setScaleX(newScale);
            drawingCanvas.setScaleY(newScale);
        }
    }

    private void handleReadjustAction() {
        drawingCanvas.setTranslateX(0);
        drawingCanvas.setTranslateY(0);
        drawingCanvas.setScaleX(1.0);
        drawingCanvas.setScaleY(1.0);
        renderTreeStructure();
    }

    private void handleAddNodeAction() {
        log.info("Add node requested...");
    }

    private void handleRootifyAction() {
        log.info("Rootify requested...");
    }
}