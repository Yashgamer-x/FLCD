package com.yashgamerx.flcd.common;

import java.util.List;

/// Shared contract for a node that a [LayoutAlgorithm] can position.
///
/// This is deliberately minimal: `layoutX`/`layoutY` plus enough tree
/// structure to walk. It says nothing about depth, role, or any other
/// algorithm-specific bookkeeping — every family (FLCD, TMEL, CMEL, RT,
/// Rings) carries very different state beyond this shared surface (e.g.
/// Rings has no `depth` field at all), so that state stays on each
/// family's own node class rather than being forced up into a common
/// type. The one thing every family's placement math ultimately produces
/// is a screen position, and that's the only thing a view is allowed to
/// read — never algorithm-internal fields — which is what keeps
/// computation and rendering cleanly separated.
///
/// `T` is the implementing type itself (`FLCDNode implements
/// AlgorithmicNode<FLCDNode>`, etc.), so `getChildren()` returns a typed
/// list without every caller needing to downcast.
public interface AlgorithmicNode<T extends AlgorithmicNode<T>> {
    double getLayoutX();

    void setLayoutX(double x);

    double getLayoutY();

    void setLayoutY(double y);

    List<T> getChildren();
}
