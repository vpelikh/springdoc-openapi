package org.springdoc.generator;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.http.server.reactive.HttpHandler;

/**
 * A {@link ReactiveWebServerFactory} that produces a no-op {@link WebServer}. Registering this
 * satisfies Spring Boot's reactive web auto-configuration (so {@code @ConditionalOnWebApplication}
 * still activates springdoc) while never binding any port: {@code WebServer.start()} is a no-op.
 */
final class NoOpReactiveWebServerFactory implements ReactiveWebServerFactory {

    @Override
    public WebServer getWebServer(HttpHandler httpHandler) {
        return new NoOpWebServer();
    }

    /**
     * A {@link WebServer} whose lifecycle methods do nothing, so no server ever binds a port.
     */
    private static final class NoOpWebServer implements WebServer {

        @Override
        public void start() throws WebServerException {
            // intentionally do not bind any port
        }

        @Override
        public void stop() throws WebServerException {
            // nothing to stop
        }

        @Override
        public int getPort() {
            return 0;
        }
    }
}