package com.yashgamerx.flcd.service.file;

import com.yashgamerx.flcd.model.FLCDNode;
import lombok.extern.java.Log;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Log
public class StandardTextFileParsingService implements FileParsingService {

    @Override
    public Optional<Map<Integer, FLCDNode>> readAndParseIdentifiedTextFile(final File textFileToProcess) {
        log.info("Executing standard parsing logic...");
        var path = textFileToProcess.toPath();

        try (Stream<String> lines = Files.lines(path)) {
            var content = lines.filter(line -> !line.isBlank()).toList();
            log.info("Parsed " + content.size() + " lines.");
        } catch (Exception e) {
            log.severe("Error: " + e.getMessage());
        }
        return Optional.empty();
    }
}
