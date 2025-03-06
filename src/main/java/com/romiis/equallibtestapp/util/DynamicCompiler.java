package com.romiis.equallibtestapp.util;

import lombok.Getter;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * DynamicCompiler.java
 * <p>
 * Compile .java files at runtime and load the compiled classes.
 */
public class DynamicCompiler {


    /**
     * Class loader for loading compiled classes
     */
    @Getter
    private static URLClassLoader classLoader;


    /**
     * Output directory for compiled classes
     */
    private static final String COMPILE_OUT = "dynamicCompileOut";

    /**
     * Compile all .java files in the specified directory.
     *
     * @param sourceFolder Path to the directory containing .java files
     * @throws IOException If an I/O error occurs
     */
    public static void compile(String sourceFolder) throws IOException {

        // Get the directory containing .java files
        File directory = new File(sourceFolder);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new FileNotFoundException("Directory not found: " + sourceFolder);
        }

        // Find all .java files in the directory
        List<File> javaFiles = findJavaFiles(directory);

        // Check if any .java files were found
        if (javaFiles.isEmpty()) {
            System.out.println("No .java files found in directory: " + sourceFolder);
            return;
        }

        // Get the system Java compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        // Get the compilation units for the .java files
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(javaFiles);

        // Create the output directory if it doesn't exist
        File outputDir = new File(COMPILE_OUT);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }


        // Create a compilation task
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, null,
                List.of("-d", outputDir.getAbsolutePath()),
                null, compilationUnits);

        // Perform the compilation
        boolean success = task.call();
        fileManager.close();

        // Print the result
        if (success) {
            System.out.println("Compilation successful.");
            refreshClassLoader();
        } else {
            System.out.println("Compilation failed.");
        }


    }

    /**
     * Find all .java files in the specified directory.
     *
     * @param directory Directory to search
     * @return List of .java files
     */
    private static List<File> findJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    javaFiles.addAll(findJavaFiles(file));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }
        return javaFiles;
    }


    /**
     * Refresh the class loader to load the newly compiled classes.
     *
     * @throws FileNotFoundException If the directory or .class files are not found
     * @throws MalformedURLException If a URL is not formatted correctly
     */
    private static void refreshClassLoader() throws FileNotFoundException, MalformedURLException {

        File classDir = new File(COMPILE_OUT);
        List<URL> urls = getUrls(classDir);

        // Create a new URLClassLoader with the URLs
        if (!urls.isEmpty()) {
            classLoader = new URLClassLoader(urls.toArray(new URL[0]));
        } else {
            throw new FileNotFoundException("No .class files found in the directory.");
        }
    }

    /**
     * Get the URLs for the .class files in the specified directory.
     *
     * @param classDir Directory containing .class files
     * @return List of URLs
     * @throws FileNotFoundException If the directory or .class files are not found
     * @throws MalformedURLException If a URL is not formatted correctly
     */
    private static List<URL> getUrls(File classDir) throws FileNotFoundException, MalformedURLException {

        if (!classDir.exists() || !classDir.isDirectory()) {
            throw new FileNotFoundException("Directory not found: " + classDir.getAbsolutePath());
        }

        // Get all .class files in the directory
        File[] files = classDir.listFiles();
        if (files == null || files.length == 0) {
            throw new FileNotFoundException("No class files found in directory: " + classDir.getAbsolutePath());
        }

        // Create a URL for each .class file
        List<URL> urls = new ArrayList<>();
        for (File file : files) {
            if (file.getName().endsWith(".class")) {
                // Add the parent directory URL, not the individual class file
                urls.add(file.getParentFile().toURI().toURL());
            }
        }
        return urls;
    }


    /**
     * Load a class by its name
     *
     * @param className Name of the class
     * @return Loaded class
     * @throws ClassNotFoundException If the class is not found
     * @throws IOException            If an I/O error occurs
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException, IOException {

        // Check if the class loader is initialized
        if (classLoader == null) {
            throw new ClassNotFoundException("Class loader not initialized.");
        }


        try {
            Class<?> clazz = classLoader.loadClass(className);
            System.out.println("Class loaded: " + clazz.getName());
            return clazz;
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Class not found: " + className, e);
        }
    }


    /**
     * Get the names of all compiled classes.
     * <p>
     * Enum and abstract classes are excluded.
     *
     * @return Array of class names
     * @throws IOException            If an I/O error occurs
     * @throws ClassNotFoundException If a class is not found
     */
    public static Class<?>[] getAllCompiledObjects() throws IOException, ClassNotFoundException {
        List<Class<?>> compiledObjects = new ArrayList<>();

        // Check if the class loader is initialized
        File classDir = new File(COMPILE_OUT);
        if (!classDir.exists() || !classDir.isDirectory()) {
            throw new FileNotFoundException("Directory not found: " + classDir.getAbsolutePath());
        }

        // Get all .class files in the directory
        File[] files = classDir.listFiles((dir, name) -> name.endsWith(".class"));
        if (files == null || files.length == 0) {
            throw new FileNotFoundException("No .class files found in directory: " + classDir.getAbsolutePath());
        }

        // Get the class names from the .class files
        for (File file : files) {
            String className = file.getName();

            className = className.substring(0, className.length() - 6);

            // Load the class
            Class<?> clazz = loadClass(className);

            // Exclude enum and abstract classes
            if (clazz.isEnum() || Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            // Add the class name to the list
            compiledObjects.add(clazz);
        }

        return compiledObjects.toArray(new Class[0]);
    }


}
