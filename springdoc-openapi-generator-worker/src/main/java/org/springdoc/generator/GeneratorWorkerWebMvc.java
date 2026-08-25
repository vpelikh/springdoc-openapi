package org.springdoc.generator;

import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Worker that boots a <b>WebMvc (servlet)</b> Spring Boot application, lets springdoc-openapi
 * build the OpenAPI document, write it to disk, and shut the context down. Runs in a forked JVM.
 * <p>
 * Unlike WebFlux, the servlet model requires a real servlet container (the DispatcherServlet must
 * be able to register in it). To honor "generate without starting the serial server" as closely as
 * the servlet model allows, the embedded container is bound to an ephemeral port (0) and the
 * context is shut down immediately after generation, so no port is exposed and nothing stays
 * listening.
 */
public class GeneratorWorkerWebMvc {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException(
                    "Usage: GeneratorWorkerWebMvc <mainClass> <outputDir> [outputFileName] [format]");
        }
        String mainClass = args[0];
        String outputDir = args[1];
        String outputFileName = args.length > 2 ? args[2] : "openapi";
        String format = args.length > 3 ? args[3] : "json";
        validateFormat(format);
        new GeneratorWorkerWebMvc().generate(mainClass, outputDir, outputFileName, format);
    }

    public void generate(String mainClass, String outputDir, String outputFileName, String format) throws Exception {
        SpringApplication app = new SpringApplication(Class.forName(mainClass));
        app.setWebApplicationType(WebApplicationType.SERVLET);
        // Bind an ephemeral port; the context stops immediately after generation.
        app.setDefaultProperties(Map.of(
                "server.port", "0",
                "spring.main.banner-mode", "off"));

        ConfigurableApplicationContext context = null;
        try {
            context = app.run();
            OpenApiWebMvcResource resource = context.getBean(OpenApiWebMvcResource.class);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs");
            request.setScheme("http");
            request.setServerName("localhost");
            request.setServerPort(80);
            String lower = format.toLowerCase(Locale.ROOT);
            byte[] bytes;
            if (isYaml(lower)) {
                bytes = resource.openapiYaml(request, "/v3/api-docs", Locale.ENGLISH);
            }
            else {
                bytes = resource.openapiJson(request, "/v3/api-docs", Locale.ENGLISH);
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