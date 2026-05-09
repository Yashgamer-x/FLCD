package com.yashgamerx.flcd.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = "children")
public class TreeNode {
    private final int identifier;
    private final List<TreeNode> children = new ArrayList<>();

    // The current absolute angle relative to the root (Step 2)
    private double currentAbsoluteAngle;

    // Algorithm Quality Measures: Dimensions of the subtree rooted here
    // Used to calculate optimal radius and prevent overlaps [cite: 109, 110]
    private double subtreeWidth;
    private double subtreeHeight;

    // Coordinates on the integer grid [cite: 11]
    private double gridX;
    private double gridY;

    public TreeNode(int identifier) {
        this.identifier = identifier;
    }

    public void addChild(TreeNode childNode) {
        this.children.add(childNode);
    }
}