module com.yashgamerx.flcd {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires java.logging;

    opens com.yashgamerx.flcd to javafx.fxml;
    opens com.yashgamerx.flcd.view to javafx.fxml, javafx.graphics;
    exports com.yashgamerx.flcd;
}