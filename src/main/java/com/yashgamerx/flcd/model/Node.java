package com.yashgamerx.flcd.model;

import com.yashgamerx.flcd.service.precompute.FirstChildPreCompute;
import com.yashgamerx.flcd.service.precompute.HeightChildPreCompute;

public class Node extends AbstractNode {

    public Node(int identifier) {
        super(identifier);
    }

    @Override
    public void precompute() {
        precomputable.precompute(this);
    }

    @Override
    public void compute() {
        computable.compute(this);
    }

    @Override
    public void readjust() {
        var parent = getParent();
        if (parent == null) throw new IllegalStateException("Parent cannot be null.");

        if (precomputable instanceof FirstChildPreCompute) return;

        if (precomputable instanceof HeightChildPreCompute) {
            boolean hasReadjusted = parent.getChildren()
                    .stream()
                    .anyMatch(node -> node.getStatus() == NodeStatus.READJUSTED && node != this);

            if (hasReadjusted) {
                throw new IllegalStateException("A READJUSTED child node already exists.");
            }
        }

        // Readjusts the status of the current node if it belongs to the following list:
        // SecondChildPrecompute: There are no restrictions on readjusting the width of a node as all its siblings can be readjusted.
        // HeightChildPrecompute: Only one READJUSTED child node is allowed.
        // WidthChildPreCompute: There are no restrictions on readjusting the width of a node as all its siblings can be readjusted.
        this.setStatus(NodeStatus.READJUSTED);

        parent.readjust();
    }

    @Override
    public void rootify() {

    }

}
