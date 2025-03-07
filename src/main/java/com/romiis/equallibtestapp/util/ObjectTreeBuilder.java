package com.romiis.equallibtestapp.util;

import com.romiis.equallibtestapp.components.common.ObjectReference;
import javafx.scene.control.TreeItem;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

@Slf4j
public class ObjectTreeBuilder {

    /**
     * Create a tree structure for the given object
     *
     * @return The root TreeItem for the object
     */
    public static TreeItem<ObjectReference> createTree(TreeItem<ObjectReference> parent, Set<Object> visited) {

        // Get the object instance from the parent TreeItem
        Object obj = parent.getValue().getInObject();

        if (parent.getValue().getIndex() != null) {
            obj = Array.get(obj, parent.getValue().getIndex());
        }

        // if the object is null, create a TreeItem with a placeholder ObjectReference
        if (obj == null) return new TreeItem<>(new ObjectReference(null, null));


        // If we've already visited this object, add a cyclic reference leaf and return.
        if (visited.contains(obj)) {
            parent.getChildren().add(new TreeItem<>(new ObjectReference(obj, null)));
            return parent;
        }

        visited.add(obj);


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
                    parent.getChildren().add(handleArray(new TreeItem<>(new ObjectReference(finalObj, field)), visited));
                } else if (field.getType().isEnum()) {
                    parent.getChildren().add(new TreeItem<>(new ObjectReference(finalObj, field)));
                }
                // Recursively handle nested objects
                else if (!field.getType().isPrimitive() && !ReflectionUtil.isWrapperOrString(field.getType())) {
                    parent.getChildren().add(createTree(new TreeItem<>(new ObjectReference(fieldValue, field)), visited));
                } else {
                    TreeItem<ObjectReference> childNode = new TreeItem<>(new ObjectReference(finalObj, field));
                    parent.getChildren().add(childNode);

                }
            } catch (IllegalAccessException e) {
                log.error("Error getting field value", e);
            }
        });

        return parent;
    }


    /**
     * Handle an array object
     *
     * @return The root TreeItem for the array object
     */
    private static TreeItem<ObjectReference> handleArray(TreeItem<ObjectReference> parent, Set<Object> visited) {
        ObjectReference parentValue = parent.getValue();
        Object array = ReflectionUtil.getFieldValue(parentValue.getInObject(), parentValue.getField());
        int length = Array.getLength(array);

        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            TreeItem<ObjectReference> child = new TreeItem<>(new ObjectReference(array, parentValue.getField(), i));

            if (element != null && visited.contains(element)) {
                child.getChildren().add(new TreeItem<>(new ObjectReference(element, null)));
            }
            if (element != null && !element.getClass().isPrimitive() && !ReflectionUtil.isWrapperOrString(element.getClass())) {
                child = createTree(child, visited);
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
    public static void expandAll(TreeItem<?> item) {
        item.setExpanded(true);
        for (TreeItem<?> child : item.getChildren()) {
            expandAll(child);
        }
    }
}
