package com.yashgamerx.flcd.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/// A single node in the FLCD layout tree.
///
/// This is a pure data holder — all layout behavior (precompute, compute,
/// readjust) lives in [com.yashgamerx.flcd.service.engine.FLCDNodeEngine].
/// A node knows only its own state: where it sits structurally ([#role]),
/// which wing it belongs to ([#side]), and its current lifecycle
/// [#status]. There is deliberately no per-node behavioral object (no
/// Strategy instance, no injector) — dispatch on `role`/`status` inside
/// the engine replaces that entirely.
@Getter
@Setter
public class FLCDNode {
    // Constants for spacing
    public static final double WIDTH_SPACER = 5.0;
    public static final double HEIGHT_SPACER = 5.0;
    public static final double NODE_DIAMETER = 10.0;
    public static final double NODE_RADIUS = NODE_DIAMETER / 2.0;

    private final int identifier;
    private String name;
    private List<FLCDNode> children = new ArrayList<>();
    private FLCDNode parent;

    // Algorithmic state as per the FLCD document
    private double subtreeWidth;
    private double subtreeHeight;
    private double nodeOffset; // Scalar distance from parent
    private double localRadianAngle; // Angle relative to parent
    private double globalRadianAngle; // Per-child angular step for subtree
    private double gridX; // Screen X coordinate
    private double gridY; // Screen Y coordinate
    private int depth;

    // Structural/behavioral markers (read by FLCDNodeEngine)
    private NodeRole role;
    private Side side = Side.NONE;
    private NodeStatus status;

    public FLCDNode(int identifier, String name, FLCDNode parent) {
        this.identifier = identifier;
        this.name = name;
        this.parent = parent;
        this.status = NodeStatus.NORMAL;
    }

    public FLCDNode(int identifier) {
        this(identifier, "", null);
    }

    /// Utility method to add a child
    public void addChild(FLCDNode child) {
        this.children.add(child);
        child.setParent(this);
    }

    public void incrementDepth() {
        int maxChildDepth = this.children.stream()
                .mapToInt(FLCDNode::getDepth)
                .max()
                .orElse(0);
        this.depth = maxChildDepth + 1;
    }
}
