package com.yashgamerx.flcd.flcd.view;

import com.yashgamerx.flcd.common.NodeRole;
import com.yashgamerx.flcd.common.metrics.*;
import com.yashgamerx.flcd.flcd.algorithm.TreeLayoutAlgorithm;
import com.yashgamerx.flcd.flcd.engine.FLCDNodeEngine;
import com.yashgamerx.flcd.flcd.model.FLCDNode;
import com.yashgamerx.flcd.flcd.model.NodeStatus;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.FileChooser;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.yashgamerx.flcd.flcd.model.FLCDNode.NODE_DIAMETER;

@Log
public class FLCDTreeVisualizationView extends BorderPane {

    // ─────────────────────────────────────────────────────────────────────────
    // Mode enum — tracks what a node click should do
    // ─────────────────────────────────────────────────────────────────────────

    /// Matches the diameter of 5.0 established in the preCompute phase.
    private static final double NODE_RADIUS = FLCDNode.NODE_RADIUS;
    private static final double VIRTUAL_CANVAS_SIZE = 8000.0;
    private static final double ZOOM_INTENSITY = 0.1;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;
    /// Default node fill and the "light red" highlight applied by the Light Red button.
    private static final Color DEFAULT_NODE_FILL = Color.AZURE;
    private static final Color LIGHT_RED = Color.web("#FF9999");
    /// Rectangle and triangle are sized to sit inside the node's circle of radius
    /// NODE_RADIUS, so they never extend past the footprint the layout reserved
    /// for the node. The rectangle is 4:3 (corners land exactly on the circle).
    private static final double RECT_WIDTH = NODE_RADIUS * 1.6;
    private static final double RECT_HEIGHT = NODE_RADIUS * 1.2;
    /// Half the base of an equilateral triangle inscribed in the node's circle.
    private static final double TRIANGLE_HALF_BASE = NODE_RADIUS * Math.sqrt(3.0) / 2.0;
    private final Pane drawingCanvas;
    private final ScrollPane scrollPaneContainer;
    private final Map<Integer, FLCDNode> nodeMap;
    /// Single behavioral engine shared by all layout/interaction actions —
    /// replaces the old per-node Precomputable/Computable Strategy objects.
    private final FLCDNodeEngine engine = new FLCDNodeEngine();
    private final TreeLayoutAlgorithm layoutAlgorithm;
    /// Tracks all currently visible node info panels, keyed by node identifier,
    /// so any number of them can stay open at once. A panel is only ever
    /// removed when its own ✕ button is clicked, or when the tree is redrawn.
    private final Map<Integer, Region> openInfoPanels = new HashMap<>();
    /// Shape references keyed by node identifier, so edge-length selection,
    /// recoloring and reshaping can reach a node's shape without re-scanning the canvas.
    private final Map<Integer, Shape> nodeShapes = new HashMap<>();
    /// Per-node fill colors set by the user. Kept outside the canvas so they
    /// survive a full redraw (Readjust and Rootify both re-render the tree).
    private final Map<Integer, Color> nodeFillColors = new HashMap<>();
    /// Per-node shapes set by the user. Same persistence reason as the colors.
    /// A node with no entry is drawn as a circle.
    private final Map<Integer, NodeShape> nodeShapeTypes = new HashMap<>();
    /// Shape picked in the Node menu, applied to nodes clicked in CHANGE_SHAPE mode.
    private NodeShape selectedShape = NodeShape.RECTANGLE;
    /// Nodes picked so far for the EDGE_LENGTH click-to-select flow. Holds
    /// 0 or 1 nodes between clicks; a second click completes the pair,
    /// shows the result, and the list is cleared for the next pair.
    private final List<FLCDNode> edgeLengthSelection = new ArrayList<>();
    /// Current interaction mode — determines what happens when a node is clicked.
    private ActiveMode activeMode = ActiveMode.NONE;
    private double mouseDragAnchorX;
    private double mouseDragAnchorY;
    /// The "Inspect" menu item, which is the default (NONE) mode. Selecting it
    /// is how a mode is switched off, since a radio menu item cannot be clicked off.
    private RadioMenuItem inspectItem;
    /// Toolbar hint label — kept as a field so the EDGE_LENGTH flow can update
    /// it mid-selection ("Node #3 selected — click a second node"), not just
    /// on mode switch.
    private Label hintLabel;

    private Label zoomLabel;

    /// Records calculation-only and calculation+draw timings for every
    /// redraw, feeding the "Completion Time Graph" button.
    private final MetricsHistory metricsHistory = new MetricsHistory();
    private static final String ALGORITHM_NAME = "FLCD";
    /// Name of the source `.txt` file the tree was parsed from, recorded
    /// alongside each exported metrics row so a CSV built up across runs
    /// still shows which input produced which numbers.
    private final String sourceFileName;
    /// File chosen for "Append to File", remembered so repeated clicks
    /// keep appending rows to the same comparison-study CSV instead of
    /// re-prompting every time.
    private File metricsExportFile;

    public FLCDTreeVisualizationView(final Map<Integer, FLCDNode> nodeMap, final TreeLayoutAlgorithm algorithm,
                                     final String sourceFileName) {
        this.nodeMap = nodeMap;
        this.layoutAlgorithm = algorithm;
        this.sourceFileName = sourceFileName;
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

    /// Sets the active mode when a mode menu item is chosen. Choosing
    /// "Inspect" returns to NONE.
    private void setMode(ActiveMode mode) {
        clearEdgeLengthSelectionHighlights();
        activeMode = mode;
        // Info panels are intentionally left open, they only close via their ✕ button.
    }

    private void initializeComponentLayout() {
        var menuArea = createMenuArea();
        scrollPaneContainer.setPannable(false);
        scrollPaneContainer.setStyle("-fx-background-color:transparent; -fx-padding: 0; -fx-background: white;");
        this.setTop(menuArea);
        this.setCenter(scrollPaneContainer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mode management
    // ─────────────────────────────────────────────────────────────────────────

    /// Resets mode to NONE and selects the "Inspect" menu item.
    private void clearMode() {
        activeMode = ActiveMode.NONE;
        if (inspectItem != null) {
            inspectItem.setSelected(true);
        }
    }

    private void renderNodeVisuals(FLCDNode node, double x, double y) {
        var shape = buildNodeShape(node, x, y);

        var text = new Label(String.valueOf(node.getIdentifier()));
        text.setStyle("-fx-font-weight: bold; -fx-font-size: 4px;");

        // Set the anchor point of the text right in its center
        text.setAlignment(Pos.CENTER);

        // Position the center anchor exactly at the node's (x, y)
        text.setLayoutX(x - NODE_RADIUS);
        text.setLayoutY(y - NODE_RADIUS);
        text.setPrefSize(NODE_DIAMETER, NODE_DIAMETER);
        text.setMouseTransparent(true);

        nodeShapes.put(node.getIdentifier(), shape);
        drawingCanvas.getChildren().addAll(shape, text);
    }

    /// Builds the visual for a node using its remembered shape and fill color
    /// (circle and azure when the user has not changed them), centered on (x, y),
    /// with its click handler attached. Used for the initial render and for
    /// swapping a single node's shape in place.
    private Shape buildNodeShape(FLCDNode node, double x, double y) {
        int id = node.getIdentifier();
        Shape shape = switch (nodeShapeTypes.getOrDefault(id, NodeShape.CIRCLE)) {
            case CIRCLE -> new Circle(x, y, NODE_RADIUS);
            case RECTANGLE -> new Rectangle(x - RECT_WIDTH / 2.0, y - RECT_HEIGHT / 2.0, RECT_WIDTH, RECT_HEIGHT);
            case TRIANGLE -> new Polygon(
                    x, y - NODE_RADIUS,
                    x + TRIANGLE_HALF_BASE, y + NODE_RADIUS / 2.0,
                    x - TRIANGLE_HALF_BASE, y + NODE_RADIUS / 2.0);
        };
        shape.setFill(nodeFillColors.getOrDefault(id, DEFAULT_NODE_FILL));
        shape.setStroke(Color.DARKSLATEGRAY);
        shape.setStrokeWidth(1);
        shape.setCursor(Cursor.HAND);

        // Dispatch to the currently active mode on click.
        // e.consume() prevents the canvas drag handler from also firing.
        shape.setOnMouseClicked(e -> {
            switch (activeMode) {
                case READJUST -> handleReadjustOnNode(node);
                case ROOTIFY -> handleRootifyOnNode(node);
                case EDGE_LENGTH -> handleNodeSelectedForEdgeLength(node, (Shape) e.getSource());
                case NAME_ONLY -> showNodeNamePanel(node, x, y);
                case LIGHT_RED -> handleLightRedOnNode(node);
                case CHANGE_SHAPE -> handleChangeShapeOnNode(node);
                default -> showNodeInfoPanel(node, x, y);
            }
            e.consume();
        });
        return shape;
    }

    /// Turns the clicked node light red. Only the one shape is touched, so no
    /// re-layout happens and the recorded timing metrics are not affected.
    private void handleLightRedOnNode(FLCDNode node) {
        nodeFillColors.put(node.getIdentifier(), LIGHT_RED);
        var shape = nodeShapes.get(node.getIdentifier());
        if (shape != null) {
            shape.setFill(LIGHT_RED);
        }
    }

    /// Changes the clicked node to the shape currently picked in the toolbar.
    /// The old shape is swapped out in place (same position in the canvas
    /// children, so the label and edges keep their stacking order). No
    /// re-layout happens and the recorded timing metrics are not affected.
    private void handleChangeShapeOnNode(FLCDNode node) {
        int id = node.getIdentifier();
        var chosen = selectedShape;
        if (nodeShapeTypes.getOrDefault(id, NodeShape.CIRCLE) == chosen) return;

        var oldShape = nodeShapes.get(id);
        int index = oldShape == null ? -1 : drawingCanvas.getChildren().indexOf(oldShape);
        if (index < 0) return;

        nodeShapeTypes.put(id, chosen);
        var newShape = buildNodeShape(node, node.getLayoutX(), node.getLayoutY());
        drawingCanvas.getChildren().set(index, newShape);
        nodeShapes.put(id, newShape);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rendering
    // ─────────────────────────────────────────────────────────────────────────

    /// Executes the rendering pass after the algorithm has computed the grid
    private void renderTreeStructure() {
        drawingCanvas.getChildren().clear();
        openInfoPanels.clear(); // Panel references are cleared with the canvas
        nodeShapes.clear();
        edgeLengthSelection.clear(); // Underlying circles are gone; no need to un-highlight them
        var rootNode = nodeMap.get(1);
        if (rootNode != null) {
            long calcStart = System.nanoTime();
            layoutAlgorithm.calculate(rootNode, VIRTUAL_CANVAS_SIZE / 2, VIRTUAL_CANVAS_SIZE / 2);
            long calcEnd = System.nanoTime();

            drawCalculatedTree(rootNode);
            long drawEnd = System.nanoTime();

            double calcMillis = (calcEnd - calcStart) / 1_000_000.0;
            double totalMillis = (drawEnd - calcStart) / 1_000_000.0;
            metricsHistory.record(nodeMap.size(), calcMillis, totalMillis);
        }
    }

    private void drawCalculatedTree(FLCDNode node) {
        // Render edges first so they sit visually behind the circles
        for (var child : node.getChildren()) {
            drawConnectionEdge(node.getLayoutX(), node.getLayoutY(), child.getLayoutX(), child.getLayoutY());
            drawCalculatedTree(child);
        }

        // Render node visuals on top
        renderNodeVisuals(node, node.getLayoutX(), node.getLayoutY());
    }

    private void handleReadjustOnNode(FLCDNode node) {
        try {
            if (node.getStatus() == NodeStatus.READJUSTED)
                throw new IllegalStateException("Node is already readjusted.");
            if (!node.getChildren().isEmpty())
                throw new IllegalStateException("Node cannot be readjusted because it has children.");
            if (isNotReadjustable(node))
                throw new IllegalStateException("Node cannot be readjusted because it is not readjustable.");

            engine.readjust(node);
//            clearMode();
            renderTreeStructure();
        } catch (IllegalStateException e) {
            log.warning(e.getMessage());
            showErrorAlert("Readjust Error", e.getMessage());
        }
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
        updateZoomLabel(1.0);
        renderTreeStructure();

        scrollPaneContainer.setHvalue(0.5);
        scrollPaneContainer.setVvalue(0.5);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Readjust action — triggered directly by clicking a node in READJUST mode
    // ─────────────────────────────────────────────────────────────────────────

    /// Builds the area above the canvas: a menu bar holding every action,
    /// and a slim status row under it with the mode hint and zoom percentage.
    private VBox createMenuArea() {
        // One ToggleGroup so exactly one mode is active at a time
        var modeGroup = new ToggleGroup();

        hintLabel = new Label();
        hintLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #777; -fx-font-style: italic;");

        zoomLabel = new Label("100%");
        zoomLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #555;");

        // ── Mode menu ────────────────────────────────────────────────────────
        inspectItem = modeItem("Inspect (default)", modeGroup, ActiveMode.NONE, "");
        inspectItem.setSelected(true);
        var menuMode = new Menu("Mode", null,
                inspectItem,
                new SeparatorMenuItem(),
                modeItem("Rootify", modeGroup, ActiveMode.ROOTIFY, "Click a node to rootify it"),
                modeItem("Readjust", modeGroup, ActiveMode.READJUST, "Click a node to readjust it"),
                modeItem("Edge Length", modeGroup, ActiveMode.EDGE_LENGTH,
                        "Click a node, then click a second node to measure the edge between them"),
                modeItem("Show Name Only", modeGroup, ActiveMode.NAME_ONLY, "Click a node to see just its name"));

        // ── Node menu ────────────────────────────────────────────────────────
        var btnAdd = new MenuItem("Add Node");

        var shapeMenu = new Menu("Change Shape");
        for (var shape : new NodeShape[]{NodeShape.RECTANGLE, NodeShape.TRIANGLE, NodeShape.CIRCLE}) {
            var item = new RadioMenuItem(shape.toString());
            item.setToggleGroup(modeGroup);
            item.setOnAction(_ -> {
                selectedShape = shape;
                setMode(ActiveMode.CHANGE_SHAPE);
                hintLabel.setText("Click a node to change it to a " + shape.name().toLowerCase());
            });
            shapeMenu.getItems().add(item);
        }

        var menuNode = new Menu("Node", null,
                btnAdd,
                new SeparatorMenuItem(),
                modeItem("Light Red", modeGroup, ActiveMode.LIGHT_RED, "Click a node to turn it light red"),
                shapeMenu);

        // ── Metrics menu ─────────────────────────────────────────────────────
        var menuMetrics = new Menu("Metrics", null,
                actionItem("Calculate Area", this::handleCalculateArea),
                actionItem("Aspect Ratio", this::handleAspectRatio),
                actionItem("Metrics", this::handleMetrics),
                actionItem("Root→Leaf Distances", this::handleLeafDistances),
                actionItem("Completion Time Graph", this::handleCompletionGraph),
                new SeparatorMenuItem(),
                actionItem("Append to File", this::handleAppendMetrics));

        var menuBar = new MenuBar(menuMode, menuNode, menuMetrics);

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var statusRow = new HBox(15, hintLabel, spacer, zoomLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setStyle("-fx-padding: 4 10 4 10; -fx-background-color: #f4f4f4; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");

        return new VBox(menuBar, statusRow);
    }

    /// Creates a radio menu item that switches to `mode` and shows `hint`.
    private RadioMenuItem modeItem(String label, ToggleGroup group, ActiveMode mode, String hint) {
        var item = new RadioMenuItem(label);
        item.setToggleGroup(group);
        item.setOnAction(_ -> {
            setMode(mode);
            hintLabel.setText(hint);
        });
        return item;
    }

    /// Creates a plain menu item that runs `action` once when chosen.
    private MenuItem actionItem(String label, Runnable action) {
        var item = new MenuItem(label);
        item.setOnAction(_ -> action.run());
        return item;
    }

    /// A node cannot be readjusted if it sits at the ROOT, FIRST_CHILD level,
    /// or is itself manually rootified.
    private boolean isNotReadjustable(FLCDNode node) {
        return node.getRole() == NodeRole.ROOT
                || node.getRole() == NodeRole.FIRST_CHILD
                || node.getStatus() == NodeStatus.ROOTIFIED;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Node info panel
    // ─────────────────────────────────────────────────────────────────────────

    /// Builds and positions a floating info panel on the canvas anchored to the
    /// clicked node. Any previously shown panel is removed first.
    private void showNodeInfoPanel(FLCDNode node, double nodeX, double nodeY) {
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
        addInfoRow(grid, row++, "Grid X", String.format("%.2f", node.getLayoutX()));
        addInfoRow(grid, row++, "Grid Y", String.format("%.2f", node.getLayoutY()));

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

        addInfoRow(grid, row++, "Status", node.getStatus().toString());
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

    /// Shows a minimal floating panel containing only the node's NAMED-file
    /// name — no identifier, coordinates, or any other algorithmic state.
    /// Reuses `openInfoPanels` so it opens/closes/redraws like the full info
    /// panel, but toggles: clicking the same node again while its name panel
    /// is open closes it (there's no ✕ button in this mode).
    private void showNodeNamePanel(FLCDNode node, double nodeX, double nodeY) {
        if (openInfoPanels.containsKey(node.getIdentifier())) {
            dismissInfoPanel(node.getIdentifier());
            return;
        }

        var nameLabel = new Label(
                (node.getName() == null || node.getName().isBlank()) ? "—" : node.getName());
        nameLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #1a1a1a;");

        var panel = new HBox(nameLabel);
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setStyle(
                "-fx-background-color: white; "
                        + "-fx-border-color: #bbb; "
                        + "-fx-border-width: 0.8; "
                        + "-fx-border-radius: 5; "
                        + "-fx-background-radius: 5; "
                        + "-fx-padding: 4 6 4 6;");

        panel.setLayoutX(nodeX + NODE_RADIUS + 4);
        panel.setLayoutY(nodeY - NODE_RADIUS);

        panel.setOnMouseClicked(javafx.event.Event::consume);
        panel.setOnMousePressed(javafx.event.Event::consume);

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
            updateZoomLabel(newScale);
        }
    }

    /// Reflects the current canvas scale in the toolbar's zoom percentage label.
    private void updateZoomLabel(double scale) {
        if (zoomLabel != null) {
            zoomLabel.setText(Math.round(scale * 100) + "%");
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
    // Toolbar
    // ─────────────────────────────────────────────────────────────────────────

    private void handleRootifyOnNode(FLCDNode node) {
        try {
            if (node.getChildren().isEmpty())
                throw new IllegalStateException("Node has no children.");

            engine.rootify(node);
//            clearMode();
            renderTreeStructure();
        } catch (IllegalStateException e) {
            log.warning(e.getMessage());
            showErrorAlert("Rootification Error", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rootify action — triggered directly by clicking a node in ROOTIFY mode
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

        FLCDNode leftMost = null, rightMost = null, topMost = null, bottomMost = null;

        for (var node : nodeMap.values()) {
            double x = node.getLayoutX();
            double y = node.getLayoutY();

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

    // ─────────────────────────────────────────────────────────────────────────
    // Aspect Ratio / Metrics / Root→Leaf Distances / Completion Graph actions
    // ─────────────────────────────────────────────────────────────────────────

    private void handleAspectRatio() {
        if (nodeMap.isEmpty()) {
            showErrorAlert("Aspect Ratio Error", "There are no nodes to measure.");
            return;
        }
        var box = TreeMetricsCalculator.computeBoundingBox(nodeMap.values(), NODE_RADIUS, FLCDNode::getIdentifier);
        MetricsDialogUtil.showAspectRatioDialog(box);
    }

    private void handleMetrics() {
        if (nodeMap.isEmpty()) {
            showErrorAlert("Metrics Error", "There are no nodes to measure.");
            return;
        }
        var box = TreeMetricsCalculator.computeBoundingBox(nodeMap.values(), NODE_RADIUS, FLCDNode::getIdentifier);
        var leafDistances = TreeMetricsCalculator.computeLeafToRootDistances(
                nodeMap.values(), FLCDNode::getParent, n -> n.getStatus() == NodeStatus.ROOTIFIED, FLCDNode::getIdentifier);
        var run = metricsHistory.latest();
        if (run == null) {
            showErrorAlert("Metrics Error", "No timed run recorded yet.");
            return;
        }
        MetricsDialogUtil.showMetricsDialog(ALGORITHM_NAME, run, box, leafDistances);
    }

    private void handleLeafDistances() {
        if (nodeMap.isEmpty()) {
            showErrorAlert("Root→Leaf Distances Error", "There are no nodes to measure.");
            return;
        }
        var leafDistances = TreeMetricsCalculator.computeLeafToRootDistances(
                nodeMap.values(), FLCDNode::getParent, n -> n.getStatus() == NodeStatus.ROOTIFIED, FLCDNode::getIdentifier);
        MetricsDialogUtil.showLeafDistancesDialog(leafDistances);
    }

    private void handleCompletionGraph() {
        MetricsChartWindow.show(ALGORITHM_NAME, metricsHistory.getRuns());
    }

    /// Appends one row (algorithm, node count, timing, area/aspect ratio,
    /// and root-to-leaf distance summary stats — no per-leaf detail) to a
    /// comparison-study CSV. Prompts for the file on the first click of a
    /// session and reuses it for every click after that.
    private void handleAppendMetrics() {
        if (nodeMap.isEmpty()) {
            showErrorAlert("Append to File Error", "There are no nodes to measure.");
            return;
        }
        var run = metricsHistory.latest();
        if (run == null) {
            showErrorAlert("Append to File Error", "No timed run recorded yet.");
            return;
        }

        if (metricsExportFile == null) {
            var chooser = new FileChooser();
            chooser.setTitle("Choose or Create Metrics CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
            chooser.setInitialFileName("metrics.csv");
            var chosen = chooser.showSaveDialog(getScene().getWindow());
            if (chosen == null) return;
            metricsExportFile = chosen;
        }

        var box = TreeMetricsCalculator.computeBoundingBox(nodeMap.values(), NODE_RADIUS, FLCDNode::getIdentifier);
        var leafDistances = TreeMetricsCalculator.computeLeafToRootDistances(
                nodeMap.values(), FLCDNode::getParent, n -> n.getStatus() == NodeStatus.ROOTIFIED, FLCDNode::getIdentifier);

        try {
            MetricsExportUtil.appendRecord(metricsExportFile, ALGORITHM_NAME, sourceFileName, run, box, leafDistances);
        } catch (IOException e) {
            log.warning("Failed to append metrics to " + metricsExportFile + ": " + e.getMessage());
            showErrorAlert("Append to File Error", "Could not write to " + metricsExportFile.getName() + ": " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Calculate Area action
    // ─────────────────────────────────────────────────────────────────────────

    /// Displays the calculated bounding-box area in a popup dialog, including
    /// which nodes defined each extreme edge.
    private void showAreaResultAlert(double width, double height, double area,
                                     FLCDNode leftMost, FLCDNode rightMost,
                                     FLCDNode topMost, FLCDNode bottomMost) {
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

    private void showErrorAlert(String header, String content) {
        var alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge Length action — triggered by clicking two nodes in EDGE_LENGTH mode
    // ─────────────────────────────────────────────────────────────────────────

    /// Records a node click made while EDGE_LENGTH mode is active. The first
    /// click highlights that node and waits for a second; the second click
    /// completes the pair, shows the result, and clears the pair so the mode
    /// stays armed for measuring another pair (mirrors Rootify/Readjust,
    /// which also stay armed after a single action).
    private void handleNodeSelectedForEdgeLength(FLCDNode node, Shape circle) {
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
            var shape = nodeShapes.get(node.getIdentifier());
            if (shape != null) {
                shape.setStroke(Color.DARKSLATEGRAY);
                shape.setStrokeWidth(1);
            }
        }
        edgeLengthSelection.clear();
    }

    /// Displays the straight-line (Euclidean) distance between the two
    /// selected nodes' grid centers — i.e. the length of the line
    /// [#drawConnectionEdge] would draw between them, regardless of whether
    /// the two nodes are actually parent/child.
    private void showEdgeLengthResultAlert(FLCDNode nodeA, FLCDNode nodeB) {
        double dx = nodeB.getLayoutX() - nodeA.getLayoutX();
        double dy = nodeB.getLayoutY() - nodeA.getLayoutY();
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
                nodeA.getIdentifier(), nodeA.getLayoutX(), nodeA.getLayoutY(),
                nodeB.getIdentifier(), nodeB.getLayoutX(), nodeB.getLayoutY(),
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

    private enum ActiveMode {NONE, READJUST, ROOTIFY, EDGE_LENGTH, NAME_ONLY, LIGHT_RED, CHANGE_SHAPE}

    /// Shapes a node can be drawn as. CIRCLE is the default and is offered in
    /// the toolbar so a reshaped node can be put back.
    private enum NodeShape {
        CIRCLE("Circle (reset)"), RECTANGLE("Rectangle"), TRIANGLE("Triangle");

        private final String label;

        NodeShape(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}