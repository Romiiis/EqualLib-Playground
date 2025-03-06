package com.romiis.equallibtestapp.components.listView;

import com.romiis.equallibtestapp.components.treeView.MyTreeView;
import javafx.scene.control.*;
import lombok.Setter;

public class MyListView extends ListView<Class<?>> {

    // Nastavovací metoda pro přiřazení MyTreeView
    @Setter
    private MyTreeView assignedTreeView;


    private final String SAVE_BUTTON_TEXT = "Save";
    private final String DISCARD_BUTTON_TEXT = "Discard";
    private final String CANCEL_BUTTON_TEXT = "Cancel";


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
            // Pokud je nový výběr stejný jako aktuálně vybraný objekt, nic nedělej
            if (isSameSelection(newValue)) {
                return;
            }

            // Pokud jsou neuložené změny, zobrazí se potvrzovací alert
            if (assignedTreeView.isModified()) {
                showUnsavedChangesAlert(newValue, oldValue);
            } else {
                handleSelectionChange(newValue);
            }
        });


    }

    private boolean isSameSelection(Class<?> newValue) {

        return this.assignedTreeView.getSelectedObject() != null && this.assignedTreeView.getSelectedObject().getClass().equals(newValue);
    }

    // Metoda pro zobrazení alertu při neuložených změnách
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

    // Metoda pro zpracování reakce na potvrzovací alert
    private void handleAlertResponse(ButtonType buttonType, Class<?> newValue, Class<?> oldValue) {
        if (buttonType.getText().equals(SAVE_BUTTON_TEXT)) {

            // TODO: Implement the save logic
        } else if (buttonType.getText().equals(DISCARD_BUTTON_TEXT)) {
            assignedTreeView.setSelectedObject(newValue);

        } else if (buttonType.getText().equals(CANCEL_BUTTON_TEXT)) {
            this.getSelectionModel().select(oldValue);
        }
    }


    // Metoda pro zpracování výběru bez neuložených změn
    private void handleSelectionChange(Class<?> newValue) {
        assignedTreeView.setSelectedObject(newValue);

    }

}
