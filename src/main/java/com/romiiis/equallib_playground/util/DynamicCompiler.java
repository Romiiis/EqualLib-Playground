package com.romiiis.equallib_playground.util;

import lombok.extern.log4j.Log4j2;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DynamicCompiler.java
 * <p>
 * This class compiles .java files at runtime using only the built-in Java Compiler.
 * It attempts to compile files individually in multiple passes—using the output of previous passes
 * as part of the classpath—so that interdependent classes compile in order.
 * Files that never compile are ignored.
 *
 * @author Romiiis
 * @version 1.0
 */
@Log4j2
public class DynamicCompiler {

    /**
     * Output directory for compiled classes.
     */
    private static final String COMPILE_OUT = "dynamicCompileOut";

    /**
     * Compile all .java files in the specified source directory.
     *
     * @param sourceFolder Path to the directory containing .java files
     * @return A ClassLoader that loads the classes that compiled successfully.
     * @throws IOException If an I/O error occurs
     */
    public static ClassLoader compile(String sourceFolder) throws IOException {
        clearOutputDirectory();

        // Get the directory containing .java files.
        File sourceDir = new File(sourceFolder);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new FileNotFoundException("Source directory not found: " + sourceFolder);
        }

        // Find all .java files recursively.
        List<File> javaFiles = findJavaFiles(sourceDir);
        if (javaFiles.isEmpty()) {
            log.error("No .java files found in directory: {}", sourceDir.getAbsolutePath());
            return new URLClassLoader(new URL[0]);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler found. Are you running a JRE instead of a JDK?");
        }

        // This set holds file names (or paths) for which we've already logged errors.
        Set<String> loggedFileErrors = new HashSet<>();

        // We'll try to compile in multiple passes.
        List<File> remainingFiles = new ArrayList<>(javaFiles);
        boolean progress;
        do {
            progress = false;
            List<File> compiledThisPass = new ArrayList<>();

            for (File file : remainingFiles) {
                // Build options: always specify output directory.
                // Also, if any classes have been compiled, add COMPILE_OUT to the classpath so that
                // dependencies from previous passes can be resolved.
                List<String> options = new ArrayList<>();
                options.add("-d");
                options.add(new File(COMPILE_OUT).getAbsolutePath());
                File outputDir = new File(COMPILE_OUT);
                if (outputDir.exists() && outputDir.listFiles() != null && outputDir.listFiles().length > 0) {
                    options.add("-classpath");
                    options.add(outputDir.getAbsolutePath());
                }

                try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
                    Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(file);

                    boolean success = compiler.getTask(
                            null,
                            fileManager,
                            diagnostic -> {
                                // Use the diagnostic source name as a key.
                                String sourceName = diagnostic.getSource() != null
                                        ? diagnostic.getSource().getName() : file.getAbsolutePath();
                                // Log only once per file.
                                if (!loggedFileErrors.contains(sourceName)) {
                                    log.error("Compilation error in {}: {}",
                                            sourceName,
                                            diagnostic.getMessage(null));
                                    loggedFileErrors.add(sourceName);
                                }
                            },
                            options,
                            null,
                            compilationUnits).call();
                    if (success) {
                        log.info("Compiled successfully: {}", file.getAbsolutePath());
                        compiledThisPass.add(file);
                        progress = true;
                    } else {
                        log.warn("Failed to compile: {}", file.getAbsolutePath());
                    }
                }
            }
            remainingFiles.removeAll(compiledThisPass);
        } while (progress && !remainingFiles.isEmpty());

        if (!remainingFiles.isEmpty()) {
            log.warn("The following files failed to compile after multiple passes:");
            for (File file : remainingFiles) {
                log.warn(" - {}", file.getAbsolutePath());
            }
        }

        return refreshClassLoader();
    }

    /**
     * Clear all files from the output directory.
     */
    private static void clearOutputDirectory() {
        File outputDir = new File(COMPILE_OUT);
        if (outputDir.exists() && outputDir.isDirectory()) {
            File[] files = outputDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Recursively find all .java files in the specified directory.
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
     * @return A new URLClassLoader for the compiled classes.
     * @throws FileNotFoundException If the directory or .class files are not found.
     * @throws MalformedURLException If a URL is not formatted correctly.
     */
    private static ClassLoader refreshClassLoader() throws FileNotFoundException, MalformedURLException {
        File classDir = new File(COMPILE_OUT);
        List<URL> urls = getUrls(classDir);
        if (!urls.isEmpty()) {
            return new URLClassLoader(urls.toArray(new URL[0]));
        } else {
            throw new FileNotFoundException("No .class files found in the directory: " + COMPILE_OUT);
        }
    }

    /**
     * Get the URLs for the .class files in the specified directory.
     *
     * @param classDir Directory containing .class files.
     * @return List of URLs.
     * @throws FileNotFoundException If the directory or .class files are not found.
     * @throws MalformedURLException If a URL is not formatted correctly.
     */
    private static List<URL> getUrls(File classDir) throws FileNotFoundException, MalformedURLException {
        if (!classDir.exists() || !classDir.isDirectory()) {
            throw new FileNotFoundException("Directory not found: " + classDir.getAbsolutePath());
        }
        File[] files = classDir.listFiles();
        if (files == null || files.length == 0) {
            throw new FileNotFoundException("No class files found in directory: " + classDir.getAbsolutePath());
        }
        List<URL> urls = new ArrayList<>();
        for (File file : files) {
            if (file.getName().endsWith(".class")) {
                // Use the parent directory URL (so that the class loader can find classes by their package names)
                urls.add(file.getParentFile().toURI().toURL());
            }
        }
        return urls;
    }

    /**
     * Load all valid classes (i.e. those that can be successfully loaded).
     * This filters out classes whose dependencies are missing.
     *
     * @param loader The ClassLoader to use.
     * @return A list of successfully loaded classes.
     * @throws IOException If an I/O error occurs.
     */
    public static List<Class<?>> loadValidClasses(ClassLoader loader) throws IOException {
        List<Class<?>> validClasses = new ArrayList<>();
        File outputDir = new File(COMPILE_OUT);
        List<File> classFiles = findClassFiles(outputDir);
        for (File classFile : classFiles) {
            String className = computeClassName(classFile, outputDir);
            try {
                // Attempt to load the class.
                Class<?> clazz = loader.loadClass(className);
                validClasses.add(clazz);
            } catch (Throwable t) {
                log.warn("Failed to load class {}: {}", className, t.getMessage());
            }
        }
        return validClasses;
    }

    /**
     * Recursively find all .class files in the specified directory.
     *
     * @param directory Directory to search.
     * @return List of .class files.
     */
    private static List<File> findClassFiles(File directory) {
        List<File> classFiles = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    classFiles.addAll(findClassFiles(file));
                } else if (file.getName().endsWith(".class")) {
                    classFiles.add(file);
                }
            }
        }
        return classFiles;
    }

    /**
     * Compute the fully qualified class name for a .class file.
     *
     * @param classFile The .class file.
     * @param baseDir   The base directory (the output directory).
     * @return The fully qualified class name.
     * @throws IOException If an I/O error occurs.
     */
    private static String computeClassName(File classFile, File baseDir) throws IOException {
        String basePath = baseDir.getCanonicalPath();
        String filePath = classFile.getCanonicalPath();
        if (filePath.startsWith(basePath)) {
            String relativePath = filePath.substring(basePath.length() + 1);
            return relativePath.replace(File.separatorChar, '.').replaceAll("\\.class$", "");
        } else {
            throw new IOException("Class file " + classFile + " is not under base directory " + baseDir);
        }
    }
}
