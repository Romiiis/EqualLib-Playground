package com.romiis.equallibtestapp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.lang.reflect.*;
import java.util.*;

public class JsonUtil {

    /**
     * Serializes an object into a JSON string.
     */
    public static String serialize(Object obj) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(serializeObject(obj));
    }

    /**
     * Recursively serializes an object into a structured Map representation.
     */
    private static Object serializeObject(Object obj) throws Exception {
        if (obj == null) {
            return null;
        }

        Class<?> clazz = obj.getClass();
        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("@class", clazz.getName());

        // Handle primitive types and Strings
        if (isPrimitiveOrWrapper(clazz) || clazz == String.class) {
            fieldMap.put("value", obj);
            return fieldMap;
        }

        // Process fields of the object
        for (Field field : ReflectionUtil.getAllFields(clazz)) {
            field.setAccessible(true);
            Object value = field.get(obj);

            if (isPrimitiveOrWrapper(field.getType()) || field.getType() == String.class) {
                fieldMap.put(field.getName(), createSimpleField(field.getType(), value));
            } else if (field.getType().isArray()) {
                fieldMap.put(field.getName(), serializeArray(field.getType(), value));
            } else if (field.getType().isEnum()) {
                fieldMap.put(field.getName(), createEnumField(field.getType(), value));
            } else {
                fieldMap.put(field.getName(), serializeObject(value));
            }
        }
        return fieldMap;
    }

    /**
     * Serializes a primitive or string field.
     */
    private static Map<String, Object> createSimpleField(Class<?> type, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put("@class", type.getName());
        map.put("value", value);
        return map;
    }

    /**
     * Serializes an array field.
     */
    private static Map<String, Object> serializeArray(Class<?> type, Object value) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("@class", type.getName());

        if (value == null) {
            map.put("value", null);
            return map;
        }

        int length = Array.getLength(value);
        List<Object> serializedArray = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            serializedArray.add(serializeObject(Array.get(value, i)));
        }
        map.put("value", serializedArray);
        return map;
    }

    /**
     * Serializes an enum field.
     */
    private static Map<String, Object> createEnumField(Class<?> type, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put("@class", type.getName());
        map.put("value", value.toString());
        return map;
    }

    /**
     * Checks if a class is a primitive type or its wrapper.
     */
    private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() || Set.of(
                Integer.class, Long.class, Double.class, Float.class,
                Boolean.class, Byte.class, Short.class, Character.class
        ).contains(clazz);
    }

    /**
     * Deserializes a JSON string into an object.
     */
    public static Object deserialize(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        String className = node.get("@class").asText();
        Class<?> clazz = DynamicCompiler.getClassLoader().loadClass(className);

        return deserializeObject(ReflectionUtil.createInstance(clazz), node);
    }

    /**
     * Recursively deserializes an object from a JSON node.
     */
    private static Object deserializeObject(Object obj, JsonNode node) throws Exception {
        if (obj == null || node == null) {
            return null;
        }

        // Handle array deserialization
        if (node instanceof ArrayNode arrayNode) {
            return deserializeArray(arrayNode, obj.getClass().getComponentType());
        }

        String className = node.get("@class").asText();
        Class<?> clazz = DynamicCompiler.getClassLoader().loadClass(className);

        for (Field field : ReflectionUtil.getAllFields(clazz)) {
            field.setAccessible(true);
            JsonNode fieldNode = node.get(field.getName());

            if (fieldNode == null || fieldNode.isNull()) {
                continue;
            }

            if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            Class<?> fieldClazz = determineFieldClass(fieldNode.get("@class").asText());

            if (fieldClazz.isArray()) {
                field.set(obj, deserializeArray(fieldNode.get("value"), fieldClazz.getComponentType()));
            } else if (fieldClazz.isEnum()) {
                field.set(obj, Enum.valueOf((Class<Enum>) fieldClazz, fieldNode.get("value").asText()));
            } else if (isPrimitiveOrWrapper(fieldClazz) || fieldClazz == String.class) {
                field.set(obj, deserializeObjectWithType(fieldNode));
            } else {
                field.set(obj, deserializeObject(field.get(obj), fieldNode));
            }
        }
        return obj;
    }

    /**
     * Deserializes an array.
     */
    private static Object deserializeArray(JsonNode node, Class<?> componentType) throws Exception {
        if (node == null || node.isNull() || !node.isArray()) {
            return null;
        }

        int length = node.size();
        Object array = Array.newInstance(componentType, length);

        for (int i = 0; i < length; i++) {
            Array.set(array, i, deserializeObjectWithType(node.get(i)));
        }
        return array;
    }

    /**
     * Determines the correct class for a field, handling primitive and array types.
     */
    private static Class<?> determineFieldClass(String className) throws Exception {
        return switch (className) {
            case "int" -> int.class;
            case "long" -> long.class;
            case "double" -> double.class;
            case "float" -> float.class;
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "char" -> char.class;

            case "[I" -> int[].class;
            case "[J" -> long[].class;
            case "[D" -> double[].class;
            case "[F" -> float[].class;
            case "[Z" -> boolean[].class;
            case "[B" -> byte[].class;
            case "[S" -> short[].class;
            case "[C" -> char[].class;
            default -> className.startsWith("[L") && className.endsWith(";")
                    ? Array.newInstance(DynamicCompiler.getClassLoader().loadClass(className.substring(2, className.length() - 1)), 0).getClass()
                    : DynamicCompiler.getClassLoader().loadClass(className);
        };
    }

    /**
     * Deserializes a primitive, wrapper, or string value.
     */
    private static Object deserializeObjectWithType(JsonNode node) throws Exception {
        if (node == null || node.isNull()) {
            return null;
        }

        String className = node.get("@class").asText();
        JsonNode valueNode = node.get("value");

        // Handle basic primitive types and wrappers directly
        return switch (className) {
            case "int" -> valueNode.asInt();
            case "long" -> valueNode.asLong();
            case "double" -> valueNode.asDouble();
            case "float" -> valueNode.floatValue();
            case "boolean" -> valueNode.asBoolean();
            case "byte" -> (byte) valueNode.asInt();
            case "short" -> (short) valueNode.asInt();
            case "char" -> valueNode.asText().charAt(0);
            case "java.lang.String" -> valueNode.isNull() ? null : valueNode.asText();

            // Handle wrapper types like Integer, Long, etc.
            case "java.lang.Integer" -> Integer.valueOf(valueNode.asInt());
            case "java.lang.Long" -> Long.valueOf(valueNode.asLong());
            case "java.lang.Double" -> Double.valueOf(valueNode.asDouble());
            case "java.lang.Float" -> Float.valueOf(valueNode.floatValue());
            case "java.lang.Boolean" -> Boolean.valueOf(valueNode.asBoolean());

            // Default case: dynamically load class and invoke its no-argument constructor
            default -> {
                Class<?> clazz = DynamicCompiler.getClassLoader().loadClass(className);
                if (clazz == Integer.class) {
                    // Special case for Integer, because Integer requires a constructor argument
                    yield Integer.valueOf(valueNode.asInt());
                } else {
                    yield clazz.getDeclaredConstructor().newInstance();
                }
            }
        };
    }

}
