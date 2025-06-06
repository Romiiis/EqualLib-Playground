package com.romiiis.equallib_playground.controllers;

import com.romiiis.equallib_playground.CacheUtil;
import com.romiiis.equallib_playground.components.ClassComboBox;
import com.romiiis.equallib_playground.components.listView.LoadObjectListView;
import com.romiiis.equallib_playground.components.treeView.MyTreeView;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import lombok.Setter;


/**
 * A controller for the LoadObject scene
 * <p>
 * This class is used to control the LoadObject scene. It is used to load an object from the cache and assign it to the tree view.
 * It also provides a filter to filter objects by class.
 *
 * @author Romiiis
 * @version 1.0
 */
public class LoadObjectController {


    /**
     * The tree view to assign the object to
     */
    @Setter
    private MyTreeView assignedTreeView;

    /**
     * The tree view in the scene
     */
    @FXML
    private MyTreeView treeView;

    /**
     * The list view to display objects
     */
    @FXML
    private LoadObjectListView objectListView;

    /**
     * The class filter combo box
     */
    @FXML
    private ClassComboBox classFilter;

    /**
     * The cancel button
     */
    @FXML
    Button cancelButton;


    /**
     * Initialize the scene
     */
    @FXML
    private void initialize() {

        treeView.setEditable(false);

        // Set the assigned tree view for the object list view
        objectListView.setAssignedTreeView(treeView);

        // Set the object list view for the class filter
        classFilter.setObjectListView(objectListView);

        // Add all classes to the class filter
        classFilter.getItems().addAll(CacheUtil.getInstance().getClasses(true));

        // Add all objects to the object list view
        objectListView.getItems().addAll(CacheUtil.getInstance().getAllObjects(null));
    }


    /**
     * Load the selected object
     * After clicking the load button, the selected object is loaded and assigned to the tree view
     */
    @FXML
    private void loadObject() {

        // Load the selected object and assign it to the tree view
        Object object = CacheUtil.getInstance().getObjectByName(objectListView.getSelectionModel().getSelectedItem(), true);

        assignedTreeView.setSelectedObject(object);

        // Take note that the tree view has been modified
        assignedTreeView.setModified(true);

        // Close the window
        close();
    }


    /**
     * Cancel the loading of the object
     * After clicking the cancel button, the window is closed without loading an object
     */
    @FXML
    private void cancel() {
        close();
    }

    /**
     * Close the window
     */
    private void close() {
        // Close the window without loading the object
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }


}
