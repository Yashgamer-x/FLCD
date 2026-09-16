package com.yashgamerx.flcd.rt.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/// A single node in the classic Reingold–Tilford tree layout, as implemented
/// by the linear-time Buchheim–Jünger–Leipert (2002) variant of Algorithm TR
/// from Reingold & Tilford, "Tidier Drawings of Trees" (1981).
///
/// This is a pure data holder, mirroring the pattern used by `FLCDNode` /
/// `TMELNode` elsewhere in this codebase — all layout behavior lives in
/// [com.yashgamerx.flcd.rt.algorithm.ReingoldTilfordAlgorithm]. Fields below
/// map directly onto the "prelim", "mod", "shift", "change", "ancestor", and
/// "thread" bookkeeping used by the two-pass (firstWalk/secondWalk) algorithm.
@Getter
@Setter
public class RTNode {
    // Constants for spacing — kept in the same units/scale as FLCDNode's
    // NODE_DIAMETER so that the RT baseline is visually/comparably sized
    // against the FLCD/TMEL/CMEL families.
    public static final double NODE_DIAMETER = 10.0;
    public static final double NODE_RADIUS = NODE_DIAMETER / 2.0;
    /// Minimum horizontal distance between the centers of two adjacent
    /// sibling subtrees at any level (Reingold & Tilford's "MINSEP").
    public static final double SIBLING_DISTANCE = NODE_DIAMETER + 5.0;
    /// Vertical distance between adjacent levels of the tree.
    public static final double LEVEL_DISTANCE = NODE_DIAMETER + 15.0;

    private final int identifier;
    private final List<RTNode> children = new ArrayList<>();
    private String name;
    private RTNode parent;
    private int depth;

    // ── Algorithmic state (Buchheim–Jünger–Leipert) ────────────────────────
    /// Preliminary x coordinate, relative to siblings, set during firstWalk.
    private double prelimX;
    /// Modifier applied to a node's whole subtree to obtain final coordinates.
    private double mod;
    /// Accumulated shift applied when spreading subtrees apart in executeShifts.
    private double shift;
    /// Accumulated change (delta applied per intermediate subtree) in executeShifts.
    private double change;
    /// Left/right contour pointer used when the natural sibling chain runs out
    /// (equivalent to the "threads" of the original TR paper).
    private RTNode thread;
    /// The ancestor used by apportion() when comparing subtrees that aren't
    /// siblings of one another (defaultAncestor bookkeeping).
    private RTNode ancestor = this;
    /// This node's index among its parent's children (0-based).
    private int number;

    // ── Final screen coordinates, filled in by the third walk ──────────────
    private double gridX;
    private double gridY;

    public RTNode(int identifier, String name, RTNode parent) {
        this.identifier = identifier;
        this.name = name;
        this.parent = parent;
    }

    public RTNode(int identifier) {
        this(identifier, "", null);
    }

    /// Utility method to add a child, recording its sibling index for the
    /// `number` field used by moveSubtree()'s spread calculation.
    public void addChild(RTNode child) {
        child.setParent(this);
        child.setNumber(this.children.size());
        this.children.add(child);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    /// The immediate left sibling of this node, or null if this is the
    /// first child (or has no parent).
    public RTNode leftSibling() {
        if (parent == null || number == 0) return null;
        return parent.getChildren().get(number - 1);
    }

    public RTNode firstChild() {
        return children.isEmpty() ? null : children.getFirst();
    }

    public RTNode lastChild() {
        return children.isEmpty() ? null : children.getLast();
    }
}
