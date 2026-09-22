package com.yashgamerx.flcd.rings.algorithm;

import com.yashgamerx.flcd.common.LayoutAlgorithm;
import com.yashgamerx.flcd.rings.model.RingNode;

/// Yash's Ring-based radial tree layout — a faithful port of the
/// standalone Ring app's `RingAlgorithm`. The math is untouched; the only
/// addition is a final recentering step (see [#recenter]) so it can be
/// invoked the same way as the other families in this app.
///
/// Two-pass approach:
///
/// 1. Bottom-up (`preCompute`): every node is assigned an angular slot
///    `theta` relative to its parent. A node's children split the full
///    circle into `childCount + 1` equal steps and are placed starting
///    one step past the direction back toward the parent — so the fan of
///    children opens away from the incoming edge instead of straddling
///    it. Each node's own required `radius` is then derived from its
///    single widest child and that angular step, so consecutive children
///    never collide:
///
///        requiredRadius = childRadius / tan(stepAngle / 2) + childRadius
///
///    (root is handled the same way but without reserving a slot for a
///    parent direction, since it has none).
///
/// 2. Top-down (`compute`): each node is translated from its parent's
///    already-known screen position by its own required radius, along
///    its own `theta`.
public class RingedCircularLayoutAlgorithm implements LayoutAlgorithm<RingNode> {

    public void calculate(RingNode root, double originX, double originY) {
        if (root == null) return;

        preComputeRoot(root);
        computeRoot(root);
        recenter(root, originX, originY);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Bottom-up pass: assign theta to every node and derive each node's
    // required radius from its widest child.
    // ─────────────────────────────────────────────────────────────────────

    private void preComputeRoot(RingNode rootNode) {
        var childrenCount = rootNode.getChildren().size();
        var stepAngle = step360AngleBasedOnChildren(childrenCount);
        var currentAngle = 0.0;
        for (var node : rootNode.getChildren()) {
            node.setTheta(currentAngle);
            preCompute(node);
            currentAngle += stepAngle;
        }

        var maxRadiusNode = rootNode.getChildren().stream().max(this::compareNodes);
        maxRadiusNode.ifPresent(node -> {
            var parentToChildRequiredRadius = parentToChildRequiredRadius(node.getRadius(), stepAngle);
            rootNode.setRadius(parentToChildRequiredRadius + node.getRadius());
        });
    }

    private void preCompute(RingNode node) {
        if (node.isLeaf()) {
            node.setRadius(RingNode.NODE_RADIUS);
            return;
        }

        var childrenCount = node.getChildren().size() + 1;
        var stepAngle = step360AngleBasedOnChildren(childrenCount);
        var currentAngle = node.getTheta() - Math.PI + stepAngle;
        for (var child : node.getChildren()) {
            child.setTheta(currentAngle);
            preCompute(child);
            currentAngle += stepAngle;
        }

        var maxRadiusNode = node.getChildren().stream().max(this::compareNodes);
        maxRadiusNode.ifPresent(child -> {
            var parentToChildRequiredRadius = parentToChildRequiredRadius(child.getRadius(), stepAngle);
            node.setRadius(parentToChildRequiredRadius + child.getRadius());
        });
    }

    private int compareNodes(RingNode a, RingNode b) {
        return Double.compare(a.getRadius(), b.getRadius());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Top-down pass: convert each node's (radius, theta) into absolute
    // screen coordinates relative to its parent's already-known position.
    // ─────────────────────────────────────────────────────────────────────

    private void computeRoot(RingNode rootNode) {
        rootNode.setLayoutX(rootNode.getRadius());
        rootNode.setLayoutY(rootNode.getRadius());

        for (var child : rootNode.getChildren()) {
            computeNode(child);
        }
    }

    private void computeNode(RingNode node) {
        var childrenCount = node.getParent().getChildren().size() + 1;
        var stepAngle = step360AngleBasedOnChildren(childrenCount);
        var parentToChildRequiredRadius = parentToChildRequiredRadius(node.getRadius(), stepAngle);
        node.translateFromParentBasedOnImplicitlyProvidedTheta(parentToChildRequiredRadius);

        for (var child : node.getChildren()) {
            computeNode(child);
        }
    }

    private double step360AngleBasedOnChildren(int children) {
        return Math.PI * 2 / children;
    }

    private double parentToChildRequiredRadius(double r, double theta) {
        return (r / Math.tan(theta / 2)) + r;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Recentering (the one addition beyond the original algorithm)
    // ─────────────────────────────────────────────────────────────────────

    /// The original algorithm always plants the root at
    /// `(rootRadius, rootRadius)`, since the standalone app sized its
    /// canvas around the tree. Every other family's view in this app draws
    /// on a large fixed virtual canvas and passes in a center point
    /// (`layoutAlgorithm.calculate(root, VIRTUAL_CANVAS_SIZE / 2, ...)`),
    /// so after the layout above finishes, shift every node by the same
    /// delta needed to move the root onto `(originX, originY)`. This is a
    /// pure rigid translation — it does not touch any angle or radius the
    /// algorithm computed, so the drawing is pixel-identical in shape to
    /// what the original produces, just repositioned on the canvas.
    private void recenter(RingNode root, double originX, double originY) {
        double deltaX = originX - root.getLayoutX();
        double deltaY = originY - root.getLayoutY();
        if (deltaX == 0 && deltaY == 0) return;
        shift(root, deltaX, deltaY);
    }

    private void shift(RingNode node, double deltaX, double deltaY) {
        node.setLayoutX(node.getLayoutX() + deltaX);
        node.setLayoutY(node.getLayoutY() + deltaY);
        for (var child : node.getChildren()) {
            shift(child, deltaX, deltaY);
        }
    }
}
