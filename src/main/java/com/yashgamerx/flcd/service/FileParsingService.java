package com.yashgamerx.flcd.service;

import java.io.File;

/// CONTRACT: Any implementation of this interface must be able to take a File and process its textual content.
public interface FileParsingService {
    void readAndParseIdentifiedTextFile(File textFileToProcess);
}