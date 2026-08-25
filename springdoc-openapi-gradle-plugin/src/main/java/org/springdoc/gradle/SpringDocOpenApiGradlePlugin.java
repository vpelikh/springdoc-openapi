package org.springdoc.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.springdoc.gradle.tasks.GenerateOpenApiTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gradle plugin that generates the OpenAPI specification for a Spring Boot application
 * without ever starting a web server. It forks a dedicated JVM that boots the application's
 * reactive web context (with a no-op web server factory), lets springdoc-openapi build the
 * spec, then writes it to disk and shuts the context down.
 */
public class SpringDocOpenApiGradlePlugin implements Plugin<Project> {

    private static final String PLUGIN_PROPERTIES = "/springdoc-openapi-gradle-plugin.properties";
    private static final String PLUGIN_VERSION = loadPluginVersion();

    @Override
    public void apply(Project project) {
        // Configuration carrying the thin worker jar (and its transitive runtime deps).
        Configuration generatorConfig = project.getConfigurations().create("springdocGenerator")
                .setVisible(false)
                .setTransitive(true);
        // Deliberately does NOT add a springdoc stack API as a safety net: forcing the
        // webflux-api jar onto the fork classpath of a WebMvc app would flip springdoc's
        // stack detection (servlet-first) and silently generate a reactive spec for a
        // servlet app. The target app's own runtimeClasspath already provides the stack API
        // it needs. This keeps the fork classpath identical in shape to the Maven Mojo's.
        generatorConfig.defaultDependencies(dependencies -> dependencies.addAll(java.util.List.of(
                project.getDependencies().create(
                        "io.github.vpelikh:springdoc-openapi-generator-worker:" + PLUGIN_VERSION)
        )));

        OpenApiGenerateExtension extension = project.getExtensions()
                .create("openApiGenerate", OpenApiGenerateExtension.class, project.getObjects());
        extension.getOutputDir().convention(project.getLayout().getBuildDirectory().dir("docs"));

        SourceSet mainSourceSet = project.getExtensions()
                .getByType(SourceSetContainer.class)
                .getByName("main");

        TaskProvider<GenerateOpenApiTask> task = project.getTasks().register("generateOpenApi", GenerateOpenApiTask.class, t -> {
            t.setGroup("documentation");
            t.setDescription("Generates the OpenAPI specification from the Spring Boot application.");
            t.dependsOn(mainSourceSet.getClassesTaskName());
            t.getClasspath().from(mainSourceSet.getOutput(), mainSourceSet.getRuntimeClasspath());
            t.getGeneratorClasspath().from(generatorConfig);
            t.getOutputDir().convention(extension.getOutputDir());
            t.getMainClass().convention(extension.getMainClass());
            t.getOutputFileName().convention(extension.getOutputFileName());
            t.getFormat().convention(extension.getFormat());
            t.getTimeoutSeconds().convention(extension.getTimeoutSeconds());
            t.getSystemProperties().set(extension.getSystemProperties());
            var javaExe = resolveJavaLauncherExecutable(project);
            if (javaExe != null) {
                t.getJavaExecutable().convention(javaExe);
            }
            // Allow skipping via the extension (`openApiGenerate.skip = true`).
            t.onlyIf(spec -> !extension.getSkip().getOrElse(false));
        });
    }

    /**
     * Resolves the {@code java} executable from the project's Java toolchain (the same JDK used
     * to compile the application), so the forked worker runs on the configured toolchain rather
     * than the Gradle daemon's JVM. Falls back to the daemon JVM if there is no toolchain.
     */
    private static org.gradle.api.provider.Provider<org.gradle.api.file.RegularFile> resolveJavaLauncherExecutable(Project project) {
        JavaToolchainService toolchainService = project.getExtensions().findByType(JavaToolchainService.class);
        org.gradle.api.plugins.JavaPluginExtension javaExtension =
                project.getExtensions().findByType(org.gradle.api.plugins.JavaPluginExtension.class);
        if (toolchainService == null || javaExtension == null || javaExtension.getToolchain() == null) {
            return null;
        }
        org.gradle.api.provider.Provider<JavaLauncher> launcher =
                toolchainService.launcherFor(javaExtension.getToolchain());
        return launcher.map(JavaLauncher::getExecutablePath);
    }

    private static String loadPluginVersion() {
        Properties properties = new Properties();
        try (InputStream stream = SpringDocOpenApiGradlePlugin.class.getResourceAsStream(PLUGIN_PROPERTIES)) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + PLUGIN_PROPERTIES);
            }
            properties.load(stream);
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to load " + PLUGIN_PROPERTIES, e);
        }
        String pluginVersion = properties.getProperty("plugin.version");
        if (pluginVersion == null || pluginVersion.trim().isEmpty()
                || (pluginVersion.startsWith("${") && pluginVersion.endsWith("}"))) {
            throw new IllegalStateException("Unresolved plugin.version in " + PLUGIN_PROPERTIES + ": " + pluginVersion);
        }
        return pluginVersion;
    }
}