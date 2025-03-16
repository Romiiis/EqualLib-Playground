module com.romiis.equallibtestapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires EqualLib;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires static lombok;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires jdk.unsupported;
    requires jdk.compiler;
    requires java.sql;
    requires java.desktop;


    exports com.romiis.equallibtestapp.util;
    opens com.romiis.equallibtestapp to javafx.fxml;
    exports com.romiis.equallibtestapp;
    exports com.romiis.equallibtestapp.controllers;
    opens com.romiis.equallibtestapp.controllers to javafx.fxml;
    exports com.romiis.equallibtestapp.components.common;
    exports com.romiis.equallibtestapp.components.mainScene;
    exports com.romiis.equallibtestapp.components.loadObjectScene;

}