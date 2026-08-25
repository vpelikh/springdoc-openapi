package org.springdoc.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

/**
 * Extension for the {@code openApiGenerate { ... }} DSL block.
 */
public abstract class OpenApiGenerateExtension {

    private final Property<String> outputFileName;
    private final DirectoryProperty outputDir;
    private final Property<String> format;
    private final Property<String> mainClass;
    private final Property<Integer> timeoutSeconds;
    private final Property<Boolean> skip;
    private final MapProperty<String, String> systemProperties;

    @Inject
    public OpenApiGenerateExtension(ObjectFactory objects) {
        this.outputFileName = objects.property(String.class).convention("openapi");
        this.outputDir = objects.directoryProperty();
        this.format = objects.property(String.class).convention("json");
        this.mainClass = objects.property(String.class);
        this.timeoutSeconds = objects.property(Integer.class).convention(120);
        this.skip = objects.property(Boolean.class).convention(false);
        this.systemProperties = objects.mapProperty(String.class, String.class);
    }

    public Property<String> getOutputFileName() {
        return outputFileName;
    }

    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    public Property<String> getFormat() {
        return format;
    }

    public Property<String> getMainClass() {
        return mainClass;
    }

    public Property<Integer> getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Whether generation should be skipped entirely. Defaults to {@code false}.
     */
    public Property<Boolean> getSkip() {
        return skip;
    }

    /**
     * Additional system properties ({@code -D}) passed to the forked worker JVM, e.g.
     * {@code spring.profiles.active=generation} or {@code spring.autoconfigure.exclude=...}.
     */
    public MapProperty<String, String> getSystemProperties() {
        return systemProperties;
    }
}