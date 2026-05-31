package com.yashgamerx.flcd.model;

public class UnknownNode extends AbstractNode{

    public UnknownNode(int identifier) {
        super(identifier);
    }

    /// This implementation must be empty as UnknownNode cannot be precomputed
    @Override
    public void precompute() {
        throw new UnsupportedOperationException("Node conversion not yet implemented.");
    }

    /// This implementation must be empty as UnknownNode cannot be computed
    @Override
    public void compute() {
        throw new UnsupportedOperationException("Node conversion not yet implemented.");
    }

    /// This implementation must be empty as UnknownNode cannot be readjusted
    @Override
    public void readjust() {
        throw new UnsupportedOperationException("Node conversion not yet implemented.");
    }

    /// This implementation must be empty as UnknownNode cannot be rootified
    @Override
    public void rootify() {
        throw new UnsupportedOperationException("Node conversion not yet implemented.");
    }
}
