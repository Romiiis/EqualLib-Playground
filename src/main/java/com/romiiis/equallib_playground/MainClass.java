package com.romiiis.equallib_playground;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;

/**
 * MainClass.java
 * <p>
 * The main class of the application. Starts the JavaFX application.
 *
 * @author Romiiis
 * @version 1.0
 */
public class MainClass extends Application {

    /**
     * FXML files
     */

    /**
     * The path to the main scene FXML file
     */
    public final static String MAIN_SCENE_FXML = "mainScene/MainScene.fxml";

    /**
     * The path to the load object scene FXML file
     */
    public final static String LOAD_OBJECTS_SCENE_FXML = "loadObjectScene/LoadObjectScene.fxml";

    /**
     * The path to the save object scene FXML file
     */
    public final static String ARRAY_EDIT_SCENE_FXML = "arrayEditScene/ArrayEditScene.fxml";

    /**
     * The path to the save object scene FXML file
     */

    public final static String COLLECTION_EDIT_SCENE_FXML = "collectionEditScene/CollectionEditScene.fxml";

    /**
     * The path to the save object scene FXML file
     */
    public final static String MAP_EDIT_SCENE_FXML = "mapEditScene/MapEditScene.fxml";

    /**
     * Start the application
     *
     * @param stage stage
     * @throws IOException if the FXML file cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        Locale.setDefault(new Locale("cs", "CZ")); // sets decimal separator to comma
        FXMLLoader fxmlLoader = new FXMLLoader(MainClass.class.getResource(MAIN_SCENE_FXML));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 650);
        stage.getIcons().add(new Image(this.getClass().getResourceAsStream("/EqualLib-PlaygroundIcon-noBgr.png")));

        stage.setTitle("EqualLib Test App");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * The main method of the application
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch();
    }


}