//package com.romiis.equallibtestapp.util;
//
//import java.lang.reflect.*;
//import java.util.*;
//import java.lang.reflect.Array;
//
//public class ObjectFillerUtil {
//
//    private static final Random RANDOM = new Random();
//    // Maximum recursion depth to avoid deep or infinite recursions.
//    private static final int MAX_DEPTH = 10;
//
//    /**
//     * Fills the objects with random values.
//     * When "equals" is true the second object will be a deep copy of the first,
//     * so that both have identical internal structures, including order in collections and maps.
//     * Default sizes for arrays and collections are used.
//     */
//    public static void fillObjects(Object obj1, Object obj2, boolean equals) {
//        fillObjects(obj1, obj2, equals, 3, 3, MAX_DEPTH);
//    }
//
//    /**
//     * Fills the objects with random values.
//     * The arraySize parameter determines the length for any arrays and the collectionSize
//     * parameter determines the number of elements inserted into collections and maps.
//     */
//    public static void fillObjects(Object obj1, Object obj2, boolean equals, int arraySize, int collectionSize, int maxDepth) {
//        if (obj1 == null || obj2 == null) {
//            return;
//        }
//        if (equals) {
//            // For equals==true we assume DeepCopyUtil.deepCopy() handles cycles.
//            fillObjectInternal(obj1, arraySize, collectionSize,
//                    Collections.newSetFromMap(new IdentityHashMap<>()), maxDepth);
//            Object copy = DeepCopyUtil.deepCopy(obj1);
//            copyFields(copy, obj2);
//        } else {
//            // For independent filling, we use two visited sets for the two branches.
//            fillObjectsDifferentInternal(obj1, obj2, arraySize, collectionSize,
//                    Collections.newSetFromMap(new IdentityHashMap<>()),
//                    Collections.newSetFromMap(new IdentityHashMap<>()), maxDepth);
//        }
//    }
//
//    /**
//     * Recursively fills one object with random values.
//     * Uses a visited set and a recursion depth limit to avoid cycles.
//     */
//    private static void fillObjectInternal(Object obj, int arraySize, int collectionSize,
//                                           Set<Object> visited, int depth) {
//        if (obj == null || depth <= 0) return;
//        if (visited.contains(obj)) return;
//        visited.add(obj);
//
//        // Skip filling internal fields of standard library classes (except arrays, collections, and maps)
//        if (obj.getClass().getName().startsWith("java.")
//                && !(obj instanceof Collection) && !(obj instanceof Map) && !obj.getClass().isArray()) {
//            return;
//        }
//
//        Class<?> clazz = obj.getClass();
//        List<Field> fields = getAllFields(clazz);
//        for (Field field : fields) {
//            if (Modifier.isStatic(field.getModifiers()))
//                continue;
//            field.setAccessible(true);
//            try {
//                Class<?> fieldType = field.getType();
//                // Primitives, wrappers and String:
//                if (fieldType.isPrimitive() || isWrapper(fieldType) || fieldType.equals(String.class)) {
//                    field.set(obj, randomValueForClass(fieldType));
//                }
//                // Enums:
//                else if (fieldType.isEnum()) {
//                    Object[] enumConstants = fieldType.getEnumConstants();
//                    if (enumConstants.length > 0) {
//                        field.set(obj, enumConstants[RANDOM.nextInt(enumConstants.length)]);
//                    }
//                }
//                // Arrays:
//                else if (fieldType.isArray()) {
//                    Class<?> compType = fieldType.getComponentType();
//                    Object arr = Array.newInstance(compType, arraySize);
//                    for (int i = 0; i < arraySize; i++) {
//                        Object elem;
//                        if (compType.isArray()) {
//                            elem = createAndFillArrayInternal(compType, arraySize, collectionSize, visited, depth - 1);
//                        } else if (compType.isPrimitive() || isWrapper(compType) ||
//                                compType.equals(String.class) || compType.isEnum()) {
//                            elem = randomValueForClass(compType);
//                        } else {
//                            if (depth <= 1) {
//                                elem = null;
//                            } else {
//                                elem = compType.getDeclaredConstructor().newInstance();
//                                fillObjectInternal(elem, arraySize, collectionSize, visited, depth - 1);
//                            }
//                        }
//                        Array.set(arr, i, elem);
//                    }
//                    field.set(obj, arr);
//                }
//                // Collections:
//                else if (Collection.class.isAssignableFrom(fieldType)) {
//                    Collection<Object> col = createCollectionInstance(fieldType);
//                    Class<?> elementType = getGenericType(field, 0);
//                    if (elementType == null)
//                        elementType = Object.class;
//                    for (int i = 0; i < collectionSize; i++) {
//                        Object elem;
//                        if (elementType.isPrimitive() || isWrapper(elementType) ||
//                                elementType.equals(String.class) || elementType.isEnum()) {
//                            elem = randomValueForClass(elementType);
//                        } else {
//                            if (depth <= 1) {
//                                elem = null;
//                            } else {
//                                elem = elementType.getDeclaredConstructor().newInstance();
//                                fillObjectInternal(elem, arraySize, collectionSize, visited, depth - 1);
//                            }
//                        }
//                        col.add(elem);
//                    }
//                    field.set(obj, col);
//                }
//                // Maps:
//                else if (Map.class.isAssignableFrom(fieldType)) {
//                    Map<Object, Object> map = createMapInstance(fieldType);
//                    Class<?> keyType = getGenericType(field, 0);
//                    Class<?> valueType = getGenericType(field, 1);
//                    if (keyType == null)
//                        keyType = Object.class;
//                    if (valueType == null)
//                        valueType = Object.class;
//                    for (int i = 0; i < collectionSize; i++) {
//                        Object key, value;
//                        if (keyType.isPrimitive() || isWrapper(keyType) ||
//                                keyType.equals(String.class) || keyType.isEnum()) {
//                            key = randomValueForClass(keyType);
//                        } else {
//                            if (depth <= 1) {
//                                key = null;
//                            } else {
//                                key = keyType.getDeclaredConstructor().newInstance();
//                                fillObjectInternal(key, arraySize, collectionSize, visited, depth - 1);
//                            }
//                        }
//                        if (valueType.isPrimitive() || isWrapper(valueType) ||
//                                valueType.equals(String.class) || valueType.isEnum()) {
//                            value = randomValueForClass(valueType);
//                        } else {
//                            if (depth <= 1) {
//                                value = null;
//                            } else {
//                                value = valueType.getDeclaredConstructor().newInstance();
//                                fillObjectInternal(value, arraySize, collectionSize, visited, depth - 1);
//                            }
//                        }
//                        map.put(key, value);
//                    }
//                    field.set(obj, map);
//                }
//                // Other custom objects:
//                else {
//                    if (depth <= 1) {
//                        // Terminate deep expansion by leaving the field as null.
//                        field.set(obj, null);
//                    } else {
//                        Object child = field.get(obj);
//                        if (child == null) {
//                            child = field.getType().getDeclaredConstructor().newInstance();
//                            field.set(obj, child);
//                        }
//                        fillObjectInternal(child, arraySize, collectionSize, visited, depth - 1);
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    /**
//     * Fills two objects with random values independently so that they are not equal.
//     * Uses separate visited sets for each branch.
//     */
//    private static void fillObjectsDifferentInternal(Object obj1, Object obj2, int arraySize, int collectionSize,
//                                                     Set<Object> visited1, Set<Object> visited2, int depth) {
//        if (obj1 == null || obj2 == null || depth <= 0) return;
//        if (visited1.contains(obj1) || visited2.contains(obj2)) return;
//        visited1.add(obj1);
//        visited2.add(obj2);
//
//        Class<?> clazz = obj1.getClass();
//        List<Field> fields = getAllFields(clazz);
//        for (Field field : fields) {
//            if (Modifier.isStatic(field.getModifiers()))
//                continue;
//            field.setAccessible(true);
//            try {
//                Class<?> fieldType = field.getType();
//                // Primitives, wrappers and String:
//                if (fieldType.isPrimitive() || isWrapper(fieldType) || fieldType.equals(String.class)) {
//                    Object value1 = randomValueForClass(fieldType);
//                    Object value2 = randomValueForClass(fieldType);
//                    field.set(obj1, value1);
//                    field.set(obj2, value2);
//                }
//                // Enums:
//                else if (fieldType.isEnum()) {
//                    Object[] enumConstants = fieldType.getEnumConstants();
//                    if (enumConstants.length > 0) {
//                        Object value1 = enumConstants[RANDOM.nextInt(enumConstants.length)];
//                        Object value2 = enumConstants[RANDOM.nextInt(enumConstants.length)];
//                        field.set(obj1, value1);
//                        field.set(obj2, value2);
//                    }
//                }
//                // Arrays:
//                else if (fieldType.isArray()) {
//                    Class<?> compType = fieldType.getComponentType();
//                    Object arr1 = Array.newInstance(compType, arraySize);
//                    Object arr2 = Array.newInstance(compType, arraySize);
//                    for (int i = 0; i < arraySize; i++) {
//                        Object elem1, elem2;
//                        if (compType.isArray()) {
//                            elem1 = createAndFillArrayInternal(compType, arraySize, collectionSize, visited1, depth - 1);
//                            elem2 = createAndFillArrayInternal(compType, arraySize, collectionSize, visited2, depth - 1);
//                        } else if (compType.isPrimitive() || isWrapper(compType) ||
//                                compType.equals(String.class) || compType.isEnum()) {
//                            elem1 = randomValueForClass(compType);
//                            elem2 = randomValueForClass(compType);
//                        } else {
//                            if (depth <= 1) {
//                                elem1 = null;
//                                elem2 = null;
//                            } else {
//                                elem1 = compType.getDeclaredConstructor().newInstance();
//                                elem2 = compType.getDeclaredConstructor().newInstance();
//                                fillObjectsDifferentInternal(elem1, elem2, arraySize, collectionSize,
//                                        visited1, visited2, depth - 1);
//                            }
//                        }
//                        Array.set(arr1, i, elem1);
//                        Array.set(arr2, i, elem2);
//                    }
//                    field.set(obj1, arr1);
//                    field.set(obj2, arr2);
//                }
//                // Collections:
//                else if (Collection.class.isAssignableFrom(fieldType)) {
//                    Collection<Object> col1 = createCollectionInstance(fieldType);
//                    Collection<Object> col2 = createCollectionInstance(fieldType);
//                    Class<?> elementType = getGenericType(field, 0);
//                    if (elementType == null)
//                        elementType = Object.class;
//                    for (int i = 0; i < collectionSize; i++) {
//                        Object elem1, elem2;
//                        if (elementType.isPrimitive() || isWrapper(elementType) ||
//                                elementType.equals(String.class) || elementType.isEnum()) {
//                            elem1 = randomValueForClass(elementType);
//                            elem2 = randomValueForClass(elementType);
//                        } else {
//                            if (depth <= 1) {
//                                elem1 = null;
//                                elem2 = null;
//                            } else {
//                                elem1 = elementType.getDeclaredConstructor().newInstance();
//                                elem2 = elementType.getDeclaredConstructor().newInstance();
//                                fillObjectsDifferentInternal(elem1, elem2, arraySize, collectionSize,
//                                        visited1, visited2, depth - 1);
//                            }
//                        }
//                        col1.add(elem1);
//                        col2.add(elem2);
//                    }
//                    field.set(obj1, col1);
//                    field.set(obj2, col2);
//                }
//                // Maps:
//                else if (Map.class.isAssignableFrom(fieldType)) {
//                    Map<Object, Object> map1 = createMapInstance(fieldType);
//                    Map<Object, Object> map2 = createMapInstance(fieldType);
//                    Class<?> keyType = getGenericType(field, 0);
//                    Class<?> valueType = getGenericType(field, 1);
//                    if (keyType == null)
//                        keyType = Object.class;
//                    if (valueType == null)
//                        valueType = Object.class;
//                    for (int i = 0; i < collectionSize; i++) {
//                        Object key1, key2, value1, value2;
//                        // Keys:
//                        if (keyType.isPrimitive() || isWrapper(keyType) ||
//                                keyType.equals(String.class) || keyType.isEnum()) {
//                            key1 = randomValueForClass(keyType);
//                            key2 = randomValueForClass(keyType);
//                        } else {
//                            if (depth <= 1) {
//                                key1 = null;
//                                key2 = null;
//                            } else {
//                                key1 = keyType.getDeclaredConstructor().newInstance();
//                                key2 = keyType.getDeclaredConstructor().newInstance();
//                                fillObjectsDifferentInternal(key1, key2, arraySize, collectionSize,
//                                        visited1, visited2, depth - 1);
//                            }
//                        }
//                        // Values:
//                        if (valueType.isPrimitive() || isWrapper(valueType) ||
//                                valueType.equals(String.class) || valueType.isEnum()) {
//                            value1 = randomValueForClass(valueType);
//                            value2 = randomValueForClass(valueType);
//                        } else {
//                            if (depth <= 1) {
//                                value1 = null;
//                                value2 = null;
//                            } else {
//                                value1 = valueType.getDeclaredConstructor().newInstance();
//                                value2 = valueType.getDeclaredConstructor().newInstance();
//                                fillObjectsDifferentInternal(value1, value2, arraySize, collectionSize,
//                                        visited1, visited2, depth - 1);
//                            }
//                        }
//                        map1.put(key1, value1);
//                        map2.put(key2, value2);
//                    }
//                    field.set(obj1, map1);
//                    field.set(obj2, map2);
//                }
//                // Other custom objects:
//                else {
//                    if (depth <= 1) {
//                        field.set(obj1, null);
//                        field.set(obj2, null);
//                    } else {
//                        Object child1 = field.get(obj1);
//                        Object child2 = field.get(obj2);
//                        if (child1 == null) {
//                            child1 = field.getType().getDeclaredConstructor().newInstance();
//                            field.set(obj1, child1);
//                        }
//                        if (child2 == null) {
//                            child2 = field.getType().getDeclaredConstructor().newInstance();
//                            field.set(obj2, child2);
//                        }
//                        fillObjectsDifferentInternal(child1, child2, arraySize, collectionSize,
//                                visited1, visited2, depth - 1);
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    /**
//     * Helper method to create and fill multi-dimensional arrays recursively.
//     */
//    private static Object createAndFillArrayInternal(Class<?> arrayType, int arraySize, int collectionSize,
//                                                     Set<Object> visited, int depth) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
//        Class<?> compType = arrayType.getComponentType();
//        Object arr = Array.newInstance(compType, arraySize);
//        for (int i = 0; i < arraySize; i++) {
//            Object elem;
//            if (compType.isArray()) {
//                elem = createAndFillArrayInternal(compType, arraySize, collectionSize, visited, depth - 1);
//            } else if (compType.isPrimitive() || isWrapper(compType) ||
//                    compType.equals(String.class) || compType.isEnum()) {
//                elem = randomValueForClass(compType);
//            } else {
//                if (depth <= 1) {
//                    elem = null;
//                } else {
//                    elem = compType.getDeclaredConstructor().newInstance();
//                    fillObjectInternal(elem, arraySize, collectionSize, visited, depth - 1);
//                }
//            }
//            Array.set(arr, i, elem);
//        }
//        return arr;
//    }
//
//    /**
//     * Copies all fields from the source object to the target object.
//     */
//    private static void copyFields(Object source, Object target) {
//        List<Field> fields = getAllFields(source.getClass());
//        for (Field field : fields) {
//            if (Modifier.isStatic(field.getModifiers()))
//                continue;
//            field.setAccessible(true);
//            try {
//                Object value = field.get(source);
//                field.set(target, value);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    /**
//     * Returns a random value for the given class.
//     */
//    private static Object randomValueForClass(Class<?> type) {
//        if (type.isEnum()) {
//            Object[] enumConstants = type.getEnumConstants();
//            return enumConstants.length > 0 ? enumConstants[RANDOM.nextInt(enumConstants.length)] : null;
//        } else if (type.equals(int.class) || type.equals(Integer.class)) {
//            return RANDOM.nextInt(100);
//        } else if (type.equals(long.class) || type.equals(Long.class)) {
//            return RANDOM.nextLong();
//        } else if (type.equals(double.class) || type.equals(Double.class)) {
//            return RANDOM.nextDouble();
//        } else if (type.equals(float.class) || type.equals(Float.class)) {
//            return RANDOM.nextFloat();
//        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
//            return RANDOM.nextBoolean();
//        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
//            return (byte) RANDOM.nextInt(128);
//        } else if (type.equals(short.class) || type.equals(Short.class)) {
//            return (short) RANDOM.nextInt(Short.MAX_VALUE);
//        } else if (type.equals(char.class) || type.equals(Character.class)) {
//            return (char) (RANDOM.nextInt(26) + 'a');
//        } else if (type.equals(String.class)) {
//            return "Str" + RANDOM.nextInt(1000);
//        }
//        return null;
//    }
//
//    /**
//     * Checks if a class is one of the wrapper types.
//     */
//    private static boolean isWrapper(Class<?> type) {
//        return type.equals(Integer.class) || type.equals(Long.class) ||
//                type.equals(Double.class) || type.equals(Float.class) ||
//                type.equals(Boolean.class) || type.equals(Byte.class) ||
//                type.equals(Short.class) || type.equals(Character.class);
//    }
//
//    /**
//     * Creates a new instance for a Collection.
//     */
//    private static Collection<Object> createCollectionInstance(Class<?> collectionClass) {
//        if (!collectionClass.isInterface() && !Modifier.isAbstract(collectionClass.getModifiers())) {
//            try {
//                return (Collection<Object>) collectionClass.getDeclaredConstructor().newInstance();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        if (List.class.isAssignableFrom(collectionClass)) {
//            return new ArrayList<>();
//        }
//        if (Set.class.isAssignableFrom(collectionClass)) {
//            return new LinkedHashSet<>();
//        }
//        return new ArrayList<>();
//    }
//
//    /**
//     * Creates a new instance for a Map.
//     * Returns a LinkedHashMap to preserve insertion order.
//     */
//    private static Map<Object, Object> createMapInstance(Class<?> mapClass) {
//        if (!mapClass.isInterface() && !Modifier.isAbstract(mapClass.getModifiers())) {
//            try {
//                Map<Object, Object> map = (Map<Object, Object>) mapClass.getDeclaredConstructor().newInstance();
//                if (!(map instanceof SortedMap) && !(map instanceof LinkedHashMap)) {
//                    return new LinkedHashMap<>();
//                }
//                return map;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        if (SortedMap.class.isAssignableFrom(mapClass)) {
//            return new TreeMap<>();
//        }
//        return new LinkedHashMap<>();
//    }
//
//    /**
//     * Recursively collects all declared fields for a class.
//     */
//    private static List<Field> getAllFields(Class<?> clazz) {
//        List<Field> fields = new ArrayList<>();
//        while (clazz != null && !clazz.equals(Object.class)) {
//            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
//            clazz = clazz.getSuperclass();
//        }
//        return fields;
//    }
//
//    /**
//     * Returns the generic type parameter of a field at the given index.
//     */
//    private static Class<?> getGenericType(Field field, int index) {
//        Type genericType = field.getGenericType();
//        if (genericType instanceof ParameterizedType) {
//            ParameterizedType pt = (ParameterizedType) genericType;
//            Type[] typeArgs = pt.getActualTypeArguments();
//            if (typeArgs.length > index && typeArgs[index] instanceof Class) {
//                return (Class<?>) typeArgs[index];
//            }
//        }
//        return null;
//    }
//}


package com.romiis.equallibtestapp.util;

import java.lang.reflect.*;
import java.util.*;
import java.lang.reflect.Array;

public class ObjectFillerUtil {

    private static final Random RANDOM = new Random();
    private static final int MAX_DEPTH = 10;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------
    public static void fillObjects(Object obj1, Object obj2, boolean equals) {
        fillObjects(obj1, obj2, equals, 3, 3, MAX_DEPTH);
    }

    public static void fillObjects(Object obj1, Object obj2, boolean equals,
                                   int arraySize, int collectionSize, int maxDepth) {
        if (obj1 == null || obj2 == null) {
            return;
        }

        if (equals) {
            // Single-object BFS fill for obj1, then deep copy, then copy fields to obj2
            fillObjectBFS(obj1, arraySize, collectionSize, maxDepth);
            Object copy = DeepCopyUtil.deepCopy(obj1); // your existing deep copy
            copyFields(copy, obj2);
        } else {
            // BFS fill for two objects with independent random values
            fillObjectsDifferentBFS(obj1, obj2, arraySize, collectionSize, maxDepth);
        }
    }

    // -------------------------------------------------------------------------
    // Single-object BFS fill
    // -------------------------------------------------------------------------
    private static void fillObjectBFS(Object root, int arraySize, int collectionSize, int maxDepth) {
        // visited set to detect cycles
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        // Each queue entry: (objectToFill, depthRemaining)
        Queue<FillTask> queue = new LinkedList<>();
        queue.offer(new FillTask(root, maxDepth));

        while (!queue.isEmpty()) {
            FillTask current = queue.poll();
            Object obj = current.obj;
            int depth = current.depth;

            if (obj == null || depth <= 0) {
                // No expansion
                continue;
            }
            if (visited.contains(obj)) {
                // cycle
                continue;
            }
            visited.add(obj);

            // Possibly skip standard library classes (like original code).
            if (obj.getClass().getName().startsWith("java.")
                    && !(obj instanceof Collection)
                    && !(obj instanceof Map)
                    && !obj.getClass().isArray()) {
                // skip
                continue;
            }

            // fill all non-static fields
            List<Field> fields = getAllFields(obj.getClass());
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);

                try {
                    Class<?> fieldType = field.getType();
                    // 1) PRIMITIVES / WRAPPERS / STRINGS
                    if (fieldType.isPrimitive() || isWrapper(fieldType) || fieldType.equals(String.class)) {
                        field.set(obj, randomValueForClass(fieldType));
                    }
                    // 2) ENUM
                    else if (fieldType.isEnum()) {
                        Object[] enumConstants = fieldType.getEnumConstants();
                        if (enumConstants.length > 0) {
                            field.set(obj, enumConstants[RANDOM.nextInt(enumConstants.length)]);
                        }
                    }
                    // 3) ARRAY
                    else if (fieldType.isArray()) {
                        Class<?> compType = fieldType.getComponentType();
                        Object arr = Array.newInstance(compType, arraySize);
                        for (int i = 0; i < arraySize; i++) {
                            Object elem;
                            if (compType.isArray()) {
                                // multi-dimensional arrays
                                elem = createAndFillArrayBFS(compType, arraySize, collectionSize, depth - 1, visited);
                            } else if (compType.isPrimitive() || isWrapper(compType)
                                    || compType.equals(String.class) || compType.isEnum()) {
                                elem = randomValueForClass(compType);
                            } else {
                                // deeper object
                                if (depth <= 1) {
                                    elem = null;
                                } else {
                                    elem = compType.getDeclaredConstructor().newInstance();
                                    queue.offer(new FillTask(elem, depth - 1));
                                }
                            }
                            Array.set(arr, i, elem);
                        }
                        field.set(obj, arr);
                    }
                    // 4) COLLECTION
                    else if (Collection.class.isAssignableFrom(fieldType)) {
                        Collection<Object> col = createCollectionInstance(fieldType);
                        Class<?> elementType = getGenericType(field, 0);
                        if (elementType == null) {
                            elementType = Object.class;
                        }
                        for (int i = 0; i < collectionSize; i++) {
                            Object elem;
                            if (elementType.isPrimitive() || isWrapper(elementType)
                                    || elementType.equals(String.class) || elementType.isEnum()) {
                                elem = randomValueForClass(elementType);
                            } else {
                                if (depth <= 1) {
                                    elem = null;
                                } else {
                                    elem = elementType.getDeclaredConstructor().newInstance();
                                    queue.offer(new FillTask(elem, depth - 1));
                                }
                            }
                            col.add(elem);
                        }
                        field.set(obj, col);
                    }
                    // 5) MAP
                    else if (Map.class.isAssignableFrom(fieldType)) {
                        Map<Object, Object> map = createMapInstance(fieldType);
                        Class<?> keyType = getGenericType(field, 0);
                        Class<?> valueType = getGenericType(field, 1);
                        if (keyType == null) {
                            keyType = Object.class;
                        }
                        if (valueType == null) {
                            valueType = Object.class;
                        }
                        for (int i = 0; i < collectionSize; i++) {
                            Object key, value;
                            // key
                            if (keyType.isPrimitive() || isWrapper(keyType)
                                    || keyType.equals(String.class) || keyType.isEnum()) {
                                key = randomValueForClass(keyType);
                            } else {
                                if (depth <= 1) {
                                    key = null;
                                } else {
                                    key = keyType.getDeclaredConstructor().newInstance();
                                    queue.offer(new FillTask(key, depth - 1));
                                }
                            }
                            // value
                            if (valueType.isPrimitive() || isWrapper(valueType)
                                    || valueType.equals(String.class) || valueType.isEnum()) {
                                value = randomValueForClass(valueType);
                            } else {
                                if (depth <= 1) {
                                    value = null;
                                } else {
                                    value = valueType.getDeclaredConstructor().newInstance();
                                    queue.offer(new FillTask(value, depth - 1));
                                }
                            }
                            map.put(key, value);
                        }
                        field.set(obj, map);
                    }
                    // 6) OTHER CUSTOM OBJECT
                    else {
                        if (depth <= 1) {
                            field.set(obj, null);
                        } else {
                            Object child = field.get(obj);
                            if (child == null) {
                                child = fieldType.getDeclaredConstructor().newInstance();
                                field.set(obj, child);
                            }
                            // enqueue child for further filling
                            queue.offer(new FillTask(child, depth - 1));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } // end for each field
        } // end while
    }

    // -------------------------------------------------------------------------
    // Two‐objects BFS fill with different random values
    // -------------------------------------------------------------------------
    private static void fillObjectsDifferentBFS(Object root1, Object root2,
                                                int arraySize, int collectionSize, int maxDepth) {
        // Two visited sets, one per object tree
        Set<Object> visited1 = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> visited2 = Collections.newSetFromMap(new IdentityHashMap<>());

        // Each queue entry: (obj1, obj2, depth)
        Queue<DualFillTask> queue = new LinkedList<>();
        queue.offer(new DualFillTask(root1, root2, maxDepth));

        while (!queue.isEmpty()) {
            DualFillTask current = queue.poll();
            Object obj1 = current.obj1;
            Object obj2 = current.obj2;
            int depth = current.depth;

            if (obj1 == null || obj2 == null || depth <= 0) {
                continue;
            }
            if (visited1.contains(obj1) || visited2.contains(obj2)) {
                // skip if either side is visited (cycle)
                continue;
            }
            visited1.add(obj1);
            visited2.add(obj2);

            // same skip logic for standard library classes if you want
            if (obj1.getClass().getName().startsWith("java.")
                    && !(obj1 instanceof Collection)
                    && !(obj1 instanceof Map)
                    && !obj1.getClass().isArray()) {
                continue;
            }
            // We assume obj1.getClass() == obj2.getClass() in typical usage
            // If not, this code might behave strangely.

            List<Field> fields = getAllFields(obj1.getClass());
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                field.setAccessible(true);

                try {
                    Class<?> fieldType = field.getType();
                    // 1) PRIMITIVES / WRAPPERS / STRINGS
                    if (fieldType.isPrimitive() || isWrapper(fieldType) || fieldType.equals(String.class)) {
                        Object val1 = randomValueForClass(fieldType);
                        Object val2 = randomValueForClass(fieldType);
                        field.set(obj1, val1);
                        field.set(obj2, val2);
                    }
                    // 2) ENUM
                    else if (fieldType.isEnum()) {
                        Object[] enumConstants = fieldType.getEnumConstants();
                        if (enumConstants.length > 0) {
                            Object val1 = enumConstants[RANDOM.nextInt(enumConstants.length)];
                            Object val2 = enumConstants[RANDOM.nextInt(enumConstants.length)];
                            field.set(obj1, val1);
                            field.set(obj2, val2);
                        }
                    }
                    // 3) ARRAY
                    else if (fieldType.isArray()) {
                        handleDifferentArrayBFS(obj1, obj2, field, arraySize, collectionSize,
                                visited1, visited2, queue, depth);
                    }
                    // 4) COLLECTION
                    else if (Collection.class.isAssignableFrom(fieldType)) {
                        handleDifferentCollectionBFS(obj1, obj2, field, arraySize, collectionSize,
                                visited1, visited2, queue, depth);
                    }
                    // 5) MAP
                    else if (Map.class.isAssignableFrom(fieldType)) {
                        handleDifferentMapBFS(obj1, obj2, field, arraySize, collectionSize,
                                visited1, visited2, queue, depth);
                    }
                    // 6) OTHER CUSTOM OBJECT
                    else {
                        if (depth <= 1) {
                            field.set(obj1, null);
                            field.set(obj2, null);
                        } else {
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
                            queue.offer(new DualFillTask(child1, child2, depth - 1));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helper for array creation BFS (SINGLE FILL)
    // -------------------------------------------------------------------------
    private static Object createAndFillArrayBFS(Class<?> arrayType,
                                                int arraySize, int collectionSize,
                                                int depth,
                                                Set<Object> visited) throws Exception {
        Object arr = Array.newInstance(arrayType.getComponentType(), arraySize);
        Class<?> compType = arrayType.getComponentType();

        for (int i = 0; i < arraySize; i++) {
            Object elem;
            if (compType.isArray()) {
                // multi-dimensional
                if (depth > 0 && !visited.contains(arr)) {
                    elem = createAndFillArrayBFS(compType, arraySize, collectionSize, depth - 1, visited);
                } else {
                    elem = null;
                }
            } else if (compType.isPrimitive() || isWrapper(compType)
                    || compType.equals(String.class) || compType.isEnum()) {
                elem = randomValueForClass(compType);
            } else {
                if (depth <= 0) {
                    elem = null;
                } else {
                    elem = compType.getDeclaredConstructor().newInstance();
                    // We do a minimal fill here or just skip.
                    // Typically you'd want to queue it, but we're inside a single-array-creation method.
                    // For simplicity, let's just do direct fill:
                    // you can also do fillObjectBFS(...) in a partial approach if you like.
                }
            }
            Array.set(arr, i, elem);
        }
        return arr;
    }

    // -------------------------------------------------------------------------
    // Helpers for "Different" BFS expansions
    // -------------------------------------------------------------------------
    private static void handleDifferentArrayBFS(Object obj1, Object obj2, Field field,
                                                int arraySize, int collectionSize,
                                                Set<Object> visited1, Set<Object> visited2,
                                                Queue<DualFillTask> queue, int depth)
            throws Exception {
        Class<?> compType = field.getType().getComponentType();
        Object arr1 = Array.newInstance(compType, arraySize);
        Object arr2 = Array.newInstance(compType, arraySize);
        field.set(obj1, arr1);
        field.set(obj2, arr2);

        for (int i = 0; i < arraySize; i++) {
            Object elem1, elem2;
            if (compType.isArray()) {
                // If you wanted to fill multi-dimensional arrays BFS style,
                // you'd do something similar to handle this, or set them to null if depth is too small.
                elem1 = null; // or create array, and queue it
                elem2 = null;
            } else if (compType.isPrimitive() || isWrapper(compType)
                    || compType.equals(String.class) || compType.isEnum()) {
                elem1 = randomValueForClass(compType);
                elem2 = randomValueForClass(compType);
            } else {
                if (depth <= 1) {
                    elem1 = null;
                    elem2 = null;
                } else {
                    elem1 = compType.getDeclaredConstructor().newInstance();
                    elem2 = compType.getDeclaredConstructor().newInstance();
                    // we queue them so they can differ
                    queue.offer(new DualFillTask(elem1, elem2, depth - 1));
                }
            }
            Array.set(arr1, i, elem1);
            Array.set(arr2, i, elem2);
        }
    }

    private static void handleDifferentCollectionBFS(Object obj1, Object obj2, Field field,
                                                     int arraySize, int collectionSize,
                                                     Set<Object> visited1, Set<Object> visited2,
                                                     Queue<DualFillTask> queue, int depth)
            throws Exception {
        Collection<Object> col1 = createCollectionInstance(field.getType());
        Collection<Object> col2 = createCollectionInstance(field.getType());
        field.set(obj1, col1);
        field.set(obj2, col2);

        Class<?> elementType = getGenericType(field, 0);
        if (elementType == null) {
            elementType = Object.class;
        }
        for (int i = 0; i < collectionSize; i++) {
            Object elem1, elem2;
            if (elementType.isPrimitive() || isWrapper(elementType)
                    || elementType.equals(String.class) || elementType.isEnum()) {
                elem1 = randomValueForClass(elementType);
                elem2 = randomValueForClass(elementType);
            } else {
                if (depth <= 1) {
                    elem1 = null;
                    elem2 = null;
                } else {
                    elem1 = elementType.getDeclaredConstructor().newInstance();
                    elem2 = elementType.getDeclaredConstructor().newInstance();
                    queue.offer(new DualFillTask(elem1, elem2, depth - 1));
                }
            }
            col1.add(elem1);
            col2.add(elem2);
        }
    }

    private static void handleDifferentMapBFS(Object obj1, Object obj2, Field field,
                                              int arraySize, int collectionSize,
                                              Set<Object> visited1, Set<Object> visited2,
                                              Queue<DualFillTask> queue, int depth)
            throws Exception {
        Map<Object, Object> map1 = createMapInstance(field.getType());
        Map<Object, Object> map2 = createMapInstance(field.getType());
        field.set(obj1, map1);
        field.set(obj2, map2);

        Class<?> keyType = getGenericType(field, 0);
        Class<?> valueType = getGenericType(field, 1);
        if (keyType == null) {
            keyType = Object.class;
        }
        if (valueType == null) {
            valueType = Object.class;
        }

        for (int i = 0; i < collectionSize; i++) {
            Object key1, key2, val1, val2;

            // Build keys
            if (keyType.isPrimitive() || isWrapper(keyType)
                    || keyType.equals(String.class) || keyType.isEnum()) {
                key1 = randomValueForClass(keyType);
                key2 = randomValueForClass(keyType);
            } else {
                if (depth <= 1) {
                    key1 = null;
                    key2 = null;
                } else {
                    key1 = keyType.getDeclaredConstructor().newInstance();
                    key2 = keyType.getDeclaredConstructor().newInstance();
                    queue.offer(new DualFillTask(key1, key2, depth - 1));
                }
            }

            // Build values
            if (valueType.isPrimitive() || isWrapper(valueType)
                    || valueType.equals(String.class) || valueType.isEnum()) {
                val1 = randomValueForClass(valueType);
                val2 = randomValueForClass(valueType);
            } else {
                if (depth <= 1) {
                    val1 = null;
                    val2 = null;
                } else {
                    val1 = valueType.getDeclaredConstructor().newInstance();
                    val2 = valueType.getDeclaredConstructor().newInstance();
                    queue.offer(new DualFillTask(val1, val2, depth - 1));
                }
            }
            map1.put(key1, val1);
            map2.put(key2, val2);
        }
    }

    // -------------------------------------------------------------------------
    // Copy fields (unchanged from your original code)
    // -------------------------------------------------------------------------
    private static void copyFields(Object source, Object target) {
        List<Field> fields = getAllFields(source.getClass());
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()))
                continue;
            field.setAccessible(true);
            try {
                Object value = field.get(source);
                field.set(target, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Random Value for Class (unchanged from your original code)
    // -------------------------------------------------------------------------
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
        // otherwise null
        return null;
    }

    // -------------------------------------------------------------------------
    // Create Collection / Map instances (unchanged from your original code)
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Reflection utilities (unchanged from your original code)
    // -------------------------------------------------------------------------
    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && !clazz.equals(Object.class)) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

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

    private static boolean isWrapper(Class<?> type) {
        return type.equals(Integer.class) || type.equals(Long.class) ||
                type.equals(Double.class) || type.equals(Float.class) ||
                type.equals(Boolean.class) || type.equals(Byte.class) ||
                type.equals(Short.class) || type.equals(Character.class);
    }

    // -------------------------------------------------------------------------
    // "Task" holder classes for BFS
    // -------------------------------------------------------------------------
    // For single-object BFS
    private static class FillTask {
        final Object obj;
        final int depth;

        FillTask(Object obj, int depth) {
            this.obj = obj;
            this.depth = depth;
        }
    }

    // For two-object BFS
    private static class DualFillTask {
        final Object obj1;
        final Object obj2;
        final int depth;

        DualFillTask(Object o1, Object o2, int d) {
            this.obj1 = o1;
            this.obj2 = o2;
            this.depth = d;
        }
    }
}
