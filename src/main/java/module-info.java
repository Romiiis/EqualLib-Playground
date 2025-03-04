module com.romiis.equallibtestapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires java.desktop;
    requires java.compiler;
    requires org.objenesis;

    exports com.romiis.equallibtestapp.util;
    opens com.romiis.equallibtestapp to javafx.fxml;
    exports com.romiis.equallibtestapp;
    exports com.romiis.equallibtestapp.controllers;
    opens com.romiis.equallibtestapp.controllers to javafx.fxml;
}