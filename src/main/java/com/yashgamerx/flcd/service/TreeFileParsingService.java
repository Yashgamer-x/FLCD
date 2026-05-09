package com.yashgamerx.flcd.service;

import com.yashgamerx.flcd.model.TreeNode;
import lombok.extern.java.Log;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.stream.Stream;

@Log
public class TreeFileParsingService implements FileParsingService {

    @Override
    public void readAndParseIdentifiedTextFile(final File textFileToProcess) {
        var nodeLookupMap = new HashMap<Integer, TreeNode>();

        try (Stream<String> lineStream = Files.lines(textFileToProcess.toPath())) {
            lineStream.filter(line -> !line.isBlank())
                    .forEach(line -> parseLineIntoTree(line, nodeLookupMap));

            var rootNode = nodeLookupMap.get(1);
            log.info("Tree construction finished. Root status: " + (rootNode != null ? "Active" : "Missing"));

            analyzeTreeStructure(rootNode);

        } catch (Exception exception) {
            log.severe("Parsing failed: " + exception.getMessage());
        }
    }

    private void parseLineIntoTree(String line, HashMap<Integer, TreeNode> nodeMap) {
        // PRINCIPLE: Split-and-Process
        // We convert the string line into an array of integers immediately
        var parts = line.trim().split("\\s+");
        if (parts.length < 1) return;

        // The first element is the parent
        var parentNode = nodeMap.computeIfAbsent(Integer.parseInt(parts[0]), TreeNode::new);

        // All subsequent elements are children
        for (int i = 1; i < parts.length; i++) {
            var childId = Integer.parseInt(parts[i]);
            var childNode = nodeMap.computeIfAbsent(childId, TreeNode::new);
            parentNode.addChild(childNode);
        }
    }

    private void analyzeTreeStructure(TreeNode root) {
        if (root != null) {
            log.info("Ready for algorithm execution on Root ID: " + root.getIdentifier());
        }
    }
}