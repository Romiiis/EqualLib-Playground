package com.romiiis.equallib_playground.components.treeView;

import com.romiiis.equallib_playground.util.ObjectTreeBuilder;
import lombok.Getter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * FieldNode.java
 * <p>
 * Represents a node in the object tree, encapsulating field information and its value.
 * <p>
 * This class is used to represent a node in the object tree. It contains information about the field, its value,
 * and its children. It also provides methods to update the value and get the children.
 *
 * @author Romiiis
 * @version 1.0
 */
@Getter
public class FieldNode {
    private final String name;
    private final Class<?> type;
    private Object value;
    private final Field field;
    private final Object parentObject;
    private List<FieldNode> children;
    private boolean childrenLoaded = false;
    private final int currentDepth;
    private final int maxDepth;
    private final FieldNodeType nodeType; // New field indicating node type
    private final ObjectTreeBuilder builder;

    /**
     * Constructor for FieldNode.
     *
     * @param builder      The ObjectTreeBuilder instance used to build the tree.
     * @param name         The name of the field.
     * @param type         The type of the field.
     * @param value        The value of the field.
     * @param field        The Field object representing the field.
     * @param parentObject The parent object containing this field.
     * @param currentDepth The current depth in the tree.
     * @param maxDepth     The maximum depth allowed in the tree.
     * @param nodeType     The type of this node (e.g., ARRAY_EDITABLE, INFO).
     */
    public FieldNode(ObjectTreeBuilder builder, String name, Class<?> type, Object value, Field field, Object parentObject,
                     int currentDepth, int maxDepth, FieldNodeType nodeType) {
        this.builder = builder;
        this.name = name;
        this.type = type;
        this.value = value;
        this.field = field;
        this.parentObject = parentObject;
        this.currentDepth = currentDepth;
        this.maxDepth = maxDepth;
        this.nodeType = nodeType;
        this.children = new ArrayList<>();
    }


    /**
     * Returns the children of this node. Children are computed lazily when first requested,
     * provided that the current depth is less than the max depth.
     * @return a list of child FieldNode objects
     */
    public List<FieldNode> getChildren() {
        // If this node is part of an array and isn't the special ARRAY_EDITABLE node,
        // then it should not be expandable.
        if (parentObject != null && parentObject.getClass().isArray() && nodeType != FieldNodeType.ARRAY_EDITABLE) {
            return Collections.emptyList();
        }
        if (nodeType == FieldNodeType.ARRAY_EDITABLE && (parentObject != null && (Collection.class.isAssignableFrom(parentObject.getClass()) || parentObject.getClass().isArray() || Map.class.isAssignableFrom(parentObject.getClass())))) {
            return Collections.emptyList();
        }
        if (!childrenLoaded && currentDepth < maxDepth) {
            Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            children = builder.buildChildren(value, currentDepth + 1, maxDepth, visited, this.parentObject);
            childrenLoaded = true;
        }
        return children;
    }


    /**
     * Updates the value represented by this node and writes the change back to the underlying object,
     * if possible.
     *
     * @param newValue the new value to set
     */
    public void updateValue(Object newValue) {
        if (field != null && parentObject != null) {
            try {
                field.setAccessible(true);
                field.set(parentObject, newValue);
                this.value = newValue;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            this.value = newValue;
        }
    }

    /**
     * Returns a string representing the field's modifiers as icons.
     */
    private String getModifierIcons() {
        if (field == null) {
            return "";
        }
        int mod = field.getModifiers();
        List<String> modifiers = new ArrayList<>();

        if (Modifier.isPublic(mod)) {
            modifiers.add("\uD83C\uDF0D");   // public
        }
        if (Modifier.isPrivate(mod)) {
            modifiers.add("\uD83D\uDD12");   // private
        }
        if (Modifier.isProtected(mod)) {
            modifiers.add("\uD83D\uDEE1"); // protected
        }
        if (Modifier.isStatic(mod)) {
            modifiers.add("⚡");   // static
        }
        if (Modifier.isFinal(mod)) {
            modifiers.add("\uD83C\uDFC1");   // final
        }
        if (Modifier.isVolatile(mod)) {
            modifiers.add("\uD83D\uDCA8");   // volatile
        }
        if (Modifier.isTransient(mod)) {
            modifiers.add("\uD83D\uDEAB");   // transient
        }

        if ((mod & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED)) == 0) {
            modifiers.add("\uD83D\uDCE6");   // package-private (default)
        }

        // return the modifiers as a string
        return modifiers.isEmpty() ? "" : "[" + String.join(", ", modifiers) + "]";
    }


    @Override
    public String toString() {
        String typeName;
        if (type != null) {
            typeName = type.getSimpleName();
        } else if (value != null) {
            typeName = value.getClass().getSimpleName();
        } else {
            typeName = "unknown";
        }

        // For Collection or Map, append generic type info if available.
        if (field != null && type != null && (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))) {
            typeName += getGenericTypeInfo();
        }

        // For nodes that are ARRAY_EDITABLE or INFO, show only the name.
        if (nodeType == FieldNodeType.ARRAY_EDITABLE || nodeType == FieldNodeType.INFO) {
            return String.format("%s", name);
        }
        if (parentObject != null && parentObject.getClass().isArray()) {
            return String.format("%s %s = %s", name, parentObject.getClass().getComponentType().getSimpleName(), value);
        }

        if (type != null && (type.isArray() || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))) {

            return String.format("%s %s (%s)", getModifierIcons(), name, typeName);

        }
        return String.format("%s %s (%s) = %s", getModifierIcons(), name, typeName, value);
    }

    /**
     * Returns a string representation of the generic type parameters, e.g. "<String, Integer>".
     */
    private String getGenericTypeInfo() {
        try {
            java.lang.reflect.Type genericType = field.getGenericType();
            if (genericType instanceof java.lang.reflect.ParameterizedType) {
                java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) genericType;
                java.lang.reflect.Type[] typeArgs = pt.getActualTypeArguments();
                StringBuilder sb = new StringBuilder("<");
                for (int i = 0; i < typeArgs.length; i++) {
                    java.lang.reflect.Type arg = typeArgs[i];
                    if (arg instanceof Class<?>) {
                        sb.append(((Class<?>) arg).getSimpleName());
                    } else {
                        sb.append(arg.toString());
                    }
                    if (i < typeArgs.length - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(">");
                return sb.toString();
            }
        } catch (Exception e) {
            // In case of any exception, we simply return an empty string.
        }
        return "";
    }


}
