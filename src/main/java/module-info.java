module com.yashgamerx.flcd {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires java.logging;

    opens com.yashgamerx.flcd to javafx.fxml;
    opens com.yashgamerx.flcd.selector to javafx.fxml, javafx.graphics;
    opens com.yashgamerx.flcd.flcd.view to javafx.fxml, javafx.graphics;
    opens com.yashgamerx.flcd.tmel.view to javafx.fxml, javafx.graphics;
    opens com.yashgamerx.flcd.cmel.view to javafx.fxml, javafx.graphics;

    exports com.yashgamerx.flcd;
    exports com.yashgamerx.flcd.common;
    exports com.yashgamerx.flcd.common.angular;
    exports com.yashgamerx.flcd.selector;
    exports com.yashgamerx.flcd.flcd.model;
    exports com.yashgamerx.flcd.flcd.algorithm;
    exports com.yashgamerx.flcd.flcd.dimension;
    exports com.yashgamerx.flcd.flcd.engine;
    exports com.yashgamerx.flcd.flcd.file;
    exports com.yashgamerx.flcd.flcd.view;
    exports com.yashgamerx.flcd.tmel.model;
    exports com.yashgamerx.flcd.tmel.algorithm;
    exports com.yashgamerx.flcd.tmel.dimension;
    exports com.yashgamerx.flcd.tmel.engine;
    exports com.yashgamerx.flcd.tmel.file;
    exports com.yashgamerx.flcd.tmel.view;
    exports com.yashgamerx.flcd.cmel.model;
    exports com.yashgamerx.flcd.cmel.algorithm;
    exports com.yashgamerx.flcd.cmel.file;
    exports com.yashgamerx.flcd.cmel.view;
}