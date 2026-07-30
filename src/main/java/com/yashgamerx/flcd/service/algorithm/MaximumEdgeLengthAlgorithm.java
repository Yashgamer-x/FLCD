package com.yashgamerx.flcd.service.algorithm;

import com.yashgamerx.flcd.model.MaximumEdgeLengthNode;

import java.util.HashMap;
import java.util.Map;

/// Standalone layout algorithm for [MaximumEdgeLengthNode] trees.
///
/// Intentionally does not implement [TreeLayoutAlgorithm] — that interface
/// is FLCD's contract over [com.yashgamerx.flcd.model.FLCDNode], and this
/// algorithm operates on a different node type with a different
/// placement strategy entirely.
public class MaximumEdgeLengthAlgorithm {

    private static final double RADIUS_STEP = 500.0;
    private static final double TWO_PI = 2.0 * Math.PI;

    /// Minimum straight-line arc gap we require between two adjacent
    /// siblings' centers at whatever radius they end up on. This is what
    /// actually prevents circle overlap — plug in NODE_DIAMETER (+ a
    /// margin) from the shared node-size constant if one exists elsewhere
    /// in the codebase, so this stays in sync with how big nodes are
    /// actually drawn.
    private static final double MIN_ARC_SEPARATION = 10.0;

    /// PLACEHOLDER: a recursive radial layout so the view has something
    /// reasonable to render before the real maximum-edge-length placement
    /// math is written. Replace this body once that algorithm exists.
    ///
    /// Shape: every node at depth `d` sits on a circle of radius
    /// `radius[d]` centered on the root. The root's children split the
    /// full circle (0..2π) evenly between them; every other node's
    /// children split *that node's own inherited angular slice* evenly
    /// again — so each subtree keeps growing outward as an arc within the
    /// wedge its parent was given, rather than wrapping around into a
    /// sibling's territory. Planarity holds regardless of how lopsided the
    /// tree is, since a child's whole sub-wedge always nests strictly
    /// inside its parent's wedge.
    ///
    /// Unlike a fixed `depth * RADIUS_STEP`, the radius per depth is
    /// computed up front from the *tightest* wedge that occurs anywhere at
    /// that depth (a bushy branch several levels down can shrink a node's
    /// step angle far more than a sparse one), so that even the closest
    /// pair of siblings anywhere in the tree ends up at least
    /// [#MIN_ARC_SEPARATION] apart. That's what actually stops circles
    /// from overlapping — a plain per-depth radius increment doesn't
    /// account for how narrow the angle gets in a heavily branching
    /// subtree.
    public void calculate(MaximumEdgeLengthNode root, double originX, double originY) {
        if (root == null) return;

        root.setDepth(0);
        root.setGridX(originX);
        root.setGridY(originY);

        var minStepAngleByDepth = new HashMap<Integer, Double>();
        collectMinStepAngles(root, 0.0, TWO_PI, 0, minStepAngleByDepth);

        var radiusByDepth = computeRadii(minStepAngleByDepth);

        placeChildrenInArc(root, 0.0, TWO_PI, originX, originY, radiusByDepth);
    }

    /// First pass: walks the same wedge-subdivision logic as the real
    /// placement pass, but only to record — for every depth — the
    /// smallest step angle any node's children get squeezed into
    /// anywhere in the tree at that depth.
    private void collectMinStepAngles(MaximumEdgeLengthNode node, double startAngle, double endAngle,
                                      int depth, Map<Integer, Double> minStepAngleByDepth) {
        var children = node.getChildren();
        if (children.isEmpty()) return;

        double stepAngle = (endAngle - startAngle) / children.size();
        int childDepth = depth + 1;
        minStepAngleByDepth.merge(childDepth, stepAngle, Math::min);

        double angleCursor = startAngle;
        for (var child : children) {
            double childStart = angleCursor;
            double childEnd = angleCursor + stepAngle;
            collectMinStepAngles(child, childStart, childEnd, childDepth, minStepAngleByDepth);
            angleCursor = childEnd;
        }
    }

    /// Derives the radius for every depth level from the worst-case (most
    /// cramped) step angle recorded at that depth: `radius[d]` is the
    /// larger of the plain `radius[d - 1] + RADIUS_STEP` growth and
    /// whatever radius is needed for `MIN_ARC_SEPARATION` to hold at the
    /// narrowest angle seen at depth `d`. Depths with no recorded step
    /// angle (i.e. nothing branches that deep) just fall back to the
    /// plain increment.
    private Map<Integer, Double> computeRadii(Map<Integer, Double> minStepAngleByDepth) {
        int maxDepth = 0;
        for (var depth : minStepAngleByDepth.keySet()) {
            maxDepth = Math.max(maxDepth, depth);
        }

        var radiusByDepth = new HashMap<Integer, Double>();
        radiusByDepth.put(0, 0.0);

        for (int depth = 1; depth <= maxDepth; depth++) {
            double naiveRadius = radiusByDepth.get(depth - 1) + RADIUS_STEP;
            Double minStepAngle = minStepAngleByDepth.get(depth);
            double requiredRadius = (minStepAngle != null)
                    ? MIN_ARC_SEPARATION / minStepAngle
                    : naiveRadius;

            radiusByDepth.put(depth, Math.max(naiveRadius, requiredRadius));
        }

        return radiusByDepth;
    }

    /// Splits `[startAngle, endAngle)` — the arc `node` was itself given by
    /// its own parent (the full circle, for root) — into `children.size()`
    /// equal steps, places each child at the midpoint of its own step on
    /// the shared circle for `node.getDepth() + 1` (from `radiusByDepth`),
    /// then recurses so that child's children continue subdividing *that*
    /// step the same way.
    private void placeChildrenInArc(MaximumEdgeLengthNode node, double startAngle, double endAngle,
                                    double originX, double originY,
                                    Map<Integer, Double> radiusByDepth) {
        var children = node.getChildren();
        if (children.isEmpty()) return;

        double stepAngle = (endAngle - startAngle) / children.size();
        double radius = radiusByDepth.get(node.getDepth() + 1);
        double angleCursor = startAngle;

        for (var child : children) {
            double childStart = angleCursor;
            double childEnd = angleCursor + stepAngle;
            double childAngle = (childStart + childEnd) / 2.0;

            child.setDepth(node.getDepth() + 1);
            child.setGridX(originX + radius * Math.cos(childAngle));
            child.setGridY(originY - radius * Math.sin(childAngle));

            placeChildrenInArc(child, childStart, childEnd, originX, originY, radiusByDepth);

            angleCursor = childEnd;
        }
    }
}