package com.yashgamerx.flcd.view;

import com.yashgamerx.flcd.service.algorithm.AlgorithmFamily;
import com.yashgamerx.flcd.service.algorithm.MaximumEdgeLengthAlgorithm;
import com.yashgamerx.flcd.service.algorithm.PlanarGridAlgorithm;
import com.yashgamerx.flcd.service.file.FileParsingService;
import com.yashgamerx.flcd.service.file.MaximumEdgeLengthFileParsingService;
import com.yashgamerx.flcd.service.file.TreeFileParsingService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
    @FXML
    private ComboBox<AlgorithmFamily> algorithmComboBox;

    private final FileParsingService flcdFileParsingService = new TreeFileParsingService();
    private final MaximumEdgeLengthFileParsingService maximumEdgeLengthFileParsingService = new MaximumEdgeLengthFileParsingService();
    private File currentlySelectedTextFile;

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

        algorithmComboBox.getItems().setAll(AlgorithmFamily.values());
        algorithmComboBox.getSelectionModel().selectFirst();
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
    /// and dispatches to the parsing/view pipeline matching the selected [AlgorithmFamily].
    private void onProcessAlgorithmButtonClicked() {
        if (currentlySelectedTextFile == null) return;

        log.info("Initiating submission logic for: " + currentlySelectedTextFile.getName());

        switch (algorithmComboBox.getValue()) {
            case FLCD -> runFlcdPipeline();
            case MAXIMUM_EDGE_LENGTH -> runMaximumEdgeLengthPipeline();
        }
    }

    /// Parses the selected file into an `FLCDNode` tree and transitions to
    /// [FLCDTreeVisualizationView] using [PlanarGridAlgorithm].
    private void runFlcdPipeline() {
        var parsingResult = flcdFileParsingService.readAndParseIdentifiedTextFile(currentlySelectedTextFile);

        parsingResult.ifPresentOrElse(map -> {
            var visualizationView = new FLCDTreeVisualizationView(map, new PlanarGridAlgorithm());

            var currentScene = this.getScene();
            currentScene.setRoot(visualizationView);
            log.info("Transitioned to FLCDTreeVisualizationView.");
        }, () -> log.warning("Parsing failed; transition aborted."));
    }

    /// Parses the selected file into a `MaximumEdgeLengthNode` tree and
    /// transitions to [MaximumEdgeLengthVisualizationView] using
    /// [MaximumEdgeLengthAlgorithm]. A distinct pipeline from
    /// [#runFlcdPipeline] — different node type, different algorithm,
    /// different (button-free) view.
    private void runMaximumEdgeLengthPipeline() {
        var parsingResult = maximumEdgeLengthFileParsingService.readAndParseIdentifiedTextFile(currentlySelectedTextFile);

        parsingResult.ifPresentOrElse(map -> {
            var visualizationView = new MaximumEdgeLengthVisualizationView(map, new MaximumEdgeLengthAlgorithm());

            var currentScene = this.getScene();
            currentScene.setRoot(visualizationView);
            log.info("Transitioned to MaximumEdgeLengthVisualizationView.");
        }, () -> log.warning("Parsing failed; transition aborted."));
    }
}
