package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.dimension.NodeDimensionCalculator;
import com.yashgamerx.flcd.service.dimension.VerticalSubtreeDimensionCalculator;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;
import com.yashgamerx.flcd.service.precompute.inject.HeightChildPreComputeInjector;
import com.yashgamerx.flcd.service.precompute.inject.PrecomputeInjectable;

import static com.yashgamerx.flcd.model.AbstractNode.NODE_DIAMETER;

public class WidthChildPreCompute implements Precomputable {

    private final PrecomputeInjectable injectable = new HeightChildPreComputeInjector();
    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();
    private final NodeDimensionCalculator nodeDimensionCalculator = new VerticalSubtreeDimensionCalculator();

    @Override
    public void precompute(AbstractNode widthNode) {
        if (isLeafNode(widthNode)) {
            initializeLeafDimensions(widthNode);
            return;
        }

        widthNode.getChildren().forEach(this::injectAndPrecomputeChildren);
        widthNode.incrementDepth();

        nodeDimensionCalculator.calculate(widthNode);
    }

    /// Checks if the node has no children
    private boolean isLeafNode(AbstractNode node) {
        return emptyListChecker.isEmpty(node.getChildren());
    }

    /// Initializes leaf node dimensions
    private void initializeLeafDimensions(AbstractNode node) {
        node.setSubtreeWidth(NODE_DIAMETER);
        node.setSubtreeHeight(NODE_DIAMETER);
        node.setDepth(0);
    }

    /// Injects the dependency and then precomputes that child.
    private void injectAndPrecomputeChildren(AbstractNode heightChild) {
        injectable.inject(heightChild);
        heightChild.precompute();
    }

}
