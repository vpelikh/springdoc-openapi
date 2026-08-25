package org.springdoc.gradle.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates the OpenAPI specification by forking a dedicated JVM that boots the target Spring
 * Boot application (WebFlux or WebMvc) against the application's own classpath, writes the spec
 * to disk, and shuts the context down. Forking avoids contaminating the Gradle daemon or mixing
 * classloaders.
 */
@CacheableTask
public abstract class GenerateOpenApiTask extends DefaultTask {

    private final ConfigurableFileCollection classpath = getProject().getObjects().fileCollection();
    private final ConfigurableFileCollection generatorClasspath = getProject().getObjects().fileCollection();
    private final DirectoryProperty outputDir = getProject().getObjects().directoryProperty();
    private final Property<String> outputFileName = getProject().getObjects().property(String.class).convention("openapi");
    private final Property<String> format = getProject().getObjects().property(String.class).convention("json");
    private final Property<String> mainClass = getProject().getObjects().property(String.class);
    private final RegularFileProperty javaExecutable = getProject().getObjects().fileProperty();
    private final Property<Integer> timeoutSeconds = getProject().getObjects().property(Integer.class).convention(120);
    private final MapProperty<String, String> systemProperties = getProject().getObjects().mapProperty(String.class, String.class);

    @Inject
    public GenerateOpenApiTask() {
    }

    @Classpath
    public ConfigurableFileCollection getClasspath() {
        return classpath;
    }

    @Classpath
    public ConfigurableFileCollection getGeneratorClasspath() {
        return generatorClasspath;
    }

    @OutputDirectory
    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    @Input
    @Optional
    public Property<String> getOutputFileName() {
        return outputFileName;
    }

    @Input
    @Optional
    public Property<String> getFormat() {
        return format;
    }

    @Input
    @Optional
    public Property<String> getMainClass() {
        return mainClass;
    }

    /**
     * The {@code java} executable to use for the forked worker. When the project applies the
     * {@code java} plugin, this is wired to the compilation toolchain's launcher rather than the
     * Gradle daemon JVM, keeping the worker on the same JDK as the application.
     */
    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.ABSOLUTE)
    public RegularFileProperty getJavaExecutable() {
        return javaExecutable;
    }

    /**
     * Upper bound (seconds) for the forked worker. Defaults to 120.
     */
    @Input
    @Optional
    public Property<Integer> getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Input
    @Optional
    public MapProperty<String, String> getSystemProperties() {
        return systemProperties;
    }

    @TaskAction
    public void generate() {
        String main = mainClass.getOrNull();
        if (main == null || main.isBlank()) {
            throw new GradleException("openApiGenerate.mainClass must be set to the application's @SpringBootApplication class");
        }

        File appOutput = outputDir.getAsFile().get();
        if (!appOutput.exists() && !appOutput.mkdirs()) {
            throw new GradleException("Could not create output dir " + appOutput);
        }

        String cpString = Stream.concat(classpath.getFiles().stream(), generatorClasspath.getFiles().stream())
                .distinct()
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(File.pathSeparator));

        String javaBin;
        org.gradle.api.file.RegularFile javaExeRef = javaExecutable.getOrNull();
        File javaExe = javaExeRef != null ? javaExeRef.getAsFile() : null;
        if (javaExe != null && javaExe.isFile()) {
            javaBin = javaExe.getAbsolutePath();
        }
        else {
            javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        }
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        // Pass through user-supplied system properties (e.g. spring.profiles.active,
        // spring.autoconfigure.exclude) so infra-dependent apps can be generated against
        // a generation-time profile/overrides.
        systemProperties.getOrElse(java.util.Collections.emptyMap())
                .forEach((k, v) -> command.add("-D" + k + "=" + v));
        command.add("-cp");
        command.add(cpString);
        command.add("org.springdoc.generator.GeneratorWorkerMain");
        command.add(main);
        command.add(appOutput.getAbsolutePath());
        command.add(outputFileName.getOrElse("openapi"));
        command.add(format.getOrElse("json"));

        getLogger().lifecycle("Launching generator worker JVM (" + javaBin + ") for main class " + mainClass);
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    getLogger().lifecycle(line);
                }
            }
            int timeout = timeoutSeconds.getOrElse(120);
            boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new GradleException("Generator worker did not finish within " + timeout + "s and was terminated");
            }
            int exit = process.exitValue();
            if (exit != 0) {
                throw new GradleException("Generator worker exited with code " + exit);
            }
        }
        catch (IOException e) {
            throw new GradleException("Failed to launch generator worker: " + e.getMessage(), e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            throw new GradleException("Generator worker interrupted", e);
        }
    }
}