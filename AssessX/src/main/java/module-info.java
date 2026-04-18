module com.assessx.assessx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.kordamp.ikonli.javafx;
    requires com.google.gson;
    requires java.net.http;
    requires org.kordamp.ikonli.fontawesome5;

    opens com.assessx.assessx to javafx.fxml;
    opens com.assessx.assessx.controller to javafx.fxml;
    opens com.assessx.assessx.controller.pages to javafx.fxml;
    opens com.assessx.assessx.dto to javafx.fxml, com.google.gson;
    opens com.assessx.assessx.session to javafx.fxml;
    opens com.assessx.assessx.controller.dialogs to javafx.fxml;

    exports com.assessx.assessx;
    exports com.assessx.assessx.api;
    exports com.assessx.assessx.session;
}
