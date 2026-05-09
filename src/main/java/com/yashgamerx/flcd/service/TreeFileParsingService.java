package com.yashgamerx.flcd.service;

import com.yashgamerx.flcd.model.TreeNode;
import lombok.extern.java.Log;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;

@Log
public class TreeFileParsingService implements FileParsingService {

    @Override
    public Optional<TreeNode> readAndParseIdentifiedTextFile(final File textFileToProcess) {
        // PRINCIPLE: Local State Isolation
        // We keep the map local so the service remains stateless and thread-safe.
        var nodeLookupMap = new HashMap<Integer, TreeNode>();

        try (Stream<String> lineStream = Files.lines(textFileToProcess.toPath())) {
            lineStream.filter(line -> !line.isBlank())
                    .forEach(line -> parseLineIntoTree(line, nodeLookupMap));

            // Logic: Your requirement stated 1 is always the root.
            var rootNode = nodeLookupMap.get(1);

            if (rootNode == null) {
                log.warning("Parsing completed, but Root (ID 1) was not found in the dataset.");
            }

            return Optional.ofNullable(rootNode);

        } catch (Exception exception) {
            log.severe("Parsing failed critically: " + exception.getMessage());
            return Optional.empty();
        }
    }

    private void parseLineIntoTree(String line, HashMap<Integer, TreeNode> nodeMap) {
        var parts = line.trim().split("\\s+");
        if (parts.length < 1) return;

        try {
            // PRINCIPLE: Identity Map Pattern
            // Ensuring every ID points to exactly one object instance.
            var parentId = Integer.parseInt(parts[0]);
            var parentNode = nodeMap.computeIfAbsent(parentId, TreeNode::new);

            for (int i = 1; i < parts.length; i++) {
                var childId = Integer.parseInt(parts[i]);
                var childNode = nodeMap.computeIfAbsent(childId, TreeNode::new);
                parentNode.getChildren().add(childNode);
            }
        } catch (NumberFormatException e) {
            log.warning("Skipping malformed line (invalid numbers): " + line);
        }
    }
}