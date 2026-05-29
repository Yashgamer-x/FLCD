package com.yashgamerx.flcd.model;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class AbstractNode {
    // Constants for spacing
    protected static final double WIDTH_SPACER = 5.0;
    protected static final double HEIGHT_SPACER = 5.0;

    private final int identifier;
    private String name; // Added name as per document (Case Study format)
    private AbstractNode parentNode;
    private final List<AbstractNode> childrenNodes = new ArrayList<>();

    // Algorithmic State as per FLCD document
    protected double subtreeWidth;
    protected double subtreeHeight;
    protected double nodeOffset; // Scalar distance from parent
    protected double localRadianAngle; // Angle relative to parent
    protected double globalRadianAngle; // Per-child angular step for subtree
    protected double gridX; // Screen X coordinate
    protected double gridY; // Screen Y coordinate

    // Constructor
    public AbstractNode(int identifier, String name, AbstractNode parentNode) {
        this.identifier = identifier;
        this.name = name;
        this.parentNode = parentNode;
    }

    public AbstractNode(int identifier) {
        this(identifier, "", null);
    }

    // Abstract methods for FLCD phases
    public abstract void preCompute();
    public abstract void compute();
    public abstract void readjust();
    public abstract void rootify();

    /// Helper method for type conversion.
    ///
    /// ---
    /// This method will be implemented in GeneralTree or a utility class, but the concept is that a node can be
    /// converted to a different type while preserving its identity.
    /// ---
    ///  For now, we'll just have a placeholder.
    protected <T extends AbstractNode> T convertTo(Class<T> targetType) {
        // This will involve creating a new instance of targetType, copying relevant data,
        // and updating the parent's children list and the global map.
        // The actual implementation will be in GeneralTree or a dedicated factory.
        throw new UnsupportedOperationException("Node conversion not yet implemented.");
    }

    /// Utility method to add a child
    public void addChild(AbstractNode child) {
        this.childrenNodes.add(child);
        child.setParentNode(this);
    }

    /// Utility method to calculate radiusToFitChildren (Section 5.2)
    protected double radiusToFitChildren(double W, double H, double alpha) {
        if (alpha == 0) return 0; // Avoid division by zero for degenerate cases
        return W / Math.sin(alpha) + H;
    }
}
