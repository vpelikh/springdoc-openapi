package org.springdoc.generator;

/**
 * Entry point for the generator worker, run in a forked JVM by the Gradle and Maven plugins.
 * <p>
 * It detects the target application's web stack from the fork classpath (the app's own
 * dependencies are on it) and delegates to the matching worker:
 * <ul>
 *   <li>WebMvc (servlet)</li>
 *   <li>WebFlux (reactive)</li>
 * </ul>
 * The two worker classes are only loaded when selected. Because the JVM resolves constant-pool
 * references lazily, the non-selected worker is never loaded on a fork that lacks that stack, so
 * this works on WebFlux-only and WebMvc-only classpaths alike.
 * <p>
 * When both stacks are on the classpath (a mixed application), WebMvc (servlet) wins, matching
 * {@code SpringApplication}'s {@code WebApplicationType.deduceFromClasspath}.
 * <p>
 * Arguments: {@code <mainClass> <outputDir> [outputFileName] [format]}
 */
public final class GeneratorWorkerMain {

    private GeneratorWorkerMain() {
    }

    public static void main(String[] args) {
        int exitCode = 0;
        try {
            run(args);
        }
        catch (Throwable t) {
            // Print to stderr so the failure is visible even if the app reconfigured logging.
            t.printStackTrace(System.err);
            exitCode = 1;
        }
        // The worker is a one-shot forked JVM. The booted application may leave non-daemon
        // threads running (e.g. a Vert.x instance or thread pool that is not shut down when the
        // application context closes), which would otherwise keep this JVM alive indefinitely
        // after the spec has been written. The parent build waits for the process to exit (and
        // for its stdout to reach EOF), so exit explicitly instead of relying on natural exit.
        System.exit(exitCode);
    }

    private static void run(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException(
                    "Usage: GeneratorWorkerMain <mainClass> <outputDir> [outputFileName] [format]");
        }
        // Spring Boot prefers servlet when both stacks are present, so check WebMvc first.
        if (isOnClasspath("org.springframework.web.servlet.DispatcherServlet")) {
            GeneratorWorkerWebMvc.main(args);
        }
        else if (isOnClasspath("org.springframework.web.reactive.DispatcherHandler")) {
            GeneratorWorkerWebFlux.main(args);
        }
        else {
            throw new IllegalStateException(
                    "Could not detect a WebMvc or WebFlux stack on the application classpath.");
        }
    }

    private static boolean isOnClasspath(String className) {
        try {
            Class.forName(className, false, GeneratorWorkerMain.class.getClassLoader());
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }
}