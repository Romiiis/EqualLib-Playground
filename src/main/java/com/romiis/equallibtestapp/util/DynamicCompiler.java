package com.romiis.equallibtestapp.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class DynamicCompiler {


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
    public static ClassLoader compile(String sourceFolder) throws IOException {

        // Get the directory containing .java files
        File directory = new File(sourceFolder);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new FileNotFoundException("Directory not found: " + sourceFolder);
        }

        // Find all .java files in the directory
        List<File> javaFiles = findJavaFiles(directory);

        // Check if any .java files were found
        if (javaFiles.isEmpty()) {
            log.error("No .java files found in directory: {}", directory.getAbsolutePath());
            return new URLClassLoader(new URL[0]);
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
            log.info("Compilation successful.");
            return refreshClassLoader();
        } else {
            log.error("Compilation failed.");
            return new URLClassLoader(new URL[0]);
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
    private static ClassLoader refreshClassLoader() throws FileNotFoundException, MalformedURLException {

        File classDir = new File(COMPILE_OUT);
        List<URL> urls = getUrls(classDir);

        // Create a new URLClassLoader with the URLs
        if (!urls.isEmpty()) {
            return new URLClassLoader(urls.toArray(new URL[0]));
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





}
