package com.romiis.equallibtestapp.util;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;

public class ReflectionUtil {
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

    public static Object createInstance(Class<?> clazz) {
        // Simply call empty constructor DONT USE OBJECTENESIS
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static Object getFieldValue(Object obj, Field field) {
        try {
            field.setAccessible(true);
            return field.get(obj);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // Metoda pro vypsání všech polí objektu
    public static void printFields(Object obj, IdentityHashMap<Object, Boolean> visited) {
        if (obj == null) {
            System.out.println("Objekt je null");
            return;
        }

        // Pokud jsme již objekt navštívili, vypíšeme pouze jeho hash (cyklické odkazy)
        if (visited.containsKey(obj)) {
            System.out.println("Cyclic reference: " + obj.hashCode());
            return;
        }
        visited.put(obj, true);

        // Vypíše třídu objektu
        System.out.println("Class: " + obj.getClass().getName());

        // Získání všech polí objektu
        Field[] fields = getAllFields(obj.getClass());

        for (Field field : fields) {
            field.setAccessible(true); // Abychom se dostali k privátním polím
            try {
                Object value = field.get(obj); // Získání hodnoty pole
                System.out.print(field.getName() + " = ");

                if (value == null) {
                    System.out.println("null");
                } else {
                    // Pokud je to primitivní typ
                    if (field.getType().isPrimitive()) {
                        System.out.println(value);
                    } else if (value instanceof String || value instanceof Integer || value instanceof Boolean || value instanceof Double) {
                        // Pokud je to referenční typ, který lze jednoduše vypisovat
                        System.out.println(value);
                    } else if (value instanceof Collection) {
                        // Pokud je to kolekce (např. HashSet), vypíše každý prvek
                        System.out.println("Collection (size: " + ((Collection<?>) value).size() + "):");
                        for (Object element : (Collection<?>) value) {
                            printFields(element, visited);  // Rekurzivní volání pro každý prvek kolekce
                        }
                    } else if (value.getClass().isArray()) {
                        // Pokud je to pole, prochází každou hodnotu v poli
                        System.out.println("Array (length: " + ((Object[]) value).length + "):");
                        for (Object element : (Object[]) value) {
                            printFields(element, visited);  // Rekurzivní volání pro každý prvek pole
                        }
                    } else {
                        // Pro ostatní referenční objekty, rekurzivně vypíše jejich atributy
                        System.out.println("Object:");
                        printFields(value, visited); // Rekurzivní volání pro referenční typy
                    }
                }
            } catch (IllegalAccessException e) {
                System.out.println("Nelze získat hodnotu pole: " + field.getName());
            }
        }
    }
}
