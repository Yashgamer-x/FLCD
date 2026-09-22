package com.yashgamerx.flcd.common.metrics;

/// Distance from the root's center to a single leaf's center.
///
/// `straightLineDistance` is the direct Euclidean center-to-center distance
/// (root center to leaf center, ignoring the path taken through the tree).
/// `pathLength` is the sum of Euclidean edge lengths walking root -> ... ->
/// leaf along the actual tree structure. The two coincide only when the
/// root and leaf are directly connected.
public record RootLeafDistance(int leafId, int depth, double straightLineDistance, double pathLength) {
}
