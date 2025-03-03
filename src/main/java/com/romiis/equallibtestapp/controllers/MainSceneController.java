package com.romiis.equallibtestapp.controllers;

import com.romiis.equallibtestapp.MainClass;
import com.romiis.equallibtestapp.util.DynamicCompiler;
import com.romiis.equallibtestapp.util.TreeViewFiller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;

public class MainSceneController {

    // --- UI Components ---
    @FXML private TreeView<String> treeView1;
    @FXML private TreeView<String> treeView2;
    @FXML private ListView<String> objectListView1;
    @FXML private ListView<String> objectListView2;
    @FXML private Label comparisonResult;
    @FXML private ListView<String> ignoredFieldsList;
    @FXML private TextField newIgnoredField;
    @FXML private Spinner<Integer> maxDepthSpinner;

    // --- Object references ---
    private Object selectedObject1;
    private Object selectedObject2;

    // --- Initialization ---
    @FXML
    private void initialize() {
        initializeMaxDepthSpinner();
        initializeObjects();


    }

    // Initialize the spinner with appropriate value factory
    private void initializeMaxDepthSpinner() {
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(-1, 100, -1); // Min: -1, Max: 100, Default: -1
        maxDepthSpinner.setValueFactory(valueFactory);
    }

    private void initializeObjects() {
        try {
            DynamicCompiler.compile("objects");
            objectListView1.getItems().addAll(DynamicCompiler.getAllCompiledObjects());
            objectListView2.getItems().addAll(DynamicCompiler.getAllCompiledObjects());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        objectListView1.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            TreeViewFiller.handleSelectionChange(newValue, treeView1);
        });

        objectListView2.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            TreeViewFiller.handleSelectionChange(newValue, treeView2);
        });
    }



    // --- Ignored Fields ---
    @FXML
    private void onAddIgnoredField(ActionEvent event) {
        String ignoredField = newIgnoredField.getText().trim();
        if (!ignoredField.isEmpty()) {
            addIgnoredFieldToList(ignoredField);
        }
    }

    private void addIgnoredFieldToList(String ignoredField) {
        ignoredFieldsList.getItems().add(ignoredField);
        newIgnoredField.clear(); // Clear the text field after adding
    }

    @FXML
    private void onRemoveIgnoredField(ActionEvent event) {
        String selectedField = ignoredFieldsList.getSelectionModel().getSelectedItem();
        if (selectedField != null) {
            removeIgnoredFieldFromList(selectedField);
        }
    }

    private void removeIgnoredFieldFromList(String selectedField) {
        ignoredFieldsList.getItems().remove(selectedField);
    }


    // Method to add an object to the TreeView
    public void addObjectToTreeView(String selectedObject, TreeView<String> treeView) {
        if (selectedObject != null) {
            treeView.getRoot().getChildren().add(new TreeItem<>(selectedObject));
        }
    }

    // --- Comparison ---
    @FXML
    public void onCompareButtonClick() {
        comparisonResult.setText("Comparing TreeViews...");
        // Logic to compare treeView1 and treeView2 will go here
    }

    // --- Settings (Placeholder for future functionality) ---
    @FXML
    public void onSettingsOption1() {
        // Handle setting option 1
    }

    @FXML
    public void onSettingsOption2() {
        // Handle setting option 2
    }

}
