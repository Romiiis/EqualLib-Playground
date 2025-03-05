package com.romiis.equallibtestapp.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class ObjectReference {
    private final Object inObject;
    private final Field field;
    private Integer index;


    public ObjectReference(Object reference, Field fieldName) {
        this.inObject = reference;
        this.field = fieldName;
        index = null;

    }

    public ObjectReference(Object reference, Field fieldName, int index) {
        this.inObject = reference;
        this.field = fieldName;
        this.index = index;
    }


    public void modifyFieldValue(String newValue) {
        try {
            if (field.getType().isArray()) {
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
            } else {
                // Set the value for non-primitive fields
                field.set(inObject, newValue);
            }
        } catch (IllegalAccessException e) {
            log.error("Error setting field value", e);
        }
    }

    public Object getFieldValue() {
        try {
            // Check if the field is an array type
            if (field.getType().isArray()) {
                // Get the array value
                return Array.get(inObject, index);

            }

            // For non-array fields (primitives, Strings, etc.)
            return field.get(inObject);

        } catch (IllegalAccessException e) {
            log.error("Error getting field value", e);
            return "";
        }
    }


    @Override
    public String toString() {
        if (field == null) {
            return inObject.getClass().getSimpleName();
        }

        String fieldType = field.getType().getSimpleName();
        String fieldName = field.getName();
        String modifiers = getModifiers(field);

        if (index != null && field.getType().isArray()) {
            return String.format("%s %s[%d] (%s): %s", modifiers, fieldName, index, field.getType().getComponentType().getSimpleName(), getFieldValue());
        } else if (field.getType().isPrimitive() || ReflectionUtil.isWrapperOrString(field.getType())) {
            return String.format("%s %s (%s): %s", modifiers, fieldName, fieldType, getFieldValue());
        } else {
            return String.format("%s %s (%s)", modifiers, fieldName, fieldType);
        }
    }

    /**
     * Pomocná metoda pro získání zkrácených modifikátorů
     */
    private String getModifiers(Field field) {
        int mod = field.getModifiers();
        List<String> modifiers = new ArrayList<>();

        // Přidání emoji pro modifikátory
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
            modifiers.add("🏁");   // final - Cíl nebo závěr, něco, co nelze změnit
        }

        // Pokud je více než jeden modifikátor, zkrátíme a oddělíme je čárkami
        return modifiers.isEmpty() ? "" : "[" + String.join(", ", modifiers) + "]";
    }


}
