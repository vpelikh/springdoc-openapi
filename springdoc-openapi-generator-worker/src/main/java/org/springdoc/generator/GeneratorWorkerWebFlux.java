package org.springdoc.generator;

import org.springdoc.webflux.api.OpenApiWebfluxResource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Workers that boot a <b>WebFlux (reactive)</b> Spring Boot application, let springdoc-openapi
 * build the OpenAPI document, write it to disk, and shut the context down. Runs in a forked JVM.
 * <p>
 * A no-op {@link ReactiveWebServerFactory} is registered so springdoc's
 * {@code @ConditionalOnWebApplication} activates without ever binding a port.
 */
public class GeneratorWorkerWebFlux {

    @Configuration
    static class NoServerConfiguration {

        @Bean
        @ConditionalOnMissingBean(ReactiveWebServerFactory.class)
        ReactiveWebServerFactory reactiveWebServerFactory() {
            return new NoOpReactiveWebServerFactory();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException(
                    "Usage: GeneratorWorkerWebFlux <mainClass> <outputDir> [outputFileName] [format]");
        }
        String mainClass = args[0];
        String outputDir = args[1];
        String outputFileName = args.length > 2 ? args[2] : "openapi";
        String format = args.length > 3 ? args[3] : "json";
        validateFormat(format);
        new GeneratorWorkerWebFlux().generate(mainClass, outputDir, outputFileName, format);
    }

    public void generate(String mainClass, String outputDir, String outputFileName, String format) throws Exception {
        SpringApplication app = new SpringApplication(Class.forName(mainClass));
        app.setWebApplicationType(WebApplicationType.REACTIVE);
        app.addPrimarySources(java.util.List.of(NoServerConfiguration.class));
        app.setDefaultProperties(Map.of("spring.main.banner-mode", "off"));

        ConfigurableApplicationContext context = null;
        try {
            context = app.run();
            OpenApiWebfluxResource resource = context.getBean(OpenApiWebfluxResource.class);
            ServerHttpRequest request = MockServerHttpRequest.get("http://localhost/v3/api-docs").build();
            String lower = format.toLowerCase(Locale.ROOT);
            byte[] bytes;
            if (isYaml(lower)) {
                bytes = resource.openapiYaml(request, "/v3/api-docs", Locale.ENGLISH).block();
            }
            else {
                bytes = resource.openapiJson(request, "/v3/api-docs", Locale.ENGLISH).block();
            }
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("OpenAPI generation returned no content");
            }
            String ext = isYaml(lower) ? "yaml" : "json";
            Path out = Path.of(outputDir).resolve(outputFileName + "." + ext);
            WriteUtils.writeAtomic(out, bytes);
            System.out.println("Generated OpenAPI spec at " + out.toAbsolutePath());
        }
        finally {
            if (context != null) {
                context.close();
            }
        }
    }

    private static boolean isYaml(String lower) {
        return "yaml".equals(lower) || "yml".equals(lower);
    }

    /**
     * Validates a user-supplied {@code format} argument. Only {@code json}, {@code yaml},
     * {@code yml} are supported; anything else is rejected rather than silently falling back
     * to JSON output (which would produce a document in a format the user did not ask for).
     */
    private static void validateFormat(String format) {
        String lower = format.toLowerCase(Locale.ROOT);
        boolean valid = "json".equals(lower) || "yaml".equals(lower) || "yml".equals(lower);
        if (!valid) {
            throw new IllegalArgumentException(
                    "Unsupported format '" + format + "'. Supported formats: json, yaml, yml.");
        }
    }
}