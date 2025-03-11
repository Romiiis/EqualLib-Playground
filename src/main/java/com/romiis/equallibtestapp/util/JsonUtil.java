package com.romiis.equallibtestapp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.romiis.equallibtestapp.CacheUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
public class JsonUtil {

    /**
     * Serializes an object into a JSON string.
     */
    public static String serialize(Object obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Create a new visited map for cycle detection.
            Map<Object, Integer> visited = new IdentityHashMap<>();
            return mapper.writeValueAsString(serializeObject(obj, visited));
        } catch (Exception e) {
            log.error("Failed to serialize object: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Deserializes a JSON string into an object.
     */
    public static Object deserialize(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        if (node == null || node.isNull()) {
            return null;
        }
        String className = node.get("@class").asText();
        Class<?> clazz = CacheUtil.getInstance().getClassByName(className);
        // Create an instance (using a no-argument constructor) and then populate it.
        return deserializeObject(ReflectionUtil.createInstance(clazz), node);
    }

    //region ----------Serialization methods----------

    /**
     * Recursively serializes an object into a structured Map representation.
     * Uses cycle detection to avoid infinite recursion.
     */
    private static Object serializeObject(Object obj, Map<Object, Integer> visited) throws Exception {
        if (obj == null) {
            return null;
        }

        // If we've already serialized this object, return a reference marker.
        if (visited.containsKey(obj)) {
            Map<String, Object> refMap = new HashMap<>();
            refMap.put("@ref", visited.get(obj));
            return refMap;
        }

        Class<?> clazz = obj.getClass();

        // For primitives, wrappers and String, just return a simple map.
        if (isPrimitiveOrWrapper(clazz) || clazz == String.class) {
            Map<String, Object> simpleMap = new HashMap<>();
            simpleMap.put("@class", clazz.getName());
            simpleMap.put("value", obj);
            return simpleMap;
        }

        // If the object is an array, delegate to serializeArray.
        if (clazz.isArray()) {
            return serializeArray(clazz, obj, visited);
        }

        // Mark the current object as visited with a unique ID.
        int currentId = visited.size() + 1;
        visited.put(obj, currentId);

        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("@class", clazz.getName());
        fieldMap.put("@id", currentId);

        for (Field field : ReflectionUtil.getAllFields(clazz)) {
            // Skip transient fields unless they are collections or maps.
            if (Modifier.isTransient(field.getModifiers())
                    && !Collection.class.isAssignableFrom(field.getType())
                    && !Map.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            Object value = field.get(obj);
            if (isPrimitiveOrWrapper(field.getType()) || field.getType() == String.class) {
                fieldMap.put(field.getName(), createSimpleField(field.getType(), value));
            } else if (field.getType().isArray()) {
                fieldMap.put(field.getName(), serializeArray(field.getType(), value, visited));
            } else if (field.getType().isEnum()) {
                fieldMap.put(field.getName(), createEnumField(field.getType(), value));
            } else {
                fieldMap.put(field.getName(), serializeObject(value, visited));
            }
        }
        return fieldMap;
    }

    /**
     * Serializes a primitive or String field.
     */
    private static Map<String, Object> createSimpleField(Class<?> type, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put("@class", type.getName());
        map.put("value", value);
        return map;
    }

    /**
     * Serializes an array field with cycle detection.
     */
    private static Map<String, Object> serializeArray(Class<?> type, Object value, Map<Object, Integer> visited) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("@class", type.getName());
        if (value == null) {
            map.put("value", null);
            return map;
        }
        int length = Array.getLength(value);
        List<Object> serializedArray = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            Object element = Array.get(value, i);
            serializedArray.add(serializeObject(element, visited));
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

    //endregion

    //region ----------Deserialization methods----------

    /**
     * Recursively deserializes an object from a JSON node.
     */
    private static Object deserializeObject(Object obj, JsonNode node) throws Exception {
        if (obj == null || node == null) {
            return null;
        }

        // If the node's "value" is an array and obj is an array, deserialize as an array.
        if (node.has("value") && node.get("value").isArray() && obj.getClass().isArray()) {
            return deserializeArray(node.get("value"), obj.getClass().getComponentType());
        }

        String className = node.get("@class").asText();
        Class<?> clazz = CacheUtil.getInstance().getClassByName(className);
        for (Field field : ReflectionUtil.getAllFields(clazz)) {
            field.setAccessible(true);
            JsonNode fieldNode = node.get(field.getName());
            if (fieldNode == null || fieldNode.isNull()) {
                continue;
            }
            // Skip final static fields.
            if (Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
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
     * Deserializes an array from a JSON node.
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
     * Determines the correct class for a field given its JSON "@class" string,
     * handling both primitive types and multi-dimensional arrays.
     */
    public static Class<?> determineFieldClass(String className) throws Exception {
        return switch (className) {
            case "int" -> int.class;
            case "long" -> long.class;
            case "double" -> double.class;
            case "float" -> float.class;
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "char" -> char.class;
            default -> {
                if (!className.startsWith("[")) {
                    yield CacheUtil.getInstance().getClassByName(className);
                }
                int dimensions = 0;
                while (dimensions < className.length() && className.charAt(dimensions) == '[') {
                    dimensions++;
                }
                Class<?> baseClass;
                char typeChar = className.charAt(dimensions);
                baseClass = switch (typeChar) {
                    case 'I' -> int.class;
                    case 'J' -> long.class;
                    case 'D' -> double.class;
                    case 'F' -> float.class;
                    case 'Z' -> boolean.class;
                    case 'B' -> byte.class;
                    case 'S' -> short.class;
                    case 'C' -> char.class;
                    case 'L' -> {
                        String baseName = className.substring(dimensions + 1, className.length() - 1);
                        yield CacheUtil.getInstance().getClassByName(baseName);
                    }
                    default -> throw new IllegalArgumentException("Unknown array type descriptor: " + className);
                };
                int[] dims = new int[dimensions];
                yield Array.newInstance(baseClass, dims).getClass();
            }
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

            // Handle wrapper types explicitly.
            case "java.lang.Integer" -> Integer.valueOf(valueNode.asInt());
            case "java.lang.Long" -> Long.valueOf(valueNode.asLong());
            case "java.lang.Double" -> Double.valueOf(valueNode.asDouble());
            case "java.lang.Float" -> Float.valueOf(valueNode.floatValue());
            case "java.lang.Boolean" -> Boolean.valueOf(valueNode.asBoolean());
            case "java.lang.Byte" -> Byte.valueOf((byte) valueNode.asInt());
            case "java.lang.Short" -> Short.valueOf((short) valueNode.asInt());
            case "java.lang.Character" -> valueNode.asText().charAt(0);

            // Default case: dynamically load class and instantiate.
            default -> {
                Class<?> clazz = CacheUtil.getInstance().getClassByName(className);
                if (clazz.isArray()) {
                    if (valueNode != null && valueNode.isArray()) {
                        yield deserializeArray(valueNode, clazz.getComponentType());
                    } else {
                        yield Array.newInstance(clazz.getComponentType(), 0);
                    }
                } else {
                    yield clazz.getDeclaredConstructor().newInstance();
                }
            }
        };
    }


    //endregion
}
