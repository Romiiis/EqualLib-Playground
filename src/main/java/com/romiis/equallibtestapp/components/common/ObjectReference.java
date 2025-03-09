package com.romiis.equallibtestapp.components.common;

import com.romiis.equallibtestapp.util.ReflectionUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.sql.Wrapper;
import java.util.*;

import static com.fasterxml.jackson.databind.type.LogicalType.Collection;

/**
 * ObjectReference.java
 * <p>
 * Represents a reference to a field in an object for the TreeView. Contains information about the field and the object
 * Used to display and modify field values in the TreeView
 */
@Slf4j
@Getter
public class ObjectReference {

    // Reference to the object containing the field
    @Setter
    private Object inObject;

    // Field that is referenced
    private final Field field;

    // Index of the array element (if the field is an array)
    private final Integer index;

    // String representation of a null value
    private final String NULL = "null";

    // Cycle detection flag
    private boolean cyclic = false;

    private boolean editContentItem = false;


    /**
     * Create a new ObjectReference instance
     *
     * @param reference Object containing the field
     * @param fieldName Field that is referenced
     */
    public ObjectReference(Object reference, Field fieldName) {
        this.inObject = reference;
        this.field = fieldName;
        index = null;

    }

    public ObjectReference(boolean editContentItem, Object reference, Field fieldName) {
        this.inObject = reference;
        this.field = fieldName;
        index = null;
        this.editContentItem = editContentItem;

    }


    public ObjectReference(Object reference, Field fieldName, boolean cyclic) {
        this.inObject = reference;
        this.field = fieldName;
        index = null;
        this.cyclic = cyclic;
    }

    /**
     * Create a new ObjectReference instance for an array element
     *
     * @param reference Object containing the array
     * @param fieldName Field that is referenced
     * @param index     Index of the array element
     */
    public ObjectReference(Object reference, Field fieldName, int index) {
        this.inObject = reference;
        this.field = fieldName;
        this.index = index;
    }


    /**
     * Modify the value of the field
     *
     * @param newValue New value to set
     */
    public void modifyFieldValue(String newValue) {
        try {
            // If the field is an array, update the element at the given index.
            if (field.getType().isArray()) {
                if (index == null) {
                    return;
                }
                Class<?> componentType = field.getType().getComponentType();
                Object convertedValue;
                // For primitives or wrappers/Strings, convert the newValue to the proper type.
                if (componentType.isPrimitive() || ReflectionUtil.isWrapperOrString(componentType)) {
                    convertedValue = ReflectionUtil.convertStringToPrimitive(newValue, componentType);
                } else {
                    // For non-primitive components, assume the newValue is acceptable as-is.
                    convertedValue = newValue;
                }
                Array.set(inObject, index, convertedValue);

            } else if (field.getType().isPrimitive()) {
                // Convert the newValue to the correct primitive type.
                Object convertedValue = ReflectionUtil.convertStringToPrimitive(newValue, field.getType());
                // Use the appropriate setter method for each primitive type.
                if (field.getType().equals(int.class)) {
                    field.setInt(inObject, (Integer) convertedValue);
                } else if (field.getType().equals(double.class)) {
                    field.setDouble(inObject, (Double) convertedValue);
                } else if (field.getType().equals(float.class)) {
                    field.setFloat(inObject, (Float) convertedValue);
                } else if (field.getType().equals(long.class)) {
                    field.setLong(inObject, (Long) convertedValue);
                } else if (field.getType().equals(short.class)) {
                    field.setShort(inObject, (Short) convertedValue);
                } else if (field.getType().equals(byte.class)) {
                    field.setByte(inObject, (Byte) convertedValue);
                } else if (field.getType().equals(boolean.class)) {
                    field.setBoolean(inObject, (Boolean) convertedValue);
                } else if (field.getType().equals(char.class)) {
                    field.setChar(inObject, (Character) convertedValue);
                }

            } else if (field.getType().isEnum()) {
                // Convert the string to the appropriate enum constant.
                Object enumValue = Arrays.stream(field.getType().getEnumConstants())
                        .filter(e -> e.toString().equals(newValue))
                        .findFirst()
                        .orElse(null);
                if (enumValue != null) {
                    field.set(inObject, enumValue);
                } else {
                    throw new IllegalArgumentException("Invalid enum value: " + newValue);
                }

            } else if (ReflectionUtil.isWrapperOrString(field.getType())) {
                // For String types, just set the new value.
                if (field.getType().equals(String.class)) {
                    field.set(inObject, newValue);
                } else {
                    // For wrapper classes, convert using the helper method.
                    Object convertedValue = ReflectionUtil.convertStringToPrimitive(newValue, field.getType());
                    field.set(inObject, convertedValue);
                }

            } else {
                // For other object types, attempt to set the value directly.
                field.set(inObject, newValue);
            }
        } catch (IllegalAccessException e) {
            log.error("Error setting field value", e);
        }
    }



    /**
     * Get the value of the field
     *
     * @return Value of the field
     */
    public Object getFieldValue() {
        try {
            // Check if the field is an array type
            if (field.getType().isArray()) {
                // Get the array value
                if (index == null) {
                    return NULL;
                }
                Object val = Array.get(inObject, index);
                return Objects.requireNonNullElse(val, NULL);

            }

            // For non-array fields (primitives, Strings, etc.)
            return Objects.requireNonNullElse(field.get(inObject), NULL);

        } catch (IllegalAccessException e) {
            log.error("Error getting field value", e);
            return "";
        }
    }


    /**
     * Check if the field is modifiable
     *
     * @return True if the field is modifiable, false otherwise
     */
    public boolean isModifiable() {
        boolean isModifiable = true;
        if (editContentItem) {
            return true;
        }
        if (field == null) {
            isModifiable = false;
        } else if (!field.getType().isPrimitive() && !ReflectionUtil.isWrapperOrString(field.getType()) && !field.getType().isEnum()) {
            isModifiable = false;
        } // Static final fields are not modifiable
        else if (!ReflectionUtil.isModifiable(field.getModifiers())) {
            isModifiable = false;
        }
        return isModifiable;
    }


    @Override
    public String toString() {

        if (editContentItem) {
            return "(Edit content ...)";
        }

        String cyclicMarker = cyclic ? " (\uD83D\uDD01)" : "";
        String fullClassName = inObject.getClass().getName();
        String objectIdHex = Integer.toHexString(System.identityHashCode(inObject));
        String objectId = fullClassName + "@" + objectIdHex;
        String simpleClassName = inObject.getClass().getSimpleName();

        if (field == null) {
            return String.format("%s {%s}%s", simpleClassName, objectId, cyclicMarker);
        }

        String modifiers = getModifiers(field);
        String fieldName = field.getName();
        String fieldType = field.getType().getSimpleName();

        if (index != null && field.getType().isArray()) {
            String componentType = field.getType().getComponentType().getSimpleName();
            return String.format("%s[%d]: %s", componentType, index, getFieldValue());
        } else if (field.getType().isPrimitive() || ReflectionUtil.isWrapperOrString(field.getType()) || field.getType().isEnum()) {
            return String.format("%s %s {%s}: %s", modifiers, fieldName, fieldType, getFieldValue());
        } else if (field.getType().isArray()) {
            try {
                Object array = field.get(inObject);
                if (array == null) {
                    return String.format("%s %s {%s} [%s]", modifiers, fieldName, fieldType, NULL);
                }
            return String.format("%s %s {%s} [%d]", modifiers, fieldName, fieldType, Array.getLength(field.get(inObject)));
            } catch (IllegalAccessException e) {
                log.error("Error getting field value", e);
                return "";
            }

        } else if (Collection.class.isAssignableFrom(field.getType())) {
            // Show what type of collection and content type
            String genericType = field.getGenericType().getTypeName().getClass().getSimpleName();
            return String.format("%s %s {%s<%s>} {%s%s}", modifiers, fieldName, fieldType, genericType, objectId, cyclicMarker);

        } else if (Map.class.isAssignableFrom(field.getType())) {
            String keyTypeStr = "Unknown";
            String valueTypeStr = "Unknown";

            if (field.getGenericType() instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) field.getGenericType();
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs != null && typeArgs.length == 2) {
                    keyTypeStr = typeArgs[0].getTypeName(); // e.g., "java.lang.String"
                    valueTypeStr = typeArgs[1].getTypeName(); // e.g., "java.lang.Integer"
                }
            }

            return String.format("%s %s {%s<%s, %s>} {%s%s}",
                    modifiers, fieldName, fieldType, keyTypeStr, valueTypeStr, objectId, cyclicMarker);
        }
        else {
            return String.format("%s %s {%s} {%s%s}", modifiers, fieldName, fieldType, objectId, cyclicMarker);
        }
    }




    /**
     * Get the modifiers of the field as a string
     */
    private String getModifiers(Field field) {
        int mod = field.getModifiers();
        List<String> modifiers = new ArrayList<>();

        if (Modifier.isPublic(mod)) {
            modifiers.add("🌍");   // public
        }
        if (Modifier.isPrivate(mod)) {
            modifiers.add("🔒");   // private
        }
        if (Modifier.isProtected(mod)) {
            modifiers.add("\uD83D\uDEE1"); // protected
        }
        if (Modifier.isStatic(mod)) {
            modifiers.add("⚡");   // static
        }
        if (Modifier.isFinal(mod)) {
            modifiers.add("🏁");   // final
        }
        if (Modifier.isVolatile(mod)) {
            modifiers.add("💨");   // volatile
        }
        if (Modifier.isTransient(mod)) {
            modifiers.add("🚫");   // transient
        }

        if ((mod & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED)) == 0) {
            modifiers.add("📦");   // package-private (default)
        }

        // return the modifiers as a string
        return modifiers.isEmpty() ? "" : "[" + String.join(", ", modifiers) + "]";
    }


}
