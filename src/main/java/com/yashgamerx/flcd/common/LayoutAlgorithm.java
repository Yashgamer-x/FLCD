package com.yashgamerx.flcd.common;

/// Shared contract for every family's layout algorithm.
///
/// A single call to [#calculate] must fully populate `layoutX`/`layoutY`
/// (and whatever other algorithm-internal state that family needs) on
/// every node reachable from `root` — purely against the data model, with
/// no JavaFX types involved anywhere in an implementation. A view is only
/// ever built by walking the tree *after* [#calculate] returns; it must
/// never be interleaved with computation (no creating a `Circle` for a
/// node, then computing its child, then creating that child's `Circle`).
/// This is what "restructure so the algorithm computes first, the view
/// renders second" means at the type level — the compiler enforces the
/// two phases can't be mixed, because a [LayoutAlgorithm] implementation
/// has no way to reach a view class in the first place.
public interface LayoutAlgorithm<T extends AlgorithmicNode<T>> {
    void calculate(T root, double originX, double originY);
}
