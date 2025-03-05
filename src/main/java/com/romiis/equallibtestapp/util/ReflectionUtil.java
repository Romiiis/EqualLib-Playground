package com.romiis.equallibtestapp.util;

import java.lang.reflect.Field;

public class ReflectionUtil {

    /**
     * Get all fields of a class including fields from superclasses
     *
     * @param clazz class to get fields from
     * @return array of fields
     */
    public static Field[] getAllFields(Class<?> clazz) {

        if (clazz.equals(Object.class)) {
            return new Field[0];
        }

        // go to the superclass and get all fields
        Field[] fields = clazz.getDeclaredFields();

        // if superclass is not Object, get all fields from superclass
        while (clazz.getSuperclass() != Object.class) {
            clazz = clazz.getSuperclass();
            Field[] superFields = clazz.getDeclaredFields();
            Field[] temp = new Field[fields.length + superFields.length];
            System.arraycopy(fields, 0, temp, 0, fields.length);
            System.arraycopy(superFields, 0, temp, fields.length, superFields.length);
            fields = temp;
        }
        return fields;
    }

    /**
     * Create instance of a class
     *
     * @param clazz class to create instance of
     * @return instance of the class
     */
    public static Object createInstance(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Get value of a field in an object
     *
     * @param obj   object to get field value from
     * @param field field to get value of
     * @return value of the field
     */
    public static Object getFieldValue(Object obj, Field field) {
        try {
            field.setAccessible(true);
            return field.get(obj);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Check if the given class is a wrapper class or a String
     *
     * @param type The class to check
     * @return True if the class is a wrapper class or a String, false otherwise
     */
    public static boolean isWrapperOrString(Class<?> type) {
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
}
