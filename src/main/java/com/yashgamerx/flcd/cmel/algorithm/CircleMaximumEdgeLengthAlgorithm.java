package com.yashgamerx.flcd.cmel.algorithm;

import com.yashgamerx.flcd.cmel.model.CircleMaximumEdgeLengthNode;

import java.util.ArrayList;
import java.util.List;

/// Standalone layout algorithm for [CircleMaximumEdgeLengthNode] trees.
///
/// Intentionally does not implement [TreeLayoutAlgorithm] — that interface
/// is FLCD's contract over [com.yashgamerx.flcd.flcd.model.FLCDNode], and this
/// algorithm operates on a different node type with a different
/// placement strategy entirely.
public class CircleMaximumEdgeLengthAlgorithm {

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
    /// sibling pair anywhere in the tree ends up at least
    /// [#MIN_ARC_SEPARATION] apart.
    ///
    /// NOTE (accepted approximation): this only guards the angular gap
    /// *within* a node's own children (siblings sharing a parent). Two
    /// nodes that sit at a wedge boundary — the last child of one wedge
    /// and the first child of the neighboring wedge, under different
    /// parents — are not directly checked against each other. Because
    /// wedges are contiguous and non-overlapping and each is independently
    /// kept tight via MIN_ARC_SEPARATION, boundary nodes are pushed toward
    /// the edges of their own already-tight wedges in practice, but this
    /// is not a proven bound. Revisit if the real max-edge-length
    /// algorithm needs a hard guarantee here.
    public void calculate(CircleMaximumEdgeLengthNode root, double originX, double originY) {
        if (root == null) return;

        root.setDepth(0);
        root.setGridX(originX);
        root.setGridY(originY);

        var minStepAngleByDepth = new ArrayList<Double>();
        minStepAngleByDepth.add(null); // depth 0 has no incoming step angle
        collectMinStepAngles(root, 0.0, TWO_PI, 0, minStepAngleByDepth);

        var radiusByDepth = computeRadii(minStepAngleByDepth);

        placeChildrenInArc(root, 0.0, TWO_PI, originX, originY, radiusByDepth);
    }

    /// First pass: walks the same wedge-subdivision logic as the real
    /// placement pass, but only to record — for every depth — the
    /// smallest step angle any node's children get squeezed into
    /// anywhere in the tree at that depth. Backed by a dense
    /// depth-indexed list rather than a hash map since depths are
    /// contiguous integers starting at 0.
    private void collectMinStepAngles(CircleMaximumEdgeLengthNode node, double startAngle, double endAngle,
                                      int depth, List<Double> minStepAngleByDepth) {
        var children = node.getChildren();
        if (children.isEmpty()) return;

        double stepAngle = (endAngle - startAngle) / children.size();
        int childDepth = depth + 1;

        while (minStepAngleByDepth.size() <= childDepth) {
            minStepAngleByDepth.add(null);
        }
        Double current = minStepAngleByDepth.get(childDepth);
        minStepAngleByDepth.set(childDepth, current == null ? stepAngle : Math.min(current, stepAngle));

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
    private double[] computeRadii(List<Double> minStepAngleByDepth) {
        int maxDepth = minStepAngleByDepth.size() - 1;
        var radiusByDepth = new double[maxDepth + 1];
        radiusByDepth[0] = 0.0;

        for (int depth = 1; depth <= maxDepth; depth++) {
            double naiveRadius = radiusByDepth[depth - 1] + RADIUS_STEP;
            Double minStepAngle = minStepAngleByDepth.get(depth);
            double requiredRadius = (minStepAngle != null)
                    ? MIN_ARC_SEPARATION / minStepAngle
                    : naiveRadius;

            radiusByDepth[depth] = Math.max(naiveRadius, requiredRadius);
        }

        return radiusByDepth;
    }

    /// Splits `[startAngle, endAngle)` — the arc `node` was itself given by
    /// its own parent (the full circle, for root) — into `children.size()`
    /// equal steps, places each child at the midpoint of its own step on
    /// the shared circle for `node.getDepth() + 1` (from `radiusByDepth`),
    /// then recurses so that child's children continue subdividing *that*
    /// step the same way.
    private void placeChildrenInArc(CircleMaximumEdgeLengthNode node, double startAngle, double endAngle,
                                    double originX, double originY,
                                    double[] radiusByDepth) {
        var children = node.getChildren();
        if (children.isEmpty()) return;

        double stepAngle = (endAngle - startAngle) / children.size();
        double radius = radiusByDepth[node.getDepth() + 1];
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