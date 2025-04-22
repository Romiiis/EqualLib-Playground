package com.romiis.equallib_playground.util;

import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for reflection operations
 *
 */
@Log4j2
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


    /**
     * Check if the given classes have a common superclass
     *
     * @param clazz1 the first class
     * @param clazz2 the second class
     * @return true if the classes have a common superclass, false otherwiseł
     */
    public static boolean hasCommonSuperclass(Class<?> clazz1, Class<?> clazz2) {

        if (clazz1.equals(clazz2)) {
            return true;
        }

        // Build the set of superclasses for clazz1, excluding Object.
        Set<Class<?>> superClasses1 = new HashSet<>();
        for (Class<?> c = clazz1.getSuperclass(); c != null && !c.equals(Object.class); c = c.getSuperclass()) {
            superClasses1.add(c);
        }

        // Walk up the hierarchy of clazz2 (excluding Object) and check for a match.
        for (Class<?> c = clazz2.getSuperclass(); c != null && !c.equals(Object.class); c = c.getSuperclass()) {
            if (superClasses1.contains(c)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Get the field from the parent object
     *
     * @param parent parent object
     * @param obj    object to get the field for
     * @return field of the object in the parent object
     */
    public static Field getFieldFromParent(Object parent, Object obj) {
        if (parent == null || obj == null) {
            return null;
        }
        Field[] fields = ReflectionUtil.getAllFields(parent.getClass());
        for (Field f : fields) {
            f.setAccessible(true);
            try {
                if (f.get(parent) == obj) {
                    return f;
                }
            } catch (IllegalAccessException e) {
                log.error("Could not access field " + f.getName(), e);
            }
        }
        return null;
    }

    /**
     * Get the class from the type
     *
     * @param type the type to get the class from
     * @return the class of the type
     */
    public static Class<?> getClassFromType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
        }
        throw new IllegalArgumentException("Type " + type + " is not a class or parameterized type");
    }

    /**
     * Converts a string value to its corresponding primitive type value.
     *
     * @param value         the string representation of the primitive value
     * @param primitiveType the Class object representing the primitive type (e.g., int.class)
     * @return the converted primitive value as an Object
     * @throws IllegalArgumentException if the value cannot be converted or if the type is unsupported
     */
    public static Object convertStringToPrimitive(String value, Class<?> primitiveType) {
        if (value == null) {
            throw new IllegalArgumentException("Input value is null");
        }
        if (primitiveType == int.class || primitiveType == Integer.class) {
            return Integer.parseInt(value);
        } else if (primitiveType == long.class || primitiveType == Long.class) {
            return Long.parseLong(value);
        } else if (primitiveType == double.class || primitiveType == Double.class) {
            return Double.parseDouble(value);
        } else if (primitiveType == float.class || primitiveType == Float.class) {
            return Float.parseFloat(value);
        } else if (primitiveType == boolean.class || primitiveType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (primitiveType == byte.class || primitiveType == Byte.class) {
            return Byte.parseByte(value);
        } else if (primitiveType == short.class || primitiveType == Short.class) {
            return Short.parseShort(value);
        } else if (primitiveType == char.class || primitiveType == Character.class) {
            if (value.length() != 1) {
                throw new IllegalArgumentException("Cannot convert string \"" + value + "\" to char. Expected a single character.");
            }
            return value.charAt(0);
        } else if (primitiveType == String.class) {
            return value;
        } else {
            throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType);
        }
    }

    /**
     * Get the default value for a given type
     *
     * @param type the type to get the default value for
     * @return the default value for the type
     */
    public static Object getDefaultValue(Class<?> type) {
        if (type.equals(int.class) || type.equals(Integer.class)) {
            return 0;
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return 0L;
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return 0.0;
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return 0.0f;
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return false;
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            return (byte) 0;
        } else if (type.equals(short.class) || type.equals(Short.class)) {
            return (short) 0;
        } else if (type.equals(char.class) || type.equals(Character.class)) {
            return '\u0000';
        } else {
            return null;
        }
    }


    /**
     * Check if the given value is in the correct format for the given class
     *
     * @param value the value to check
     * @param clazz the class to check the value against
     * @return true if the value is in the correct format, false otherwise
     */
    public static boolean isCorrectFormat(Object value, Class<?> clazz) {
        if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
            try {
                Integer.parseInt(value.toString());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (clazz.equals(long.class) || clazz.equals(Long.class)) {
            try {
                Long.parseLong(value.toString());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (clazz.equals(double.class) || clazz.equals(Double.class)) {
            try {
                Double.parseDouble(value.toString());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (clazz.equals(float.class) || clazz.equals(Float.class)) {
            try {
                Float.parseFloat(value.toString());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
            String val = value.toString().toLowerCase();
            return val.equals("true") || val.equals("false");
        } else if (clazz.equals(byte.class) || clazz.equals(Byte.class)) {
            try {
                Byte.parseByte(value.toString());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (clazz.equals(short.class) || clazz.equals(Short.class)) {
            try {
                Short.parseShort(value.toString());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (clazz.equals(char.class) || clazz.equals(Character.class)) {
            return value.toString().length() == 1;
        } else {
            // For any other types, assume the value is in a correct format.
            return true;
        }
    }


}


