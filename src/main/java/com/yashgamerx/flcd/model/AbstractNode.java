package com.yashgamerx.flcd.model;

import com.yashgamerx.flcd.service.compute.Computable;
import com.yashgamerx.flcd.service.precompute.Precomputable;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class AbstractNode {
    // Constants for spacing
    public static final double WIDTH_SPACER = 5.0;
    public static final double HEIGHT_SPACER = 5.0;
    public static final double NODE_DIAMETER = 10.0;
    public static final double NODE_RADIUS = 5.0;

    private final int identifier;
    private String name; // Added name as per document (Case Study format)
    protected List<AbstractNode> children = new ArrayList<>();
    private AbstractNode parent;

    // Algorithmic State as per FLCD document
    protected double subtreeWidth;
    protected double subtreeHeight;
    protected double nodeOffset; // Scalar distance from parent
    protected double localRadianAngle; // Angle relative to parent
    protected double globalRadianAngle; // Per-child angular step for subtree
    protected double gridX; // Screen X coordinate
    protected double gridY; // Screen Y coordinate
    protected int depth;

    // Services
    protected Precomputable precomputable;
    protected Computable computable;

    protected NodeStatus status;

    // Constructor
    public AbstractNode(int identifier, String name, AbstractNode parent) {
        this.identifier = identifier;
        this.name = name;
        this.parent = parent;
        this.status = NodeStatus.NORMAL;
    }

    public AbstractNode(int identifier) {
        this(identifier, "", null);
    }

    // Abstract methods for FLCD phases
    public abstract void precompute();
    public abstract void compute();
    public abstract void readjust();
    public abstract void rootify();


    /// Utility method to add a child
    public void addChild(AbstractNode child) {
        this.children.add(child);
        child.setParent(this);
    }

    public void incrementDepth() {
        if (parent == null) this.depth = 0;
        else this.depth = parent.getDepth() + 1;
    }
}
