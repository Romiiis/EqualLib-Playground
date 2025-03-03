package com.romiis.equallibtestapp.util;

import java.lang.reflect.Field;

public class ReflectionUtil {
    public static Field[] getAllFields(Class<?> clazz) {

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


    public static Object getFieldValue(Object obj, Field field) {
        try {
            field.setAccessible(true);
            return field.get(obj);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
