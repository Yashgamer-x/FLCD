package com.yashgamerx.flcd.flcd.file;

import com.yashgamerx.flcd.flcd.model.FLCDNode;
import lombok.extern.java.Log;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Log
public class TreeFileParsingService implements FileParsingService {

    /// Marker header line that switches parsing into "Named" mode.
    /// In this mode the root line is `<id> <name...>` and every other
    /// line is `<id> <parentId> <name...>`.
    private static final String NAMED_FORMAT_HEADER = "Named";

    @Override
    public Optional<Map<Integer, FLCDNode>> readAndParseIdentifiedTextFile(final File textFileToProcess) {
        // PRINCIPLE: Local State Isolation
        // We keep the map local so the service remains stateless and thread-safe.
        var nodeLookupMap = new HashMap<Integer, FLCDNode>();

        try (Stream<String> lineStream = Files.lines(textFileToProcess.toPath())) {
            List<String> lines = lineStream.filter(line -> !line.isBlank()).toList();

            if (lines.isEmpty()) {
                log.warning("Parsing completed, but the file contained no content.");
                return Optional.of(nodeLookupMap);
            }

            // Logic: A file that opens with the "Named" header uses the
            // <id> <parentId> <n> format (root is just <id> <n>).
            // Otherwise fall back to the legacy adjacency-list format.
            boolean isNamedFormat = lines.getFirst().trim().equalsIgnoreCase(NAMED_FORMAT_HEADER);
            List<String> dataLines = isNamedFormat ? lines.subList(1, lines.size()) : lines;

            dataLines.forEach(line -> {
                if (isNamedFormat) {
                    parseNamedLineIntoTree(line, nodeLookupMap);
                } else {
                    parseLineIntoTree(line, nodeLookupMap);
                }
            });

            // Logic: Your requirement stated 1 is always the root.
            var rootNode = nodeLookupMap.get(1);

            if (rootNode == null) {
                log.warning("Parsing completed, but Root (ID 1) was not found in the dataset.");
            }

            return Optional.of(nodeLookupMap);

        } catch (Exception exception) {
            log.severe("Parsing failed critically: " + exception.getMessage());
            return Optional.empty();
        }
    }

    /// Legacy adjacency-list format: `<parentId> <childId1> <childId2> ...`
    private void parseLineIntoTree(String line, HashMap<Integer, FLCDNode> nodeMap) {
        var parts = line.trim().split("\\s+");
        if (parts.length < 1) return;

        try {
            // PRINCIPLE: Identity Map Pattern
            // Ensuring every ID points to exactly one object instance.
            var parentId = Integer.parseInt(parts[0]);
            var parentNode = nodeMap.computeIfAbsent(parentId, FLCDNode::new);

            for (int i = 1; i < parts.length; i++) {
                var childId = Integer.parseInt(parts[i]);
                var childNode = nodeMap.computeIfAbsent(childId, FLCDNode::new);
                parentNode.addChild(childNode);
            }
        } catch (NumberFormatException e) {
            log.warning("Skipping malformed line (invalid numbers): " + line);
        }
    }

    /// "Named" format: root line is `<id> <name...>`, every other line is
    /// `<id> <parentId> <name...>`. Names may contain spaces (e.g. file paths
    /// like `C:\Program Files (x86)`), so once the numeric prefix is consumed
    /// the remainder of the line is taken verbatim as the name.
    private void parseNamedLineIntoTree(String line, HashMap<Integer, FLCDNode> nodeMap) {
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

            // PRINCIPLE: Identity Map Pattern
            // Ensuring every ID points to exactly one object instance.
            var node = nodeMap.computeIfAbsent(identifier, FLCDNode::new);

            if (identifier == 1) {
                // Root line: "<id> <name...>" — no parent to attach.
                node.setName(remainder);
                return;
            }

            // Child line: "<id> <parentId> <name...>"
            var secondSpaceIndex = remainder.indexOf(' ');
            if (secondSpaceIndex < 0) {
                log.warning("Skipping malformed named line (missing parent id or name): " + line);
                return;
            }

            var parentIdToken = remainder.substring(0, secondSpaceIndex);
            var name = remainder.substring(secondSpaceIndex + 1).trim();

            var parentId = Integer.parseInt(parentIdToken);
            var parentNode = nodeMap.computeIfAbsent(parentId, FLCDNode::new);

            node.setName(name);
            parentNode.addChild(node);
        } catch (NumberFormatException e) {
            log.warning("Skipping malformed named line (invalid numbers): " + line);
        }
    }
}
