package com.romiis.equallibtestapp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.romiis.equallibtestapp.CacheUtil;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;


/**
 * Utility class for serializing and deserializing objects to and from JSON.
 */
@Log4j2
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
        // Create an instance using a no-argument constructor.
        Object instance = ReflectionUtil.createInstance(clazz);
        // Create a context map for cycle detection.
        Map<Integer, Object> context = new HashMap<>();
        return deserializeObject(instance, node, context);
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
        // Handle collections.
        if (Collection.class.isAssignableFrom(clazz)) {
            return serializeCollection((Collection<?>) obj, visited);
        }
        // Handle maps.
        if (Map.class.isAssignableFrom(clazz)) {
            return serializeMap((Map<?, ?>) obj, visited);
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
            if (value == null) {
                fieldMap.put(field.getName(), null);
                continue;
            }
            if (isPrimitiveOrWrapper(field.getType()) || field.getType() == String.class) {
                fieldMap.put(field.getName(), createSimpleField(field.getType(), value));
            } else if (field.getType().isArray()) {
                fieldMap.put(field.getName(), serializeArray(field.getType(), value, visited));
            }
            // Handle collections.
            else if (Collection.class.isAssignableFrom(field.getType())) {
                fieldMap.put(field.getName(), serializeCollection((Collection<?>) value, visited));
            }
            // Handle maps.
            else if (Map.class.isAssignableFrom(field.getType())) {
                fieldMap.put(field.getName(), serializeMap((Map<?, ?>) value, visited));
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

    /**
     * Serializes a Collection by iterating over its elements.
     */
    private static Map<String, Object> serializeCollection(Collection<?> collection, Map<Object, Integer> visited) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("@class", collection.getClass().getName());
        if (collection == null) {
            map.put("value", null);
            return map;
        }
        List<Object> serializedList = new ArrayList<>();
        for (Object element : collection) {
            serializedList.add(serializeObject(element, visited));
        }
        map.put("value", serializedList);
        return map;
    }

    /**
     * Serializes a Map by iterating over its entries.
     */
    private static Map<String, Object> serializeMap(Map<?, ?> m, Map<Object, Integer> visited) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("@class", m.getClass().getName());
        if (m == null) {
            map.put("value", null);
            return map;
        }
        List<Object> serializedEntries = new ArrayList<>();
        for (Map.Entry<?, ?> entry : m.entrySet()) {
            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("key", serializeObject(entry.getKey(), visited));
            entryMap.put("value", serializeObject(entry.getValue(), visited));
            serializedEntries.add(entryMap);
        }
        map.put("value", serializedEntries);
        return map;
    }
    //endregion

    //region ----------Deserialization methods----------

    /**
     * Recursively deserializes an object from a JSON node using the provided context.
     */
    private static Object deserializeObject(Object obj, JsonNode node, Map<Integer, Object> context) throws Exception {
        if (obj == null || node == null) {
            return null;
        }
        // Handle the case where the root object is a Collection or Map.
        if (obj instanceof Collection) {
            return deserializeCollection(node, obj.getClass(), null, context);
        } else if (obj instanceof Map) {
            return deserializeMap(node, obj.getClass(), null, context);
        }
        // If the node's "value" is an array and obj is an array, deserialize as an array.
        if (node.has("value") && node.get("value").isArray() && obj.getClass().isArray()) {
            return deserializeArray(node.get("value"), obj.getClass().getComponentType(), context);
        }
        // Handle reference nodes.
        if (node.has("@ref")) {
            int refId = node.get("@ref").asInt();
            Object refObj = context.get(refId);
            if (refObj == null) {
                throw new IllegalStateException("Reference id " + refId + " not found in context.");
            }
            return refObj;
        }
        // Store this object in context if it has an ID.
        if (node.has("@id")) {
            int currentId = node.get("@id").asInt();
            context.put(currentId, obj);
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
            // If the field node is a reference marker, retrieve and set the referenced object.
            if (fieldNode.has("@ref")) {
                int refId = fieldNode.get("@ref").asInt();
                Object refValue = context.get(refId);
                if (refValue == null) {
                    throw new IllegalStateException("Reference id " + refId + " not found in context for field " + field.getName());
                }
                field.set(obj, refValue);
                continue;
            }
            // Check for the "@class" node in the field.
            JsonNode classNode = fieldNode.get("@class");
            if (classNode == null || classNode.isNull()) {
                log.warn("Field {} has no @class info, skipping.", field.getName());
                continue;
            }
            Class<?> fieldClazz = determineFieldClass(classNode.asText());
            Object fieldValue;
            if (fieldClazz.isArray()) {
                fieldValue = deserializeArray(fieldNode.get("value"), fieldClazz.getComponentType(), context);
            }
            // Handle collections.
            else if (Collection.class.isAssignableFrom(fieldClazz)) {
                fieldValue = deserializeCollection(fieldNode, field.getType(), field.getGenericType(), context);
            }
            // Handle maps.
            else if (Map.class.isAssignableFrom(fieldClazz)) {
                fieldValue = deserializeMap(fieldNode, field.getType(), field.getGenericType(), context);
            } else if (fieldClazz.isEnum()) {
                fieldValue = Enum.valueOf((Class<Enum>) fieldClazz, fieldNode.get("value").asText());
            }
            // Special-case String fields.
            else if (fieldClazz == String.class) {
                JsonNode valueNode = fieldNode.get("value");
                fieldValue = (valueNode == null || valueNode.isNull()) ? null : valueNode.asText();
            } else if (isPrimitiveOrWrapper(fieldClazz)) {
                fieldValue = deserializeObjectWithType(fieldNode, context);
            } else {
                Object fieldInstance = ReflectionUtil.createInstance(fieldClazz);
                fieldValue = deserializeObject(fieldInstance, fieldNode, context);
            }
            field.set(obj, fieldValue);
        }
        return obj;
    }

    /**
     * Deserializes an array from a JSON node using the provided context.
     */
    private static Object deserializeArray(JsonNode node, Class<?> componentType, Map<Integer, Object> context) throws Exception {
        if (node == null || node.isNull() || !node.isArray()) {
            return null;
        }
        int length = node.size();
        Object array = Array.newInstance(componentType, length);
        for (int i = 0; i < length; i++) {
            Array.set(array, i, deserializeObjectWithType(node.get(i), context));
        }
        return array;
    }

    private static Object deserializeCollection(JsonNode node, Class<?> collectionClass, Type genericType, Map<Integer, Object> context) throws Exception {
        if (node == null || node.isNull() || !node.has("value") || node.get("value").isNull()) {
            return null;
        }
        Collection<Object> collection = createCollectionInstance(collectionClass);
        for (JsonNode elementNode : node.get("value")) {
            Object deserializedElement = deserializeElement(elementNode, context);
            collection.add(deserializedElement);
        }
        return collection;
    }

    /**
     * Deserializes a Map from a JSON node using the provided context.
     */
    private static Object deserializeMap(JsonNode node, Class<?> mapClass, Type genericType, Map<Integer, Object> context) throws Exception {
        if (node == null || node.isNull() || !node.has("value") || node.get("value").isNull()) {
            return null;
        }
        Map<Object, Object> map = createMapInstance(mapClass);
        for (JsonNode entryNode : node.get("value")) {
            JsonNode keyNode = entryNode.get("key");
            JsonNode valueNode = entryNode.get("value");
            Object key = deserializeElement(keyNode, context);
            Object value = deserializeElement(valueNode, context);
            map.put(key, value);
        }
        return map;
    }

    /**
     * Helper method that inspects a JSON node and deserializes it appropriately using the provided context.
     */
    private static Object deserializeElement(JsonNode node, Map<Integer, Object> context) throws Exception {
        if (node == null || node.isNull()) {
            return null;
        }
        // If the node is a reference, return the referenced object.
        if (node.has("@ref")) {
            int refId = node.get("@ref").asInt();
            Object refObj = context.get(refId);
            if (refObj == null) {
                throw new IllegalStateException("Reference id " + refId + " not found in context.");
            }
            return refObj;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        // If node is a simple structure with only "@class" and "value", use the simple deserialization.
        if (node.has("@class") && node.has("value") && node.size() == 2) {
            return deserializeObjectWithType(node, context);
        }
        String className = node.get("@class").asText();
        Class<?> clazz = CacheUtil.getInstance().getClassByName(className);
        Object instance = ReflectionUtil.createInstance(clazz);
        return deserializeObject(instance, node, context);
    }

    /**
     * Deserializes a primitive, wrapper, or string value using the provided context.
     */
    private static Object deserializeObjectWithType(JsonNode node, Map<Integer, Object> context) throws Exception {
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
            case "java.lang.Integer" -> Integer.valueOf(valueNode.asInt());
            case "java.lang.Long" -> Long.valueOf(valueNode.asLong());
            case "java.lang.Double" -> Double.valueOf(valueNode.asDouble());
            case "java.lang.Float" -> Float.valueOf(valueNode.floatValue());
            case "java.lang.Boolean" -> Boolean.valueOf(valueNode.asBoolean());
            case "java.lang.Byte" -> Byte.valueOf((byte) valueNode.asInt());
            case "java.lang.Short" -> Short.valueOf((short) valueNode.asInt());
            case "java.lang.Character" -> valueNode.asText().charAt(0);
            default -> {
                Class<?> clazz = CacheUtil.getInstance().getClassByName(className);
                if (clazz.isArray()) {
                    if (valueNode != null && valueNode.isArray()) {
                        yield deserializeArray(valueNode, clazz.getComponentType(), context);
                    } else {
                        yield Array.newInstance(clazz.getComponentType(), 0);
                    }
                } else {
                    yield clazz.getDeclaredConstructor().newInstance();
                }
            }
        };
    }

    // Overloaded version for backward compatibility.
    private static Object deserializeObjectWithType(JsonNode node) throws Exception {
        return deserializeObjectWithType(node, new HashMap<>());
    }
    //endregion


    /**
     * Creates an instance for a Collection.
     */
    private static Collection<Object> createCollectionInstance(Class<?> collectionClass) throws Exception {
        if (!collectionClass.isInterface() && !Modifier.isAbstract(collectionClass.getModifiers())) {
            return (Collection<Object>) collectionClass.getDeclaredConstructor().newInstance();
        }
        if (List.class.isAssignableFrom(collectionClass)) {
            return new ArrayList<>();
        }
        if (Set.class.isAssignableFrom(collectionClass)) {
            return new HashSet<>();
        }
        return new ArrayList<>();
    }

    /**
     * Creates an instance for a Map.
     */
    private static Map<Object, Object> createMapInstance(Class<?> mapClass) throws Exception {
        if (!mapClass.isInterface() && !Modifier.isAbstract(mapClass.getModifiers())) {
            return (Map<Object, Object>) mapClass.getDeclaredConstructor().newInstance();
        }
        if (SortedMap.class.isAssignableFrom(mapClass)) {
            return new TreeMap<>();
        }
        return new HashMap<>();
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

}
