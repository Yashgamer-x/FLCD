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
        var algorithmSelector = new AlgorithmSelectorView();
        Scene scene = new Scene(algorithmSelector, 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }
}
