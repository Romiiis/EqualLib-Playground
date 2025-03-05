package com.romiis.equallibtestapp.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
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
                if (objectReference.getField() != null) {
                    log.info("Field: " + objectReference);
                    startEdit(newValue);

                }
            }
        });
    }

    private void startEdit(TreeItem<ObjectReference> item) {
        ObjectReference objectReference = item.getValue();
        Field field = objectReference.getField();

        if (field == null) {
            return;
        }

        // Check if the field is editable (String, primitive types)
        if (field.getType().isPrimitive() || field.getType() == String.class) {
            startTextFieldEditor(item, objectReference, field);
        }
        // Handle other types (arrays, nested objects) if needed
    }

    private void startTextFieldEditor(TreeItem<ObjectReference> item, ObjectReference objectReference, Field field) {
        // Get the current value of the field
        String currentValue = String.valueOf(objectReference.getFieldValue());

        // Create a TextField for editing
        TextField textField = new TextField(currentValue);

        // Create a HBox to contain the TextField and add a listener to handle editing
        HBox editorBox = new HBox(textField);
        editorBox.setStyle("-fx-padding: 5;");

        // Show the editor box in place of the item
        setCellFactoryForEditor(item, textField);

        // Commit the edit when Enter is pressed
        textField.setOnAction(event -> {
            String newValue = textField.getText();
            objectReference.modifyFieldValue(newValue);  // Modify the field with the new value
            item.setValue(new ObjectReference(objectReference.getInObject(), field)); // Refresh the tree item
            log.info("Updated value for field " + field.getName() + ": " + newValue);
        });

        // Cancel the edit on ESC press
        textField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                // You can add some logic here to revert changes if needed
                setCellFactoryForEditor(item, null);  // Restore the cell to its original state
            }
        });
    }

    private void setCellFactoryForEditor(TreeItem<ObjectReference> item, TextField textField) {
        // Replace the cell with the TextField editor (could be done using a custom cell factory in TreeView)
        if (textField != null) {
            item.setGraphic(textField);
        } else {
            // If it's a cancel, revert to the original cell (like restoring text view)
            item.setGraphic(null); // Assuming it's just text. If needed, use the ObjectReference's toString method.
        }
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
        TreeItem<ObjectReference> rootItem = createTree(selectedObject, null, null);

        // Set the root item of the TreeView
        this.setRoot(rootItem);

        // Expand all items in the tree view (root and all children)
        expandAll(rootItem);

    }


    /**
     * Create a tree structure for the given object
     *
     * @param obj         The object to create a tree for
     * @param fieldInObject The field in the parent object that contains the object
     * @param index       The index of the object in the parent object (if it is an array element)
     * @return The root TreeItem for the object
     */
    private TreeItem<ObjectReference> createTree(Object obj, Field fieldInObject, Integer index) {
        if (obj == null) return new TreeItem<>(new ObjectReference(null, null));

        TreeItem<ObjectReference> root = new TreeItem<>(new ObjectReference(obj, fieldInObject));
        Field[] fields = ReflectionUtil.getAllFields(obj.getClass());


        Arrays.stream(fields).forEach(field -> {
            field.setAccessible(true);
            try {
                Object fieldValue = field.get(obj);

                if (fieldValue == null) {
                    root.getChildren().add(new TreeItem<>(new ObjectReference(obj, field)));
                    return;
                }
                if (field.getType().isArray()) {
                    root.getChildren().add(handleArray(fieldValue, field));
                }
                // Recursively handle nested objects
                else if (!field.getType().isPrimitive() && !ReflectionUtil.isWrapperOrString(field.getType())) {
                    root.getChildren().add(createTree(fieldValue, field,  index));
                } else {
                    TreeItem<ObjectReference> childNode = new TreeItem<>(new ObjectReference(obj, field));
                    root.getChildren().add(childNode);

                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        return root;
    }


    /**
     * Handle an array object
     *
     * @param array         The array object to handle
     * @param fieldInObject The field in the parent object that contains the array
     * @return The root TreeItem for the array object
     */
    private TreeItem<ObjectReference> handleArray(Object array, Field fieldInObject) {

        // Get the length of the array
        int length = Array.getLength(array);

        // Create the root TreeItem for the array object
        TreeItem<ObjectReference> root = new TreeItem<>(new ObjectReference(array, fieldInObject));

        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            TreeItem<ObjectReference> elementNode = new TreeItem<>(new ObjectReference(array, fieldInObject, i));

            // Handle primitive or wrapper types (e.g., int, String)
            if (element != null && (element.getClass().isPrimitive() || ReflectionUtil.isWrapperOrString(element.getClass()))) {
                elementNode.setValue(new ObjectReference(array, fieldInObject, i));  // Set primitive/wrapper element value
            }
            // Handle nested objects or arrays inside the array
            else if (element != null) {
                // Recursively create a tree for nested objects
                elementNode.getChildren().add(createTree(element, fieldInObject, i));
            }

            root.getChildren().add(elementNode);
        }

        return root;
    }

    /**
     * Expand all items in the TreeView (root and all children)
     *
     * @param item The root TreeItem to start expanding from
     */
    private void expandAll(TreeItem<?> item) {
        item.setExpanded(true);
        for (TreeItem child : item.getChildren()) {
            expandAll(child);
        }
    }


}
