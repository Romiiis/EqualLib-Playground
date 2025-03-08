package com.romiis.equallibtestapp.components.common;

import com.romiis.equallibtestapp.MainClass;
import com.romiis.equallibtestapp.controllers.ArrayEditController;
import com.romiis.equallibtestapp.controllers.LoadObjectController;
import com.romiis.equallibtestapp.util.ReflectionUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
public class EditorsUtil {

    private MyTreeView treeView;

    /**
     * Starts editing a text field for the given field.
     *
     * @param item            The TreeItem representing the field.
     * @param objectReference The object reference containing the field.
     * @param field           The field to be edited.
     */
    private void startTextFieldEditor(TreeItem<ObjectReference> item, ObjectReference objectReference, Field field) {
        String currentValue = String.valueOf(objectReference.getFieldValue());
        TextField textField = new TextField(currentValue);
        HBox editorBox = new HBox(textField);
        editorBox.setStyle("-fx-padding: 5;");

        // Attach common editor listeners and focus behavior.
        attachEditorControl(textField, item);
        textField.requestFocus();

        // Commit the edit on Enter.
        textField.setOnAction(event -> {
            String text = textField.getText();
            if (text.isEmpty() && field.getType().equals(String.class)) {
                text = null;
            }
            updateField(item, objectReference, field, text);
        });
    }

    /**
     * Starts editing a boolean field using a ComboBox.
     *
     * @param item            The TreeItem representing the field.
     * @param objectReference The object reference containing the field.
     * @param field           The boolean field to be edited.
     */
    private void startBooleanEditor(TreeItem<ObjectReference> item, ObjectReference objectReference, Field field) {
        ComboBox<Boolean> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(true, false);
        comboBox.setValue((Boolean) objectReference.getFieldValue());

        // Commit the edit when a new value is selected.
        comboBox.setOnAction(event ->
                updateField(item, objectReference, field, comboBox.getValue())
        );
        attachEditorControl(comboBox, item);
        comboBox.requestFocus();
    }

    /**
     * Starts editing an enum field using a ComboBox.
     *
     * @param item            The TreeItem representing the field.
     * @param objectReference The object reference containing the field.
     * @param field           The enum field to be edited.
     */
    private void startEnumEditor(TreeItem<ObjectReference> item, ObjectReference objectReference, Field field) {
        ComboBox<Object> comboBox = new ComboBox<>();
        Object[] enumConstants = field.getType().getEnumConstants();
        comboBox.getItems().addAll(enumConstants);
        comboBox.setValue(objectReference.getFieldValue());

        // Commit the edit when a new enum value is selected.
        comboBox.setOnAction(event ->
                updateField(item, objectReference, field, comboBox.getValue())
        );
        attachEditorControl(comboBox, item);
        comboBox.requestFocus();
    }

    /**
     * Decides which editor to use based on the field's type.
     *
     * @param tree            The MyTreeView instance.
     * @param item            The TreeItem to be edited.
     * @param objectReference The object reference containing the field.
     * @param field           The field to be edited.
     */
    public void decideEditor(MyTreeView tree, TreeItem<ObjectReference> item, ObjectReference objectReference, Field field) {
        this.treeView = tree;

        if (item.getValue().isEditContentItem()) {
            decideContentEditor(item, objectReference, field);
            return;
        }

        if (field.getType().equals(boolean.class) || field.getType().equals(Boolean.class)) {
            startBooleanEditor(item, objectReference, field);
        } else if (ReflectionUtil.isWrapperOrString(field.getType()) || field.getType().isPrimitive()) {
            startTextFieldEditor(item, objectReference, field);
        } else if (field.getType().isEnum()) {
            startEnumEditor(item, objectReference, field);
        } else {
            log.info("Field type not supported for editing: {}", field.getType());
        }
    }


    private void decideContentEditor(TreeItem<ObjectReference> item, ObjectReference objectReference, Field field) {
        if (field.getType().isArray()) {
            log.debug("Array field detected: {}", field.getName());
            startArrayEditor(objectReference);
        } else if (Collection.class.isAssignableFrom(field.getType())) {
            log.debug("Collection field detected: {}", field.getName());
        } else if (Map.class.isAssignableFrom(field.getType())) {
            log.debug("Map field detected: {}", field.getName());
        }


    }

    private void startArrayEditor(ObjectReference objectReference) {
        try {
            FXMLLoader loader = new FXMLLoader(MainClass.class.getResource(MainClass.ARRAY_EDIT_SCENE_FXML));
            Parent root = loader.load();

            // Pass this TreeView to the controller.
            ArrayEditController controller = loader.getController();


            controller.setAssignedArray(objectReference);

            Stage stage = new Stage();
            stage.setTitle("Array editor");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (Exception e) {
            log.error("Error loading object", e);
        }
    }

    /**
     * Finishes editing by clearing the editor control.
     *
     * @param item The TreeItem currently being edited.
     */
    private void finishEdit(TreeItem<ObjectReference> item) {
        setCellFactoryForEditor(item, null);
    }

    /**
     * Sets the editor control for the given TreeItem.
     *
     * @param item     The TreeItem to update.
     * @param editable The control to use as the editor, or null to remove it.
     */
    private void setCellFactoryForEditor(TreeItem<ObjectReference> item, Control editable) {
        item.setGraphic(editable);
    }

    /**
     * Attaches common behavior to an editor control including ESC key handling and focus loss.
     *
     * @param control The control used for editing.
     * @param item    The TreeItem being edited.
     */
    private void attachEditorControl(Control control, TreeItem<ObjectReference> item) {
        setCellFactoryForEditor(item, control);
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
     * Updates the field value and refreshes the TreeItem with the new ObjectReference.
     *
     * @param item            The TreeItem representing the field.
     * @param objectReference The object reference containing the field.
     * @param field           The field to update.
     * @param newValue        The new value to set.
     */
    private void updateField(TreeItem<ObjectReference> item, ObjectReference objectReference, Field field, Object newValue) {
        objectReference.modifyFieldValue(newValue == null ? null : String.valueOf(newValue));
        item.setValue(new ObjectReference(objectReference.getInObject(), field));
        log.info("Updated value for field {}: {}", field.getName(), newValue);
        finishEdit(item);
        treeView.setModified(true);
    }
}
