package test;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample app that simulates an application leaving a non-daemon thread running after the
 * application context is closed (e.g. an un-managed Vert.x instance or a custom thread pool).
 * The generator worker must still terminate; otherwise the whole build would hang waiting for
 * the forked JVM to exit. This mirrors a real failure where a leaked, non-daemon Vert.x event
 * loop kept the OpenAPI generator worker alive until the CI job timed out.
 */
@SpringBootApplication
public class SampleApp {

    @PostConstruct
    void leakNonDaemonThread() {
        Thread t = new Thread(() -> {
            // Never finishes: keeps the JVM alive on natural exit.
            while (true) {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "leaked-non-daemon-thread");
        t.setDaemon(false);
        t.start();
    }
}