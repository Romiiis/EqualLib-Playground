package com.romiiis.equallib_playground.controllers;

import com.romiiis.equallib_playground.CacheUtil;
import com.romiiis.equallib_playground.util.ReflectionUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.DefaultStringConverter;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.*;

/**
 * MapEditorController.java
 * <p>
 * This class is used to edit a map in a JavaFX TableView.
 * It allows the user to edit the keys and values of the map entries.
 * <p>
 * The map is displayed in a TableView with two columns: one for keys and one for values.
 * The user can edit the keys and values directly in the table.
 *
 * @author Romiiis
 * @version 1.0
 */
@Log4j2
public class MapEditorController {

    @Getter
    private Map<?, ?> map;

    private String mapName;
    private Class<?> keyType;
    private Class<?> valueType;
    private List<Map.Entry<Object, Object>> backupEntries;
    @FXML
    private TableView<Map.Entry<Object, Object>> entriesTable;
    @FXML
    private TableColumn<Map.Entry<Object, Object>, String> keyColumn;
    @FXML
    private TableColumn<Map.Entry<Object, Object>, String> valueColumn;
    @FXML
    private Label mapTitleLabel;
    @FXML
    private Button addButton;
    @FXML
    private Button removeButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button cancelButton;

    private ObservableList<Map.Entry<Object, Object>> observableEntries;

    /**
     * Sets the map to be edited.
     *
     * @param map       Map to be edited
     * @param mapName   Name of the map
     * @param keyType   Type of the keys
     * @param valueType Type of the values
     */
    public void setAssignedMap(Map<?, ?> map, String mapName, Class<?> keyType, Class<?> valueType) {
        if (map == null) {
            throw new IllegalArgumentException("Provided map is null.");
        }
        this.map = map;
        // Create a backup copy of the original entries.
        this.backupEntries = copyEntries(map);
        this.mapName = mapName;
        this.keyType = keyType;
        this.valueType = valueType;
        init();
    }

    /**
     * Initializes the editor with the given map and expected key/value types.
     */
    private void init() {
        if (mapName != null) {
            String title = String.format("%s<%s, %s> %s",
                    map.getClass().getSimpleName(),
                    keyType.getSimpleName(),
                    valueType.getSimpleName(),
                    mapName);
            mapTitleLabel.setText(title);
        }
        loadMapIntoObservableEntries();
        setupTableView();
        wireButtons();
    }

    /**
     * Creates a copy of the map entries for display in the TableView.
     * This is necessary to avoid modifying the original map directly.
     *
     * @param map The map to copy entries from
     * @return A list of entries copied from the map
     */
    private List<Map.Entry<Object, Object>> copyEntries(Map<?, ?> map) {
        List<Map.Entry<Object, Object>> entries = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            entries.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
        }
        return entries;
    }

    /**
     * Loads the map entries into an observable list for the TableView.
     */
    private void loadMapIntoObservableEntries() {
        observableEntries = FXCollections.observableArrayList(copyEntries(map));
        entriesTable.setItems(observableEntries);
    }

    /**
     * Sets up the TableView columns and assigns custom editors based on the key/value types.
     */
    private void setupTableView() {
        keyColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getKey() != null ?
                        cellData.getValue().getKey().toString() : "null"));
        valueColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getValue() != null ?
                        cellData.getValue().getValue().toString() : "null"));

        entriesTable.setEditable(true);

        // Configure cell editors for both key and value columns.
        setupColumnEditor(keyColumn, keyType);
        setupColumnEditor(valueColumn, valueType);

        // Commit handlers for converting string input to the proper type.
        keyColumn.setOnEditCommit(event -> {
            int rowIndex = event.getTablePosition().getRow();
            String newKeyStr = event.getNewValue();
            if (!ReflectionUtil.isCorrectFormat(newKeyStr, keyType)) {
                showError("Invalid Format", "Incorrect Key Format at row: " + rowIndex,
                        "The key \"" + newKeyStr + "\" is not a valid " + keyType.getSimpleName());
                refreshTableView();
                return;
            }
            Object newKey = convertStringToElement(newKeyStr, keyType);
            if (isDuplicateKey(newKey, rowIndex)) {
                showError("Duplicate Key", "Duplicate Key at row: " + rowIndex,
                        "The key \"" + newKeyStr + "\" already exists in another entry.");
                refreshTableView();
                return;
            }
            updateEntryKey(rowIndex, newKey);
            refreshTableView();
        });

        valueColumn.setOnEditCommit(event -> {
            int rowIndex = event.getTablePosition().getRow();
            String newValueStr = event.getNewValue();
            if (!ReflectionUtil.isCorrectFormat(newValueStr, valueType)) {
                showError("Invalid Format", "Incorrect Value Format at row: " + rowIndex,
                        "The value \"" + newValueStr + "\" is not a valid " + valueType.getSimpleName());
                refreshTableView();
                return;
            }
            Object newValue = convertStringToElement(newValueStr, valueType);
            observableEntries.get(rowIndex).setValue(newValue);
            refreshTableView();
        });
    }

    /**
     * Sets up the cell editor for a given column based on the expected type.
     *
     * @param column The column to set up
     * @param type   The expected type of the column
     */
    private void setupColumnEditor(TableColumn<Map.Entry<Object, Object>, String> column, Class<?> type) {
        if (type.isEnum()) {
            String[] options = Arrays.stream(type.getEnumConstants())
                    .map(Object::toString)
                    .toArray(String[]::new);
            column.setCellFactory(col -> {
                ComboBoxTableCell<Map.Entry<Object, Object>, String> cell =
                        new ComboBoxTableCell<>(FXCollections.observableArrayList(options));
                cell.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !cell.isEmpty()) {
                        entriesTable.edit(cell.getIndex(), column);
                    }
                });
                return cell;
            });
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            column.setCellFactory(col -> {
                ComboBoxTableCell<Map.Entry<Object, Object>, String> cell =
                        new ComboBoxTableCell<>(FXCollections.observableArrayList("true", "false"));
                cell.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !cell.isEmpty()) {
                        entriesTable.edit(cell.getIndex(), column);
                    }
                });
                return cell;
            });
        } else if (ReflectionUtil.isWrapperOrString(type) || type.isPrimitive() || type.equals(String.class)) {
            column.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        } else {
            // Fallback: use a ComboBox with default options from CacheUtil.
            column.setCellFactory(col -> {
                ComboBoxTableCell<Map.Entry<Object, Object>, String> cell =
                        new ComboBoxTableCell<>(FXCollections.observableArrayList(
                                CacheUtil.getInstance().getObjectsFitNames(type).toArray(new String[0])
                        ));
                cell.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !cell.isEmpty()) {
                        entriesTable.edit(cell.getIndex(), column);
                    }
                });
                return cell;
            });
        }
    }

    /**
     * Wires the buttons to their respective actions.
     */
    private void wireButtons() {
        // Disable add and remove buttons because we do not allow changing map length.
        addButton.setOnAction(e -> handleAddAction());
        removeButton.setOnAction(e -> handleRemoveAction());
        saveButton.setOnAction(e -> handleSaveAction());
        cancelButton.setOnAction(e -> handleCancelAction());
    }

    /**
     * Checks if the new key is a duplicate of any existing keys in the observable entries.
     *
     * @param newKey     The new key to check
     * @param currentRow The current row index
     * @return true if the new key is a duplicate, false otherwise
     */
    private boolean isDuplicateKey(Object newKey, int currentRow) {
        for (int i = 0; i < observableEntries.size(); i++) {
            if (i != currentRow) {
                Object existingKey = observableEntries.get(i).getKey();
                if (existingKey != null && existingKey.equals(newKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Updates the key of an entry at the specified row index.
     *
     * @param rowIndex The index of the entry to update
     * @param newKey   The new key to set
     */
    private void updateEntryKey(int rowIndex, Object newKey) {
        Map.Entry<Object, Object> oldEntry = observableEntries.get(rowIndex);
        Map.Entry<Object, Object> updatedEntry = new AbstractMap.SimpleEntry<>(newKey, oldEntry.getValue());
        observableEntries.set(rowIndex, updatedEntry);
    }

    /**
     * Converts a string value to the specified type.
     *
     * @param value The string value to convert
     * @param type  The target type
     * @return The converted object, or null if conversion fails
     */
    private Object convertStringToElement(String value, Class<?> type) {
        if (type.isEnum()) {
            Object[] enumConstants = type.getEnumConstants();
            for (Object constant : enumConstants) {
                if (constant.toString().equals(value))
                    return constant;
            }
            return null;
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return Boolean.parseBoolean(value);
        } else if (ReflectionUtil.isWrapperOrString(type) || type.isPrimitive() || type.equals(String.class)) {
            return ReflectionUtil.convertStringToPrimitive(value, type);
        }
        return CacheUtil.getInstance().getObjectByName(value, true);
    }

    /**
     * Refreshes the TableView to reflect any changes made to the observable entries.
     */
    private void refreshTableView() {
        entriesTable.refresh();
    }

    /**
     * Handles the action of adding a new entry to the map.
     * It creates a new entry with default values for the key and value types.
     */
    private void handleAddAction() {
        Object defaultKey = ReflectionUtil.getDefaultValue(keyType);
        Object defaultValue = ReflectionUtil.getDefaultValue(valueType);
        observableEntries.add(new AbstractMap.SimpleEntry<>(defaultKey, defaultValue));
        refreshTableView();
    }

    /**
     * Handles the action of removing the selected entry from the map.
     * It removes the selected entry from the observable entries list.
     */
    private void handleRemoveAction() {
        Map.Entry<Object, Object> selectedEntry = entriesTable.getSelectionModel().getSelectedItem();
        if (selectedEntry != null) {
            observableEntries.remove(selectedEntry);
            refreshTableView();
        }
    }

    /**
     * Handles saving: updates the original map with the modified entries.
     */
    @SuppressWarnings("unchecked")
    private void handleSaveAction() {
        ((Map<Object, Object>) map).clear();
        for (Map.Entry<Object, Object> entry : observableEntries) {
            ((Map<Object, Object>) map).put(entry.getKey(), entry.getValue());
        }
        backupEntries = copyEntries(map);
        log.debug("Changes saved to map.");
        closeWindow();
    }

    /**
     * Cancels editing and restores the original map.
     */
    @SuppressWarnings("unchecked")
    private void handleCancelAction() {
        ((Map<Object, Object>) map).clear();
        for (Map.Entry<Object, Object> entry : backupEntries) {
            ((Map<Object, Object>) map).put(entry.getKey(), entry.getValue());
        }
        loadMapIntoObservableEntries();
        refreshTableView();
        log.debug("Changes canceled; original map reloaded.");
        closeWindow();
    }

    /**
     * Closes the window.
     */
    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Displays an error alert.
     */
    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
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

}
