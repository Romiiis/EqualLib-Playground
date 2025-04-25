package com.romiiis.equallib_playground.util;

import com.romiiis.equallib_playground.components.treeView.FieldNode;
import com.romiiis.equallib_playground.components.treeView.FieldNodeType;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Builds a tree of objects with lazy-loaded children.
 * This class is responsible for creating a tree structure from an object,
 *
 * @author Romiiis
 * @version 1.0
 */
@Log4j2

public class ObjectTreeBuilder {

    /**
     * The default length of an array to show in the tree.
     */
    private final int DEFAULT_ARRAY_LENGTH = 10;

    /**
     * The set of visited objects to prevent infinite recursion.
     */
    private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());


    /**
     * Builds a tree starting from the given root object with lazy-loaded children.
     *
     * @param root     the object to build the tree from
     * @param maxDepth the maximum depth to explore (0 means only the root)
     * @return the root FieldNode representing the object tree
     */
    public FieldNode buildTree(Object root, int maxDepth) {
        return buildNode("root", root, null, null, 0, maxDepth);
    }


    /**
     * Builds a FieldNode from the given object, field, and parent object.
     *
     * @param name         the name of the field
     * @param obj          the object to build the node from
     * @param field        the field of the object
     * @param parentObject the parent object of the object
     * @param currentDepth the current depth of the node
     * @param maxDepth     the maximum depth to explore
     * @return the FieldNode representing the object
     */
    protected FieldNode buildNode(String name, Object obj, Field field, Object parentObject,
                                  int currentDepth, int maxDepth) {
        Class<?> type;
        if (obj != null) {
            type = obj.getClass();
        } else if (field != null) {
            type = field.getType();
        } else {
            type = null;
        }
        // If the object is non-null, add it to the visited set.
        if (obj != null) {
            visited.add(obj);
        }

        return new FieldNode(this, name, type, obj, field, parentObject, currentDepth, maxDepth, determineNodeType(parentObject, field));
    }


    /**
     * Builds the children of the given object with lazy-loaded children.
     *
     * @param obj          the object to build the children from
     * @param currentDepth the current depth of the node
     * @param maxDepth     the maximum depth to explore
     * @param visited      the set of visited objects
     * @param owner        the parent object of the object
     * @return the list of FieldNodes representing the children of the object
     */
    public List<FieldNode> buildChildren(Object obj, int currentDepth, int maxDepth, Set<Object> visited, Object owner) {
        List<FieldNode> children = new ArrayList<>();
        if (obj == null || currentDepth > maxDepth) {
            return children;
        }
        // Check if this object has been visited already
        if (visited.contains(obj)) {
            // Optionally, you can add a cyclic reference node instead of simply returning.
            FieldNode cycleNode = new FieldNode(this, "(cyclic reference)", null, null, null, obj, currentDepth, maxDepth, FieldNodeType.NON_EDITABLE);
            children.add(cycleNode);
            return children;
        }

        Class<?> clazz = obj.getClass();

        // Do not expand simple types (primitives, wrappers, String, enums)
        if (isSimpleType(clazz)) {
            return children;
        }

        // Handling arrays, collections, maps, and regular objects remains the same,
        // but ensure you pass the visited set to all recursive calls.
        if (clazz.isArray()) {
            int length = Array.getLength(obj);
            int countToShow = Math.min(length, DEFAULT_ARRAY_LENGTH);
            // Only add the edit node if the owner is not a Collection or Map.
            if (!(owner instanceof Collection) && !(owner instanceof Map)) {
                FieldNode editNode = new FieldNode(
                        this, "(Edit Array...)", null, obj, null, obj, currentDepth, maxDepth, FieldNodeType.ARRAY_EDITABLE
                );
                children.add(editNode);
            }

            // Get the array's component type (even if elements are null)
            Class<?> componentType = clazz.getComponentType();

            for (int i = 0; i < countToShow; i++) {
                Object element = Array.get(obj, i);
                FieldNode node;
                if (element == null) {
                    // When the element is null, pass the component type as the type info.
                    node = new FieldNode(this, "[" + i + "]", componentType, null, null, obj, currentDepth, maxDepth, FieldNodeType.NON_EDITABLE);
                } else {
                    // Otherwise, use the usual buildNode method.
                    node = buildNode("[" + i + "]", element, null, obj, currentDepth, maxDepth);
                }
                children.add(node);
            }
            if (length > DEFAULT_ARRAY_LENGTH) {
                FieldNode summaryNode = new FieldNode(
                        this, "(" + (length - DEFAULT_ARRAY_LENGTH) + " more...)", null, null, null, obj, currentDepth, maxDepth, FieldNodeType.INFO
                );
                children.add(summaryNode);
            }
        } else {

            if (Collection.class.isAssignableFrom(clazz) && !(owner instanceof Collection) && !(owner instanceof Map)) {
                Field node = ReflectionUtil.getFieldFromParent(owner, obj);
                FieldNode editNode = new FieldNode(this, "(Edit Collection...)", clazz.getComponentType(), obj, node, obj, currentDepth, maxDepth, FieldNodeType.ARRAY_EDITABLE);
                children.add(editNode);
            } else if (Map.class.isAssignableFrom(clazz) && !(owner instanceof Map) && !(owner instanceof Collection)) {
                Field node = ReflectionUtil.getFieldFromParent(owner, obj);

                FieldNode editNode = new FieldNode(this, "(Edit Map...)", clazz.getComponentType(), obj, node, obj, currentDepth, maxDepth, FieldNodeType.ARRAY_EDITABLE);
                children.add(editNode);
            }

            Field[] fields = ReflectionUtil.getAllFields(clazz);
            for (Field f : fields) {
                f.setAccessible(true);
                try {
                    Object fieldValue = f.get(obj);
                    FieldNode node = buildNode(f.getName(), fieldValue, f, obj, currentDepth, maxDepth);
                    children.add(node);
                } catch (IllegalAccessException e) {
                    log.error("Could not access field " + f.getName(), e);
                }
            }
        }
        return children;
    }


    /**
     * Determines the type of the field node based on the field and parent object.
     *
     * @param parentObject the parent object of the field
     * @param f            the field to determine the type of
     * @return the type of the field node
     */
    private FieldNodeType determineNodeType(Object parentObject, Field f) {
        if (f == null || parentObject == null) {
            return FieldNodeType.NON_EDITABLE;
        } else if (Modifier.isFinal(f.getModifiers()) && Modifier.isStatic(f.getModifiers())) {
            return FieldNodeType.NON_EDITABLE;
        } else if (parentObject.getClass().isArray()) {
            return FieldNodeType.NON_EDITABLE;
        } else if (isSimpleType(f.getType())) {
            return FieldNodeType.EDITABLE;
        }


        return FieldNodeType.NON_EDITABLE;
    }

    /**
     * Checks if the given class is a simple type.
     *
     * @param clazz the class to check
     * @return true if the class is a simple type, false otherwise
     */
    public boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz.equals(String.class)
                || clazz.equals(Boolean.class)
                || clazz.equals(Byte.class)
                || clazz.equals(Character.class)
                || clazz.equals(Short.class)
                || clazz.equals(Integer.class)
                || clazz.equals(Long.class)
                || clazz.equals(Float.class)
                || clazz.equals(Double.class)
                || clazz.isEnum();
    }

}
