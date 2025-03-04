package com.romiis.equallibtestapp.util;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.io.IOException;
import java.lang.reflect.Field;

public class TreeViewFiller {

    // Handles the selection change event and populates the TreeView based on the selected class
    public static void handleSelectionChange(String selectedObject, TreeView<String> treeView) {
        try {
            // Load the class dynamically based on its name
            Class<?> clazz = DynamicCompiler.loadClass(selectedObject);
            // Retrieve all fields (attributes) of the class
            Field[] fields = ReflectionUtil.getAllFields(clazz);

            // Create the root TreeItem to be displayed in the TreeView
            TreeItem<String> rootItem = new TreeItem<>(selectedObject);

            // Iterate over the fields of the class and add them to the TreeView
            for (Field field : fields) {
                // Make private fields accessible for reflection
                field.setAccessible(true);

                // If the field is a primitive type or a String, directly add it to the TreeView
                if (field.getType().isPrimitive() || field.getType().equals(String.class)) {
                    rootItem.getChildren().add(createTreeItem(field));
                }
                // If the field is an enum, create a nested TreeItem for the enum values
                else if (field.getType().isEnum()) {
                    rootItem.getChildren().add(createEnumTreeItem(field));
                }
                // If the field is a reference type (i.e., an object), recursively process its fields
                else {
                    rootItem.getChildren().add(createNestedObjectTreeItem(field));
                }
            }

            // Set the root item of the TreeView
            treeView.setRoot(rootItem);

            // Expand all items in the tree view (root and all children)
            expandAll(rootItem);


        } catch (ClassNotFoundException e) {
            e.printStackTrace();  // Log the error if class is not found
        } catch (IOException e) {
            throw new RuntimeException(e);  // Rethrow the IOException as RuntimeException
        }
    }

    // Helper method to create TreeItem for fields (primitives and Strings)
    private static TreeItem<String> createTreeItem(Field field) {
        return new TreeItem<>(field.getName() + " (" + field.getType().getSimpleName() + ")" + " : " + getDefaultValue(field.getType()));
    }

    // Helper method to create TreeItem for enum fields
    private static TreeItem<String> createEnumTreeItem(Field field) {
        // Get the enum class
        Class<?> enumClass = field.getType();

        // Get the first enum constant as the default value
        Object enumConstant = enumClass.getEnumConstants()[0];

        // Create a TreeItem with the default value of the enum
        String enumValue = enumConstant.toString(); // This will give you the enum value as a String

        // Add the TreeItem for the enum field with the default value


        return new TreeItem<>(field.getName() + " (" + field.getType().getSimpleName() + ") : " + enumValue);
    }

    // Helper method to create TreeItem for nested object fields
    private static TreeItem<String> createNestedObjectTreeItem(Field field) {
        TreeItem<String> nestedItem = new TreeItem<>(field.getName() + " (" + field.getType().getSimpleName() + ")");
        handleNestedObject(field.getType(), nestedItem);  // Recursively handle the nested object fields
        return nestedItem;
    }

    // Recursive method to process fields of nested objects
    private static void handleNestedObject(Class<?> clazz, TreeItem<String> parentItem) {
        // Retrieve all fields of the nested object
        Field[] fields = clazz.getDeclaredFields();

        // Iterate over the fields and add them to the parent TreeItem
        for (Field field : fields) {
            field.setAccessible(true);  // Make private fields accessible

            // If the field is a primitive type or a String, add it directly
            if (field.getType().isPrimitive() || field.getType().equals(String.class)) {
                // Add default value for primitive types
                parentItem.getChildren().add(new TreeItem<>(field.getType().getSimpleName() + " " + field.getName() + " : " + getDefaultValue(field.getType())));
            }
            // If the field is an enum, add its constants to the TreeView
            else if (field.getType().isEnum()) {
                // Get the enum class
                Class<?> enumClass = field.getType();

                // Get the first enum constant as the default value
                Object enumConstant = enumClass.getEnumConstants()[0];

                // Create a TreeItem with the default value of the enum
                String enumValue = enumConstant.toString(); // This will give you the enum value as a String

                // Add the TreeItem for the enum field with the default value
                parentItem.getChildren().add(new TreeItem<>(field.getName() + " : " + enumValue));
            }
            // If the field is another object, process its fields recursively
            else {
                TreeItem<String> nestedItem = new TreeItem<>(field.getType().getSimpleName() + " " + field.getName() + " : object");
                parentItem.getChildren().add(nestedItem);
                // Recursive call to process this nested object's fields
                handleNestedObject(field.getType(), nestedItem);
            }
        }
    }

    // Method to expand all items in the TreeView recursively
    private static void expandAll(TreeItem<String> item) {
        item.setExpanded(true);  // Expand the current item
        for (TreeItem<String> child : item.getChildren()) {
            expandAll(child);  // Recursively expand all child items
        }
    }

    private static Object getDefaultValue(Class<?> type) {
        if (type.equals(boolean.class)) {
            return false;
        } else if (type.equals(byte.class)) {
            return (byte) 0;
        } else if (type.equals(short.class)) {
            return (short) 0;
        } else if (type.equals(int.class)) {
            return 0;
        } else if (type.equals(long.class)) {
            return 0L;
        } else if (type.equals(float.class)) {
            return 0.0f;
        } else if (type.equals(double.class)) {
            return 0.0d;
        } else if (type.equals(char.class)) {
            return '\u0000';
        } else {
            return null;
        }
    }
}
