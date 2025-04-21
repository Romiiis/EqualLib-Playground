module com.romiis.equallibtestapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires EqualLib;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires static lombok;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires jdk.unsupported;
    requires jdk.compiler;
    requires java.desktop;



    exports com.romiis.equallib_playground.util;
    opens com.romiis.equallib_playground to javafx.fxml;
    exports com.romiis.equallib_playground;
    exports com.romiis.equallib_playground.controllers;
    opens com.romiis.equallib_playground.controllers to javafx.fxml;
    exports com.romiis.equallib_playground.components;
    exports com.romiis.equallib_playground.components.treeView;
    exports com.romiis.equallib_playground.components.listView;

}