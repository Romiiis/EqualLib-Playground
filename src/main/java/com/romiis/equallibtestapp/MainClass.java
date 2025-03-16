package com.romiis.equallibtestapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainClass extends Application {

    public final static String MAIN_SCENE_FXML = "mainScene/MainScene.fxml";
    public final static String LOAD_OBJECTS_SCENE_FXML = "loadObjectScene/LoadObjectScene.fxml";
    public final static String ARRAY_EDIT_SCENE_FXML = "arrayEditScene/ArrayEditScene.fxml";
    public final static String COLLECTION_EDIT_SCENE_FXML = "collectionEditScene/CollectionEditScene.fxml";
    public final static String MAP_EDIT_SCENE_FXML = "mapEditScene/MapEditScene.fxml";

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainClass.class.getResource(MAIN_SCENE_FXML));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 650);
        stage.setTitle("EqualLib Test App");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {

        launch();
    }


}