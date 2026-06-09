package com.yashgamerx.flcd.service.precompute;

import com.yashgamerx.flcd.model.AbstractNode;
import com.yashgamerx.flcd.service.dimension.NodeDimensionCalculator;
import com.yashgamerx.flcd.service.dimension.VerticalSubtreeDimensionCalculator;
import com.yashgamerx.flcd.service.list.EmptyListChecker;
import com.yashgamerx.flcd.service.list.EmptyListCheckerImplementation;
import com.yashgamerx.flcd.service.precompute.inject.PrecomputeInjectable;
import com.yashgamerx.flcd.service.precompute.inject.factory.PreComputeInjectorFactory;
import com.yashgamerx.flcd.service.precompute.inject.factory.PreComputeInjectorOption;

import static com.yashgamerx.flcd.model.AbstractNode.NODE_DIAMETER;

public class WidthChildPreCompute implements Precomputable {

    private final PrecomputeInjectable injectable;
    private final EmptyListChecker emptyListChecker = new EmptyListCheckerImplementation();
    private final NodeDimensionCalculator nodeDimensionCalculator = new VerticalSubtreeDimensionCalculator();

    public WidthChildPreCompute(PreComputeInjectorFactory preComputableFactoryImplementation) {
        this.injectable = preComputableFactoryImplementation.getInjector(PreComputeInjectorOption.HEIGHT_CHILD);
    }

    @Override
    public void precompute(AbstractNode widthNode) {
        if (isLeafNode(widthNode)) {
            initializeLeafDimensions(widthNode);
            return;
        }

        widthNode.getChildren().forEach(this::injectAndPrecomputeChildren);

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
    }

    /// Injects the dependency and then precomputes that child.
    private void injectAndPrecomputeChildren(AbstractNode heightChild) {
        injectable.inject(heightChild);
        heightChild.precompute();
    }

}
