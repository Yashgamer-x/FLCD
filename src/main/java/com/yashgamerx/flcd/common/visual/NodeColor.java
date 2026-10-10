package com.yashgamerx.flcd.common.visual;

import javafx.scene.paint.Color;

/// Fill colors a tree node can be given. The tones are medium-light on purpose
/// so the small black node identifier stays readable on every one of them.
/// DEFAULT is the azure every node starts with and doubles as the reset.
public enum NodeColor {
    DEFAULT("Default (Azure)", Color.AZURE),
    RED("Red", Color.web("#EF5350")),
    LIGHT_RED("Light Red", Color.web("#FF9999")),
    GREEN("Green", Color.web("#66BB6A")),
    BLUE("Blue", Color.web("#42A5F5")),
    YELLOW("Yellow", Color.web("#FFEE58")),
    ORANGE("Orange", Color.web("#FFA726")),
    PURPLE("Purple", Color.web("#AB47BC")),
    PINK("Pink", Color.web("#F48FB1")),
    TEAL("Teal", Color.web("#26A69A")),
    CYAN("Cyan", Color.web("#26C6DA")),
    LIME("Lime", Color.web("#D4E157")),
    BROWN("Brown", Color.web("#A1887F")),
    GREY("Grey", Color.web("#B0BEC5"));

    private final String label;
    private final Color color;

    NodeColor(String label, Color color) {
        this.label = label;
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public String toString() {
        return label;
    }
}
