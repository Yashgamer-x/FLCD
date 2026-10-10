package com.yashgamerx.flcd.common.visual;

import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.function.Consumer;

/// Builds the "Change Color" and "Change Shape" submenus shared by every
/// tree visualization view, so each view only supplies what should happen
/// when an option is picked. Each entry shows a small preview swatch.
public final class NodeStyleMenus {

    private static final double PREVIEW_SIZE = 14.0;

    private NodeStyleMenus() {
    }

    /// One radio entry per [NodeColor], each with a colored swatch.
    public static Menu colorMenu(ToggleGroup group, Consumer<NodeColor> onSelected) {
        var menu = new Menu("Change Color");
        for (var option : NodeColor.values()) {
            var swatch = new Rectangle(PREVIEW_SIZE - 2, PREVIEW_SIZE - 2, option.getColor());
            swatch.setStroke(Color.DARKSLATEGRAY);
            swatch.setStrokeWidth(0.8);
            menu.getItems().add(radioItem(option.toString(), group, preview(swatch), () -> onSelected.accept(option)));
        }
        return menu;
    }

    /// One radio entry per [NodeShape], each with a small outline of the shape.
    public static Menu shapeMenu(ToggleGroup group, Consumer<NodeShape> onSelected) {
        var menu = new Menu("Change Shape");
        for (var option : NodeShape.values()) {
            Shape outline = option.create(PREVIEW_SIZE / 2.0, PREVIEW_SIZE / 2.0, PREVIEW_SIZE / 2.0 - 1.0);
            outline.setFill(Color.LIGHTGRAY);
            outline.setStroke(Color.DARKSLATEGRAY);
            outline.setStrokeWidth(0.8);
            menu.getItems().add(radioItem(option.toString(), group, preview(outline), () -> onSelected.accept(option)));
        }
        return menu;
    }

    private static RadioMenuItem radioItem(String label, ToggleGroup group, StackPane graphic, Runnable action) {
        var item = new RadioMenuItem(label);
        item.setToggleGroup(group);
        item.setGraphic(graphic);
        item.setOnAction(_ -> action.run());
        return item;
    }

    private static StackPane preview(Shape shape) {
        var box = new StackPane(shape);
        box.setPrefSize(PREVIEW_SIZE, PREVIEW_SIZE);
        box.setMinSize(PREVIEW_SIZE, PREVIEW_SIZE);
        return box;
    }
}
