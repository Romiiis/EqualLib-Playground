package com.romiis.equallibtestapp.util;

import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;


@Slf4j
public class MyTreeView extends TreeView<ObjectReference> {


    /**
     * The object instance that is currently selected in the ListView
     */
    @Getter
    private Object selectedObject;

    /**
     * Whether to treat fields as objects or just print simple values
     */
    private boolean treatAsObjects = true;


    /**
     * Create a new MyTreeView instance
     */
    public MyTreeView() {
        super();
        initializeClickHandler();
    }

    private void initializeClickHandler() {
        this.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                ObjectReference objectReference = newValue.getValue();
                if (objectReference.getField() != null && objectReference.isModifiable()) {
                    log.info("Field: {}", objectReference);
                }
            }
        });

        this.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 ) {
                TreeItem<ObjectReference> selectedItem = this.getSelectionModel().getSelectedItem();
                if (selectedItem == null) {
                    return;
                }
                if (!selectedItem.getChildren().isEmpty()) {
                    return;
                }
                if (selectedItem.getValue().isModifiable()) {
                    startEdit(selectedItem);
                } else {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Warning");
                    alert.setHeaderText("Field is not modifiable");
                    alert.setContentText("The selected field is not modifiable");
                    alert.showAndWait();
                }
            }
        });
    }

    private void startEdit(TreeItem<ObjectReference> item) {
        ObjectReference objectReference = item.getValue();
        Field field = objectReference.getField();

        if (field == null || !objectReference.isModifiable()) {
            return;
        }

        startTextFieldEditor(item, objectReference, field);
    }

    private void startTextFieldEditor(TreeItem<ObjectReference> item, ObjectReference objectReference, Field field) {
        String currentValue = String.valueOf(objectReference.getFieldValue());
        TextField textField = new TextField(currentValue);
        HBox editorBox = new HBox(textField);
        editorBox.setStyle("-fx-padding: 5;");

        setCellFactoryForEditor(item, textField);
        textField.requestFocus();

        textField.setOnAction(event -> {
            objectReference.modifyFieldValue(textField.getText());
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

    private void finishEdit(TreeItem<ObjectReference> item) {
        setCellFactoryForEditor(item, null);
    }

    private void setCellFactoryForEditor(TreeItem<ObjectReference> item, TextField textField) {
        item.setGraphic(textField != null ? textField : null);
    }




    /**
     * Set the selected object instance in the ListView
     *
     * @param selectedObjectName The name of the selected object
     */
    public void setSelectedObject(String selectedObjectName) {
        try {
            Class<?> clazz = DynamicCompiler.loadClass(selectedObjectName);
            selectedObject = ReflectionUtil.createInstance(clazz);
        } catch (ClassNotFoundException | IOException e) {
        }
    }


    /**
     * Handle the selection change in the ListView of objects
     *
     * @param treatAsObjects Whether to treat fields as objects or just print simple values
     */
    public void handleSelectionChange(boolean treatAsObjects) {

        this.treatAsObjects = treatAsObjects;

        // Create the root TreeItem to be displayed in the TreeView
        TreeItem<ObjectReference> rootItem = new TreeItem<>(new ObjectReference(selectedObject, null));


        // Create the tree structure for the selected object
        rootItem = createTree(rootItem);

        // Set the root item of the TreeView
        this.setRoot(rootItem);

        // Expand all items in the tree view (root and all children)
        expandAll(rootItem);

    }


    /**
     * Create a tree structure for the given object
     *
     * @return The root TreeItem for the object
     */
    private TreeItem<ObjectReference> createTree(TreeItem<ObjectReference> parent) {

        // Get the object instance from the parent TreeItem
        Object obj = parent.getValue().getInObject();

        if (parent.getValue().getIndex() != null) {
            obj = Array.get(obj, parent.getValue().getIndex());
        }

        // if the object is null, create a TreeItem with a placeholder ObjectReference
        if (obj == null) return new TreeItem<>(new ObjectReference(null, null));


        Field[] fields = ReflectionUtil.getAllFields(obj.getClass());

        Object finalObj = obj;
        Arrays.stream(fields).forEach(field -> {
            field.setAccessible(true);
            try {
                Object fieldValue = field.get(finalObj);

                if (fieldValue == null) {
                    parent.getChildren().add(new TreeItem<>(new ObjectReference(finalObj, field)));
                    return;
                }
                if (field.getType().isArray()) {
                    parent.getChildren().add(handleArray(new TreeItem<>(new ObjectReference(finalObj, field))));
                }
                // Recursively handle nested objects
                else if (!field.getType().isPrimitive() && !ReflectionUtil.isWrapperOrString(field.getType())) {
                    parent.getChildren().add(createTree(new TreeItem<>(new ObjectReference(fieldValue, field))));
                } else {
                    TreeItem<ObjectReference> childNode = new TreeItem<>(new ObjectReference(finalObj, field));
                    parent.getChildren().add(childNode);

                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        return parent;
    }


    /**
     * Handle an array object
     *
     * @return The root TreeItem for the array object
     */
    private TreeItem<ObjectReference> handleArray(TreeItem<ObjectReference> parent) {
        ObjectReference parentValue = parent.getValue();
        Object array = ReflectionUtil.getFieldValue(parentValue.getInObject(), parentValue.getField());
        int length = Array.getLength(array);

        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            TreeItem<ObjectReference> child = new TreeItem<>(new ObjectReference(array, parentValue.getField(), i));

            if (element != null && !element.getClass().isPrimitive() && !ReflectionUtil.isWrapperOrString(element.getClass())) {
                child = createTree(child);
            }

            parent.getChildren().add(child);
        }
        return parent;
    }

    /**
     * Expand all items in the TreeView (root and all children)
     *
     * @param item The root TreeItem to start expanding from
     */
    private void expandAll(TreeItem<?> item) {
        item.setExpanded(true);
        for (TreeItem<?> child : item.getChildren()) {
            expandAll(child);
        }
    }


}
