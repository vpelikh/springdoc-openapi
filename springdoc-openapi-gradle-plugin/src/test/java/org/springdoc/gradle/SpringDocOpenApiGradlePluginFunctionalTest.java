package org.springdoc.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.gradle.testkit.runner.TaskOutcome.SKIPPED;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringDocOpenApiGradlePluginFunctionalTest {

    @TempDir
    Path testProjectDir;

    // Resolved version of the springdoc artifacts that were actually built/installed
    // (e.g. 5.1.0 at release time, 5.1.0-SNAPSHOT during development). Passed in via the
    // springdocVersion system property by build.gradle, falling back to the snapshot for
    // local development.
    private static final String SPRINGDOC_VERSION =
            System.getProperty("springdocVersion", "5.1.0-SNAPSHOT");

    @Test
    void generatesOpenApiSpecFromReactiveApp() throws IOException {
        Path sampleApp = Paths.get("src/test/resources/sample-app-webflux");
        copyRecursively(sampleApp, testProjectDir);

        Files.writeString(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'sample-app-webflux'\n");
        Files.writeString(testProjectDir.resolve("build.gradle"),
                buildGradle("webflux", """
                        mainClass = 'test.SampleApp'
                        """));

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("generateOpenApi", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertTrue(result.task(":generateOpenApi").getOutcome() == SUCCESS);
        Path spec = testProjectDir.resolve("build/docs/openapi.json");
        assertTrue(Files.exists(spec), "Expected generated spec at " + spec + " but it does not exist");
        String content = Files.readString(spec);
        assertTrue(content.contains("/pets"), "expected /pets path in spec but was: " + content.substring(0, Math.min(200, content.length())));
        assertTrue(content.contains("openapi") || content.contains("swagger"), "expected OpenAPI doc root");
    }

    @Test
    void generatesOpenApiSpecFromServletApp() throws IOException {
        copyRecursively(Paths.get("src/test/resources/sample-app-webmvc"), testProjectDir);

        Files.writeString(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'sample-app-webmvc'\n");
        Files.writeString(testProjectDir.resolve("build.gradle"),
                buildGradle("webmvc", """
                        mainClass = 'test.SampleApp'
                        systemProperties = [
                            'spring.main.banner-mode': 'off'
                        ]
                        """));

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("generateOpenApi", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertTrue(result.task(":generateOpenApi").getOutcome() == SUCCESS);
        Path spec = testProjectDir.resolve("build/docs/openapi.json");
        assertTrue(Files.exists(spec), "Expected generated webmvc spec at " + spec + " but it does not exist");
        String content = Files.readString(spec);
        assertTrue(content.contains("/pets"), "expected /pets path in webmvc spec but was: " + content.substring(0, Math.min(200, content.length())));
    }

    @Test
    void skipFlagProducesNoSpec() throws IOException {
        Path sampleApp = Paths.get("src/test/resources/sample-app-webflux");
        copyRecursively(sampleApp, testProjectDir);

        Files.writeString(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'sample-app-skip'\n");
        Files.writeString(testProjectDir.resolve("build.gradle"),
                buildGradle("webflux", """
                        mainClass = 'test.SampleApp'
                        skip = true
                        """));

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("generateOpenApi", "--stacktrace")
                .withPluginClasspath()
                .build();

        // The task is skipped, not exercised, and the build still succeeds.
        assertTrue(result.task(":generateOpenApi").getOutcome() == SKIPPED,
                "expected generateOpenApi to be SKIPPED when skip=true");
        Path spec = testProjectDir.resolve("build/docs/openapi.json");
        assertTrue(!Files.exists(spec), "Expected no spec at " + spec + " when skip=true");
    }

    @Test
    void generatesOpenApiYamlFormat() throws IOException {
        Path sampleApp = Paths.get("src/test/resources/sample-app-webflux");
        copyRecursively(sampleApp, testProjectDir);

        Files.writeString(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'sample-app-yaml'\n");
        Files.writeString(testProjectDir.resolve("build.gradle"),
                buildGradle("webflux", """
                        mainClass = 'test.SampleApp'
                        format = 'yaml'
                        """));

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("generateOpenApi", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertTrue(result.task(":generateOpenApi").getOutcome() == SUCCESS);
        Path spec = testProjectDir.resolve("build/docs/openapi.yaml");
        assertTrue(Files.exists(spec), "Expected generated yaml spec at " + spec + " but it does not exist");
        String content = Files.readString(spec);
        assertTrue(content.contains("openapi:"), "expected YAML root marker in spec but was: " + content.substring(0, Math.min(200, content.length())));
        assertTrue(content.contains("/pets"), "expected /pets path in yaml spec but was: " + content.substring(0, Math.min(200, content.length())));
        // A JSON file would start with '[' or '{'; a YAML spec must not.
        String trimmed = content.trim();
        assertTrue(!trimmed.startsWith("{") && !trimmed.startsWith("["),
                "expected YAML output but got JSON: " + content);
    }

    /**
     * Builds the sample app build.gradle for the functional tests.
     * @param starterModule springdoc starter module suffix, e.g. "webflux" or "webmvc".
     * @param openApiExtension the extra {@code openApiGenerate { ... }} block body.
     */
    private String buildGradle(String starterModule, String openApiExtension) {
        String bootDependency = "implementation 'org.springframework.boot:spring-boot-starter-" + starterModule + ":4.1.1'\n";
        String springdocDependency =
                "implementation 'io.github.vpelikh:springdoc-openapi-starter-" + starterModule + "-api:" + SPRINGDOC_VERSION + "'\n";
        return """
                plugins {
                    id 'java'
                    id 'io.github.vpelikh.springdoc-openapi-gradle-plugin'
                }

                repositories {
                mavenLocal()
                mavenCentral()
                }

                dependencies {
                """ + bootDependency + springdocDependency + """
                }

                openApiGenerate {
                """ + openApiExtension + """
                }
                """;
    }

    private void copyRecursively(Path source, Path target) throws IOException {
        try (var stream = Files.walk(source)) {
            for (Path src : (Iterable<Path>) stream::iterator) {
                Path dest = target.resolve(source.relativize(src).toString());
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                }
                else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(src, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
