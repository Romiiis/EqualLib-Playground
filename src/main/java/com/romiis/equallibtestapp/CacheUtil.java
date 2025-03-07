package com.romiis.equallibtestapp;

import com.romiis.DeepCopyUtil;
import com.romiis.equallibtestapp.io.FileManager;
import com.romiis.equallibtestapp.util.DynamicCompiler;
import com.romiis.equallibtestapp.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CacheUtil.java
 * <p>
 * Utility class to cache classes and objects.
 */
@Slf4j
public class CacheUtil {

    /**
     * The instance of the CacheUtil class
     */
    private static final CacheUtil INSTANCE = new CacheUtil();

    /**
     * The pool of loaded objects
     */
    private final Map<String, Object> loadedObjectsPool;

    /**
     * The pool of loaded classes
     */
    private final List<Class<?>> loadedClassesPool;


    /**
     * The class loader to load classes from the objects folder
     */
    private ClassLoader classLoader;


    /**
     * The path to the objects folder
     */
    private static final String OBJECTS_PATH = "objects";


    /**
     * Create a new instance of the CacheUtil class
     */
    private CacheUtil() {
        super();
        loadedClassesPool = new ArrayList<>();
        loadedObjectsPool = new HashMap<>();
        try {
            classLoader = DynamicCompiler.compile(OBJECTS_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * Update the cache by loading classes and objects
     * This method is called when the cache is updated.
     */
    public void updateCache() {
        loadClasses();
        loadObjects();
    }

    //region ----------Classes methods----------


    /**
     * Load classes from the class loader and add them to the pool.
     * This method is called when the cache is updated.
     */
    private void loadClasses() {

        // Load classes from the class loader
        String[] classes = getAllClasses();

        // Go through pool and load only new classes
        for (String className : classes) {
            try {
                Class<?> clazz = classLoader.loadClass(className);
                if (!loadedClassesPool.contains(clazz)) {
                    loadedClassesPool.add(clazz);
                }
            } catch (ClassNotFoundException e) {
                log.error("Could not load class: {}", className);
            }
        }

    }

    /**
     * Get all classes from the objects folder (names only).
     *
     * @return an array of class names
     */
    public String[] getAllClasses() {
        File folder = new File(OBJECTS_PATH);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            return new String[0];
        }
        List<String> classes = new ArrayList<>();
        for (File file : listOfFiles) {
            if (file.isFile()) {
                classes.add(file.getName().replace(".java", ""));
            }
        }
        return classes.toArray(new String[0]);
    }


    /**
     * Get all classes from the pool.
     *
     * @param excludeAbsEnums if true, exclude abstract classes and enums
     * @return an array of classes
     */
    public Class<?>[] getClasses(boolean excludeAbsEnums) {
        List<Class<?>> classes = new ArrayList<>();
        for (Class<?> clazz : loadedClassesPool) {
            if (excludeAbsEnums && (clazz.isEnum() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface())) {
                continue;
            }
            classes.add(clazz);
        }
        return classes.toArray(new Class<?>[0]);
    }


    /**
     * Get the class by its name from the pool.
     *
     * @param name the name of the class
     * @return the class
     */
    public Class<?> getClassByName(String name) {
        for (Class<?> clazz : loadedClassesPool) {
            if (clazz.getSimpleName().equals(name)) {
                return clazz;
            }
        }

        // Also try to load the class from the class loader
        try {
            return classLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    //endregion


    //region ----------Objects methods----------

    /**
     * Load objects from the file and add them to the pool.
     * This method is called when the cache is updated.
     */
    private void loadObjects() {

        // Load objects from the file
        String[] objects = FileManager.getSavedFiles();

        // Go through pool and load only new objects
        for (String objectName : objects) {
            try {
                String json = FileManager.loadFile(objectName);
                Object obj = JsonUtil.deserialize(json);

                if (!loadedObjectsPool.containsKey(objectName)) {
                    loadedObjectsPool.put(objectName, obj);
                }
            } catch (Exception e) {
                log.error("Could not load object: {}", objectName);
            }
        }

    }


    /**
     * Get the object by its name from the pool.
     * Creates a deep copy of the object to prevent circular references.
     *
     * @param name the name of the object
     * @return the object
     */
    public Object getObjectByName(String name, boolean copy) {
        if (!loadedObjectsPool.containsKey(name)) {
            return null;
        }

        if (!copy) {
            return loadedObjectsPool.get(name);
        }

        return DeepCopyUtil.deepCopy(loadedObjectsPool.get(name));
    }

    //endregion


    /**
     * Get the instance of the CacheUtil class.
     *
     * @return the instance of the CacheUtil class
     */
    public static CacheUtil getInstance() {
        return INSTANCE;
    }


    /**
     * Get all objects from the pool.
     *
     * @param clazz the class of the objects to get (null for all objects)
     * @return an array of object names
     */
    public String[] getAllObjects(Class<?> clazz) {

        List<String> names = new ArrayList<>();
        for (String name : loadedObjectsPool.keySet()) {
            Object obj = loadedObjectsPool.get(name);
            if (clazz == null || clazz.isInstance(obj)) {
                names.add(name);
            }
        }

        return names.toArray(new String[0]);

    }


    /**
     * Get the class of the object by its name.
     *
     * @param name the name of the object
     * @return the class of the object
     */
    public Class<?> getObjectClass(String name) {
        Object obj = loadedObjectsPool.get(name);
        if (obj == null) {
            return null;
        }
        return obj.getClass();
    }


}
