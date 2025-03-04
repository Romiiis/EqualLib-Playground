package com.romiis.equallibtestapp.util;

import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class MyTreeView extends TreeView<String> {


    /**
     * The object instance that is currently selected in the ListView
     */
    private Object selectedObject;

    /**
     * Whether to treat fields as objects or just print simple values
     */
    private static boolean treatAsObjects = true;

    /**
     * The item that is currently being edited
     */
    private MyTreeItem currentEditingItem;


    /**
     * Create a new MyTreeView instance
     */
    public MyTreeView() {
        super();
        enableEditOnDoubleClick();
    }


    /**
     * Enable editing of TreeItems on double-click
     */
    private void enableEditOnDoubleClick() {

        this.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                MyTreeItem selectedItem = (MyTreeItem) this.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    // Cancel editing if there's an item already being edited
                    if (currentEditingItem != null) {
                        cancelEditing(currentEditingItem);
                    }
                    startEditing(selectedItem);
                }
            }
        });
    }

    /**
     * Start editing the given TreeItem
     *
     * @param item The TreeItem to start editing
     */
    private void startEditing(MyTreeItem item) {
        currentEditingItem = item;  // Track the item being edited

        if (isEnum(item)) {
            startEditingEnum(item);
        } else if (isEditableField(item)) {
            startEditingTextField(item);
        }
    }



    /**
     * Check if the given TreeItem represents an editable field
     *
     * @param item The TreeItem to check
     * @return True if the field is editable, false otherwise
     */
    private boolean isEditableField(MyTreeItem item) {
        Field field = findFieldByReference(((MyTreeItem)item.getParent()).getReference(), item.getReference());
        if (field == null) {
            return false;
        }

        // Check if the field is static and final
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
            // Skip modification of static final fields
            System.out.println("Skipping static final field: " + field.getName());
            return false; // Don't modify static final fields
        }
        return isWrapperOrString(item.getReference().getClass());
    }


    /**
     * Start editing a TextField for the given TreeItem
     *
     * @param item The TreeItem to start editing
     */
    private void startEditingTextField(MyTreeItem item) {
        TextField textField = new TextField(getValue(item));

        // When the user presses Enter, commit the changes
        textField.setOnAction(e -> {
            String newVal = findAndModifyAttribute(item, textField.getText());
            if (newVal != null) {
                item.setValue(newVal);
            }
            cancelEditing(item);  // Finish editing
            ReflectionUtil.printFields(selectedObject, new IdentityHashMap<>());
        });

        // If ESC is pressed, cancel editing and revert the original value
        textField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                cancelEditing(item);  // Cancel on ESC

            }
        });

        // Set the TextField as the graphic of the TreeItem
        item.setGraphic(textField);

    }


    /**
     * Start editing an enum field for the given TreeItem
     *
     * @param item The TreeItem to start editing
     */
    private void startEditingEnum(MyTreeItem item) {
        ComboBox<String> comboBox = new ComboBox<>();
        MyTreeItem parent = (MyTreeItem) item.getParent();
        Object parentObject = parent.getReference();
        Field field = findFieldByReference(parentObject, item.getReference());

        if (field != null && field.getType().isEnum()) {
            Object[] enumConstants = field.getType().getEnumConstants();
            for (Object constant : enumConstants) {
                comboBox.getItems().add(constant.toString());
            }

            comboBox.setValue(getValue(item));

            comboBox.setOnAction(e -> {
                String res = findAndModifyAttribute(item, comboBox.getValue());
                if (res != null) {
                    item.setValue(res);
                }
                cancelEditing(item);  // Finish editing
                ReflectionUtil.printFields(selectedObject, new IdentityHashMap<>());
            });

            item.setGraphic(comboBox);
        }
    }


    /**
     * Check if the given TreeItem represents an enum field
     *
     * @param item The TreeItem to check
     * @return True if the field is an enum, false otherwise
     */
    private boolean isEnum(MyTreeItem item) {
        MyTreeItem parent = (MyTreeItem) item.getParent();
        Object parentObject = parent.getReference();
        Field field = findFieldByReference(parentObject, item.getReference());
        return field != null && field.getType().isEnum();
    }


    /**
     * Find a field in the parent object by its reference name
     *
     * @param parentObject The parent object to search in
     * @param reference    The reference name of the field
     * @return The field with the given reference name, or null if not found
     */
    private Field findFieldByReference(Object parentObject, Object reference) {
        if (!(reference instanceof String)) {
            return null;
        }

        Field[] fields = ReflectionUtil.getAllFields(parentObject.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getName().equals(reference.toString())) {
                return field;
            }
        }
        return null;
    }


    /**
     * Cancel editing of the given TreeItem
     *
     * @param item The TreeItem to cancel editing
     */
    private void cancelEditing(MyTreeItem item) {
        item.setGraphic(null); // Remove the editor
        currentEditingItem = null;  // Reset the current editing item
    }


    /**
     * Find and modify the attribute of the given TreeItem
     *
     * @param currentItem The TreeItem to modify
     * @param value       The new value to set
     */
    private String findAndModifyAttribute(MyTreeItem currentItem, String value) {
        if (value == null) {
            return null;
        }

        MyTreeItem parent = (MyTreeItem) currentItem.getParent();
        Object parentObject = parent.getReference();

        Field field = findFieldByReference(parentObject, currentItem.getReference());
        if (field != null) {
            Object valueToSet = convertStringToFieldType(value, field.getType());
            if (valueToSet != null) {
                try {
                    field.set(parentObject, valueToSet);
                    return currentItem.getReference().toString() + " (" + field.getType().getSimpleName() + ") : " + value;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    /**
     * Convert a string value to the given field type
     *
     * @param value    The string value to convert
     * @param fieldType The type to convert the value to
     * @return The converted value, or null if the conversion failed
     */
    private Object convertStringToFieldType(String value, Class<?> fieldType) {
        try {
            if (fieldType.equals(String.class)) {
                return value;
            } else if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
                return Integer.valueOf(value);
            } else if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
                return Double.valueOf(value);
            } else if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
                return Long.valueOf(value);
            } else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
                return Boolean.valueOf(value);
            } else if (fieldType.equals(float.class) || fieldType.equals(Float.class)) {
                return Float.valueOf(value);
            } else if (fieldType.equals(char.class) || fieldType.equals(Character.class)) {
                return value.charAt(0);
            } else if (fieldType.isEnum()) {
                Object[] enumConstants = fieldType.getEnumConstants();
                for (Object constant : enumConstants) {
                    if (constant.toString().equals(value)) {
                        return constant;
                    }
                }
            }
        } catch (Exception e) {
            // Show error message to user in dialog
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error setting value");
            alert.setContentText("Error setting value: " + e.getMessage());
            alert.showAndWait();

        }
        return null;
    }


    /**
     * Get the value of the given TreeItem
     *
     * @param item The TreeItem to get the value from
     * @return The value of the TreeItem
     */
    private String getValue(MyTreeItem item) {
        MyTreeItem parent = (MyTreeItem) item.getParent();
        Object parentObject = parent.getReference();

        Field field = findFieldByReference(parentObject, item.getReference());
        if (field != null) {
            try {
                Object value = field.get(parentObject);
                return value != null ? value.toString() : null;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
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
            e.printStackTrace();
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
        MyTreeItem rootItem = new MyTreeItem(selectedObject.getClass().getSimpleName(), selectedObject);

        // Handle the nested object and add it to the tree view
        handleNestedObject(selectedObject, rootItem, treatAsObjects, new IdentityHashMap<>());

        // Set the root item of the TreeView
        this.setRoot(rootItem);

        // Expand all items in the tree view (root and all children)
        expandAll(rootItem);

    }

    /**
     * Create a TreeItem for a primitive type or a String field, and populate it with the actual value
     *
     * @param field  The field for which to create the TreeItem
     * @param object The object instance from which the value is fetched
     * @return The created TreeItem
     */
    private static MyTreeItem createTreeItem(Field field, Object object) {
        try {
            // Get the value of the field from the object
            Object value = field.get(object);

            String type = field.getType().getSimpleName();
            String valueStr = value != null ? value.toString() : "null";
            return new MyTreeItem(field.getName() + " (" + type + ")" + " : " + valueStr, field.getName());

        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return new MyTreeItem(field.getName() + " : Error accessing value", null);
        }
    }

    /**
     * Handle the fields of a nested object recursively and add them to the parent TreeItem
     *
     * @param object         The object instance to inspect
     * @param parentItem     The parent TreeItem to which the fields will be added
     * @param treatAsObjects Whether to treat fields as objects or just print simple values
     */
    private static void handleNestedObject(Object object, MyTreeItem parentItem, boolean treatAsObjects, IdentityHashMap<Object, Boolean> visited) {


        // Retrieve all fields of the object
        Field[] fields = ReflectionUtil.getAllFields(object.getClass());

        if (visited.containsKey(object)) {
            parentItem.getChildren().add(new MyTreeItem("(Cyclic reference detected)", null));
            return;
        }
        visited.put(object, true);

        // Iterate over the fields and add them to the parent TreeItem
        for (Field field : fields) {
            field.setAccessible(true);  // Make private fields accessible

            // If the field is an array, expand it and show its elements
            if (field.getType().isArray()) {
                handleArrays(field, object, parentItem, visited);
            }
            else if (field.getType().isPrimitive() || field.getType().equals(String.class)) {
                parentItem.getChildren().add(createTreeItem(field, object));
            } else if (field.getType().isEnum()) {
                parentItem.getChildren().add(createTreeItem(field, object));
            } else if (field.getType().isInterface()) {
                parentItem.getChildren().add(new MyTreeItem(field.getName() + " : Cannot instantiate interface", null));
            } else {

                if (!treatAsObjects && (Collection.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType()))) {
                    handleColAndMapsByKeys(field, object, parentItem);
                    continue;
                }

                handleObject(field, object, parentItem, visited);


            }
        }
    }

    private static void handleObject(Field field, Object object, MyTreeItem parentItem, IdentityHashMap<Object, Boolean> visited) {
        try {
            Object nestedObject = field.get(object);

            MyTreeItem nestedItem = new MyTreeItem(field.getName() + " (" + field.getType().getSimpleName() + ")", nestedObject);
            parentItem.getChildren().add(nestedItem);


            // If the nested object is not null, recursively handle its fields
            if (nestedObject != null) {
                handleNestedObject(nestedObject, nestedItem, treatAsObjects, visited);
            } else {
                nestedItem.getChildren().add(new TreeItem<>("null"));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            parentItem.getChildren().add(new TreeItem<>("Error accessing nested object"));
        }
    }

    private static void handleColAndMapsByKeys(Field field, Object object, MyTreeItem parentItem) {
        String collectionType = "Collection";
        MyTreeItem collectionItem = new MyTreeItem(field.getName() + " (" + collectionType + ")", null);
        int size = 0;

        // Pokud je to Map, nastavíme správný typ
        if (Map.class.isAssignableFrom(field.getType())) {
            // Add children for each key-value pair in the map
            Map<?, ?> map = (Map<?, ?>) ReflectionUtil.getFieldValue(object, field);
            if (map != null) {
                size = map.size();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    MyTreeItem entryItem = new MyTreeItem(entry.getKey() + " : " + entry.getValue(), null);
                    collectionItem.getChildren().add(entryItem);
                }
            }

        }
        // Pokud je to List nebo Set, nastavíme správný typ
        else if (List.class.isAssignableFrom(field.getType())) {
            // Add children for each element in the list
            List<?> list = (List<?>) ReflectionUtil.getFieldValue(object, field);
            if (list != null) {
                size = list.size();
                for (int i = 0; i < list.size(); i++) {
                    MyTreeItem listItem = new MyTreeItem(field.getName() + "[" + i + "] : " + list.get(i), null);
                    collectionItem.getChildren().add(listItem);
                }
            }

        } else if (Set.class.isAssignableFrom(field.getType())) {
            // Add children for each element in the set
            Set<?> set = (Set<?>) ReflectionUtil.getFieldValue(object, field);
            if (set != null) {
                size = set.size();
                int i = 0;
                for (Object element : set) {
                    MyTreeItem setItem = new MyTreeItem(field.getName() + "[" + i + "] : " + element, null);
                    collectionItem.getChildren().add(setItem);
                    i++;
                }
            }

        }

        // Přidáme element do TreeItem
        collectionItem.setValue(field.getName() + " (" + field.getType().getSimpleName() + ") [size=" + size + "]");
        parentItem.getChildren().add(collectionItem);

    }

    private static void handleArrays(Field field, Object object, MyTreeItem parentItem, IdentityHashMap<Object, Boolean> visited) {
        try {
            Object array = field.get(object);
            int length = 0;
            if (array != null) {
                length = Array.getLength(array);
            }
            MyTreeItem arrayItem = new MyTreeItem(field.getName() + " (" + field.getType().getSimpleName() + ") [size=" + length + "]", array);
            parentItem.getChildren().add(arrayItem);

            // Iterate through the array and add its elements to the TreeItem
            for (int i = 0; i < length; i++) {
                Object element = Array.get(array, i);
                String elementStr = "null";

                if (element != null) {
                    if (isWrapperOrString(element.getClass())) {
                        elementStr = String.valueOf(element);
                        MyTreeItem arrayElementItem = new MyTreeItem(field.getName() + "[" + i + "] : " + elementStr, element);
                        arrayItem.getChildren().add(arrayElementItem);
                    } else {
                        // Wrapper třídy nebo referenční typy
                        MyTreeItem arrayElementItem = new MyTreeItem(field.getName() + "[" + i + "]", element);
                        arrayItem.getChildren().add(arrayElementItem);
                        handleNestedObject(element, arrayElementItem, treatAsObjects, visited);
                    }
                } else {
                    MyTreeItem arrayElementItem = new MyTreeItem(field.getName() + "[" + i + "] : " + elementStr, null);
                    arrayItem.getChildren().add(arrayElementItem);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            parentItem.getChildren().add(new MyTreeItem(field.getName() + " : Error accessing array", null));
        }
    }

    /**
     * Check if the given class is a wrapper class or a String
     *
     * @param type The class to check
     * @return True if the class is a wrapper class or a String, false otherwise
     */
    private static boolean isWrapperOrString(Class<?> type) {
        return type.equals(Integer.class) ||
                type.equals(Double.class) ||
                type.equals(Long.class) ||
                type.equals(Float.class) ||
                type.equals(Character.class) ||
                type.equals(Short.class) ||
                type.equals(Byte.class) ||
                type.equals(Boolean.class) ||
                type.equals(String.class);
    }

    /**
     * Expand all items in the TreeView (root and all children)
     *
     * @param item The root TreeItem to start expanding from
     */
    private static void expandAll(TreeItem<String> item) {
        item.setExpanded(true);
        for (TreeItem<String> child : item.getChildren()) {
            expandAll(child);
        }
    }


}
