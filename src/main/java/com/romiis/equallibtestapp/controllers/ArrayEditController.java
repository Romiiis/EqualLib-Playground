package com.romiis.equallibtestapp.controllers;

import com.romiis.equallibtestapp.CacheUtil;
import com.romiis.equallibtestapp.MainClass;
import com.romiis.equallibtestapp.components.common.ObjectReference;
import com.romiis.equallibtestapp.util.ReflectionUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DefaultStringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class ArrayEditController {

    /**
     * -- GETTER --
     * Returns the updated array after editing.
     */
    // The array to edit and its name.
    @Getter
    private Object array;
    private String arrayName;

    // For nested arrays, these hold the reference to the parent array and the index where this array is stored.
    private Object parentArray;
    private Integer parentIndex;

    @FXML
    private ListView<String> elementsList;
    @FXML
    private Label arrayNameLabel;
    @FXML
    private TextField lengthField;
    @FXML
    private Button changeLengthButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    // Local copy of array elements (as Object so that nested arrays and non-string types are preserved)
    private ObservableList<Object> observableElements;

    /*==================== Initialization ====================*/

    /**
     * Initializes a top-level editor with an array and a name.
     */
    public void setAssignedArray(Object array, String arrayName) {
        setAssignedArray(array, arrayName, null, -1);
    }

    /**
     * Initializes an editor for a nested array.
     *
     * @param array       the array to edit
     * @param arrayName   the name to display (e.g. "Nested Array at index 2")
     * @param parentArray the parent array containing this array
     * @param parentIndex the index within the parent array where this array is stored
     */
    public void setAssignedArray(Object array, String arrayName, Object parentArray, int parentIndex) {
        if (array == null || !array.getClass().isArray()) {
            throw new IllegalArgumentException("The provided object is not an array.");
        }
        this.array = array;
        this.arrayName = arrayName;
        this.parentArray = parentArray;
        this.parentIndex = parentIndex;
        init();
    }

    /**
     * Initializes the editor: loads the array elements, sets up the ListView,
     * configures cell factories based on element type, and wires UI controls.
     */
    private void init() {
        if (arrayNameLabel != null) {
            arrayNameLabel.setText(arrayName);
        }
        loadArrayIntoObservableElements();
        // Set the ListView items using plain string conversion (the custom cells add index info).
        elementsList.setItems(convertToStringList(observableElements));
        elementsList.setEditable(true);

        Class<?> elementType = array.getClass().getComponentType();
        setupListViewEditor(elementType);

        elementsList.setOnEditCommit(event -> {
            int index = event.getIndex();
            String newValue = event.getNewValue();
            boolean isCorrectFormat = ReflectionUtil.isCorrectFormat(newValue, elementType);
            if (!isCorrectFormat) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Format");
                alert.setHeaderText("Incorrect Format for index: " + index);
                alert.setContentText("The value \"" + newValue + "\" is not a valid " + elementType.getSimpleName());
                alert.showAndWait();
                return;
            }
            log.debug("Editing element at index {}: {}", index, newValue);
            Object converted = convertStringToElement(newValue, elementType);
            if (newValue.isEmpty()) {
                converted = null;
            }
            observableElements.set(index, converted);
            refreshListView();
        });

        lengthField.setText(String.valueOf(observableElements.size()));

        saveButton.setOnAction(e -> handleSaveAction());
        cancelButton.setOnAction(e -> handleCancelAction());
        changeLengthButton.setOnAction(e -> handleChangeLength());
    }


    /**
     * Loads the current array elements into the local observable list.
     */
    private void loadArrayIntoObservableElements() {
        observableElements = FXCollections.observableArrayList();
        int len = Array.getLength(array);
        for (int i = 0; i < len; i++) {
            Object element = Array.get(array, i);
            observableElements.add(element);
        }
    }

    /**
     * Converts the local observableElements into a list of Strings for display.
     * (This conversion does not include index info; the custom cell classes add that.)
     */
    private ObservableList<String> convertToStringList(ObservableList<Object> list) {
        ObservableList<String> strings = FXCollections.observableArrayList();
        for (Object o : list) {
            strings.add(o != null ? o.toString() : ObjectReference.NULL);
        }
        return strings;
    }

    /**
     * Refreshes the ListView display.
     */
    private void refreshListView() {
        elementsList.setItems(convertToStringList(observableElements));
    }

    /*==================== Custom Cell Classes ====================*/

    /**
     * A ComboBox cell that displays its index before the item.
     */
    private class IndexedComboBoxCell extends ComboBoxListCell<String> {
        public IndexedComboBoxCell(String ... options) {
            super(FXCollections.observableArrayList(options));
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(getIndex() + ": " + item);
            }
        }
    }

    /**
     * A TextField cell that displays its index before the item.
     */
    private class IndexedTextFieldCell extends TextFieldListCell<String> {
        public IndexedTextFieldCell() {
            super(new DefaultStringConverter());
        }

        @Override
        public void startEdit() {
            super.startEdit();
            // Get index of the item being edited.
            int index = getIndex();
            if (index < 0) return;
            TextField textField = new TextField(observableElements.get(index) != null ? observableElements.get(index).toString() : "");
            textField.setOnAction(event -> {
                commitEdit(textField.getText());
            });
            setGraphic(textField);
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(getIndex() + ": " + item);
            }
        }
    }

    /**
     * A generic ListCell that displays its index before the item.
     * Used for nested arrays (where editing is disabled).
     */
    private class IndexedListCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(getIndex() + ": " + item);
            }
        }
    }

    /*==================== Cell Factory Setup ====================*/

    /**
     * Configures the ListView cell factory based on the element type.
     * - For enums and booleans: uses an IndexedComboBoxCell.
     * - For nested arrays: uses an IndexedListCell with editing disabled (double-click opens nested editor).
     * - For primitives, wrappers, and Strings: uses an IndexedTextFieldCell.
     * - For generic Objects: uses an IndexedComboBoxCell with default options.
     */
    private void setupListViewEditor(Class<?> elementType) {
        if (elementType.isEnum()) {
            Object[] enumConstants = elementType.getEnumConstants();
            String[] options = Arrays.stream(enumConstants)
                    .map(Object::toString)
                    .toArray(String[]::new);
            elementsList.setCellFactory(lv -> {
                IndexedComboBoxCell cell = new IndexedComboBoxCell(options);
                // Start editing on single-click.
                cell.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !cell.isEmpty()) {
                        lv.edit(cell.getIndex());
                    }
                });
                return cell;
            });
        } else if (elementType.equals(boolean.class) || elementType.equals(Boolean.class)) {
            elementsList.setCellFactory(lv -> {
                IndexedComboBoxCell cell = new IndexedComboBoxCell("true", "false");
                cell.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !cell.isEmpty()) {
                        lv.edit(cell.getIndex());
                    }
                });
                return cell;
            });
        } else if (elementType.isArray()) {
            // For nested arrays, we use an IndexedListCell that is not editable.
            elementsList.setCellFactory(lv -> new IndexedListCell() {
                {
                    // Disable editing on single-click.
                    setEditable(false);
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2) {
                            int selectedIndex = getIndex();
                            if (selectedIndex < 0) return;
                            if (selectedIndex >= observableElements.size()) {
                                log.warn("Selected index is out of bounds.");
                                return;
                            }

                            Object nestedArray = observableElements.get(selectedIndex);
                            if (nestedArray == null || !nestedArray.getClass().isArray()) {
                                log.warn("Selected element is not a nested array.");
                                return;
                            }
                            try {
                                FXMLLoader loader = new FXMLLoader(MainClass.class.getResource(MainClass.ARRAY_EDIT_SCENE_FXML));
                                Parent root = loader.load();
                                ArrayEditController controller = loader.getController();
                                // Pass the nested array along with its name and parent info.
                                controller.setAssignedArray(nestedArray, arrayName + " [index " + selectedIndex + "]", array, selectedIndex);
                                Stage stage = new Stage();
                                stage.setTitle("Nested Array Editor");
                                stage.setScene(new Scene(root));
                                stage.initModality(Modality.APPLICATION_MODAL);
                                stage.showAndWait();
                                // After the nested editor is closed, update the local copy.
                                observableElements.set(selectedIndex, controller.getArray());
                                refreshListView();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(getIndex() + ": " + "[Nested Array]");
                    }
                }
            });
        } else if (ReflectionUtil.isWrapperOrString(elementType) || elementType.isPrimitive() || elementType.equals(String.class)) {
            elementsList.setCellFactory(lv -> {
                IndexedTextFieldCell cell = new IndexedTextFieldCell();
                cell.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !cell.isEmpty()) {
                        lv.edit(cell.getIndex());
                    }
                });
                return cell;
            });
        } else {
            // For generic Objects, use an IndexedComboBoxCell with default options.
            elementsList.setCellFactory(lv -> {
                IndexedComboBoxCell cell = new IndexedComboBoxCell(CacheUtil.getInstance().getObjectsFitNames(elementType).toArray(new String[0]));
                cell.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !cell.isEmpty()) {
                        lv.edit(cell.getIndex());
                    }
                });
                return cell;
            });
        }
    }

    /*==================== Conversion and Save ====================*/

    /**
     * Converts a String from the editor into an object of the correct type.
     * For enums, booleans, primitives, or wrappers, uses ReflectionUtil; otherwise, returns the String.
     */
    private Object convertStringToElement(String value, Class<?> elementType) {
        if (elementType.isEnum()) {
            Object[] enumConstants = elementType.getEnumConstants();
            for (Object constant : enumConstants) {
                if (constant.toString().equals(value))
                    return constant;
            }
            return null;
        } else if (elementType.equals(boolean.class) || elementType.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        } else if (ReflectionUtil.isWrapperOrString(elementType) || elementType.isPrimitive() || elementType.equals(String.class)) {
            return ReflectionUtil.convertStringToPrimitive(value, elementType);
        }
        return CacheUtil.getInstance().getObjectByName(value, true);
    }

    /**
     * Handles saving: builds a new array from the local copy and, if nested, updates the parent's array.
     */
    private void handleSaveAction() {
        int len = observableElements.size();
        Class<?> elementType = array.getClass().getComponentType();
        Object newArray = Array.newInstance(elementType, len);
        for (int i = 0; i < len; i++) {
            Object value = observableElements.get(i);
            if (!elementType.isArray()) {
                if (value != null && !value.getClass().equals(elementType)) {
                    value = convertStringToElement(value.toString() ,elementType);
                }

            }
            Array.set(newArray, i, value);
        }
        // Update our array reference.
        this.array = newArray;
        log.debug("Changes saved to array.");

        // If this is a nested array editor, update the parent's array at the correct index.
        if (parentArray != null && parentIndex != null && parentIndex >= 0) {
            Array.set(parentArray, parentIndex, newArray);
            log.debug("Nested array rewired into parent array at index {}.", parentIndex);
        }
        closeWindow();
    }

    /**
     * Cancels editing and reloads the original array.
     */
    private void handleCancelAction() {
        loadArrayIntoObservableElements();
        refreshListView();
        log.debug("Changes canceled; original array reloaded.");
        closeWindow();
    }

    /**
     * Handles changing the array's length.
     */
    private void handleChangeLength() {
        String text = lengthField.getText();
        if (text == null || text.isEmpty()) return;
        int newLength = Integer.parseInt(text);
        int currentLength = observableElements.size();
        if (newLength == currentLength) return;

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Change Array Length");
        alert.setHeaderText("Changing the array length will erase all elements.");
        alert.setContentText("Are you sure you want to proceed?");
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(okButton, cancelButtonType);

        alert.showAndWait().ifPresent(response -> {
            if (response == okButton) {
                changeLength(newLength);
                refreshListView();
            }
        });
    }

    /**
     * Adjusts the local copy's length. When increasing, new entries get default values.
     */
    private void changeLength(int newLength) {
        int currentLength = observableElements.size();
        Class<?> elementType = array.getClass().getComponentType();

        if (newLength == currentLength) return;

        if (newLength < 0) {
            log.warn("Invalid length: {}", newLength);
            return;
        }

        observableElements.clear();

        for (int i = 0; i < newLength; i++) {
            Object defaultValue;
            if (array.getClass().getComponentType().isArray()) {
                defaultValue = Array.newInstance(elementType.getComponentType(), 0);
            } else {
                defaultValue = ReflectionUtil.getDefaultValue(elementType);
            }
            observableElements.add(defaultValue);
        }



    }

    /**
     * Closes the current window.
     */
    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
