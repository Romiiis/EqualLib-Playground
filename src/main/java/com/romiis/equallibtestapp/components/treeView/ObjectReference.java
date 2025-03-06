package com.romiis.equallibtestapp.components.treeView;

import com.romiis.equallibtestapp.util.ReflectionUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Getter
public class ObjectReference {

    // Reference to the object containing the field
    private final Object inObject;

    // Field that is referenced
    private final Field field;

    // Index of the array element (if the field is an array)
    private final Integer index;

    private final String NULL = "NULL";


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
            if (field.getType().isArray()) {
                if (index == null) {
                    return;
                }
                // Set the array value
                Array.set(inObject, index, newValue);

            } else if (field.getType().isPrimitive()) {
                // Set the primitive value
                if (field.getType().equals(int.class)) {
                    field.setInt(inObject, Integer.parseInt(newValue));
                } else if (field.getType().equals(double.class)) {
                    field.setDouble(inObject, Double.parseDouble(newValue));
                } else if (field.getType().equals(float.class)) {
                    field.setFloat(inObject, Float.parseFloat(newValue));
                } else if (field.getType().equals(long.class)) {
                    field.setLong(inObject, Long.parseLong(newValue));
                } else if (field.getType().equals(short.class)) {
                    field.setShort(inObject, Short.parseShort(newValue));
                } else if (field.getType().equals(byte.class)) {
                    field.setByte(inObject, Byte.parseByte(newValue));
                } else if (field.getType().equals(boolean.class)) {
                    field.setBoolean(inObject, Boolean.parseBoolean(newValue));
                } else if (field.getType().equals(char.class)) {
                    field.setChar(inObject, newValue.charAt(0));
                }
            } else if (field.getType().isEnum()) {

                // Set the enum value
                Object enumValue = Arrays.stream(field.getType().getEnumConstants())
                        .filter(e -> e.toString().equals(newValue))
                        .findFirst()
                        .orElse(null);

                field.set(inObject, enumValue);
            } else {
                // Set the value for non-primitive fields
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
        if (field == null) {
            isModifiable = false;
        } else if (!field.getType().isPrimitive() && !ReflectionUtil.isWrapperOrString(field.getType()) && !field.getType().isEnum()) {
            isModifiable = false;
        } // Static final fields are not modifiable
        else if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
            isModifiable = false;
        }
        return isModifiable;
    }


    /**
     * Get the string representation of the ObjectReference
     *
     * @return String representation of the ObjectReference
     */
    @Override
    public String toString() {
        if (field == null) {
            return inObject.getClass().getSimpleName();
        }

        String fieldType = field.getType().getSimpleName();
        String fieldName = field.getName();
        String modifiers = getModifiers(field);

        if (index != null && field.getType().isArray()) {
            return String.format("%s[%d]: %s", field.getType().getComponentType().getSimpleName(), index, getFieldValue());
        } else if (field.getType().isPrimitive() || ReflectionUtil.isWrapperOrString(field.getType())) {
            return String.format("%s %s (%s): %s", modifiers, fieldName, fieldType, getFieldValue());
        } else if (field.getType().isEnum()) {
            return String.format("%s %s (%s): %s", modifiers, fieldName, fieldType, getFieldValue());
        } else {
            return String.format("%s %s (%s)", modifiers, fieldName, fieldType);
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

        /// return the modifiers as a string
        return modifiers.isEmpty() ? "" : "[" + String.join(", ", modifiers) + "]";
    }


}
