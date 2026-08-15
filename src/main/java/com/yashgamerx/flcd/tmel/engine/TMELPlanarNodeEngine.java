package com.yashgamerx.flcd.tmel.engine;

import com.yashgamerx.flcd.common.NodeRole;
import com.yashgamerx.flcd.common.angular.Angle180Calculator;
import com.yashgamerx.flcd.common.angular.Angle360Calculator;
import com.yashgamerx.flcd.common.angular.AngularCalculator;
import com.yashgamerx.flcd.tmel.dimension.TMELHorizontalSubtreeDimensionCalculator;
import com.yashgamerx.flcd.tmel.dimension.TMELVerticalSubtreeDimensionCalculator;
import com.yashgamerx.flcd.tmel.model.TMELNode;
import com.yashgamerx.flcd.tmel.model.TMELSide;
import lombok.extern.java.Log;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.yashgamerx.flcd.tmel.model.TMELNode.*;

/// Single behavioral engine for the FLCD layout algorithm.
///
/// This replaces the previous `Precomputable`/`Computable` Strategy
/// hierarchy and its matching Injector classes (~30 files). A node no
/// longer carries a per-instance behavioral object for each phase —
/// instead it carries plain state ([NodeRole], [TMELSide]),
/// and this engine dispatches on that state. Since `NodeRole` is assigned
/// once during precompute and read again unchanged during compute, one
/// role enum serves both phases (there is no separate "compute role").
///
/// A manually-rootified node (`status == ROOTIFIED`) is checked first,
/// ahead of the role switch, since rootification is an override that can
/// apply at any structural position rather than a position of its own.
@Log
public class TMELPlanarNodeEngine {

    private final AngularCalculator angle360Calculator = new Angle360Calculator();
    private final AngularCalculator angle180Calculator = new Angle180Calculator();
    private final TMELVerticalSubtreeDimensionCalculator verticalDimensionCalculator = new TMELVerticalSubtreeDimensionCalculator();
    private final TMELHorizontalSubtreeDimensionCalculator horizontalDimensionCalculator = new TMELHorizontalSubtreeDimensionCalculator();

    // ─────────────────────────────────────────────────────────────────
    // Public entry points
    // ─────────────────────────────────────────────────────────────────

    public void precompute(TMELNode node) {
        switch (node.getRole()) {
            case ROOT -> precomputeRoot(node);
            case FIRST_CHILD -> precomputeFirstChild(node);
            case SECOND_CHILD -> precomputeSecondChild(node);
            case HEIGHT_CHILD -> precomputeHeightChild(node);
            case WIDTH_CHILD -> precomputeWidthChild(node);
        }
    }

    public void compute(TMELNode node) {
        switch (node.getRole()) {
            case ROOT -> computeRoot(node);
            case FIRST_CHILD -> computeFirstChild(node);
            case SECOND_CHILD -> computeSecondChild(node);
            case HEIGHT_CHILD -> computeHeightChild(node);
            case WIDTH_CHILD -> computeWidthChild(node);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Precompute phase
    // ─────────────────────────────────────────────────────────────────

    private void precomputeRoot(TMELNode root) {
        root.getChildren().forEach(this::assignFirstChildRoleAndPrecompute);
        root.incrementDepth();
        calculateCircularSubtree(root, angle360Calculator);
    }

    private void assignFirstChildRoleAndPrecompute(TMELNode firstChild) {
        firstChild.setRole(NodeRole.FIRST_CHILD);
        precompute(firstChild);
    }

    /// Shared by ROOT and ROOTIFIED: children are spread circularly (ROOT)
    /// or semi-circularly (ROOTIFIED, `halfHeight = true`) around the node.
    private void calculateCircularSubtree(TMELNode node, AngularCalculator angularCalculator) {
        var firstChildren = node.getChildren();
        double angularStep = angularCalculator.calculate(firstChildren.size());
        double maxOffset = 0;

        for (var firstChild : firstChildren) {
            double clearanceHeight = firstChild.getSubtreeWidth() / (2.0 * Math.tan(angularStep / 2.0));
            double safeScalarOffset = clearanceHeight + firstChild.getSubtreeHeight();
            maxOffset = Math.max(maxOffset, safeScalarOffset);
        }

        node.setSubtreeWidth(maxOffset + maxOffset + NODE_DIAMETER);
        node.setSubtreeHeight(maxOffset + maxOffset + NODE_DIAMETER);
    }

    private void precomputeFirstChild(TMELNode firstChild) {
        if (firstChild.getChildren().isEmpty()) {
            initializeLeafDimensions(firstChild);
            return;
        }

        firstChild.getChildren().forEach(child -> {
            child.setRole(NodeRole.SECOND_CHILD);
            precompute(child);
        });
        firstChild.incrementDepth();

        calculateBalancedSubtreeDimensions(firstChild);
    }

    private double calculateTopRadius(double width, int children) {
        //  Why children+1 ??
        // We assume that this calculation is happening in an assumption that another node has a potential to be
        // on the top side.
        var stepAngle = angle180Calculator.calculate(children + 1);
        return width / (2.0 * Math.tan(stepAngle / 2.0));
    }

    /// ![Fully Occupied Width Calculation](calculate_fully_occupied_width.png)
    private double calculateActualOccupiedWidth(double width, double height, double angle) {
        double extraWidth = height / Math.tan(angle);
        return width + extraWidth;
    }

    /// Sorts children by descending area, greedily balances them across
    /// the left/right wings by accumulated area, and assigns each its
    /// [TMELSide] — replacing the old Left/RightSecondChildComputeInjector
    /// pair, which did the same assignment via object substitution.
    private void calculateBalancedSubtreeDimensions(TMELNode firstChild) {
        firstChild.getChildren().sort(Comparator.comparingDouble(this::calculateArea).reversed());

        var childrenCount = firstChild.getChildren().size();
        var stepAngle = angle360Calculator.calculate(childrenCount);

        var metrics = new WingMetrics();


        firstChild.getChildren()
                .forEach(secondChild -> {
                    double nodeArea = calculateArea(secondChild);

                    double topMaximumWidth = Math.max(metrics.topMaximumWidth, secondChild.getSubtreeWidth());
                    double calculatedTopRadius = calculateTopRadius(topMaximumWidth, metrics.topAccumulatedChildren);
                    double topRadius = Math.max(metrics.topMaximumRadius, calculatedTopRadius);

                    double leftOccupiedWidth = calculateActualOccupiedWidth(metrics.leftWidth, metrics.maxLeftHeight, stepAngle);
                    double rightOccupiedWidth = calculateActualOccupiedWidth(metrics.rightWidth, metrics.maxRightHeight, stepAngle);

                    boolean leftFits = leftOccupiedWidth <= topRadius;
                    boolean rightFits = rightOccupiedWidth <= topRadius;

                    if (leftFits && (!rightFits || leftOccupiedWidth <= rightOccupiedWidth)) {
                        secondChild.setSide(TMELSide.LEFT);
                        metrics.leftAccumulatedArea += nodeArea;
                        metrics.leftWidth += secondChild.getSubtreeWidth() + WIDTH_SPACER;
                        metrics.maxLeftHeight = Math.max(metrics.maxLeftHeight, secondChild.getSubtreeHeight());
                    } else if (rightFits) {
                        secondChild.setSide(TMELSide.RIGHT);
                        metrics.rightAccumulatedArea += nodeArea;
                        metrics.rightWidth += secondChild.getSubtreeWidth() + WIDTH_SPACER;
                        metrics.maxRightHeight = Math.max(metrics.maxRightHeight, secondChild.getSubtreeHeight());
                    } else {
                        secondChild.setSide(TMELSide.TOP);
                        metrics.topAccumulatedChildren++;
                        metrics.topMaximumWidth = Math.max(metrics.topMaximumWidth, topMaximumWidth);
                        metrics.topMaximumRadius = Math.max(metrics.topMaximumRadius, topRadius);
                    }
                    metrics.maxDepth = Math.max(metrics.maxDepth, secondChild.getDepth());
                });

        double maxSideWidth = Math.max(metrics.leftWidth, metrics.rightWidth);
        double totalWidth = (maxSideWidth * 2) + NODE_DIAMETER;

        firstChild.setSubtreeWidth(Math.max(NODE_DIAMETER, totalWidth));
        firstChild.setSubtreeHeight(NODE_DIAMETER + HEIGHT_SPACER + Math.max(metrics.maxLeftHeight, metrics.maxRightHeight));
        firstChild.setDepth(metrics.maxDepth + 1);
    }

    private void precomputeSecondChild(TMELNode secondChild) {
        if (secondChild.getChildren().isEmpty()) {
            initializeLeafDimensions(secondChild);
            return;
        }

        secondChild.getChildren().forEach(child -> {
            child.setRole(NodeRole.HEIGHT_CHILD);
            precompute(child);
        });
        secondChild.incrementDepth();

        verticalDimensionCalculator.calculate(secondChild);
    }

    private void precomputeHeightChild(TMELNode heightNode) {
        if (heightNode.getChildren().isEmpty()) {
            initializeLeafDimensions(heightNode);
            return;
        }

        heightNode.getChildren().forEach(child -> {
            child.setRole(NodeRole.WIDTH_CHILD);
            precompute(child);
        });
        heightNode.incrementDepth();

        horizontalDimensionCalculator.calculate(heightNode);
    }

    private void precomputeWidthChild(TMELNode widthNode) {
        if (widthNode.getChildren().isEmpty()) {
            initializeLeafDimensions(widthNode);
            return;
        }

        widthNode.getChildren().forEach(child -> {
            child.setRole(NodeRole.HEIGHT_CHILD);
            precompute(child);
        });
        widthNode.incrementDepth();

        verticalDimensionCalculator.calculate(widthNode);
    }

    private void initializeLeafDimensions(TMELNode node) {
        node.setSubtreeWidth(NODE_DIAMETER);
        node.setSubtreeHeight(NODE_DIAMETER);
        node.setDepth(0);
    }

    private double calculateArea(TMELNode node) {
        return node.getSubtreeHeight() * node.getSubtreeWidth();
    }

    private void computeRoot(TMELNode rootNode) {
        if (rootNode.getChildren().isEmpty()) return;

        var children = rootNode.getChildren();
        int totalChildren = children.size();
        double angularStep = angle360Calculator.calculate(totalChildren);

        for (int i = 0; i < totalChildren; i++) {
            var firstChild = children.get(i);
            double currentAngle = i * angularStep;

            double clearanceHeight = firstChild.getSubtreeWidth() / (2.0 * Math.tan(angularStep / 2.0));
            double safeScalarOffset = clearanceHeight + firstChild.getSubtreeHeight();
            firstChild.setNodeOffset(safeScalarOffset);
            firstChild.setLocalRadianAngle(currentAngle);

            double nodeCenter = safeScalarOffset - NODE_RADIUS;
            firstChild.setGridX(rootNode.getGridX() + (nodeCenter * Math.cos(currentAngle)));
            firstChild.setGridY(rootNode.getGridY() - (nodeCenter * Math.sin(currentAngle)));

            compute(firstChild);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Compute phase
    // ─────────────────────────────────────────────────────────────────

    private void computeFirstChild(TMELNode firstChild) {
        if (firstChild.getChildren().isEmpty()) return;

        double parentAngle = firstChild.getLocalRadianAngle();
        double rightAngle = parentAngle - (Math.PI / 2.0);
        double leftAngle = parentAngle + (Math.PI / 2.0);
        double forwardStepLength = NODE_RADIUS + HEIGHT_SPACER + NODE_RADIUS;

        double anchorX = firstChild.getGridX() - (forwardStepLength * Math.cos(parentAngle));
        double anchorY = firstChild.getGridY() + (forwardStepLength * Math.sin(parentAngle));

        // [0] accumulatedRightDistance, [1] accumulatedLeftDistance
        double[] accumulatedDistances = {0.0, 0.0};

        var secondChildren = firstChild.getChildren();

        Map<TMELSide, List<TMELNode>> childrenBySide =
                secondChildren.stream()
                        .collect(Collectors.groupingBy(TMELNode::getSide));

        List<TMELNode> left = childrenBySide.getOrDefault(TMELSide.LEFT, List.of());
        List<TMELNode> right = childrenBySide.getOrDefault(TMELSide.RIGHT, List.of());
        List<TMELNode> top = childrenBySide.getOrDefault(TMELSide.TOP, List.of());
        List<TMELNode> none = childrenBySide.getOrDefault(TMELSide.NONE, List.of());

        if (!none.isEmpty()) {
            throw new IllegalStateException("None side should not be present");
        }

        right.forEach(child -> {
            final int slot = 0;
            accumulatedDistances[slot] = projectSecondChildAlongVector(
                    child, anchorX, anchorY, rightAngle, accumulatedDistances[slot]);
        });

        left.forEach(child -> {
            final int slot = 1;
            accumulatedDistances[slot] = projectSecondChildAlongVector(
                    child, anchorX, anchorY, leftAngle, accumulatedDistances[slot]);
        });

        var root = firstChild.getParent();
        var stepAngle = angle360Calculator.calculate(root.getChildren().size());
        var currentAngle = stepAngle / 2;
        top.forEach(child -> {
            System.out.println("Top side is not supported for id: " + child.getIdentifier());

            projectSecondTopChildAlongVector(child, firstChild.getGridX(), firstChild.getGridY(), currentAngle, top.size());
        });
    }

    private void projectSecondTopChildAlongVector(TMELNode child, double anchorX, double anchorY,
                                                  double currentAngle, int size) {
        double radius = calculateTopRadius(child.getSubtreeWidth(), size);
        double centerPoint = radius - NODE_RADIUS;
        // TODO:
    }

    private double projectSecondChildAlongVector(TMELNode child, double anchorX, double anchorY,
                                                 double baselineAngle, double currentDistance) {
        double newDistance = currentDistance + WIDTH_SPACER + child.getSubtreeWidth();
        double centerPoint = newDistance - NODE_RADIUS;

        child.setGridX(anchorX + (centerPoint * Math.cos(baselineAngle)));
        child.setGridY(anchorY - (centerPoint * Math.sin(baselineAngle)));
        child.setLocalRadianAngle(baselineAngle);

        compute(child);
        return newDistance;
    }

    /// SECOND_CHILD level: children become HEIGHT_CHILD, spaced using
    /// each child's own subtree HEIGHT.
    private void computeSecondChild(TMELNode secondChild) {
        if (secondChild.getChildren().isEmpty()) return;

        TMELSide side = secondChild.getSide();
        double turnSign = side == TMELSide.LEFT ? 1.0 : -1.0;
        double myAngle = secondChild.getLocalRadianAngle();
        double childAngleTrajectory = myAngle + turnSign * (Math.PI / 2.0);

        double forwardStepLength = NODE_RADIUS + WIDTH_SPACER + NODE_RADIUS;
        double anchorX = secondChild.getGridX() - (forwardStepLength * Math.cos(myAngle));
        double anchorY = secondChild.getGridY() + (forwardStepLength * Math.sin(myAngle));

        double[] offset = {NODE_RADIUS + HEIGHT_SPACER};

        secondChild.getChildren()
                .forEach(heightChild -> {
                    heightChild.setRole(NodeRole.HEIGHT_CHILD);
                    heightChild.setSide(side);
                    offset[0] = projectAlongVector(heightChild, anchorX, anchorY, childAngleTrajectory, offset[0], true);
                });
    }

    /// HEIGHT_CHILD level: children become WIDTH_CHILD, spaced using
    /// each child's own subtree WIDTH.
    private void computeHeightChild(TMELNode heightChild) {
        if (heightChild.getChildren().isEmpty()) return;

        TMELSide side = heightChild.getSide();
        double turnSign = side == TMELSide.LEFT ? 1.0 : -1.0;
        double myAngle = heightChild.getLocalRadianAngle();
        double childAngleTrajectory = myAngle + turnSign * (Math.PI / 2.0);

        double forwardStepLength = NODE_RADIUS + HEIGHT_SPACER + NODE_RADIUS;
        double anchorX = heightChild.getGridX() + (forwardStepLength * Math.cos(myAngle));
        double anchorY = heightChild.getGridY() - (forwardStepLength * Math.sin(myAngle));

        double[] offset = {NODE_RADIUS + WIDTH_SPACER};

        heightChild.getChildren()
                .forEach(widthChild -> {
                    widthChild.setRole(NodeRole.WIDTH_CHILD);
                    widthChild.setSide(side);
                    offset[0] = projectAlongVector(widthChild, anchorX, anchorY, childAngleTrajectory, offset[0], false);
                });
    }

    /// WIDTH_CHILD level: children become HEIGHT_CHILD again, spaced
    /// using each child's own subtree HEIGHT.
    ///
    /// NOTE: the rootified branch here is preserved verbatim from the
    /// original code, including two quirks that don't mirror the other
    /// two levels: (1) both LEFT and RIGHT use the same "+90°" offset
    /// (no sign flip), and (2) the return step uses WIDTH_SPACER rather
    /// than this level's own HEIGHT_SPACER convention. These look like
    /// pre-existing inconsistencies rather than intentional asymmetries —
    /// worth a second look, but left as-is here since this pass is a
    /// structural refactor, not an algorithm change.
    private void computeWidthChild(TMELNode widthNode) {
        if (widthNode.getChildren().isEmpty()) return;

        TMELSide side = widthNode.getSide();
        double turnSign = side == TMELSide.LEFT ? -1.0 : 1.0;
        double myAngle = widthNode.getLocalRadianAngle();
        double childAngleTrajectory = myAngle + turnSign * (Math.PI / 2.0);

        double forwardStepLength = NODE_RADIUS + WIDTH_SPACER + NODE_RADIUS;
        double anchorX = widthNode.getGridX() + (forwardStepLength * Math.cos(myAngle));
        double anchorY = widthNode.getGridY() - (forwardStepLength * Math.sin(myAngle));

        double[] offset = {NODE_RADIUS + HEIGHT_SPACER};

        widthNode.getChildren()
                .forEach(heightChild -> {
                    heightChild.setRole(NodeRole.HEIGHT_CHILD);
                    heightChild.setSide(side);

                    offset[0] = projectAlongVector(heightChild, anchorX, anchorY, childAngleTrajectory, offset[0], true);
                });
    }

    /// Shared non-rootified projection step for SECOND_CHILD/HEIGHT_CHILD/
    /// WIDTH_CHILD levels. `useHeightDimension` picks which of the
    /// child's own subtree dimensions (and matching spacer) governs
    /// stepping to the next sibling.
    private double projectAlongVector(TMELNode child, double anchorX, double anchorY,
                                      double angle, double currentOffset, boolean useHeightDimension) {
        double centerPoint = currentOffset + NODE_RADIUS;

        child.setGridX(anchorX + (centerPoint * Math.cos(angle)));
        child.setGridY(anchorY - (centerPoint * Math.sin(angle)));
        child.setLocalRadianAngle(angle);

        compute(child);

        double childSpan = useHeightDimension ? child.getSubtreeHeight() : child.getSubtreeWidth();
        double spacer = useHeightDimension ? HEIGHT_SPACER : WIDTH_SPACER;
        return currentOffset + childSpan + spacer;
    }

    /// Mutable per-wing accumulator for [#calculateBalancedSubtreeDimensions].
    private static final class WingMetrics {
        double leftAccumulatedArea;
        double rightAccumulatedArea;
        double leftWidth;
        double rightWidth;
        double maxLeftHeight;
        double maxRightHeight;
        int maxDepth;
        int topAccumulatedChildren;
        double topMaximumWidth;
        double topMaximumRadius;
    }
}
