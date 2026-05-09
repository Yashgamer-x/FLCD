package com.yashgamerx.flcd;

import com.yashgamerx.flcd.view.AlgorithmSelectorView;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        var algorithmSelectorView = new AlgorithmSelectorView();
        var mainScene = new Scene(algorithmSelectorView, 400, 300);

        stage.setTitle("Algorithm Selector Pro");
        stage.setScene(mainScene);
        stage.show();
    }
}
