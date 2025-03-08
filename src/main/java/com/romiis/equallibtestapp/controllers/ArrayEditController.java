package com.romiis.equallibtestapp.controllers;

import com.romiis.equallibtestapp.components.common.ObjectReference;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;

@Slf4j
public class ArrayEditController {

    private ObjectReference assignedArray;

    @FXML
    private ListView<String> elementsList;

    @FXML
    Label arrayNameLabel;

    @FXML
    private TextField lengthField;

    @FXML
    private TextField indexField;

    @FXML
    private Button changeLengthButton;

    private Object editedArray;


    public void setAssignedArray(ObjectReference assignedArray) {
        this.assignedArray = assignedArray;
        this.editedArray = assignedArray.getInObject();
        initialize();
    }


    private void initialize() {
        arrayNameLabel.setText(assignedArray.getInObject().getClass().getComponentType() + " " + assignedArray.getField().getName());
        lengthField.setText(String.valueOf(Array.getLength(editedArray)));

        updateElementsList();

        changeLengthButton.setOnAction(event -> changeLengthButton());

        elementsList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox vBox = new VBox();
                    vBox.setAlignment(Pos.CENTER);
                    vBox.getChildren().add(new Label(item));
                    setGraphic(vBox);
                }
            }
        });

        // Double click to edit the element
        elementsList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                editElement();
            }
        });



    }

    private void editElement() {
        // TODO
    }



    private void changeLengthButton() {
        if (lengthField.getText().isEmpty()) {
            return;
        }

        if (Array.getLength(editedArray) == Integer.parseInt(lengthField.getText())) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Change Array Length");
        alert.setHeaderText("Changing the array length will erase all the elements.");
        alert.setContentText("Are you sure you want to proceed?");
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);


        ButtonType buttonTypeOK = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(buttonTypeOK, buttonTypeCancel);


        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == buttonTypeOK) {
                changeLength();
                updateElementsList();
            }
        });

    }

    private void changeLength() {
        editedArray = Array.newInstance(editedArray.getClass().getComponentType(), Integer.parseInt(lengthField.getText()));
    }


    private void updateElementsList() {
        elementsList.getItems().clear();
        for (int i = 0; i < Array.getLength(editedArray); i++) {
            Object element = Array.get(editedArray, i);
            if (element == null) {
                elementsList.getItems().add("null");
                continue;
            }
            elementsList.getItems().add(Array.get(editedArray, i).toString());
        }
    }


}
