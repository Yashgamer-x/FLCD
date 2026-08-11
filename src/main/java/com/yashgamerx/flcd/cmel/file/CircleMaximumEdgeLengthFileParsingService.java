package com.yashgamerx.flcd.cmel.file;

import com.yashgamerx.flcd.cmel.model.CircleMaximumEdgeLengthNode;
import lombok.extern.java.Log;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/// Parses the same source text formats as [TreeFileParsingService], but
/// builds a [CircleMaximumEdgeLengthNode] tree instead of an [com.yashgamerx.flcd.flcd.model.FLCDNode]
/// one — kept as its own class rather than a generic parser so each node
/// type's construction stays simple and type-specific.
@Log
public class CircleMaximumEdgeLengthFileParsingService {

    private static final String NAMED_FORMAT_HEADER = "Named";

    public Optional<Map<Integer, CircleMaximumEdgeLengthNode>> readAndParseIdentifiedTextFile(final File textFileToProcess) {
        var nodeLookupMap = new HashMap<Integer, CircleMaximumEdgeLengthNode>();

        try (Stream<String> lineStream = Files.lines(textFileToProcess.toPath())) {
            List<String> lines = lineStream.filter(line -> !line.isBlank()).toList();

            if (lines.isEmpty()) {
                log.warning("Parsing completed, but the file contained no content.");
                return Optional.of(nodeLookupMap);
            }

            boolean isNamedFormat = lines.getFirst().trim().equalsIgnoreCase(NAMED_FORMAT_HEADER);
            List<String> dataLines = isNamedFormat ? lines.subList(1, lines.size()) : lines;

            dataLines.forEach(line -> {
                if (isNamedFormat) {
                    parseNamedLineIntoTree(line, nodeLookupMap);
                } else {
                    parseLineIntoTree(line, nodeLookupMap);
                }
            });

            if (nodeLookupMap.get(1) == null) {
                log.warning("Parsing completed, but Root (ID 1) was not found in the dataset.");
            }

            return Optional.of(nodeLookupMap);

        } catch (Exception exception) {
            log.severe("Parsing failed critically: " + exception.getMessage());
            return Optional.empty();
        }
    }

    private void parseLineIntoTree(String line, HashMap<Integer, CircleMaximumEdgeLengthNode> nodeMap) {
        var parts = line.trim().split("\\s+");
        if (parts.length < 1) return;

        try {
            var parentId = Integer.parseInt(parts[0]);
            var parentNode = nodeMap.computeIfAbsent(parentId, CircleMaximumEdgeLengthNode::new);

            for (int i = 1; i < parts.length; i++) {
                var childId = Integer.parseInt(parts[i]);
                var childNode = nodeMap.computeIfAbsent(childId, CircleMaximumEdgeLengthNode::new);
                parentNode.addChild(childNode);
            }
        } catch (NumberFormatException e) {
            log.warning("Skipping malformed line (invalid numbers): " + line);
        }
    }

    private void parseNamedLineIntoTree(String line, HashMap<Integer, CircleMaximumEdgeLengthNode> nodeMap) {
        var trimmedLine = line.trim();
        var firstSpaceIndex = trimmedLine.indexOf(' ');

        if (firstSpaceIndex < 0) {
            log.warning("Skipping malformed named line (missing name): " + line);
            return;
        }

        var idToken = trimmedLine.substring(0, firstSpaceIndex);
        var remainder = trimmedLine.substring(firstSpaceIndex + 1).trim();

        try {
            var identifier = Integer.parseInt(idToken);
            var node = nodeMap.computeIfAbsent(identifier, CircleMaximumEdgeLengthNode::new);

            if (identifier == 1) {
                node.setName(remainder);
                return;
            }

            var secondSpaceIndex = remainder.indexOf(' ');
            if (secondSpaceIndex < 0) {
                log.warning("Skipping malformed named line (missing parent id or name): " + line);
                return;
            }

            var parentIdToken = remainder.substring(0, secondSpaceIndex);
            var name = remainder.substring(secondSpaceIndex + 1).trim();

            var parentId = Integer.parseInt(parentIdToken);
            var parentNode = nodeMap.computeIfAbsent(parentId, CircleMaximumEdgeLengthNode::new);

            node.setName(name);
            parentNode.addChild(node);
        } catch (NumberFormatException e) {
            log.warning("Skipping malformed named line (invalid numbers): " + line);
        }
    }
}
