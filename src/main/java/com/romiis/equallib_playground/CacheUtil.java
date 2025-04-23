package com.romiis.equallib_playground;


import com.romiis.equallib_playground.io.FileManager;
import com.romiis.equallib_playground.util.DeepCopyUtil;
import com.romiis.equallib_playground.util.DynamicCompiler;
import com.romiis.equallib_playground.util.JsonUtil;
import lombok.extern.log4j.Log4j2;

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
 *
 * @author Romiis
 * @version 1.0
 */
@Log4j2
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
    private static final String CLASSES_DIRECTORY = "classes";


    /**
     * Create a new instance of the CacheUtil class
     * Singleton pattern
     */
    private CacheUtil() {
        super();
        loadedClassesPool = new ArrayList<>();
        loadedObjectsPool = new HashMap<>();
        try {
            classLoader = DynamicCompiler.compile(CLASSES_DIRECTORY);

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
        File folder = new File(CLASSES_DIRECTORY);
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
        // If the name is an array descriptor (starts with '['), handle it specially.
        if (name.startsWith("[")) {
            try {
                return JsonUtil.determineFieldClass(name);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        // Try to match using the full class name first.
        for (Class<?> clazz : loadedClassesPool) {
            if (clazz.getName().equals(name)) {
                return clazz;
            }
        }

        // Then try matching by simple name.
        for (Class<?> clazz : loadedClassesPool) {
            if (clazz.getSimpleName().equals(name)) {
                return clazz;
            }
        }

        // Lastly, try to load it from the class loader.
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
                log.error("Could not load object: {}, {}", objectName, e.getMessage());
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


    /**
     * Get the names of all objects that fit the given class.
     *
     * @param clazz the class to filter by
     * @return a list of object names
     */
    public List<String> getObjectsFitNames(Class<?> clazz) {
        List<String> names = new ArrayList<>();
        for (String name : loadedObjectsPool.keySet()) {
            Object obj = loadedObjectsPool.get(name);
            if (clazz.isInstance(obj)) {
                names.add(name);
            }
        }
        return names;
    }


}
