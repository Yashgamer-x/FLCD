package com.yashgamerx.flcd.service.engine;

import com.yashgamerx.flcd.model.FLCDNode;
import com.yashgamerx.flcd.model.FLCDSide;
import com.yashgamerx.flcd.model.NodeRole;
import com.yashgamerx.flcd.model.NodeStatus;
import com.yashgamerx.flcd.service.angular.Angle180Calculator;
import com.yashgamerx.flcd.service.angular.Angle360Calculator;
import com.yashgamerx.flcd.service.angular.AngularCalculator;
import com.yashgamerx.flcd.service.dimension.HorizontalSubtreeDimensionCalculator;
import com.yashgamerx.flcd.service.dimension.NodeDimensionCalculator;
import com.yashgamerx.flcd.service.dimension.VerticalSubtreeDimensionCalculator;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.yashgamerx.flcd.model.FLCDNode.*;

/// Single behavioral engine for the FLCD layout algorithm.
///
/// This replaces the previous `Precomputable`/`Computable` Strategy
/// hierarchy and its matching Injector classes (~30 files). A node no
/// longer carries a per-instance behavioral object for each phase —
/// instead it carries plain state ([NodeRole], [FLCDSide], [NodeStatus]),
/// and this engine dispatches on that state. Since `NodeRole` is assigned
/// once during precompute and read again unchanged during compute, one
/// role enum serves both phases (there is no separate "compute role").
///
/// A manually-rootified node (`status == ROOTIFIED`) is checked first,
/// ahead of the role switch, since rootification is an override that can
/// apply at any structural position rather than a position of its own.
@Log
public class FLCDNodeEngine {

    private final AngularCalculator angle360Calculator = new Angle360Calculator();
    private final AngularCalculator angle180Calculator = new Angle180Calculator();
    private final NodeDimensionCalculator verticalDimensionCalculator = new VerticalSubtreeDimensionCalculator();
    private final NodeDimensionCalculator horizontalDimensionCalculator = new HorizontalSubtreeDimensionCalculator();

    // ─────────────────────────────────────────────────────────────────
    // Public entry points
    // ─────────────────────────────────────────────────────────────────

    public void precompute(FLCDNode node) {
        if (node.getStatus() == NodeStatus.ROOTIFIED) {
            precomputeRootified(node);
            return;
        }

        switch (node.getRole()) {
            case ROOT -> precomputeRoot(node);
            case FIRST_CHILD -> precomputeFirstChild(node);
            case SECOND_CHILD -> precomputeSecondChild(node);
            case HEIGHT_CHILD -> precomputeHeightChild(node);
            case WIDTH_CHILD -> precomputeWidthChild(node);
        }
    }

    public void compute(FLCDNode node) {
        if (node.getStatus() == NodeStatus.ROOTIFIED) {
            computeRootified(node);
            return;
        }

        switch (node.getRole()) {
            case ROOT -> computeRoot(node);
            case FIRST_CHILD -> computeFirstChild(node);
            case SECOND_CHILD -> computeSecondChild(node);
            case HEIGHT_CHILD -> computeHeightChild(node);
            case WIDTH_CHILD -> computeWidthChild(node);
        }
    }

    /// Marks a node as manually rootified. Its structural `role` is left
    /// untouched — dispatch always checks `status` first, so the role
    /// underneath is simply ignored while the node stays rootified.
    public void rootify(FLCDNode node) {
        node.setStatus(NodeStatus.ROOTIFIED);
    }

    /// Walks a readjustable leaf's ancestry upward, marking each node
    /// READJUSTED until (but not including) its governing FIRST_CHILD.
    public void readjust(FLCDNode node) {
        var parent = node.getParent();
        if (parent == null) throw new IllegalStateException("Parent cannot be null.");

        if (node.getRole() == NodeRole.FIRST_CHILD) return;

        if (node.getRole() == NodeRole.HEIGHT_CHILD) {
            boolean siblingAlreadyReadjusted = parent.getChildren().stream()
                    .anyMatch(sibling -> sibling.getStatus() == NodeStatus.READJUSTED && sibling != node);
            if (siblingAlreadyReadjusted) {
                throw new IllegalStateException("A READJUSTED child node already exists.");
            }
        }

        readjust(parent);
        node.setStatus(NodeStatus.READJUSTED);
    }

    // ─────────────────────────────────────────────────────────────────
    // Precompute phase
    // ─────────────────────────────────────────────────────────────────

    private void precomputeRoot(FLCDNode root) {
        root.getChildren().forEach(this::assignFirstChildRoleAndPrecompute);
        root.incrementDepth();
        calculateCircularSubtree(root, angle360Calculator, false);
    }

    private void precomputeRootified(FLCDNode node) {
        node.getChildren().forEach(this::assignFirstChildRoleAndPrecompute);
        node.incrementDepth();
        calculateCircularSubtree(node, angle180Calculator, true);
    }

    private void assignFirstChildRoleAndPrecompute(FLCDNode firstChild) {
        firstChild.setRole(NodeRole.FIRST_CHILD);
        precompute(firstChild);
    }

    /// Shared by ROOT and ROOTIFIED: children are spread circularly (ROOT)
    /// or semi-circularly (ROOTIFIED, `halfHeight = true`) around the node.
    private void calculateCircularSubtree(FLCDNode node, AngularCalculator angularCalculator, boolean halfHeight) {
        var firstChildren = node.getChildren();
        double angularStep = angularCalculator.calculate(firstChildren.size());
        double maxOffset = 0;

        for (var firstChild : firstChildren) {
            double clearanceHeight = firstChild.getSubtreeWidth() / (2.0 * Math.tan(angularStep / 2.0));
            double safeScalarOffset = clearanceHeight + firstChild.getSubtreeHeight();
            maxOffset = Math.max(maxOffset, safeScalarOffset);
        }

        node.setSubtreeWidth(maxOffset + maxOffset + NODE_DIAMETER);
        node.setSubtreeHeight(halfHeight ? maxOffset + NODE_DIAMETER : maxOffset + maxOffset + NODE_DIAMETER);
    }

    private void precomputeFirstChild(FLCDNode firstChild) {
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

    /// Sorts children by descending area, greedily balances them across
    /// the left/right wings by accumulated area, and assigns each its
    /// [FLCDSide] — replacing the old Left/RightSecondChildComputeInjector
    /// pair, which did the same assignment via object substitution.
    private void calculateBalancedSubtreeDimensions(FLCDNode firstChild) {
        firstChild.getChildren().sort(Comparator.comparingDouble(this::calculateArea).reversed());

        // [0] leftAccumulatedArea, [1] rightAccumulatedArea
        // [2] leftWidth,           [3] rightWidth
        // [4] maxLeftHeight,       [5] maxRightHeight
        // [6] maxDepth
        double[] metrics = new double[7];

        var readjustedNodes = bucket(firstChild, NodeStatus.READJUSTED, false);
        var normalNodes = bucket(firstChild, NodeStatus.NORMAL, false);
        var rootifiedNodes = bucket(firstChild, NodeStatus.ROOTIFIED, false);

        Stream.of(readjustedNodes, normalNodes, rootifiedNodes)
                .flatMap(Arrays::stream)
                .forEach(secondChild -> {
                    double nodeArea = calculateArea(secondChild);

                    if (metrics[0] <= metrics[1]) {
                        secondChild.setSide(FLCDSide.LEFT);
                        metrics[0] += nodeArea;
                        metrics[2] += secondChild.getSubtreeWidth() + WIDTH_SPACER;
                        metrics[4] = Math.max(metrics[4], secondChild.getSubtreeHeight());
                    } else {
                        secondChild.setSide(FLCDSide.RIGHT);
                        metrics[1] += nodeArea;
                        metrics[3] += secondChild.getSubtreeWidth() + WIDTH_SPACER;
                        metrics[5] = Math.max(metrics[5], secondChild.getSubtreeHeight());
                    }

                    metrics[6] = Math.max(metrics[6], secondChild.getDepth());
                });

        double maxSideWidth = Math.max(metrics[2], metrics[3]);
        double totalWidth = (maxSideWidth * 2) + NODE_DIAMETER;

        firstChild.setSubtreeWidth(Math.max(NODE_DIAMETER, totalWidth));
        firstChild.setSubtreeHeight(NODE_DIAMETER + HEIGHT_SPACER + Math.max(metrics[4], metrics[5]));
        firstChild.setDepth((int) (metrics[6] + 1));
    }

    private void precomputeSecondChild(FLCDNode secondChild) {
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

    private void precomputeHeightChild(FLCDNode heightNode) {
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

    private void precomputeWidthChild(FLCDNode widthNode) {
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

    private void initializeLeafDimensions(FLCDNode node) {
        node.setSubtreeWidth(NODE_DIAMETER);
        node.setSubtreeHeight(NODE_DIAMETER);
        node.setDepth(0);
    }

    private double calculateArea(FLCDNode node) {
        return node.getSubtreeHeight() * node.getSubtreeWidth();
    }

    // ─────────────────────────────────────────────────────────────────
    // Compute phase
    // ─────────────────────────────────────────────────────────────────

    private void computeRoot(FLCDNode rootNode) {
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

    private void computeRootified(FLCDNode rootNode) {
        if (rootNode.getChildren().isEmpty()) return;

        log.info("Rootified Computation");

        var children = rootNode.getChildren();
        int totalChildren = children.size();
        double angularStep = angle180Calculator.calculate(totalChildren);
        double beginningAngle = rootNode.getLocalRadianAngle() - Math.PI / 2;

        for (int i = 0; i < totalChildren; i++) {
            var firstChild = children.get(i);
            double currentAngle = beginningAngle + i * angularStep + (angularStep / 2);
            double clearanceHeight = firstChild.getSubtreeWidth() / (2.0 * Math.tan(angularStep / 2.0));

            double nodeCenter = firstChild.getStatus() == NodeStatus.ROOTIFIED
                    ? clearanceHeight
                    : clearanceHeight + firstChild.getSubtreeHeight() - NODE_DIAMETER;

            firstChild.setLocalRadianAngle(currentAngle);
            firstChild.setGridX(rootNode.getGridX() + (nodeCenter * Math.cos(currentAngle)));
            firstChild.setGridY(rootNode.getGridY() - (nodeCenter * Math.sin(currentAngle)));

            compute(firstChild);
        }
    }

    private void computeFirstChild(FLCDNode firstChild) {
        if (firstChild.getChildren().isEmpty()) return;

        double parentAngle = firstChild.getLocalRadianAngle();
        double rightAngle = parentAngle - (Math.PI / 2.0);
        double leftAngle = parentAngle + (Math.PI / 2.0);
        double additionalAngle = Math.PI / 2.0;
        double forwardStepLength = NODE_RADIUS + HEIGHT_SPACER + NODE_RADIUS;

        double anchorX = firstChild.getGridX() - (forwardStepLength * Math.cos(parentAngle));
        double anchorY = firstChild.getGridY() + (forwardStepLength * Math.sin(parentAngle));

        // [0] accumulatedRightDistance, [1] accumulatedLeftDistance
        double[] accumulatedDistances = {0.0, 0.0};

        var readjustedNodes = bucket(firstChild, NodeStatus.READJUSTED, true);
        var normalNodes = bucket(firstChild, NodeStatus.NORMAL, false);
        var rootifiedNodes = bucket(firstChild, NodeStatus.ROOTIFIED, false);

        Stream.of(readjustedNodes, normalNodes, rootifiedNodes)
                .flatMap(Arrays::stream)
                .forEach(child -> {
                    boolean isRight = child.getSide() == FLCDSide.RIGHT;
                    double baselineAngle = isRight ? rightAngle : leftAngle;
                    int slot = isRight ? 0 : 1;

                    if (child.getStatus() != NodeStatus.ROOTIFIED) {
                        accumulatedDistances[slot] = projectSecondChildAlongVector(
                                child, anchorX, anchorY, baselineAngle, accumulatedDistances[slot]);

                        if (isReadjustable(child)) {
                            log.info("Readjusted Node: " + child.getIdentifier());
                            readjustNode(child);
                        }
                    } else {
                        double extra = isRight ? -additionalAngle : additionalAngle;
                        accumulatedDistances[slot] = projectRootifiedSecondChildAlongVector(
                                child, anchorX, anchorY, baselineAngle, extra, accumulatedDistances[slot]);
                    }
                });
    }

    private double projectSecondChildAlongVector(FLCDNode child, double anchorX, double anchorY,
                                                 double baselineAngle, double currentDistance) {
        double newDistance = currentDistance + WIDTH_SPACER + child.getSubtreeWidth();
        double centerPoint = newDistance - NODE_RADIUS;

        child.setGridX(anchorX + (centerPoint * Math.cos(baselineAngle)));
        child.setGridY(anchorY - (centerPoint * Math.sin(baselineAngle)));
        child.setLocalRadianAngle(baselineAngle);

        compute(child);
        return newDistance;
    }

    private double projectRootifiedSecondChildAlongVector(FLCDNode child, double anchorX, double anchorY,
                                                          double baselineAngle, double additionalAngle,
                                                          double currentDistance) {
        double centerPoint = currentDistance + WIDTH_SPACER + child.getSubtreeWidth() / 2;

        child.setGridX(anchorX + (centerPoint * Math.cos(baselineAngle)));
        child.setGridY(anchorY - (centerPoint * Math.sin(baselineAngle)));
        child.setLocalRadianAngle(baselineAngle + additionalAngle);

        compute(child);
        return centerPoint + child.getSubtreeWidth() / 2;
    }

    /// SECOND_CHILD level: children become HEIGHT_CHILD, spaced using
    /// each child's own subtree HEIGHT.
    private void computeSecondChild(FLCDNode secondChild) {
        if (secondChild.getChildren().isEmpty()) return;

        FLCDSide side = secondChild.getSide();
        double turnSign = side == FLCDSide.LEFT ? 1.0 : -1.0;
        double myAngle = secondChild.getLocalRadianAngle();
        double childAngleTrajectory = myAngle + turnSign * (Math.PI / 2.0);

        double forwardStepLength = NODE_RADIUS + WIDTH_SPACER + NODE_RADIUS;
        double anchorX = secondChild.getGridX() - (forwardStepLength * Math.cos(myAngle));
        double anchorY = secondChild.getGridY() + (forwardStepLength * Math.sin(myAngle));

        double[] offset = {NODE_RADIUS + HEIGHT_SPACER};

        var rootifiedChildren = bucket(secondChild, NodeStatus.ROOTIFIED, false);
        var normalChildren = bucket(secondChild, NodeStatus.NORMAL, false);
        var readjustedNodes = bucket(secondChild, NodeStatus.READJUSTED, false);

        Stream.of(rootifiedChildren, normalChildren, readjustedNodes)
                .flatMap(Arrays::stream)
                .forEach(heightChild -> {
                    heightChild.setRole(NodeRole.HEIGHT_CHILD);
                    heightChild.setSide(side);

                    if (heightChild.getStatus() != NodeStatus.ROOTIFIED) {
                        offset[0] = projectAlongVector(heightChild, anchorX, anchorY, childAngleTrajectory, offset[0], true);
                    } else {
                        double extra = turnSign * (Math.PI / 2.0);
                        offset[0] = projectRootifiedAlongVector(heightChild, anchorX, anchorY, childAngleTrajectory, extra, offset[0], HEIGHT_SPACER);
                    }

                    if (isReadjustable(heightChild)) {
                        log.info("Readjusted Node: " + heightChild.getIdentifier());
                        readjustNode(heightChild);
                    }
                });
    }

    /// HEIGHT_CHILD level: children become WIDTH_CHILD, spaced using
    /// each child's own subtree WIDTH.
    private void computeHeightChild(FLCDNode heightChild) {
        if (heightChild.getChildren().isEmpty()) return;

        FLCDSide side = heightChild.getSide();
        double turnSign = side == FLCDSide.LEFT ? 1.0 : -1.0;
        double myAngle = heightChild.getLocalRadianAngle();
        double childAngleTrajectory = myAngle + turnSign * (Math.PI / 2.0);

        double forwardStepLength = NODE_RADIUS + HEIGHT_SPACER + NODE_RADIUS;
        double anchorX = heightChild.getGridX() + (forwardStepLength * Math.cos(myAngle));
        double anchorY = heightChild.getGridY() - (forwardStepLength * Math.sin(myAngle));

        double[] offset = {NODE_RADIUS + WIDTH_SPACER};

        var readjustedNodes = bucket(heightChild, NodeStatus.READJUSTED, true);
        var rootifiedChildren = bucket(heightChild, NodeStatus.ROOTIFIED, false);
        var normalChildren = bucket(heightChild, NodeStatus.NORMAL, false);

        Stream.of(readjustedNodes, rootifiedChildren, normalChildren)
                .flatMap(Arrays::stream)
                .forEach(widthChild -> {
                    widthChild.setRole(NodeRole.WIDTH_CHILD);
                    widthChild.setSide(side);

                    if (widthChild.getStatus() != NodeStatus.ROOTIFIED) {
                        offset[0] = projectAlongVector(widthChild, anchorX, anchorY, childAngleTrajectory, offset[0], false);
                    } else {
                        double extra = -turnSign * (Math.PI / 2.0);
                        offset[0] = projectRootifiedAlongVector(widthChild, anchorX, anchorY, childAngleTrajectory, extra, offset[0], WIDTH_SPACER);
                    }

                    if (isReadjustable(widthChild)) {
                        readjustNode(widthChild);
                    }
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
    private void computeWidthChild(FLCDNode widthNode) {
        if (widthNode.getChildren().isEmpty()) return;

        FLCDSide side = widthNode.getSide();
        double turnSign = side == FLCDSide.LEFT ? -1.0 : 1.0;
        double myAngle = widthNode.getLocalRadianAngle();
        double childAngleTrajectory = myAngle + turnSign * (Math.PI / 2.0);

        double forwardStepLength = NODE_RADIUS + WIDTH_SPACER + NODE_RADIUS;
        double anchorX = widthNode.getGridX() + (forwardStepLength * Math.cos(myAngle));
        double anchorY = widthNode.getGridY() - (forwardStepLength * Math.sin(myAngle));

        double[] offset = {NODE_RADIUS + HEIGHT_SPACER};

        var rootifiedChildren = bucket(widthNode, NodeStatus.ROOTIFIED, false);
        var normalChildren = bucket(widthNode, NodeStatus.NORMAL, false);
        var readjustedNodes = bucket(widthNode, NodeStatus.READJUSTED, false);

        Stream.of(rootifiedChildren, normalChildren, readjustedNodes)
                .flatMap(Arrays::stream)
                .forEach(heightChild -> {
                    heightChild.setRole(NodeRole.HEIGHT_CHILD);
                    heightChild.setSide(side);

                    if (heightChild.getStatus() != NodeStatus.ROOTIFIED) {
                        offset[0] = projectAlongVector(heightChild, anchorX, anchorY, childAngleTrajectory, offset[0], true);
                    } else {
                        double extra = Math.PI / 2.0;
                        offset[0] = projectRootifiedAlongVector(heightChild, anchorX, anchorY, childAngleTrajectory, extra, offset[0], WIDTH_SPACER);
                    }

                    if (isReadjustable(heightChild)) {
                        readjustNode(heightChild);
                    }
                });
    }

    /// Shared non-rootified projection step for SECOND_CHILD/HEIGHT_CHILD/
    /// WIDTH_CHILD levels. `useHeightDimension` picks which of the
    /// child's own subtree dimensions (and matching spacer) governs
    /// stepping to the next sibling.
    private double projectAlongVector(FLCDNode child, double anchorX, double anchorY,
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

    /// Shared rootified projection step for SECOND_CHILD/HEIGHT_CHILD/
    /// WIDTH_CHILD levels.
    private double projectRootifiedAlongVector(FLCDNode child, double anchorX, double anchorY, double angle,
                                               double extraAngle, double currentOffset, double returnSpacer) {
        double centerPoint = currentOffset + (child.getSubtreeWidth() / 2);

        child.setGridX(anchorX + (centerPoint * Math.cos(angle)));
        child.setGridY(anchorY - (centerPoint * Math.sin(angle)));
        child.setLocalRadianAngle(angle + extraAngle);

        compute(child);
        return centerPoint + (child.getSubtreeWidth() / 2) + returnSpacer;
    }

    // ─────────────────────────────────────────────────────────────────
    // Readjust support (shared by FirstChild/SecondChild/HeightChild/
    // WidthChild compute — all four levels use identical geometry, the
    // only per-level differences already collapsed into `Side`)
    // ─────────────────────────────────────────────────────────────────

    private boolean isReadjustable(FLCDNode node) {
        return node.getChildren().isEmpty() && node.getStatus() == NodeStatus.READJUSTED;
    }

    private FLCDNode findGoverningFirstChild(FLCDNode node) {
        var firstChild = node.getParent();
        while (firstChild.getRole() != NodeRole.FIRST_CHILD) {
            firstChild = firstChild.getParent();
        }
        return firstChild;
    }

    private void readjustNode(FLCDNode readjustableNode) {
        double ax = readjustableNode.getGridX();
        double ay = readjustableNode.getGridY();

        var firstChild = findGoverningFirstChild(readjustableNode);
        double cx = firstChild.getGridX();
        double cy = firstChild.getGridY();

        var rootNode = firstChild.getParent();
        double bx = rootNode.getGridX();
        double by = rootNode.getGridY();

        int totalChildren = rootNode.getChildren().size();
        double angularStep = rootNode.getStatus() == NodeStatus.ROOTIFIED
                ? angle180Calculator.calculate(totalChildren)
                : angle360Calculator.calculate(totalChildren);

        // Perpendicular distance from A (readjustable node) to line BC (root -> firstChild)
        double bcx = cx - bx;
        double bcy = cy - by;
        double bcLength = Math.hypot(bcx, bcy);
        double ux = bcx / bcLength;
        double uy = bcy / bcLength;

        double bax = ax - bx;
        double bay = ay - by;
        double projection = bax * ux + bay * uy;

        double px = bx + projection * ux;
        double py = by + projection * uy;
        double opposite = Math.hypot(ax - px, ay - py);

        double halfAngularStep = angularStep / 2.0;
        double sign = readjustableNode.getSide() == FLCDSide.RIGHT ? -1.0 : 1.0;
        double newAngle = firstChild.getLocalRadianAngle() + sign * halfAngularStep;

        double radius = opposite / Math.sin(halfAngularStep);
        double newAx = bx + radius * Math.cos(newAngle);
        double newAy = by - radius * Math.sin(newAngle);

        double newNodeRadius = NODE_RADIUS / Math.sin(halfAngularStep);
        readjustableNode.setGridX(newAx + newNodeRadius * Math.cos(firstChild.getLocalRadianAngle()));
        readjustableNode.setGridY(newAy - newNodeRadius * Math.sin(firstChild.getLocalRadianAngle()));
    }

    // ─────────────────────────────────────────────────────────────────
    // Shared bucketing helper (replaces EmptyListChecker + the repeated
    // per-status stream/filter/toArray blocks in every old Compute class)
    // ─────────────────────────────────────────────────────────────────

    private FLCDNode[] bucket(FLCDNode node, NodeStatus status, boolean sortByDepth) {
        var stream = node.getChildren().stream().filter(child -> child.getStatus() == status);
        if (sortByDepth) {
            stream = stream.sorted(Comparator.comparingInt(FLCDNode::getDepth));
        }
        return stream.toArray(FLCDNode[]::new);
    }
}
