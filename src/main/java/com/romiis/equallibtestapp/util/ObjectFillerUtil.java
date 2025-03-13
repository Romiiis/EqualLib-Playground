package com.romiis.equallibtestapp.util;

import java.lang.reflect.*;
import java.util.*;
import java.lang.reflect.Array;

public class ObjectFillerUtil {

    private static final Random RANDOM = new Random();

    /**
     * Fills the objects with random values.
     * When "equals" is true the second object will be a deep copy of the first,
     * so that both have identical internal structures, including order in collections and maps.
     * Default sizes for arrays and collections are used.
     */
    public static void fillObjects(Object obj1, Object obj2, boolean equals) {
        fillObjects(obj1, obj2, equals, 3, 3);
    }

    /**
     * Fills the objects with random values.
     * The arraySize parameter determines the length for any arrays and the collectionSize
     * parameter determines the number of elements inserted into collections and maps.
     */
    public static void fillObjects(Object obj1, Object obj2, boolean equals, int arraySize, int collectionSize) {
        if (obj1 == null || obj2 == null) {
            return;
        }
        if (equals) {
            // Fill the first object using our single-object filler.
            fillObject(obj1, arraySize, collectionSize);
            // Then make a deep copy of the filled object and copy its fields into obj2.
            Object copy = DeepCopyUtil.deepCopy(obj1);
            copyFields(copy, obj2);
        } else {
            // Fill the two objects with independent random values.
            fillObjectsDifferent(obj1, obj2, arraySize, collectionSize);
        }
    }

    /**
     * Recursively fills one object with random values.
     * This version fills all fields (including arrays, collections, maps, and custom objects)
     * using random values.
     */
    private static void fillObject(Object obj, int arraySize, int collectionSize) {
        Class<?> clazz = obj.getClass();
        List<Field> fields = getAllFields(clazz);
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;
            field.setAccessible(true);
            try {
                Class<?> fieldType = field.getType();
                // Primitives, wrappers and String:
                if (fieldType.isPrimitive() || isWrapper(fieldType) || fieldType.equals(String.class)) {
                    field.set(obj, randomValueForClass(fieldType));
                }
                // Enums:
                else if (fieldType.isEnum()) {
                    Object[] enumConstants = fieldType.getEnumConstants();
                    if (enumConstants.length > 0) {
                        field.set(obj, enumConstants[RANDOM.nextInt(enumConstants.length)]);
                    }
                }
                // Arrays:
                else if (fieldType.isArray()) {
                    Class<?> compType = fieldType.getComponentType();
                    Object arr = Array.newInstance(compType, arraySize);
                    for (int i = 0; i < arraySize; i++) {
                        Object elem;
                        if (compType.isArray()) {
                            elem = createAndFillArray(compType, arraySize, collectionSize);
                        } else if (compType.isPrimitive() || isWrapper(compType) ||
                                compType.equals(String.class) || compType.isEnum()) {
                            elem = randomValueForClass(compType);
                        } else {
                            elem = compType.getDeclaredConstructor().newInstance();
                            fillObject(elem, arraySize, collectionSize);
                        }
                        Array.set(arr, i, elem);
                    }
                    field.set(obj, arr);
                }
                // Collections:
                else if (Collection.class.isAssignableFrom(fieldType)) {
                    Collection<Object> col = createCollectionInstance(fieldType);
                    Class<?> elementType = getGenericType(field, 0);
                    if (elementType == null)
                        elementType = Object.class;
                    for (int i = 0; i < collectionSize; i++) {
                        Object elem;
                        if (elementType.isPrimitive() || isWrapper(elementType) ||
                                elementType.equals(String.class) || elementType.isEnum()) {
                            elem = randomValueForClass(elementType);
                        } else {
                            elem = elementType.getDeclaredConstructor().newInstance();
                            fillObject(elem, arraySize, collectionSize);
                        }
                        col.add(elem);
                    }
                    field.set(obj, col);
                }
                // Maps:
                else if (Map.class.isAssignableFrom(fieldType)) {
                    Map<Object, Object> map = createMapInstance(fieldType);
                    Class<?> keyType = getGenericType(field, 0);
                    Class<?> valueType = getGenericType(field, 1);
                    if (keyType == null)
                        keyType = Object.class;
                    if (valueType == null)
                        valueType = Object.class;
                    for (int i = 0; i < collectionSize; i++) {
                        Object key, value;
                        if (keyType.isPrimitive() || isWrapper(keyType) ||
                                keyType.equals(String.class) || keyType.isEnum()) {
                            key = randomValueForClass(keyType);
                        } else {
                            key = keyType.getDeclaredConstructor().newInstance();
                            fillObject(key, arraySize, collectionSize);
                        }
                        if (valueType.isPrimitive() || isWrapper(valueType) ||
                                valueType.equals(String.class) || valueType.isEnum()) {
                            value = randomValueForClass(valueType);
                        } else {
                            value = valueType.getDeclaredConstructor().newInstance();
                            fillObject(value, arraySize, collectionSize);
                        }
                        map.put(key, value);
                    }
                    field.set(obj, map);
                }
                // Other custom objects:
                else {
                    Object child = field.get(obj);
                    if (child == null) {
                        child = fieldType.getDeclaredConstructor().newInstance();
                        field.set(obj, child);
                    }
                    fillObject(child, arraySize, collectionSize);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Fills two objects with random values independently so that they are not equal.
     * This method is essentially the original fill logic with the equals flag forced to false.
     */
    private static void fillObjectsDifferent(Object obj1, Object obj2, int arraySize, int collectionSize) {
        Class<?> clazz = obj1.getClass();
        List<Field> fields = getAllFields(clazz);
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;
            field.setAccessible(true);
            try {
                Class<?> fieldType = field.getType();
                // Primitives, wrappers and String:
                if (fieldType.isPrimitive() || isWrapper(fieldType) || fieldType.equals(String.class)) {
                    Object value1 = randomValueForClass(fieldType);
                    Object value2 = randomValueForClass(fieldType);
                    field.set(obj1, value1);
                    field.set(obj2, value2);
                }
                // Enums:
                else if (fieldType.isEnum()) {
                    Object[] enumConstants = fieldType.getEnumConstants();
                    if (enumConstants.length > 0) {
                        Object value1 = enumConstants[RANDOM.nextInt(enumConstants.length)];
                        Object value2 = enumConstants[RANDOM.nextInt(enumConstants.length)];
                        field.set(obj1, value1);
                        field.set(obj2, value2);
                    }
                }
                // Arrays:
                else if (fieldType.isArray()) {
                    Class<?> compType = fieldType.getComponentType();
                    Object arr1 = Array.newInstance(compType, arraySize);
                    Object arr2 = Array.newInstance(compType, arraySize);
                    for (int i = 0; i < arraySize; i++) {
                        Object elem1, elem2;
                        if (compType.isArray()) {
                            elem1 = createAndFillArray(compType, arraySize, collectionSize);
                            elem2 = createAndFillArray(compType, arraySize, collectionSize);
                        } else if (compType.isPrimitive() || isWrapper(compType) ||
                                compType.equals(String.class) || compType.isEnum()) {
                            elem1 = randomValueForClass(compType);
                            elem2 = randomValueForClass(compType);
                        } else {
                            elem1 = compType.getDeclaredConstructor().newInstance();
                            elem2 = compType.getDeclaredConstructor().newInstance();
                            fillObjectsDifferent(elem1, elem2, arraySize, collectionSize);
                        }
                        Array.set(arr1, i, elem1);
                        Array.set(arr2, i, elem2);
                    }
                    field.set(obj1, arr1);
                    field.set(obj2, arr2);
                }
                // Collections:
                else if (Collection.class.isAssignableFrom(fieldType)) {
                    Collection<Object> col1 = createCollectionInstance(fieldType);
                    Collection<Object> col2 = createCollectionInstance(fieldType);
                    Class<?> elementType = getGenericType(field, 0);
                    if (elementType == null)
                        elementType = Object.class;
                    for (int i = 0; i < collectionSize; i++) {
                        Object elem1, elem2;
                        if (elementType.isPrimitive() || isWrapper(elementType) ||
                                elementType.equals(String.class) || elementType.isEnum()) {
                            elem1 = randomValueForClass(elementType);
                            elem2 = randomValueForClass(elementType);
                        } else {
                            elem1 = elementType.getDeclaredConstructor().newInstance();
                            elem2 = elementType.getDeclaredConstructor().newInstance();
                            fillObjectsDifferent(elem1, elem2, arraySize, collectionSize);
                        }
                        col1.add(elem1);
                        col2.add(elem2);
                    }
                    field.set(obj1, col1);
                    field.set(obj2, col2);
                }
                // Maps:
                else if (Map.class.isAssignableFrom(fieldType)) {
                    Map<Object, Object> map1 = createMapInstance(fieldType);
                    Map<Object, Object> map2 = createMapInstance(fieldType);
                    Class<?> keyType = getGenericType(field, 0);
                    Class<?> valueType = getGenericType(field, 1);
                    if (keyType == null)
                        keyType = Object.class;
                    if (valueType == null)
                        valueType = Object.class;
                    for (int i = 0; i < collectionSize; i++) {
                        Object key1, key2, value1, value2;
                        // Keys:
                        if (keyType.isPrimitive() || isWrapper(keyType) ||
                                keyType.equals(String.class) || keyType.isEnum()) {
                            key1 = randomValueForClass(keyType);
                            key2 = randomValueForClass(keyType);
                        } else {
                            key1 = keyType.getDeclaredConstructor().newInstance();
                            key2 = keyType.getDeclaredConstructor().newInstance();
                            fillObjectsDifferent(key1, key2, arraySize, collectionSize);
                        }
                        // Values:
                        if (valueType.isPrimitive() || isWrapper(valueType) ||
                                valueType.equals(String.class) || valueType.isEnum()) {
                            value1 = randomValueForClass(valueType);
                            value2 = randomValueForClass(valueType);
                        } else {
                            value1 = valueType.getDeclaredConstructor().newInstance();
                            value2 = valueType.getDeclaredConstructor().newInstance();
                            fillObjectsDifferent(value1, value2, arraySize, collectionSize);
                        }
                        map1.put(key1, value1);
                        map2.put(key2, value2);
                    }
                    field.set(obj1, map1);
                    field.set(obj2, map2);
                }
                // Other custom objects:
                else {
                    Object child1 = field.get(obj1);
                    Object child2 = field.get(obj2);
                    if (child1 == null) {
                        child1 = field.getType().getDeclaredConstructor().newInstance();
                        field.set(obj1, child1);
                    }
                    if (child2 == null) {
                        child2 = field.getType().getDeclaredConstructor().newInstance();
                        field.set(obj2, child2);
                    }
                    fillObjectsDifferent(child1, child2, arraySize, collectionSize);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper method to create and fill multi-dimensional arrays recursively.
     */
    private static Object createAndFillArray(Class<?> arrayType, int arraySize, int collectionSize) {
        Class<?> compType = arrayType.getComponentType();
        Object arr = Array.newInstance(compType, arraySize);
        for (int i = 0; i < arraySize; i++) {
            Object elem;
            if (compType.isArray()) {
                elem = createAndFillArray(compType, arraySize, collectionSize);
            } else if (compType.isPrimitive() || isWrapper(compType) ||
                    compType.equals(String.class) || compType.isEnum()) {
                elem = randomValueForClass(compType);
            } else {
                try {
                    elem = compType.getDeclaredConstructor().newInstance();
                    fillObject(elem, arraySize, collectionSize);
                } catch (Exception e) {
                    e.printStackTrace();
                    elem = null;
                }
            }
            Array.set(arr, i, elem);
        }
        return arr;
    }

    /**
     * Copies all fields from the source object to the target object.
     */
    private static void copyFields(Object source, Object target) {
        List<Field> fields = getAllFields(source.getClass());
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;
            field.setAccessible(true);
            try {
                Object value = field.get(source);
                field.xset(target, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns a random value for the given class.
     */
    private static Object randomValueForClass(Class<?> type) {
        if (type.isEnum()) {
            Object[] enumConstants = type.getEnumConstants();
            return enumConstants.length > 0 ? enumConstants[RANDOM.nextInt(enumConstants.length)] : null;
        } else if (type.equals(int.class) || type.equals(Integer.class)) {
            return RANDOM.nextInt(100);
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return RANDOM.nextLong();
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return RANDOM.nextDouble();
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return RANDOM.nextFloat();
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return RANDOM.nextBoolean();
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            return (byte) RANDOM.nextInt(128);
        } else if (type.equals(short.class) || type.equals(Short.class)) {
            return (short) RANDOM.nextInt(Short.MAX_VALUE);
        } else if (type.equals(char.class) || type.equals(Character.class)) {
            return (char) (RANDOM.nextInt(26) + 'a');
        } else if (type.equals(String.class)) {
            return "Str" + RANDOM.nextInt(1000);
        }
        return null;
    }

    /**
     * Checks if a class is one of the wrapper types.
     */
    private static boolean isWrapper(Class<?> type) {
        return type.equals(Integer.class) || type.equals(Long.class) ||
                type.equals(Double.class) || type.equals(Float.class) ||
                type.equals(Boolean.class) || type.equals(Byte.class) ||
                type.equals(Short.class) || type.equals(Character.class);
    }

    /**
     * Creates a new instance for a Collection.
     */
    private static Collection<Object> createCollectionInstance(Class<?> collectionClass) {
        if (!collectionClass.isInterface() && !Modifier.isAbstract(collectionClass.getModifiers())) {
            try {
                return (Collection<Object>) collectionClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (List.class.isAssignableFrom(collectionClass)) {
            return new ArrayList<>();
        }
        if (Set.class.isAssignableFrom(collectionClass)) {
            return new LinkedHashSet<>();
        }
        return new ArrayList<>();
    }

    /**
     * Creates a new instance for a Map.
     * Returns a LinkedHashMap to preserve insertion order.
     */
    private static Map<Object, Object> createMapInstance(Class<?> mapClass) {
        if (!mapClass.isInterface() && !Modifier.isAbstract(mapClass.getModifiers())) {
            try {
                Map<Object, Object> map = (Map<Object, Object>) mapClass.getDeclaredConstructor().newInstance();
                if (!(map instanceof SortedMap) && !(map instanceof LinkedHashMap)) {
                    return new LinkedHashMap<>();
                }
                return map;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (SortedMap.class.isAssignableFrom(mapClass)) {
            return new TreeMap<>();
        }
        return new LinkedHashMap<>();
    }

    /**
     * Recursively collects all declared fields for a class.
     */
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && !clazz.equals(Object.class)) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * Returns the generic type parameter of a field at the given index.
     */
    private static Class<?> getGenericType(Field field, int index) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > index && typeArgs[index] instanceof Class) {
                return (Class<?>) typeArgs[index];
            }
        }
        return null;
    }
}
