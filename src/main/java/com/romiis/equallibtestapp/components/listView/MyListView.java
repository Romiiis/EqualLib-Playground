package com.romiis.equallibtestapp.components.listView;

import com.romiis.equallibtestapp.components.treeView.MyTreeView;
import com.romiis.equallibtestapp.io.FileManager;
import com.romiis.equallibtestapp.util.JsonUtil;
import javafx.scene.control.*;
import lombok.Setter;

import java.util.Optional;


public class MyListView extends ListView<Class<?>> {

    @Setter
    private MyTreeView assignedTreeView;


    private final String SAVE_BUTTON_TEXT = "Save";
    private final String DISCARD_BUTTON_TEXT = "Discard";
    private final String CANCEL_BUTTON_TEXT = "Cancel";


    /**
     * Create a new MyListView instance
     */
    public MyListView() {
        super();

        setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Class item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getSimpleName());
                }
            }
        });

        initializeClickHandler();
    }


    private void initializeClickHandler() {
        this.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

            // If the selection is the same, do nothing
            if (this.assignedTreeView.getSelectedObject() != null
                    && this.assignedTreeView.getSelectedObject().getClass().equals(newValue)) return;

            // If the tree view is modified, show an alert
            if (assignedTreeView.isModified()) {
                showUnsavedChangesAlert(newValue, oldValue);
            } else {
                assignedTreeView.setSelectedObject(newValue);
            }
        });
    }



    /**
     * Show an alert if there are unsaved changes (dirty flag)
     *
     * @param newValue The new value to select
     * @param oldValue The old value to select
     */
    private void showUnsavedChangesAlert(Class<?> newValue, Class<?> oldValue) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Unsaved changes");
        alert.setContentText("There are unsaved changes in the current object. Do you want to save them?");

        ButtonType saveButton = new ButtonType(SAVE_BUTTON_TEXT);
        ButtonType discardButton = new ButtonType(DISCARD_BUTTON_TEXT);
        ButtonType cancelButton = new ButtonType(CANCEL_BUTTON_TEXT, ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveButton, discardButton, cancelButton);

        alert.showAndWait().ifPresent(buttonType -> handleAlertResponse(buttonType, newValue, oldValue));
    }

    /**
     * Handle the response from the alert
     *
     * @param buttonType The button type that was clicked
     * @param newValue   The new value to select
     * @param oldValue   The old value to select
     */
    private void handleAlertResponse(ButtonType buttonType, Class<?> newValue, Class<?> oldValue) {
        if (buttonType.getText().equals(SAVE_BUTTON_TEXT)) {
            boolean saved = assignedTreeView.save();
            if (saved) {
                assignedTreeView.setSelectedObject(newValue);
            }
        } else if (buttonType.getText().equals(DISCARD_BUTTON_TEXT)) {
            assignedTreeView.setSelectedObject(newValue);

        } else if (buttonType.getText().equals(CANCEL_BUTTON_TEXT)) {
            this.getSelectionModel().select(oldValue);
        }
    }




}
