package com.yashgamerx.flcd.rings.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/// A single node in Yash's Ring-based radial layout ("Rings").
///
/// Pure data holder, mirroring `FLCDNode`/`RTNode` elsewhere in this
/// codebase — all layout behavior lives in
/// [com.yashgamerx.flcd.rings.algorithm.RingedCircularLayoutAlgorithm].
/// Field-for-field port of the standalone Ring app's `RingTreeNode`.
@Getter
@Setter
public class RingNode {

    public static final double NODE_RADIUS = 5.0;
    public static final double NODE_DIAMETER = NODE_RADIUS * 2;

    private final int identifier;
    private final List<RingNode> children = new ArrayList<>();
    private RingNode parent;
    /// Angle (radians) this node sits at, relative to its parent's center.
    private double theta;

    /// Bottom-up "required radius": the distance this node must be placed
    /// from its widest child so that none of the children's circles
    /// collide, given the angular step between siblings. For leaves this
    /// is just NODE_RADIUS.
    private double radius;

    /// Final screen coordinates, filled in by the top-down placement pass.
    private double layoutX;
    private double layoutY;

    public RingNode(int identifier) {
        this.identifier = identifier;
    }

    public void addChild(RingNode child) {
        child.setParent(this);
        this.children.add(child);
    }

    /// Places this node relative to its parent's already-computed center,
    /// at distance `r` along this node's own `theta`. Same method name as
    /// the original `RingTreeNode.translateFromParentBasedOnImplicitlyProvidedTheta`.
    public void translateFromParentBasedOnImplicitlyProvidedTheta(double r) {
        this.layoutX = parent.getLayoutX() + r * Math.cos(theta);
        this.layoutY = parent.getLayoutY() + r * Math.sin(theta);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }
}
