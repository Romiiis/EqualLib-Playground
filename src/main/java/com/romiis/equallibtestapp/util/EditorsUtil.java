package com.romiis.equallibtestapp.util;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;


@Slf4j
public class EditorsUtil {

    /**
     * Start editing the selected field
     *
     * @param item The TreeItem to start editing
     * @param objectReference The object reference to edit
     * @param field The field to edit
     */
    public void startTextFieldEditor(TreeItem<ObjectReference> item, ObjectReference objectReference, Field field) {
        String currentValue = String.valueOf(objectReference.getFieldValue());
        TextField textField = new TextField(currentValue);
        HBox editorBox = new HBox(textField);
        editorBox.setStyle("-fx-padding: 5;");

        setCellFactoryForEditor(item, textField);
        textField.requestFocus();

        textField.setOnAction(event -> {
            String text = textField.getText();
            if (text.isEmpty() && field.getType().equals(String.class)) {
                text = null;
            }
            objectReference.modifyFieldValue(text);
            item.setValue(new ObjectReference(objectReference.getInObject(), field));
            log.info("Updated value for field {}: {}", field.getName(), textField.getText());
            finishEdit(item);
        });

        textField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                finishEdit(item);
            }
        });

        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                finishEdit(item);
            }
        });
    }



    /**
     * Finish editing the selected field
     *
     * @param item The TreeItem to finish editing
     */
    private void finishEdit(TreeItem<ObjectReference> item) {
        setCellFactoryForEditor(item, null);
    }


    /**
     * Set the cell factory for the editor
     *
     * @param item The TreeItem to set the cell factory for
     */
    private void setCellFactoryForEditor(TreeItem<ObjectReference> item, Control editable) {
        item.setGraphic(editable);
    }


    public void decideEditor(TreeItem<ObjectReference> item, ObjectReference objectReference, Field field) {
        if (field.getType().equals(boolean.class) || field.getType().equals(Boolean.class)) {
            startBooleanEditor(item, objectReference, field);
        } else if (ReflectionUtil.isWrapperOrString(field.getType()) || field.getType().isPrimitive()) {
            startTextFieldEditor(item, objectReference, field);
        } else if (field.getType().isEnum()) {
            startEnumEditor(item, objectReference, field);;
        } else {
            log.info("Field type not supported for editing: {}", field.getType());
        }
    }

    private void startBooleanEditor(TreeItem<ObjectReference> item, ObjectReference objectReference, Field field) {
        ComboBox<Boolean> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(true, false);
        comboBox.setValue((Boolean) objectReference.getFieldValue());
        comboBox.setOnAction(event -> {
            objectReference.modifyFieldValue(String.valueOf(comboBox.getValue()));
            item.setValue(new ObjectReference(objectReference.getInObject(), field));
            log.info("Updated value for field {}: {}", field.getName(), comboBox.getValue());
            finishEdit(item);
        });
        setCellFactoryForEditor(item, comboBox);
        comboBox.requestFocus();


        comboBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                finishEdit(item);
            }
        });

        comboBox.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                finishEdit(item);
            }
        });


    }

    private void startEnumEditor(TreeItem<ObjectReference> item, ObjectReference objectReference, Field field) {
        // ComboBox for enum types
        ComboBox<Object> comboBox = new ComboBox<>();
        Object[] enumConstants = field.getType().getEnumConstants();
        comboBox.getItems().addAll(enumConstants);
        comboBox.setValue(objectReference.getFieldValue());
        comboBox.setOnAction(event -> {
            objectReference.modifyFieldValue(String.valueOf(comboBox.getValue()));
            item.setValue(new ObjectReference(objectReference.getInObject(), field));
            log.info("Updated value for field {}: {}", field.getName(), comboBox.getValue());
            finishEdit(item);
        });

        setCellFactoryForEditor(item, comboBox);
        comboBox.requestFocus();

        comboBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                finishEdit(item);
            }
        });

        comboBox.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                finishEdit(item);
            }
        });


    }

}
