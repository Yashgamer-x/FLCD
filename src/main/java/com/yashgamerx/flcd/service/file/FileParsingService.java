package com.yashgamerx.flcd.service.file;

import com.yashgamerx.flcd.model.AbstractNode;

import java.io.File;
import java.util.Map;
import java.util.Optional;

/// CONTRACT: Any implementation of this interface must be able to take a File and process its textual content.
public interface FileParsingService {
    Optional<Map<Integer, AbstractNode>> readAndParseIdentifiedTextFile(File textFileToProcess);
}