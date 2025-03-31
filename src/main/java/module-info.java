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



    exports com.romiis.equallibtestapp.util;
    opens com.romiis.equallibtestapp to javafx.fxml;
    exports com.romiis.equallibtestapp;
    exports com.romiis.equallibtestapp.controllers;
    opens com.romiis.equallibtestapp.controllers to javafx.fxml;
    exports com.romiis.equallibtestapp.components;
    exports com.romiis.equallibtestapp.components.treeView;
    exports com.romiis.equallibtestapp.components.listView;

}