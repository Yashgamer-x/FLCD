package com.yashgamerx.flcd.rt.algorithm;

import com.yashgamerx.flcd.rt.model.RTNode;

/// Classic tree layout baseline used for comparison against FLCD/TMEL/CMEL.
///
/// Implements the four aesthetics from Reingold & Tilford, "Tidier Drawings
/// of Trees" (IEEE TSE, 1981):
///   1. Nodes at the same depth lie on a straight, level-aligned line, and
///      level order is preserved left-to-right.
///   2. A left subtree is positioned to the left of its parent, a right
///      subtree to the right (generalized here to: siblings are ordered
///      left-to-right exactly as given in the input).
///   3. A parent is centered over its children.
///   4. A tree and its mirror image produce mirror-image drawings; a
///      subtree is drawn identically wherever it occurs (this is the
///      aesthetic the paper's own "Algorithm TR" was built specifically to
///      satisfy, fixing Wetherell & Shannon's Algorithm WS).
///
/// The 1981 paper's Algorithm TR is specified for binary trees and uses an
/// explicit two-pointer contour scan with "threads" temporarily stored in
/// unused child-link fields (Fig. 6/8 of the paper). Since this codebase's
/// trees are n-ary (see [RTNode#getChildren]), this class instead follows
/// the direct, well-known linear-time generalization of that same
/// heuristic — "two subtrees should be formed independently and then
/// pushed together as close as possible, without disturbing either" —
/// published as Buchheim, Jünger & Leipert, "Improving Walker's Algorithm
/// to Run in Linear Time" (2002). It reduces to Algorithm TR exactly for
/// binary trees and preserves all four aesthetics above; the paper's
/// "thread" bookkeeping becomes the [RTNode#getThread] contour pointer
/// below, and its ROOTSEP/CURSEP two-pointer contour scan becomes
/// `apportion`/`moveSubtree`.
public class ReingoldTilfordAlgorithm {

    /// Runs the full three-pass layout (firstWalk, secondWalk, and a final
    /// normalizing shift so the whole tree's leftmost extent sits at
    /// `originX`) and writes results into each node's gridX/gridY.
    public void calculate(RTNode root, double originX, double originY) {
        if (root == null) return;

        assignDepths(root, 0);
        firstWalk(root);

        // secondWalk: convert relative prelim/mod bookkeeping into absolute
        // coordinates, tracking the minimum x seen so the whole drawing can
        // be shifted to start flush with originX.
        var minX = new double[]{Double.POSITIVE_INFINITY};
        secondWalk(root, 0.0, minX);

        var shiftX = originX - minX[0];
        applyFinalShift(root, shiftX, originY);
    }

    private void assignDepths(RTNode node, int depth) {
        node.setDepth(depth);
        for (var child : node.getChildren()) {
            assignDepths(child, depth + 1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // First walk — bottom-up: compute each node's preliminary x relative
    // to its own subtree, pushing sibling subtrees apart only as much as
    // their contours actually require (Aesthetic 3 + the TR "push subtrees
    // together as close as possible without touching" heuristic).
    // ─────────────────────────────────────────────────────────────────────
    private void firstWalk(RTNode v) {
        if (v.isLeaf()) {
            var left = v.leftSibling();
            v.setPrelimX(left != null ? left.getPrelimX() + RTNode.SIBLING_DISTANCE : 0.0);
            return;
        }

        RTNode defaultAncestor = v.firstChild();
        for (var child : v.getChildren()) {
            firstWalk(child);
            defaultAncestor = apportion(child, defaultAncestor);
        }
        executeShifts(v);

        double midpoint = (v.firstChild().getPrelimX() + v.lastChild().getPrelimX()) / 2.0;
        var left = v.leftSibling();
        if (left != null) {
            v.setPrelimX(left.getPrelimX() + RTNode.SIBLING_DISTANCE);
            v.setMod(v.getPrelimX() - midpoint);
        } else {
            v.setPrelimX(midpoint);
        }
    }

    /// The heart of the algorithm (Buchheim–Jünger–Leipert §3). Walks the
    /// inside contours of `v`'s subtree against its left sibling's subtree —
    /// exactly the paper's "superimpose the two subtrees at their roots and
    /// move them apart until no two points are touching" — using threads to
    /// jump across subtrees of uneven height, and pushes intermediate
    /// subtrees along using moveSubtree whenever a collision is found.
    private RTNode apportion(RTNode v, RTNode defaultAncestor) {
        var w = v.leftSibling();
        if (w == null) return defaultAncestor;

        RTNode vip = v, vop = v;
        RTNode vim = w, vom = v.getParent().firstChild();
        double sip = vip.getMod(), sop = vop.getMod();
        double sim = vim.getMod(), som = vom.getMod();

        while (nextRight(vim) != null && nextLeft(vip) != null) {
            vim = nextRight(vim);
            vip = nextLeft(vip);
            vom = nextLeft(vom);
            vop = nextRight(vop);
            vop.setAncestor(v);

            double shift = (vim.getPrelimX() + sim) - (vip.getPrelimX() + sip) + RTNode.SIBLING_DISTANCE;
            if (shift > 0) {
                moveSubtree(ancestor(vim, v, defaultAncestor), v, shift);
                sip += shift;
                sop += shift;
            }
            sim += vim.getMod();
            sip += vip.getMod();
            som += vom.getMod();
            sop += vop.getMod();
        }

        if (nextRight(vim) != null && nextRight(vop) == null) {
            vop.setThread(nextRight(vim));
            vop.setMod(vop.getMod() + sim - sop);
        }
        if (nextLeft(vip) != null && nextLeft(vom) == null) {
            vom.setThread(nextLeft(vip));
            vom.setMod(vom.getMod() + sip - som);
            defaultAncestor = v;
        }
        return defaultAncestor;
    }

    /// Follows the right (outer) contour: last child if internal, else thread.
    private RTNode nextRight(RTNode v) {
        return !v.isLeaf() ? v.lastChild() : v.getThread();
    }

    /// Follows the left (inner) contour: first child if internal, else thread.
    private RTNode nextLeft(RTNode v) {
        return !v.isLeaf() ? v.firstChild() : v.getThread();
    }

    /// Distributes `shift` evenly across the subtrees strictly between
    /// `wm` and `wp` (siblings), so the push doesn't just jam `wp` against
    /// `wm` but nudges everything in between proportionally too — this is
    /// what keeps the drawing "tidy" per Aesthetic 3/4 rather than merely
    /// non-overlapping.
    private void moveSubtree(RTNode wm, RTNode wp, double shift) {
        int subtrees = wp.getNumber() - wm.getNumber();
        if (subtrees == 0) subtrees = 1; // defensive: avoid divide-by-zero on malformed trees
        wp.setChange(wp.getChange() - shift / subtrees);
        wp.setShift(wp.getShift() + shift);
        wm.setChange(wm.getChange() + shift / subtrees);
        wp.setPrelimX(wp.getPrelimX() + shift);
        wp.setMod(wp.getMod() + shift);
    }

    /// Propagates the accumulated shift/change across all of v's children
    /// (right to left) so intermediate subtrees pushed apart by apportion()
    /// actually move, not just the two subtrees directly compared.
    private void executeShifts(RTNode v) {
        double shift = 0.0;
        double change = 0.0;
        var children = v.getChildren();
        for (int i = children.size() - 1; i >= 0; i--) {
            var w = children.get(i);
            w.setPrelimX(w.getPrelimX() + shift);
            w.setMod(w.getMod() + shift);
            change += w.getChange();
            shift += w.getShift() + change;
        }
    }

    /// Picks which ancestor to push when vim and v aren't actually
    /// comparable siblings (i.e., vim's subtree is "borrowed" via a thread
    /// from elsewhere) — falls back to defaultAncestor per the paper.
    private RTNode ancestor(RTNode vim, RTNode v, RTNode defaultAncestor) {
        var vimAncestor = vim.getAncestor();
        if (vimAncestor.getParent() == v.getParent()) return vimAncestor;
        return defaultAncestor;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Second walk — top-down: accumulate mod values into absolute
    // coordinates and assign the y coordinate from depth (Aesthetic 1).
    // ─────────────────────────────────────────────────────────────────────
    private void secondWalk(RTNode v, double modSum, double[] minX) {
        double finalX = v.getPrelimX() + modSum;
        v.setGridX(finalX);
        v.setGridY(v.getDepth() * RTNode.LEVEL_DISTANCE);
        if (finalX < minX[0]) minX[0] = finalX;

        for (var child : v.getChildren()) {
            secondWalk(child, modSum + v.getMod(), minX);
        }
    }

    /// Third pass: shifts every node so the tree's leftmost node sits at
    /// `originX`, and offsets y by `originY` so the root sits where the
    /// caller expects (matching PlanarGridAlgorithm's origin-centered
    /// convention used by the other algorithm families).
    private void applyFinalShift(RTNode v, double shiftX, double originY) {
        v.setGridX(v.getGridX() + shiftX);
        v.setGridY(v.getGridY() + originY);
        for (var child : v.getChildren()) {
            applyFinalShift(child, shiftX, originY);
        }
    }
}
