package com.romiis.equallibtestapp.util;

import com.romiis.equallibtestapp.components.common.ObjectReference;
import javafx.scene.control.TreeItem;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ObjectTreeBuilder {

    /**
     * Create a tree structure for the given object, but lazily.
     * <p>
     * Instead of immediately populating children, we create a LazyTreeItem.
     * That item will load its children on first expansion.
     */
    public static TreeItem<ObjectReference> createTree(TreeItem<ObjectReference> parent,
                                                       Set<Object> visited,
                                                       boolean nestedInCollectionOrArray) {
        // We just return a LazyTreeItem that wraps the parent’s ObjectReference.
        // The actual reflection logic is in LazyTreeItem.loadChildren().
        return new LazyTreeItem(parent.getValue(), visited, nestedInCollectionOrArray);
    }

    /**
     * Handle an array object.
     * <p>
     * (Identical to your original logic except that 'parent' here should be a LazyTreeItem.)
     */
    public static TreeItem<ObjectReference> handleArray(TreeItem<ObjectReference> parent,
                                                        Set<Object> visited,
                                                        boolean nestedInCollectionOrArray) {

        ObjectReference parentValue = parent.getValue();
        Object array = ReflectionUtil.getFieldValue(parentValue.getInObject(), parentValue.getField());
        int length = Array.getLength(array);

        if (!nestedInCollectionOrArray) {
            // add the "edit" node if not nested
            TreeItem<ObjectReference> edit = new TreeItem<>(
                    new ObjectReference(true, array, parentValue.getField())
            );
            parent.getChildren().add(edit);
        }

        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);

            // Create a child that references array + field + index
            TreeItem<ObjectReference> child = new LazyTreeItem(
                    new ObjectReference(array, parentValue.getField(), i),
                    visited,
                    nestedInCollectionOrArray
            );

            // If the element is not null and already visited => cycle
            if (element != null && visited.contains(element)) {
                child.getChildren().add(
                        new TreeItem<>(new ObjectReference(element, null, true))
                );
            }

            // If the element is an enum => replace child with a direct leaf
            if (element != null && element.getClass().isEnum()) {
                child = new TreeItem<>(new ObjectReference(element, null, i));
            }
            // If the element is a complex object => we remain with the lazy item
            // (which will expand deeper if user opens it)
            // Otherwise (primitive/wrapper/string), the lazy item won't have children.

            parent.getChildren().add(child);
        }

        return parent;
    }

    /**
     * Expand all items in the TreeView (root and all children).
     * <p>
     * (Unchanged – when you expand, lazy nodes will load children.)
     */
    public static void expandAll(TreeItem<?> item) {
        item.setExpanded(true);
    }

    /**
     * A custom TreeItem that duplicates the original reflection logic,
     * but only executes it once (lazily) on first expansion.
     */
    private static class LazyTreeItem extends TreeItem<ObjectReference> {

        private final Set<Object> visited;
        private final boolean nestedInCollectionOrArray;
        private boolean childrenLoaded = false;

        public LazyTreeItem(ObjectReference value, Set<Object> visited, boolean nestedInCollectionOrArray) {
            super(value);
            this.visited = visited;
            this.nestedInCollectionOrArray = nestedInCollectionOrArray;

            // Attach a listener that loads children the first time it is expanded
            expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
                if (isNowExpanded && !childrenLoaded) {
                    loadChildren();
                    childrenLoaded = true;
                }
            });
        }

        /**
         * Perform the exact same reflection logic that was originally in createTree(...),
         * but do it here so it only runs when expanded.
         */
        private void loadChildren() {
            // The original code used "parent" TreeItem, but here "this" is the TreeItem
            // so we replicate that logic with "this" references.
            Object obj = getValue().getInObject();

            // If index != null, it means this TreeItem references a specific array element
            if (getValue().getIndex() != null && obj != null) {
                obj = Array.get(obj, getValue().getIndex());
            }

            // if the object is null, there's nothing to reflect
            if (obj == null) {
                // add a placeholder if you want (the original code returned a new node)
                return;
            }

            // If we've already visited, add a cycle leaf and stop
            if (visited.contains(obj)) {
                if (!getValue().getField().getType().isArray()){
                    getChildren().add(
                            new TreeItem<>(new ObjectReference(obj, null, true))
                    );
            }

                return;
            }

            visited.add(obj);

            // If it's a collection or map at top level, add "edit" node
            if ((obj instanceof Collection || obj instanceof Map) && !nestedInCollectionOrArray) {
                TreeItem<ObjectReference> edit = new TreeItem<>(
                        new ObjectReference(true, obj, getValue().getField())
                );
                getChildren().add(edit);
            }

            // Reflect all fields (same code as your original)
            Field[] fields = ReflectionUtil.getAllFields(obj.getClass());
            Object finalObj = obj;

            Arrays.stream(fields).forEach(field -> {
                field.setAccessible(true);
                try {
                    Object fieldValue = field.get(finalObj);

                    // If the object is itself a collection/map, or we were already nested
                    // pass that forward
                    boolean nestedInCol = (finalObj instanceof Collection || finalObj instanceof Map)
                            || nestedInCollectionOrArray;

                    // If fieldValue is null
                    if (fieldValue == null) {
                        getChildren().add(
                                new TreeItem<>(new ObjectReference(finalObj, field, null))
                        );
                        return;
                    }

                    if (visited.contains(fieldValue)) {
                        getChildren().add(
                                new TreeItem<>(new ObjectReference(finalObj, field, true))
                        );
                        return;
                    }

                    // If it's an array => handle array
                    if (field.getType().isArray()) {
                        TreeItem<ObjectReference> arrayItem = new LazyTreeItem(
                                new ObjectReference(finalObj, field),
                                visited,
                                nestedInCol
                        );
                        getChildren().add(
                                handleArray(arrayItem, visited, nestedInCol)
                        );
                    }
                    // If it's an enum
                    else if (field.getType().isEnum()) {
                        getChildren().add(
                                new TreeItem<>(new ObjectReference(finalObj, field))
                        );
                    }
                    // If it's a complex object => recurse
                    else if (!field.getType().isPrimitive()
                            && !ReflectionUtil.isWrapperOrString(field.getType())) {
                        TreeItem<ObjectReference> child = new LazyTreeItem(
                                new ObjectReference(fieldValue, field),
                                visited,
                                nestedInCol
                        );
                        getChildren().add(child);
                    }
                    // else => it's a primitive/wrapper/string => direct leaf
                    else {
                        TreeItem<ObjectReference> childNode = new TreeItem<>(
                                new ObjectReference(finalObj, field)
                        );
                        getChildren().add(childNode);
                    }

                } catch (IllegalAccessException e) {
                    log.error("Error getting field value", e);
                }
            });
        }

        /**
         * By default, we return false so it can be expanded.
         * Otherwise, JavaFX might treat it as a leaf before we load children.
         */
        @Override
        public boolean isLeaf() {
            // If you truly want to disable expansion for, say, primitives or strings,
            // you'd do a type check here. But to mirror the original code's structure
            // (where everything can be expanded), we return false.
            return false;
        }
    }
}
