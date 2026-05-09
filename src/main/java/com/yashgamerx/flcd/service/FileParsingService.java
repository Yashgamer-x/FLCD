package com.yashgamerx.flcd.service;

import com.yashgamerx.flcd.model.TreeNode;

import java.io.File;
import java.util.Optional;

/// CONTRACT: Any implementation of this interface must be able to take a File and process its textual content.
public interface FileParsingService {
    Optional<TreeNode> readAndParseIdentifiedTextFile(File textFileToProcess);
}