package com.yashgamerx.flcd.model;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
public class TreeNode {
    private final int identifier;
    private final List<TreeNode> children;

    public TreeNode(int identifier) {
        this.identifier = identifier;
        this.children = new ArrayList<>();
    }

    public void addChild(TreeNode childNode) {
        this.children.add(childNode);
    }
}