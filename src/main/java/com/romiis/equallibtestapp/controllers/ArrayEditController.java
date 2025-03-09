package com.romiis.equallibtestapp.controllers;

import com.romiis.equallibtestapp.components.common.ObjectReference;
import com.romiis.equallibtestapp.util.ReflectionUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.converter.DefaultStringConverter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.util.Arrays;

@Slf4j
public class ArrayEditController {

    private ObjectReference assignedArray;

    @FXML
    private ListView<String> elementsList;

    @FXML
    private Label arrayNameLabel;

    @FXML
    private TextField lengthField;

    @FXML
    private TextField indexField;

    @FXML
    private Button changeLengthButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    // Local copy of array elements as strings (for editing purposes)
    private ObservableList<String> observableElements;

    public void setAssignedArray(ObjectReference assignedArray) {
        this.assignedArray = assignedArray;
        init();
    }

    public void init() {
        // Load the original array into the observableElements list.
        loadArrayIntoObservableElements();
        elementsList.setItems(observableElements);

        // Set up the ListView for editing.
        elementsList.setEditable(true);

        // Determine the array element type from the assigned array.
        Class<?> fieldType = assignedArray.getInObject().getClass().getComponentType();

        // Set up the ListView editor based on the field type.
        setUpListViewEditor(elementsList, fieldType, observableElements);


        // When an element is edited, update the ObservableList only.
        elementsList.setOnEditCommit(event -> {
            int index = event.getIndex();
            String newValue = event.getNewValue();
            log.debug("Local update at index {} to: {}", index, newValue);
            observableElements.set(index, newValue);
        });

        lengthField.setText(String.valueOf(observableElements.size()));

        saveButton.setOnAction(event -> handleSaveAction());

        cancelButton.setOnAction(event -> handleCancelAction());

        changeLengthButton.setOnAction(event -> changeLengthButton());



    }

    public void setUpListViewEditor(ListView<String> listView, Class<?> fieldType, ObservableList<String> observableElements) {
        if (fieldType.isEnum()) {
            // Convert enum constants to strings.
            Object[] enumConstants = fieldType.getEnumConstants();
            String[] options = Arrays.stream(enumConstants)
                    .map(Object::toString)
                    .toArray(String[]::new);
            listView.setCellFactory(ComboBoxListCell.forListView(options));
            listView.setOnEditCommit(event -> {
                int index = event.getIndex();
                String newValue = event.getNewValue();
                log.debug("Enum update at index {} to: {}", index, newValue);
                observableElements.set(index, newValue);
            });
        } else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
            listView.setCellFactory(ComboBoxListCell.forListView("true", "false"));
            listView.setOnEditCommit(event -> {
                int index = event.getIndex();
                String newValue = event.getNewValue();
                log.debug("Boolean update at index {} to: {}", index, newValue);
                observableElements.set(index, newValue);
            });
        } else if (fieldType.equals(Object.class)) {
            // For generic Object type, we supply some default options.
            listView.setCellFactory(ComboBoxListCell.forListView("Option1", "Option2", "Option3"));
            listView.setOnEditCommit(event -> {
                int index = event.getIndex();
                String newValue = event.getNewValue();
                log.debug("Object update at index {} to: {}", index, newValue);
                observableElements.set(index, newValue);
            });
        } else {
            // Otherwise use a TextField for editing.
            listView.setCellFactory(TextFieldListCell.forListView(new DefaultStringConverter()));
            listView.setOnEditCommit(event -> {
                int index = event.getIndex();
                String newValue = event.getNewValue();
                log.debug("Text update at index {} to: {}", index, newValue);
                observableElements.set(index, newValue);
            });
        }
    }

    /**
     * Loads the current array values into the observableElements list.
     */
    private void loadArrayIntoObservableElements() {
        Object array = assignedArray.getInObject();
        observableElements = FXCollections.observableArrayList();
        int len = Array.getLength(array);
        for (int i = 0; i < len; i++) {
            Object element = Array.get(array, i);
            observableElements.add(element != null ? element.toString() : "null");
        }
    }

    /**
     * Called when the Save button is clicked.
     * Copies the edited values from observableElements back to the underlying array.
     */
    @FXML
    private void handleSaveAction() {
        int len = elementsList.getItems().size();

        // Create a new array of the same type and length as the original.;
        Object array = Array.newInstance(assignedArray.getField().getType().getComponentType(), len);

        for (int i = 0; i < len; i++) {
            // Here we assume the array elements are strings.
            // If conversion is needed for another type, add conversion logic.
            String value = observableElements.get(i);
            Array.set(array, i, ReflectionUtil.convertStringToPrimitive(value, array.getClass().getComponentType()));
        }

        // Set the new array to the object.
        assignedArray.setInObject(array);
        log.debug("Changes saved to assignedArray.");
        // Optionally, notify the user or update other UI elements.
        closeWindow();
    }

    /**
     * Called when the Cancel button is clicked.
     * Reloads the observableElements list from the original array,
     * discarding any unsaved edits.
     */
    @FXML
    private void handleCancelAction() {
        loadArrayIntoObservableElements();
        elementsList.setItems(observableElements);
        log.debug("Changes canceled; reloaded original array values.");
        closeWindow();

    }


    private void closeWindow() {
        // Close the window or dialog.
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }


    private void changeLengthButton() {
        if (lengthField.getText().isEmpty()) {
            return;
        }

        int newLength = Integer.parseInt(lengthField.getText());
        int currentLength = observableElements.size();
        if (currentLength == newLength) {
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
                changeLength(newLength);
                updateElementsList();
            }
        });
    }

    private void changeLength(int newLength) {
        int currentLength = observableElements.size();
        // If the new length is greater, add new entries (here using "null" as a placeholder).
        if (newLength > currentLength) {
            for (int i = currentLength; i < newLength; i++) {
                Object defaultValue = ReflectionUtil.getDefaultValue(assignedArray.getField().getType().getComponentType());
                if (defaultValue != null) {
                    observableElements.add(defaultValue.toString());
                } else {
                    observableElements.add("null");
                }
            }
        } else if (newLength < currentLength) {
            // If the new length is smaller, remove extra elements.
            observableElements.remove(newLength, currentLength);
        }
        // Note: No changes are made to assignedArray until save is called.
    }

    private void updateElementsList() {
        // Because the ListView is bound to observableElements, you can force a refresh if needed:
        elementsList.setItems(null);
        elementsList.setItems(observableElements);
    }




}
