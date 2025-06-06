package com.romiiis.equallib_playground.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * DeepCopyUtil.java
 * <p>
 * Utility class to perform deep copies of objects.
 * <p>
 * This class provides a method to create a deep copy of an object, including all its fields and sub-objects.
 * It uses reflection to access private fields and handles arrays, collections, and maps.
 *
 * @author Romiiis
 * @version 1.0
 */
public class DeepCopyUtil {

    /**
     * Deep copy an object using a new, local cache.
     *
     * @param object the object to copy
     * @param <T>    the type of the object
     * @return the deep copy of the object, including all internal fields
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(T object) {
        // Create a new cache for each deep copy operation
        Map<Object, Object> copyCache = new IdentityHashMap<>();
        return deepCopyNonRecursive(object, copyCache);
    }

    /**
     * Replaces the recursive deepCopy(...) with an iterative approach.
     * <p>
     * 1) If the object is null/immutable/already in cache, return immediately.
     * 2) Otherwise, create its copy and place (original, copy) into a queue.
     * 3) While the queue is not empty, remove (orig, cpy):
     * - If orig is an array, copy each element (recursively via queue).
     * - Else, copy all fields. For each non-null field, force-lazy-init
     * and either fetch from cache or create a new instance and enqueue it.
     */
    @SuppressWarnings("unchecked")
    private static <T> T deepCopyNonRecursive(T rootObject, Map<Object, Object> copyCache) {
        if (rootObject == null) {
            return null;
        }

        // Force lazy initialization on the *root* object
        rootObject = forceLazyInitialization(rootObject);

        // If the object is immutable, no need to copy
        if (isImmutable(rootObject.getClass())) {
            return rootObject;
        }

        // If already in cache, return cached
        if (copyCache.containsKey(rootObject)) {
            return (T) copyCache.get(rootObject);
        }

        // Allocate the copy (array or regular object)
        T rootCopy = allocateAndCache(rootObject, copyCache);

        // Process everything iteratively
        Queue<Pair> queue = new LinkedList<>();
        queue.offer(new Pair(rootObject, rootCopy));

        while (!queue.isEmpty()) {
            Pair pair = queue.poll();
            Object original = pair.original;
            Object copy = pair.copy;

            Class<?> clazz = original.getClass();
            if (clazz.isArray()) {
                // Copy array elements
                copyArrayElements(original, copy, copyCache, queue);
            } else {
                // Copy all fields (including inherited)
                copyAllFields(original, copy, copyCache, queue);
            }
        }

        return rootCopy;
    }

    /**
     * Allocate a new instance or array, store in cache, then return it.
     * We do NOT copy fields/elements here; that happens in the loop.
     */
    @SuppressWarnings("unchecked")
    private static <T> T allocateAndCache(T original, Map<Object, Object> copyCache) {
        try {
            Class<?> clazz = original.getClass();
            if (clazz.isArray()) {
                int length = Array.getLength(original);
                Object arrayCopy = Array.newInstance(clazz.getComponentType(), length);
                copyCache.put(original, arrayCopy);
                return (T) arrayCopy;
            }

            // It's not an array => allocate an instance
            T newObject = (T) allocateInstance(clazz);
            copyCache.put(original, newObject);
            return newObject;
        } catch (Exception e) {
            throw new RuntimeException("Error allocating copy", e);
        }
    }

    /**
     * Copy all elements of an array into a new array.
     * This is done iteratively to avoid recursion.
     * @param originalArray the original array
     * @param copyArray the copy array
     * @param copyCache the cache for already copied objects
     * @param queue the queue for processing pairs of original and copy objects
     */
    private static void copyArrayElements(Object originalArray,
                                          Object copyArray,
                                          Map<Object, Object> copyCache,
                                          Queue<Pair> queue) {

        int length = Array.getLength(originalArray);
        for (int i = 0; i < length; i++) {
            Object element = Array.get(originalArray, i);
            Object elementCopy = resolveSubObject(element, copyCache, queue);
            Array.set(copyArray, i, elementCopy);
        }
    }

    /**
     * Copy all fields of an object to another object using the given cache.
     * This version copies every field, including those that are normally considered
     * lazy or cached.
     *
     * @param original  the original object
     * @param copy      the object to copy to
     * @param copyCache the cache of already copied objects
     */
    private static void copyAllFields(Object original,
                                      Object copy,
                                      Map<Object, Object> copyCache,
                                      Queue<Pair> queue) {

        Class<?> clazz = original.getClass();
        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);

                // Skip static fields.
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                try {
                    Object fieldValue = field.get(original);
                    Object fieldCopy;
                    if (fieldValue != null && fieldValue.getClass().isArray()) {
                        // We handle arrays separately
                        fieldCopy = resolveSubObject(fieldValue, copyCache, queue);
                    } else {
                        fieldCopy = resolveSubObject(fieldValue, copyCache, queue);
                    }
                    field.set(copy, fieldCopy);
                } catch (Exception e) {
                    throw new RuntimeException("Error copying fields", e);
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * Resolve a sub-object by checking if it's already in the cache or needs to be copied.
     * This method handles lazy initialization and immutable objects.
     *
     * @param subObject the sub-object to resolve
     * @param copyCache the cache of already copied objects
     * @param queue the queue for processing pairs of original and copy objects
     * @return the resolved sub-object
     */
    private static Object resolveSubObject(Object subObject,
                                           Map<Object, Object> copyCache,
                                           Queue<Pair> queue) {

        if (subObject == null) {
            return null;
        }

        // Force lazy initialization
        subObject = forceLazyInitialization(subObject);

        // If immutable, return as is
        if (isImmutable(subObject.getClass())) {
            return subObject;
        }

        // If we've seen this object, reuse the copy
        if (copyCache.containsKey(subObject)) {
            return copyCache.get(subObject);
        }

        // Not in cache => allocate & store
        Object subCopy = allocateAndCache(subObject, copyCache);

        // Enqueue (subObject, subCopy) to process fields/elements
        queue.offer(new Pair(subObject, subCopy));
        return subCopy;
    }

    /**
     * Force lazy initialization on an object by invoking all public no-argument getters.
     * This will (hopefully) trigger the computation of any lazy fields.
     *
     * @param object the object to force lazy initialization on
     * @param <T>    the type of the object
     * @return the object with (hopefully) all lazy fields initialized
     */
    @SuppressWarnings("unchecked")
    private static <T> T forceLazyInitialization(T object) {
        if (object == null) return object;

        // If the object is one of the known collection types, force initialization.
        if (object instanceof Map) {
            ((Map<?, ?>) object).entrySet();
            ((Map<?, ?>) object).keySet();
        } else if (object instanceof Set) {
            ((Set<?>) object).iterator().hasNext();
        } else if (object instanceof Collection) {
            ((Collection<?>) object).size();
            ((Collection<?>) object).toArray();
        }

        // Invoke all public no-arg getters (except getClass).
        for (Method method : object.getClass().getMethods()) {
            if (method.getParameterCount() == 0 &&
                    method.getName().startsWith("get") &&
                    !method.getName().equals("getClass")) {
                try {
                    method.invoke(object);
                } catch (Exception e) {
                    // Ignore exceptions from getters.
                }
            }
        }
        return object;
    }

    /**
     * Check if a class is immutable.
     *
     * @param clazz the class to check
     * @return true if the class is immutable, false otherwise
     */
    private static boolean isImmutable(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz.equals(String.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(Double.class) ||
                clazz.equals(Boolean.class) ||
                clazz.equals(Float.class) ||
                clazz.equals(Long.class) ||
                clazz.equals(Short.class) ||
                clazz.equals(Byte.class) ||
                clazz.equals(Character.class) ||
                clazz.isEnum();
    }

    /**
     * Allocate an instance of a class using Unsafe.
     * @param clazz the class to allocate
     * @return the allocated instance
     * @throws Exception if an error occurs during allocation
     */
    private static Object allocateInstance(Class<?> clazz) throws Exception {
        Method unsafeConstructor = UnsafeHolder.UNSAFE.getClass().getDeclaredMethod("allocateInstance", Class.class);
        return unsafeConstructor.invoke(UnsafeHolder.UNSAFE, clazz);
    }

    /**
     * Holder for the Unsafe instance (same as original).
     */
    private static class UnsafeHolder {
        private static final sun.misc.Unsafe UNSAFE;

        static {
            try {
                Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                UNSAFE = (sun.misc.Unsafe) field.get(null);
            } catch (Exception e) {
                throw new RuntimeException("Could not access Unsafe", e);
            }
        }
    }

    /**
     * Simple struct to hold (original, copy) pairs we need to process.
     */
    private static class Pair {
        final Object original;
        final Object copy;

        Pair(Object original, Object copy) {
            this.original = original;
            this.copy = copy;
        }
    }
}
