package com.yashgamerx.flcd.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/// Node type for the Maximum Edge Length layout algorithm.
///
/// This is deliberately **not** [FLCDNode]. The two algorithms solve
/// different placement problems (FLCD's planar-radial-grid vs. maximum
/// edge length's own approach), and sharing a node type would mean either
/// polluting `FLCDNode` with fields one algorithm doesn't need, or forcing
/// both algorithms through a lowest-common-denominator contract. Keeping
/// them separate lets each node type carry exactly what its own algorithm
/// needs.
@Getter
@Setter
public class CircleMaximumEdgeLengthNode {
    public static final double NODE_DIAMETER = 10.0;
    public static final double NODE_RADIUS = NODE_DIAMETER / 2.0;

    private final int identifier;
    private String name;
    private List<CircleMaximumEdgeLengthNode> children = new ArrayList<>();
    private CircleMaximumEdgeLengthNode parent;

    private double gridX;
    private double gridY;
    private int depth;

    public CircleMaximumEdgeLengthNode(int identifier, String name, CircleMaximumEdgeLengthNode parent) {
        this.identifier = identifier;
        this.name = name;
        this.parent = parent;
    }

    public CircleMaximumEdgeLengthNode(int identifier) {
        this(identifier, "", null);
    }

    public void addChild(CircleMaximumEdgeLengthNode child) {
        this.children.add(child);
        child.setParent(this);
    }
}
