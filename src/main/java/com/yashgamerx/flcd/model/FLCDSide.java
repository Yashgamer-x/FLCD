package com.yashgamerx.flcd.model;

/// Which wing of the layout a node belongs to, relative to its governing
/// first-child's local angle. Assigned once (via the area-balancing pass
/// during `FIRST_CHILD` precompute) on `SECOND_CHILD`-level nodes, then
/// propagated unchanged down through their `HEIGHT_CHILD`/`WIDTH_CHILD`
/// descendants during compute.
public enum FLCDSide {
    NONE,
    LEFT,
    RIGHT
}
