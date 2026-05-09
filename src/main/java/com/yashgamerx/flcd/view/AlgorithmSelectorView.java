package com.yashgamerx.flcd.view;

import com.yashgamerx.flcd.service.FileParsingService;
import com.yashgamerx.flcd.service.PlanarGridAlgorithm;
import com.yashgamerx.flcd.service.TreeFileParsingService;
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

    private final FileParsingService textFileParsingService = new TreeFileParsingService();

    public AlgorithmSelectorView() {
        loadFXML();
    }

    /// Loads `algorithm-selector-view.fxml`
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

    /// When the [selectTextFileButton] is clicked, this method will be invoked.
    ///
    /// This method allows the user to select only `.txt` files.
    /// Invokes [AlgorithmSelectorView#updateUserInterfaceWithSelectedFile] if a file is selected.
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

    /// Updates the user interface with the newly selected file.
    private void updateUserInterfaceWithSelectedFile(File newlySelectedFile) {
        this.currentlySelectedTextFile = newlySelectedFile;

        selectedFileNameLabel.setText("Active File: " + newlySelectedFile.getName());
        selectedFileNameLabel.setStyle("-fx-text-fill: #000000; -fx-font-weight: bold;");

        processAlgorithmButton.setDisable(false);
        log.info("Successfully targeted file: " + newlySelectedFile.getAbsolutePath());
    }


    /// When the button is clicked, it ensures that the [AlgorithmSelectorView#currentlySelectedTextFile] is not null
    /// and invokes [AlgorithmSelectorView#executeFileProcessingAlgorithm] if the file still exists.
    private void onProcessAlgorithmButtonClicked() {
        if (currentlySelectedTextFile != null) {
            log.info("Initiating submission logic for: " + currentlySelectedTextFile.getName());
            executeFileProcessingAlgorithm();
        }
    }

    /// Invokes the [FileParsingService] to process the selected file.
    private void executeFileProcessingAlgorithm() {
        log.info("Algorithm execution started.");

        // 1. Get the parsed result
        var parsingResult = textFileParsingService.readAndParseIdentifiedTextFile(currentlySelectedTextFile);

        parsingResult.ifPresentOrElse(root -> {
            // 2. Create the new View
            var visualizationView = new TreeVisualizationView(root, new PlanarGridAlgorithm());

            // 3. Swap the Root of the Scene
            // Since this class is currently the root of the Scene, we replace it.
            var currentScene = this.getScene();
            currentScene.setRoot(visualizationView);

            log.info("Transitioned to TreeVisualizationView.");
        }, () -> log.warning("Parsing failed; transition aborted."));
    }
}