package com.yashgamerx.flcd.common;

/// Structural position of a node within the FLCD layout tree.
///
/// A node's role never changes once assigned during the precompute phase
/// (it is read again, unchanged, during the compute phase). The one
/// exception is a node the user has manually rootified via the UI —
/// that state is tracked separately via  rather than
/// as a role, since "rootified" is an override that can apply at any
/// position in the tree, not a structural position of its own.
public enum NodeRole {
    ROOT,
    FIRST_CHILD,
    SECOND_CHILD,
    HEIGHT_CHILD,
    WIDTH_CHILD
}
