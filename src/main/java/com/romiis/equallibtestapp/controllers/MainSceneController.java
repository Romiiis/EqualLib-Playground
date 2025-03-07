package com.romiis.equallibtestapp.controllers;

import com.romiis.core.EqualLib;
import com.romiis.core.EqualLibConfig;
import com.romiis.equallibtestapp.components.listView.MyListView;
import com.romiis.equallibtestapp.io.FileManager;
import com.romiis.equallibtestapp.util.DynamicCompiler;
import com.romiis.equallibtestapp.components.treeView.MyTreeView;
import com.romiis.equallibtestapp.util.JsonUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainSceneController {

    // --- UI Components ---
    @FXML
    private MyTreeView treeView1;
    @FXML
    private MyTreeView treeView2;



    @FXML
    private MyListView objectListView1;
    @FXML
    private MyListView objectListView2;



    @FXML
    private Label comparisonResult;



    @FXML
    private ListView<String> ignoredFieldsList;
    @FXML
    private TextField newIgnoredField;
    @FXML
    private Spinner<Integer> maxDepthSpinner;







    @FXML
    private Button serializeButton;

    @FXML
    private Button loadButton;


    // --- Initialization ---
    @FXML
    private void initialize() {
        initializeMaxDepthSpinner();
        initializeObjects();
        initializeCollectionsMapsRB();

        objectListView1.setAssignedTreeView(treeView1);
        objectListView2.setAssignedTreeView(treeView2);
    }

    private void initializeCollectionsMapsRB() {
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

    }



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
        EqualLibConfig config = new EqualLibConfig();
        config.setDebugMode(true);

        comparisonResult.setText(EqualLib.areEqual(treeView1.getSelectedObject(), treeView2.getSelectedObject(), config) ? "Objects are equal" : "Objects are not equal");

    }


    @FXML
    public void onSerializeButtonClick() throws Exception {
        treeView1.save();
    }

    @FXML
    public void onLoadButtonClick() {
        treeView2.load();


    }

}
