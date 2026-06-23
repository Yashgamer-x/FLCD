package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.dimension.HorizontalSubtreeDimensionCalculator;
import com.yashgamerx.flcd.service.dimension.NodeDimensionCalculator;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;
import com.yashgamerx.flcd.service.precompute.inject.PrecomputeInjectable;
import com.yashgamerx.flcd.service.precompute.inject.WidthChildPreComputeInjector;

import static com.yashgamerx.flcd.model.AbstractNode.NODE_DIAMETER;

public class HeightChildPreCompute implements Precomputable {

    private final PrecomputeInjectable injectable = new WidthChildPreComputeInjector();
    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();
    private final NodeDimensionCalculator nodeDimensionCalculator = new HorizontalSubtreeDimensionCalculator();

    @Override
    public void precompute(AbstractNode heightNode) {
        heightNode.incrementDepth();
        if (isLeafNode(heightNode)) {
            initializeLeafDimensions(heightNode);
            return;
        }

        heightNode.getChildren().forEach(this::injectAndPrecompute);

        nodeDimensionCalculator.calculate(heightNode);
    }

    /// Checks if the node has no children
    private boolean isLeafNode(AbstractNode node) {
        return emptyListChecker.isEmpty(node.getChildren());
    }

    /// Initializes leaf node dimensions
    private void initializeLeafDimensions(AbstractNode node) {
        node.setSubtreeWidth(NODE_DIAMETER);
        node.setSubtreeHeight(NODE_DIAMETER);
    }

    /// Injects and then precomputes the width child
    private void injectAndPrecompute(AbstractNode widthChild) {
        injectable.inject(widthChild);
        widthChild.precompute();
    }
}
