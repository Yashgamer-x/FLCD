package com.yashgamerx.flcd.rings.file;

import com.yashgamerx.flcd.rings.model.RingNode;
import lombok.extern.java.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/// File parsing for Yash's Ring-based radial layout ("Rings"). Faithful
/// port of the standalone Ring app's `RingFileParser` — only the
/// "Numbered" adjacency-list format is supported, exactly as in the
/// original (no "Named" format, unlike FLCD/TMEL/CMEL/RT's parsers).
///
/// The one adaptation is the return convention: the original threw
/// `UnknownParsingTechniqueException` on an unrecognized header, while
/// this app's other parsing services log and return `Optional.empty()`
/// so [com.yashgamerx.flcd.selector.AlgorithmSelectorView] can handle
/// every family uniformly via `parsingResult.ifPresentOrElse(...)`.
@Log
public class RingFileParsingService {

    /// Header line the original `RingFileParser` requires.
    private static final String NUMBERED_FORMAT_HEADER = "Numbered";

    public Optional<Map<Integer, RingNode>> readAndParseIdentifiedTextFile(final File textFileToProcess) {
        var nodeMap = new HashMap<Integer, RingNode>();

        try (BufferedReader bufferedLines = Files.newBufferedReader(textFileToProcess.toPath())) {
            var firstLine = bufferedLines.readLine();

            if (firstLine == null) {
                log.warning("Parsing completed, but the file contained no content.");
                return Optional.of(nodeMap);
            }

            if (!firstLine.equals(NUMBERED_FORMAT_HEADER)) {
                log.severe("The following reading technique has not been implemented yet: " + firstLine);
                return Optional.empty();
            }

            readNumberedFile(bufferedLines, nodeMap);

            if (!nodeMap.containsKey(1)) {
                log.warning("Parsing completed, but Root (ID 1) was not found in the dataset.");
            }

            return Optional.of(nodeMap);
        } catch (IOException | NumberFormatException exception) {
            log.severe("Parsing failed critically: " + exception.getMessage());
            return Optional.empty();
        }
    }

    /// Reads the lines, separates the parent and child IDs, and links the
    /// parent to each child: `<parentId> <childId1> <childId2> ...`
    private void readNumberedFile(BufferedReader bufferedLines, HashMap<Integer, RingNode> nodeMap) throws IOException {
        String line;
        while ((line = bufferedLines.readLine()) != null) {
            if (line.isBlank()) continue;

            var splitNodes = line.trim().split(" ");
            var parentNodeId = Integer.parseInt(splitNodes[0]);
            var parentNode = nodeMap.computeIfAbsent(parentNodeId, RingNode::new);

            for (int i = 1; i < splitNodes.length; i++) {
                var childNodeId = Integer.parseInt(splitNodes[i]);
                var childNode = nodeMap.computeIfAbsent(childNodeId, RingNode::new);
                parentNode.addChild(childNode);
            }
        }
    }
}
