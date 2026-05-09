package com.yashgamerx.flcd.model;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TreeNode {
    private final int identifier; // [cite: 86]
    private final List<TreeNode> children = new ArrayList<>();

    // Algorithmic State
    private NodeType nodeType;
    private double currentAbsoluteAngle; // [cite: 183]
    private double subtreeWidth;
    private double subtreeHeight;
    private double gridX;
    private double gridY;

    public enum NodeType {
        ROOT, FIRST_CHILD, SECOND_CHILD_LEFT, SECOND_CHILD_RIGHT, VERTICAL, HORIZONTAL
    }

    public TreeNode(int identifier) {
        this.identifier = identifier;
    }
}