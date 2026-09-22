package com.yashgamerx.flcd.flcd.algorithm;

import com.yashgamerx.flcd.common.LayoutAlgorithm;
import com.yashgamerx.flcd.flcd.model.FLCDNode;

/// FLCD's own name for [LayoutAlgorithm]`<FLCDNode>`, kept so existing
/// call sites ([TreeLayoutAlgorithmType], the FLCD view) don't need to
/// reference the generic type directly. Adds nothing beyond the shared
/// contract.
public interface TreeLayoutAlgorithm extends LayoutAlgorithm<FLCDNode> {
}
