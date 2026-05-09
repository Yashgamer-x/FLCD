package com.yashgamerx.flcd.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Log
public class AlgorithmSelectorView extends BorderPane {

    @FXML private Button selectTextFileButton;
    @FXML private Label selectedFileNameLabel;
    @FXML private Button processAlgorithmButton;

    private File currentlySelectedTextFile;

    public AlgorithmSelectorView() {
        loadFXML();
    }

    private void loadFXML(){
        var fxmlLoader = new FXMLLoader(getClass().getResource("/com/yashgamerx/flcd/algorithm-selector-view.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
            log.info("AlgorithmSelectorView successfully initialized via fx:root.");
        } catch (IOException exception) {
            log.severe("Failed to load FXML for AlgorithmSelectorView: " + exception.getMessage());
            throw new RuntimeException("Initialization of AlgorithmSelectorView failed.", exception);
        }
    }

    @FXML
    private void initialize(){
        selectTextFileButton.setOnAction(_->onSelectFileButtonClicked());
        processAlgorithmButton.setOnAction(_->onProcessAlgorithmButtonClicked());
    }

    private void onSelectFileButtonClicked() {
        var textFileChooser = new FileChooser();
        textFileChooser.setTitle("Open Source Text File");

        var textFileExtensionFilter = new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt");
        textFileChooser.getExtensionFilters().add(textFileExtensionFilter);

        var parentStageWindow = selectTextFileButton.getScene().getWindow();
        var selectedFileResult = textFileChooser.showOpenDialog(parentStageWindow);

        Optional.ofNullable(selectedFileResult).ifPresentOrElse(
                this::updateUserInterfaceWithSelectedFile,
                () -> log.info("File selection was cancelled by the user.")
        );
    }

    private void updateUserInterfaceWithSelectedFile(File newlySelectedFile) {
        this.currentlySelectedTextFile = newlySelectedFile;

        selectedFileNameLabel.setText("Active File: " + newlySelectedFile.getName());
        selectedFileNameLabel.setStyle("-fx-text-fill: #000000; -fx-font-weight: bold;");

        processAlgorithmButton.setDisable(false);
        log.info("Successfully targeted file: " + newlySelectedFile.getAbsolutePath());
    }


    private void onProcessAlgorithmButtonClicked() {
        if (currentlySelectedTextFile != null) {
            log.info("Initiating submission logic for: " + currentlySelectedTextFile.getName());
            executeFileProcessingAlgorithm();
        }
    }

    private void executeFileProcessingAlgorithm() {
        var absolutePath = currentlySelectedTextFile.getAbsolutePath();
        log.info("Algorithm execution started on path: " + absolutePath);
    }
}