package com.romiis.equallibtestapp.controllers;

import com.romiis.core.EqualLib;
import com.romiis.core.EqualLibConfig;
import com.romiis.equallibtestapp.CacheUtil;
import com.romiis.equallibtestapp.components.mainScene.ClassListView;
import com.romiis.equallibtestapp.components.common.MyTreeView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MainSceneController {

    // --- UI Components ---
    @FXML
    private MyTreeView treeView1;
    @FXML
    private MyTreeView treeView2;


    @FXML
    private ClassListView objectListView1;
    @FXML
    private ClassListView objectListView2;


    @FXML
    private Label comparisonResult;


    @FXML
    private ListView<String> ignoredFieldsList;
    @FXML
    private TextField newIgnoredField;
    @FXML
    private Spinner<Integer> maxDepthSpinner;

    // --- Initialization ---
    @FXML
    private void initialize() {

        CacheUtil.getInstance().updateCache();

        initializeMaxDepthSpinner();
        initializeObjects();

        objectListView1.setAssignedTreeView(treeView1);
        objectListView2.setAssignedTreeView(treeView2);

        treeView2.setClassListView(objectListView2);
        treeView1.setClassListView(objectListView1);

    }


    // Initialize the spinner with appropriate value factory
    private void initializeMaxDepthSpinner() {
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(-1, 100, -1); // Min: -1, Max: 100, Default: -1
        maxDepthSpinner.setValueFactory(valueFactory);

    }

    private void initializeObjects() {
        objectListView1.getItems().addAll(CacheUtil.getInstance().getClasses(true));
        objectListView2.getItems().addAll(CacheUtil.getInstance().getClasses(true));
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


    // --- Comparison ---
    @FXML
    public void onCompareButtonClick() {
        EqualLibConfig config = new EqualLibConfig();
        config.setDebugMode(true);

        comparisonResult.setText(EqualLib.areEqual(treeView1.getSelectedObject(), treeView2.getSelectedObject(), config) ? "Objects are equal" : "Objects are not equal");

    }


    @FXML
    public void onSaveAsButton1Click() throws Exception {
        treeView1.save();
    }

    @FXML
    public void onSaveAsButton2Click() throws Exception {
        treeView2.save();
    }

    @FXML
    public void onLoadButton1Click() {
        treeView1.load();
    }

    @FXML
    public void onLoadButton2Click() {
        treeView2.load();
    }



    @FXML
    public void onLoadButtonClick() {
        // LoadObjectSceneController.loadObject(treeView1);
        treeView1.load();



    }

}
