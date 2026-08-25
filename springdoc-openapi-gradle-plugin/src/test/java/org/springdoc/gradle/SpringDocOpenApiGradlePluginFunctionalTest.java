package org.springdoc.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
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

    private Path sampleApp;

    @BeforeEach
    void setUp() throws IOException {
        sampleApp = Paths.get("src/test/resources/sample-app-webflux");
    }

    @Test
    void generatesOpenApiSpecFromReactiveApp() throws IOException {
        // Copy sample app into the temp project
        copyRecursively(sampleApp, testProjectDir);

        // Add settings + build for the sample app, using the plugin under test
        Files.writeString(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'sample-app-webflux'\n");
        Files.writeString(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.github.vpelikh.springdoc-openapi-gradle-plugin'
                }

                repositories {
                mavenLocal()
                mavenCentral()
                }

                dependencies {
                implementation 'org.springframework.boot:spring-boot-starter-webflux:4.1.1'
                implementation 'io.github.vpelikh:springdoc-openapi-starter-webflux-api:5.0.6-SNAPSHOT'
                }

                openApiGenerate {
                mainClass = 'test.SampleApp'
                }
                """);

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
        Files.writeString(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.github.vpelikh.springdoc-openapi-gradle-plugin'
                }

                repositories {
                mavenLocal()
                mavenCentral()
                }

                dependencies {
                implementation 'org.springframework.boot:spring-boot-starter-web:4.1.1'
                implementation 'io.github.vpelikh:springdoc-openapi-starter-webmvc-api:5.0.6-SNAPSHOT'
                }

                openApiGenerate {
                mainClass = 'test.SampleApp'
                systemProperties = [
                    'spring.main.banner-mode': 'off'
                ]
                }
                """);

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
        copyRecursively(sampleApp, testProjectDir);

        Files.writeString(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'sample-app-skip'\n");
        Files.writeString(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.github.vpelikh.springdoc-openapi-gradle-plugin'
                }

                repositories {
                mavenLocal()
                mavenCentral()
                }

                dependencies {
                implementation 'org.springframework.boot:spring-boot-starter-webflux:4.1.1'
                implementation 'io.github.vpelikh:springdoc-openapi-starter-webflux-api:5.0.6-SNAPSHOT'
                }

                openApiGenerate {
                mainClass = 'test.SampleApp'
                skip = true
                }
                """);

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
        copyRecursively(sampleApp, testProjectDir);

        Files.writeString(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'sample-app-yaml'\n");
        Files.writeString(testProjectDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'io.github.vpelikh.springdoc-openapi-gradle-plugin'
                }

                repositories {
                mavenLocal()
                mavenCentral()
                }

                dependencies {
                implementation 'org.springframework.boot:spring-boot-starter-webflux:4.1.1'
                implementation 'io.github.vpelikh:springdoc-openapi-starter-webflux-api:5.0.6-SNAPSHOT'
                }

                openApiGenerate {
                mainClass = 'test.SampleApp'
                format = 'yaml'
                }
                """);

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