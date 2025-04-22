package com.romiis.equallib_playground.util;

import com.romiis.equallib_playground.MainClass;
import com.romiis.equallib_playground.components.treeView.FieldNode;
import com.romiis.equallib_playground.components.treeView.FieldNodeType;
import com.romiis.equallib_playground.components.treeView.LazyTreeItem;
import com.romiis.equallib_playground.components.treeView.MyTreeView;
import com.romiis.equallib_playground.controllers.ArrayEditController;
import com.romiis.equallib_playground.controllers.CollectionEditController;
import com.romiis.equallib_playground.controllers.MapEditorController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

import java.util.Collection;
import java.util.Map;

import static com.romiis.equallib_playground.util.ReflectionUtil.getClassFromType;

/**
 * EditorsUtil.java
 * <p>
 * Utility class to handle the creation and management of various editors for different field types.
 * This includes text fields, boolean selectors, enum selectors, and more complex structures like arrays and collections.
 */
@Log4j2
public class EditorsUtil {

    /**
     * The tree view instance to be modified.
     */
    private MyTreeView treeView;

    /**
     * Decides which editor to use based on the FieldNode's type and node type.
     *
     * @param tree The MyTreeView instance.
     * @param item The TreeItem containing the FieldNode.
     * @param node The FieldNode to be edited.
     */
    public void decideEditor(MyTreeView tree, LazyTreeItem item, FieldNode node) {
        this.treeView = tree;
        // If the node is marked as ARRAY_EDITABLE, start the array editor.
        if (node.getNodeType() == FieldNodeType.ARRAY_EDITABLE) {
            if (node.getValue().getClass().isArray()) {
                startArrayEditor(node, item);
                return;
            } else if (Collection.class.isAssignableFrom(node.getValue().getClass())) {
                startCollectionEditor(node, item);
                return;
            } else if (Map.class.isAssignableFrom(node.getValue().getClass())) {
                startMapEditor(node, item);
                return;
            }
        }
        // Non-editable or info nodes are not edited.
        if (node.getNodeType() == FieldNodeType.NON_EDITABLE) {
            log.info("This node is non-editable: {}", node.getName());
            return;
        }
        // For nodes marked as EDITABLE, decide the editor based on field type.
        Class<?> fieldType = node.getType();
        if (fieldType == null) {
            log.warn("Field type is null for node: {}", node.getName());
            return;
        }
        if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
            startBooleanEditor(item, node);
        } else if (ReflectionUtil.isWrapperOrString(fieldType) || fieldType.isPrimitive()) {
            startTextFieldEditor(item, node);
        } else if (fieldType.isEnum()) {
            startEnumEditor(item, node);
        } else {
            log.info("Field type not supported for editing: {}", fieldType);
        }
    }

    /**
     * Starts the text field editor for a String field.
     *
     * @param item The TreeItem containing the FieldNode.
     * @param node The FieldNode containing the String.
     */
    private void startTextFieldEditor(LazyTreeItem item, FieldNode node) {
        String currentValue = (node.getValue() == null) ? "" : String.valueOf(node.getValue());
        TextField textField = new TextField(currentValue);
        HBox editorBox = new HBox(textField);
        editorBox.setStyle("-fx-padding: 5;");
        attachEditorControl(textField, item);
        textField.requestFocus();

        // Commit the edit on Enter.
        textField.setOnAction(event -> {
            String text = textField.getText();
            // For String fields, an empty text is interpreted as null.
            if (text.isEmpty() && node.getType().equals(String.class)) {
                text = null;
            }
            updateField(item, node, text);
        });
    }

    /**
     * Starts the boolean editor for a boolean field.
     *
     * @param item The TreeItem containing the FieldNode.
     * @param node The FieldNode containing the boolean.
     */
    private void startBooleanEditor(LazyTreeItem item, FieldNode node) {
        ComboBox<Boolean> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(true, false);
        comboBox.setValue((Boolean) node.getValue());
        attachEditorControl(comboBox, item);
        comboBox.requestFocus();
        comboBox.setOnAction(event -> updateField(item, node, comboBox.getValue()));
    }

    /**
     * Starts the enum editor for an enum field.
     *
     * @param item The TreeItem containing the FieldNode.
     * @param node The FieldNode containing the enum.
     */
    private void startEnumEditor(LazyTreeItem item, FieldNode node) {
        ComboBox<Object> comboBox = new ComboBox<>();
        Object[] enumConstants = node.getType().getEnumConstants();
        comboBox.getItems().addAll(enumConstants);
        comboBox.setValue(node.getValue());
        attachEditorControl(comboBox, item);
        comboBox.requestFocus();
        comboBox.setOnAction(event -> updateField(item, node, comboBox.getValue()));
    }

    /**
     * Starts the array editor for an array field.
     *
     * @param node The FieldNode containing the array.
     * @param item The TreeItem containing the FieldNode.
     */
    private void startArrayEditor(FieldNode node, LazyTreeItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(MainClass.class.getResource(MainClass.ARRAY_EDIT_SCENE_FXML));
            Parent root = loader.load();
            ArrayEditController controller = loader.getController();
            // Pass the array value and field name (from node) to the array editor.
            controller.setAssignedArray(node.getValue(), node.getName());

            Stage stage = new Stage();
            stage.setTitle("Array Editor");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            log.info("Array editor closed");
            // After editing, update the node's value.
            item.getParent().getValue().updateValue(controller.getArray());
            treeView.setModified(true);
            treeView.refresh();

        } catch (Exception e) {
            log.error("Error loading array editor", e);
        }
    }

    /**
     * Starts the collection editor for a Collection field.
     *
     * @param node The FieldNode containing the Collection.
     * @param item The TreeItem containing the FieldNode.
     */
    private void startCollectionEditor(FieldNode node, LazyTreeItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(MainClass.class.getResource(MainClass.COLLECTION_EDIT_SCENE_FXML));
            Parent root = loader.load();
            CollectionEditController controller = loader.getController();
            // Pass the array value and field name (from node) to the array editor.
            controller.setAssignedCollection((Collection<?>) node.getValue(), node.getName(), node.getField().getGenericType());

            Stage stage = new Stage();
            stage.setTitle("Collection Editor");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            log.info("Array editor closed");
            // After editing, update the node's value.
            item.getParent().getValue().updateValue(controller.getCollection());
            treeView.setModified(true);
            treeView.refresh();

        } catch (Exception e) {
            log.error("Error loading array editor", e);
        }
    }


    /**
     * Starts the map editor for a Map field.
     *
     * @param node The FieldNode containing the Map.
     * @param item The TreeItem containing the FieldNode.
     */
    private void startMapEditor(FieldNode node, LazyTreeItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(MainClass.class.getResource(MainClass.MAP_EDIT_SCENE_FXML));
            Parent root = loader.load();
            MapEditorController controller = loader.getController();

            // Default types in case we cannot retrieve them.
            Class<?> keyType = Object.class;
            Class<?> valueType = Object.class;

            // If the Field is available, try to extract the generic type info.
            if (node.getField() != null) {
                java.lang.reflect.Type genericType = node.getField().getGenericType();
                if (genericType instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) genericType;
                    java.lang.reflect.Type[] typeArguments = pt.getActualTypeArguments();
                    if (typeArguments.length == 2) {
                        keyType = getClassFromType(typeArguments[0]);
                        valueType = getClassFromType(typeArguments[1]);
                    }
                }
            }

            // Pass the array value and field name (from node) to the array editor.
            controller.setAssignedMap((Map<?, ?>) node.getValue(), node.getName(), keyType, valueType);

            Stage stage = new Stage();
            stage.setTitle("Collection Editor");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            log.info("Array editor closed");
            // After editing, update the node's value.
            item.getParent().getValue().updateValue(controller.getMap());
            treeView.setModified(true);
            treeView.refresh();

        } catch (Exception e) {
            log.error("Error loading array editor", e);
        }
    }

    /**
     * Attaches common editor behavior to a control.
     *
     * @param control The control used for editing.
     * @param item    The TreeItem being edited.
     */
    private void attachEditorControl(Control control, LazyTreeItem item) {
        item.setGraphic(control);
        control.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                finishEdit(item);
            }
        });
        control.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                finishEdit(item);
            }
        });
    }

    /**
     * Finishes editing by removing the editor control.
     *
     * @param item The TreeItem currently being edited.
     */
    private void finishEdit(LazyTreeItem item) {
        item.setGraphic(null);
    }

    /**
     * Updates the FieldNode value, finishes the edit, and refreshes the tree.
     *
     * @param item     The TreeItem containing the node.
     * @param node     The FieldNode being edited.
     * @param newValue The new value to update.
     */
    private void updateField(LazyTreeItem item, FieldNode node, Object newValue) {
        // If the newValue is a String and the target type is numeric, attempt conversion.
        if (newValue instanceof String && node.getType() != null && isNumericType(node.getType())) {
            try {
                newValue = convertFromString((String) newValue, node.getType());
            } catch (NumberFormatException e) {
                // Show an error alert if conversion fails.
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Number Format");
                alert.setHeaderText("Invalid number format for field: " + node.getName());
                alert.setContentText("The value \"" + newValue + "\" is not valid for type " + node.getType().getSimpleName());
                alert.showAndWait();
                return; // Cancel the update.
            }
        }
        node.updateValue(newValue);
        finishEdit(item);
        treeView.setModified(true);
        treeView.refresh();
    }

    /**
     * Checks if a given type is a numeric type.
     */
    private boolean isNumericType(Class<?> type) {
        return type.equals(Integer.class) || type.equals(int.class)
                || type.equals(Double.class) || type.equals(double.class)
                || type.equals(Long.class) || type.equals(long.class)
                || type.equals(Float.class) || type.equals(float.class)
                || type.equals(Short.class) || type.equals(short.class)
                || type.equals(Byte.class) || type.equals(byte.class);
    }

    /**
     * Converts a String to an instance of the target numeric type.
     */
    private Object convertFromString(String input, Class<?> targetType) throws NumberFormatException {
        if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
            return Integer.parseInt(input);
        } else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
            return Double.parseDouble(input);
        } else if (targetType.equals(Long.class) || targetType.equals(long.class)) {
            return Long.parseLong(input);
        } else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
            return Float.parseFloat(input);
        } else if (targetType.equals(Short.class) || targetType.equals(short.class)) {
            return Short.parseShort(input);
        } else if (targetType.equals(Byte.class) || targetType.equals(byte.class)) {
            return Byte.parseByte(input);
        }
        // Fallback: return the input as-is (or throw an exception)
        return input;
    }

}
