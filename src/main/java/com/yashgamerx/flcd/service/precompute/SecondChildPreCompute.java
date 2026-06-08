package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.dimension.NodeDimensionCalculator;
import com.yashgamerx.flcd.service.dimension.VerticalSubtreeDimensionCalculator;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;
import com.yashgamerx.flcd.service.precompute.factory.PreComputableFactoryImplementation;
import com.yashgamerx.flcd.service.precompute.inject.HeightChildPreComputeInjector;
import com.yashgamerx.flcd.service.precompute.inject.PrecomputeInjectable;

import static com.yashgamerx.flcd.model.AbstractNode.NODE_DIAMETER;

public class SecondChildPreCompute implements Precomputable {

    private final PrecomputeInjectable injectable;
    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();
    private final NodeDimensionCalculator nodeDimensionCalculator = new VerticalSubtreeDimensionCalculator();

    public SecondChildPreCompute(PreComputableFactoryImplementation preComputableFactoryImplementation) {
        this.injectable = new HeightChildPreComputeInjector(preComputableFactoryImplementation);
    }

    @Override
    public void precompute(AbstractNode secondChild) {
        if (isLeafNode(secondChild)) {
            initializeLeafDimensions(secondChild);
            return;
        }

        // Inject and Precompute
        secondChild.getChildren().forEach(this::injectAndPrecomputeChildren);

        nodeDimensionCalculator.calculate(secondChild);
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

    /// Injects {@link HeightChildPreCompute} dependency and Precomputes the child
    private void injectAndPrecomputeChildren(AbstractNode heightChild) {
        injectable.inject(heightChild);
        heightChild.precompute();
    }
}
