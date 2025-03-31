package com.romiis.equallibtestapp.controllers;

import com.romiis.equallibtestapp.CacheUtil;
import com.romiis.equallibtestapp.MainClass;
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
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.DefaultStringConverter;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Log4j2
public class CollectionEditController {

    @Getter
    private Collection<?> collection;
    private String collectionName;
    private Class<?> elementType;

    // Backup copy to restore original content on cancel.
    private List<Object> backupCollection;

    @FXML
    private ListView<String> elementsList;
    @FXML
    private Label collectionTitleLabel;
    @FXML
    private Button addButton;
    @FXML
    private Button removeButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    // Local copy of collection elements for editing.
    private ObservableList<Object> observableElements;

    /*==================== Initialization ====================*/

    /**
     * Sets up the editor with a collection, its name, and the expected element type.
     * The provided collection is preserved and updated upon saving.
     */
    public void setAssignedCollection(Collection<?> collection, String collectionName, Type elementType) {
        if (collection == null) {
            throw new IllegalArgumentException("Provided collection is null.");
        }
        this.collection = collection;
        // Backup the original collection.
        this.backupCollection = new ArrayList<>(collection);
        this.collectionName = collectionName;
        this.elementType = ReflectionUtil.getClassFromType(elementType);

        init();
    }


    /**
     * Initializes the editor: loads the collection elements, sets up the ListView,
     * configures cell factories based on element type, and wires UI controls.
     */
    private void init() {
        if (collectionName != null) {
            String title = String.format("%s<%s> %s",
                    collection.getClass().getSimpleName(),
                    elementType.getSimpleName(),
                    collectionName);
            collectionTitleLabel.setText(title);
        }
        loadCollectionIntoObservableElements();
        elementsList.setItems(convertToStringList(observableElements));
        elementsList.setEditable(true);

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

        addButton.setOnAction(e -> handleAddAction());
        removeButton.setOnAction(e -> handleRemoveAction());
        saveButton.setOnAction(e -> handleSaveAction());
        cancelButton.setOnAction(e -> handleCancelAction());
    }

    /**
     * Loads the collection's elements into a local observable list.
     */
    private void loadCollectionIntoObservableElements() {
        observableElements = FXCollections.observableArrayList();
        observableElements.addAll(collection);
    }

    /**
     * Converts the observable elements into a String list for display.
     */
    private ObservableList<String> convertToStringList(ObservableList<Object> list) {
        ObservableList<String> strings = FXCollections.observableArrayList();
        for (Object o : list) {
            strings.add(o != null ? o.toString() : "null");
        }
        return strings;
    }

    /**
     * Refreshes the ListView display.
     */
    private void refreshListView() {
        elementsList.setItems(convertToStringList(observableElements));
    }

    /*==================== Cell Factory Setup ====================*/

    /**
     * Configures the ListView cell factory based on the element type.
     */
    private void setupListViewEditor(Class<?> elementType) {
        if (elementType.isEnum()) {
            Object[] enumConstants = elementType.getEnumConstants();
            String[] options = Arrays.stream(enumConstants)
                    .map(Object::toString)
                    .toArray(String[]::new);
            elementsList.setCellFactory(lv -> {
                IndexedComboBoxCell cell = new IndexedComboBoxCell(options);
                setupDragAndDrop(cell);
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
                setupDragAndDrop(cell);
                cell.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !cell.isEmpty()) {
                        lv.edit(cell.getIndex());
                    }
                });
                return cell;
            });
        } else if (elementType.isArray() || Collection.class.isAssignableFrom(elementType)) {
            // For nested arrays or collections, use a non-editable cell.
            elementsList.setCellFactory(lv -> new IndexedListCell() {
                {
                    setEditable(false);
                    setOnMouseClicked(event -> {
                        if (event.getClickCount() == 2) {
                            int selectedIndex = getIndex();
                            if (selectedIndex < 0 || selectedIndex >= observableElements.size()) return;
                            Object nested = observableElements.get(selectedIndex);
                            if (nested == null) {
                                log.warn("Selected element is null; cannot open nested editor.");
                                return;
                            }
                            try {
                                if (nested.getClass().isArray()) {
                                    FXMLLoader loader = new FXMLLoader(MainClass.class.getResource(MainClass.ARRAY_EDIT_SCENE_FXML));
                                    Parent root = loader.load();
                                    ArrayEditController controller = loader.getController();
                                    controller.setAssignedArray(nested, collectionName + " [index " + selectedIndex + "]", collection, selectedIndex);
                                    Stage stage = new Stage();
                                    stage.setTitle("Nested Array Editor");
                                    stage.setScene(new Scene(root));
                                    stage.initModality(Modality.APPLICATION_MODAL);
                                    stage.showAndWait();
                                    observableElements.set(selectedIndex, controller.getArray());
                                    refreshListView();
                                } else if (nested instanceof Collection) {
                                    FXMLLoader loader = new FXMLLoader(MainClass.class.getResource(MainClass.COLLECTION_EDIT_SCENE_FXML));
                                    Parent root = loader.load();
                                    CollectionEditController controller = loader.getController();
                                    controller.setAssignedCollection((Collection<?>) nested, collectionName + " [index " + selectedIndex + "]", Object.class);
                                    Stage stage = new Stage();
                                    stage.setTitle("Nested Collection Editor");
                                    stage.setScene(new Scene(root));
                                    stage.initModality(Modality.APPLICATION_MODAL);
                                    stage.showAndWait();
                                    observableElements.set(selectedIndex, controller.getCollection());
                                    refreshListView();
                                }
                            } catch (Exception ex) {
                                log.error("Error opening nested editor", ex);
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
                        String nestedType = elementType.isArray() ? "Array" : "Collection";
                        setText(getIndex() + ": " + "[Nested " + nestedType + "]");
                    }
                }
            });
        } else if (ReflectionUtil.isWrapperOrString(elementType) || elementType.isPrimitive() || elementType.equals(String.class)) {
            elementsList.setCellFactory(lv -> {
                IndexedTextFieldCell cell = new IndexedTextFieldCell();
                setupDragAndDrop(cell);
                cell.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !cell.isEmpty()) {
                        lv.edit(cell.getIndex());
                    }
                });
                return cell;
            });
        } else {
            elementsList.setCellFactory(lv -> {
                IndexedComboBoxCell cell = new IndexedComboBoxCell(CacheUtil.getInstance().getObjectsFitNames(elementType)
                        .toArray(new String[0]));
                setupDragAndDrop(cell);
                cell.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !cell.isEmpty()) {
                        lv.edit(cell.getIndex());
                    }
                });
                return cell;
            });
        }
    }

    /**
     * Sets up drag and drop on a cell to allow reordering.
     */
    private void setupDragAndDrop(ListCell<String> cell) {
        cell.setOnDragDetected(event -> {
            if (!cell.isEmpty()) {
                Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(cell.getIndex()));
                db.setContent(content);
                event.consume();
            }
        });
        cell.setOnDragOver(event -> {
            if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        cell.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                int draggedIndex = Integer.parseInt(db.getString());
                int dropIndex = cell.getIndex();
                if (draggedIndex != dropIndex) {
                    Object draggedItem = observableElements.get(draggedIndex);
                    observableElements.remove(draggedIndex);
                    observableElements.add(dropIndex, draggedItem);
                    refreshListView();
                }
                event.setDropCompleted(true);
            }
            event.consume();
        });
    }

    /*==================== Conversion and Save ====================*/

    /**
     * Converts a String from the editor into an object of the correct type.
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
     * Handles the Add action: appends a new element with a default value.
     */
    private void handleAddAction() {
        Object defaultValue;
        if (elementType.isArray()) {
            defaultValue = Array.newInstance(elementType.getComponentType(), 0);
        } else if (Collection.class.isAssignableFrom(elementType)) {
            defaultValue = new ArrayList<>();
        } else {
            defaultValue = ReflectionUtil.getDefaultValue(elementType);
        }
        observableElements.add(defaultValue);
        refreshListView();
    }

    /**
     * Handles the Remove action: deletes the currently selected element.
     */
    private void handleRemoveAction() {
        int selectedIndex = elementsList.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            observableElements.remove(selectedIndex);
            refreshListView();
        }
    }

    /**
     * Handles saving: updates the original collection with the modified elements.
     */
    @SuppressWarnings("unchecked")
    private void handleSaveAction() {
        collection.clear();
        ((Collection<Object>) collection).addAll(observableElements);
        backupCollection = new ArrayList<>(observableElements);
        log.debug("Changes saved to collection.");
        closeWindow();
    }

    /**
     * Cancels editing and restores the original collection.
     */
    @SuppressWarnings("unchecked")
    private void handleCancelAction() {
        ((Collection<Object>) collection).clear();
        ((Collection<Object>) collection).addAll(backupCollection);
        loadCollectionIntoObservableElements();
        refreshListView();
        log.debug("Changes canceled; original collection reloaded.");
        closeWindow();
    }


    /**
     * Closes the current window.
     */
    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    /*==================== Custom Cell Classes ====================*/

    /**
     * A ComboBox cell that displays its index before the item.
     */
    private class IndexedComboBoxCell extends ComboBoxListCell<String> {
        public IndexedComboBoxCell(String... items) {
            super(FXCollections.observableArrayList(items));
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
            int index = getIndex();
            if (index < 0) return;
            TextField textField = new TextField(
                    observableElements.get(index) != null ? observableElements.get(index).toString() : ""
            );
            textField.setOnAction(event -> commitEdit(textField.getText()));
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
}
