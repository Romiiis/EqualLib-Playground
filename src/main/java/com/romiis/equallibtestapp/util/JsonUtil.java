package com.romiis.equallibtestapp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class JsonUtil {

    public static String serialize(Object obj) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(serializeObject(obj));
    }

    private static Object serializeObject(Object obj) throws Exception{

        if (obj == null) {
            return null;
        }

        Map<String, Object> fieldMap = new HashMap<>();
        Class<?> clazz = obj.getClass();
        fieldMap.put("@class", clazz.getName());


        if (isPrimitiveOrWrapper(clazz) || clazz == String.class) {
            fieldMap.put("value", obj);
            return fieldMap;
        }


        for (Field field : ReflectionUtil.getAllFields(clazz)) {
            field.setAccessible(true);

            Object value = field.get(obj);

            if (isPrimitiveOrWrapper(field.getType()) || field.getType() == String.class ) {
                Map<String, Object> map = new HashMap<>();
                map.put("@class", field.getType());
                map.put("value", value);
                fieldMap.put(field.getName(), map);
            }
            else if (field.getType().isArray()) {

                if (value == null) {
                    fieldMap.put(field.getName(), null);
                    continue;
                }

                Map<String, Object> map = new HashMap<>();
                map.put("@class", field.getType());
                int length = java.lang.reflect.Array.getLength(value);
                List<Object> serializedArray = new ArrayList<>();
                for (int i = 0; i < length; i++) {
                    serializedArray.add(serializeObject(java.lang.reflect.Array.get(value, i)));
                }

                map.put("value", serializedArray);
                fieldMap.put(field.getName(), map);
            }
            else if (field.getType().isEnum()) {
                Map<String, Object> map = new HashMap<>();
                map.put("@class", field.getType());
                map.put("value", value.toString());
                fieldMap.put(field.getName(), map);
            } else {
                String name = field.getName();
                Object serializedObject = serializeObject(value);
                fieldMap.put(name, serializedObject);
            }
        }
        return fieldMap;
    }



    private static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == Integer.class || clazz == Long.class || clazz == Double.class ||
                clazz == Float.class || clazz == Boolean.class || clazz == Byte.class ||
                clazz == Short.class || clazz == Character.class;
    }


    public static Object deserialize(String json) throws Exception {


        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        String className = node.get("@class").asText();
        Class<?> clazz = DynamicCompiler.getClassLoader().loadClass(className);

        // Create instance of the class
        Object obj = ReflectionUtil.createInstance(clazz);
        return deserializeObject(obj, node);
    }

    private static Object deserializeObject(Object obj, JsonNode node) throws Exception {

        if (obj == null || node == null) {
            return null;
        }

        if (node instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode) node;
            int length = arrayNode.size();
            for (int i = 0; i < length; i++) {
                Array.set(obj, i, deserializeArrayElement(arrayNode.get(i), obj.getClass().getComponentType()));
            }
            return obj;
        }

        String className = node.get("@class").asText();
        Class<?> clazz = DynamicCompiler.getClassLoader().loadClass(className);

        for (Field field : ReflectionUtil.getAllFields(clazz)) {

            field.setAccessible(true);

            JsonNode fieldNode = node.get(field.getName());

            if (fieldNode.isNull()) {
                continue;
            }

            String fieldClass = fieldNode.get("@class").asText();
            Class<?> fieldClazz;

            if (fieldClass == null) {
                // If @class is missing, guess from field type
                fieldClazz = field.getType();
            } else if (fieldClass.startsWith("[")) {
                // Handle both object and primitive arrays

                if (fieldClass.equals("[I")) {
                    fieldClazz = int[].class;
                } else if (fieldClass.equals("[J")) {
                    fieldClazz = long[].class;
                } else if (fieldClass.equals("[D")) {
                    fieldClazz = double[].class;
                } else if (fieldClass.equals("[F")) {
                    fieldClazz = float[].class;
                } else if (fieldClass.equals("[Z")) {
                    fieldClazz = boolean[].class;
                } else if (fieldClass.equals("[B")) {
                    fieldClazz = byte[].class;
                } else if (fieldClass.equals("[S")) {
                    fieldClazz = short[].class;
                } else if (fieldClass.equals("[C")) {
                    fieldClazz = char[].class;
                } else if (fieldClass.startsWith("[L") && fieldClass.endsWith(";")) {
                    // Handle object arrays like "[Ljava.lang.String;"
                    String elementClass = fieldClass.substring(2, fieldClass.length() - 1); // Extract "java.lang.String"
                    Class<?> componentType = DynamicCompiler.getClassLoader().loadClass(elementClass);
                    fieldClazz = Array.newInstance(componentType, 0).getClass(); // Create an empty array of the correct type
                } else {
                    throw new ClassNotFoundException("Unknown array type: " + fieldClass);
                }
            } else {
                // Convert primitive names to boxed classes
                fieldClazz = switch (fieldClass) {
                    case "int" -> Integer.class;
                    case "long" -> Long.class;
                    case "double" -> Double.class;
                    case "float" -> Float.class;
                    case "boolean" -> Boolean.class;
                    case "byte" -> Byte.class;
                    case "short" -> Short.class;
                    case "char" -> Character.class;
                    default -> DynamicCompiler.getClassLoader().loadClass(fieldClass);
                };
            }

            // If static final or final field, skip
            if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (fieldClazz.isArray()) {
                int length = fieldNode.get("value").size();
                Object array = Array.newInstance(fieldClazz.getComponentType(), length);
                for (int i = 0; i < length; i++) {
                    Array.set(array, i, deserializeArrayElement(fieldNode.get("value").get(i), fieldClazz.getComponentType()));
                }
                field.set(obj, array);
            } else if (fieldClazz.isEnum()) {
                String value = fieldNode.get("value").asText();
                Object enumValue = Enum.valueOf((Class<Enum>) fieldClazz, value);

                field.set(obj, enumValue);
            } else if (fieldClazz.isPrimitive() || ReflectionUtil.isWrapperOrString(fieldClazz)) {
                field.set(obj, deserializeObjectWithType(fieldNode));
            } else {
                field.set(obj, deserializeObject(field.get(obj), fieldNode));
            }


        }

        return obj;
    }


    private static Object deserializeArrayElement(JsonNode node, Class<?> componentType) throws Exception {

        if (node == null || node.isNull()) {
            return null;
        }

        JsonNode classNode = node.get("@class");



        String className = classNode.asText();

        Class<?> clazz = switch (className) {
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "double" -> Double.class;
            case "float" -> Float.class;
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "short" -> Short.class;
            case "char" -> Character.class;
            default -> DynamicCompiler.getClassLoader().loadClass(className);
        };


        if (isPrimitiveOrWrapper(clazz) || clazz == String.class) {
            JsonNode valueNode = node.get("value");
            if (valueNode == null) {
                return null;
            }
            return deserializeObjectWithType(node);
        } else {
            return deserializeObject(componentType.getDeclaredConstructor().newInstance(), node);
        }
    }

    private static Object deserializeObjectWithType(JsonNode node) throws Exception {

        if (node == null) {
            return null;
        }
        JsonNode valueNode = node.get("value");
        JsonNode classNode = node.get("@class");

        if (valueNode == null) {
            return null;
        }

        String className = classNode.asText();

        Class<?> clazz = switch (className) {
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "double" -> Double.class;
            case "float" -> Float.class;
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "short" -> Short.class;
            case "char" -> Character.class;
            default -> DynamicCompiler.getClassLoader().loadClass(className);
        };



        if (clazz.isEnum()) {
            return Enum.valueOf((Class<Enum>) clazz, valueNode.asText());
        }

        if (clazz == Integer.class) {
            return valueNode.asInt();
        }

        if (clazz == Long.class) {
            return valueNode.asLong();
        }

        if (clazz == Double.class) {
            return valueNode.asDouble();
        }

        if (clazz == Float.class) {
            return valueNode.floatValue();
        }

        if (clazz == Boolean.class) {
            return valueNode.asBoolean();
        }

        if (clazz == Byte.class) {
            return valueNode.asText().getBytes()[0];
        }

        if (clazz == Short.class) {
            return valueNode.shortValue();
        }

        if (clazz == Character.class) {
            return valueNode.asText().charAt(0);
        }

        if (clazz == String.class) {
            if (valueNode.isNull()) {
                return null;
            }
            return valueNode.asText();
        }

        return null;


    }

    private static Class<?> primitiveToWrapper(Class<?> clazz) {
        return switch (clazz.getName()) {
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "double" -> Double.class;
            case "float" -> Float.class;
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "short" -> Short.class;
            case "char" -> Character.class;
            default -> clazz;
        };
    }


}
