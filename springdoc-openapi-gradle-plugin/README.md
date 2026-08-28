# springdoc-openapi-gradle-plugin

A Gradle plugin that generates the [OpenAPI](https://swagger.io/specification/) specification
for a Spring Boot application without leaving a web server running. It forks a dedicated JVM and
supports both stacks with no per-stack config; the worker detects the target app's stack from its
classpath and dispatches to the matching generator.

## Important: WebFlux vs WebMvc

> **WebFlux (reactive) — genuinely serverless.** A no-op `ReactiveWebServerFactory` keeps springdoc
> active and **no port is ever bound**. This fully honors the "generate without starting the app"
> goal.
>
> **WebMvc (servlet) — starts a real, ephemeral server.** The servlet model requires an actual
> container (the `DispatcherServlet` must register in a `ServletContext`), so generation boots the
> embedded server on an **ephemeral port (0)** and shuts the context down immediately after writing
> the spec. No fixed/exposed port is used and nothing stays listening, but a real container does
> briefly start. This is a servlet-model constraint, not a plugin choice.

## Status

Feature/experimental. Open for review on branch `feature/openapi-gradle-plugin`.

## Project layout

```
springdoc-openapi-gradle-plugin/        (this Gradle build, the plugin)
  src/main/java/org/springdoc/gradle/   plugin, extension, task
  src/test/...                          Gradle TestKit functional test + sample app
```

The actual JVM worker (`springdoc-openapi-generator-worker`, which boots the app and writes the
spec) lives in a sibling Maven module shared with the Maven plugin, so both build systems use a
single implementation rather than duplicating it.

## Usage

Apply the plugin to the Gradle project containing your Spring Boot application and set the main
class:

```groovy
plugins {
    id 'java'
    id 'io.github.vpelikh.springdoc-openapi-gradle-plugin' version '5.1.0-SNAPSHOT'
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webflux:4.1.1'
    implementation 'io.github.vpelikh:springdoc-openapi-starter-webflux-api:5.1.0-SNAPSHOT'
}

openApiGenerate {
    mainClass = 'com.example.YourApplication'
}
```

Then:

```
./gradlew generateOpenApi
```

The spec is written to `build/docs/openapi.json` by default.

## Extension options (`openApiGenerate { ... }`)

| Property       | Type     | Default          | Description                                   |
|----------------|----------|------------------|-----------------------------------------------|
| `mainClass`    | `String` | *required*       | `@SpringBootApplication` class to boot          |
| `outputDir`    | `File`   | `build/docs`     | Directory for the generated document             |
| `outputFileName`| `String` | `openapi`        | Base file name (`.json` / `.yaml` appended)     |
| `format`       | `String` | `json`           | `json` or `yaml`                               |
| `timeoutSeconds`| `int`   | `120`            | Worker time bound; aborts the fork on timeout  |
| `skip`          | `boolean`| `false`         | Skip `generateOpenApi` entirely                 |
| `systemProperties`| `Map<String,String>` | `{}`   | Extra `-D` props for the worker JVM |

## How it works

The plugin:

1. Resolves the application's `runtimeClasspath` plus a small `springdocGenerator` configuration
   carrying only the fork's worker JAR and its transitive runtime deps. The relevant springdoc
   stack API (`webflux-api` or `webmvc-api`) comes from the application's own dependencies, so the
   fork classpath matches the Maven Mojo's and never forces a stack onto the process.
2. Forks a JVM (`java -cp <appClasspath> org.springdoc.gradle.GeneratorWorkerMain ...`).
3. Inside that JVM the worker boots the application with `WebApplicationType.REACTIVE` and
   registers a **no-op `ReactiveWebServerFactory`** (a `WebServer` whose `start()` does nothing),
   so the reactive web context exists for springdoc but **no port is ever bound**.
4. It invokes springdoc's existing `OpenApiWebfluxResource` (with a mock request) to produce the
   JSON/YAML document, writes it to the configured output, and closes the context.
5. The plugin task is `@Cacheable`, so unchanged inputs are up-to-date.

## Building & testing

From this directory:

```
./gradlew test    # TestKit functional test
```

The functional test boots the bundled sample reactive app and asserts the generated document
contains the `/pets` paths.

## Notes / limitations

- Uses the fork's modules (`io.github.vpelikh:springdoc-openapi-starter-webflux-api` /
  `springdoc-openapi-starter-webmvc-api`) at `5.1.0-SNAPSHOT`; install them and the shared
  `springdoc-openapi-generator-worker` into `~/.m2` (via the root Maven build) first.
- Supports both WebFlux (reactive, fully serverless) and WebMvc (servlet, ephemeral auto-stopped
  embedded server). The stack is detected automatically from the app's classpath.
- **WebFlux "no port bound" caveat:** the no-op `ReactiveWebServerFactory` is registered with
  `@ConditionalOnMissingBean`. If the application itself defines a `ReactiveWebServerFactory` bean,
  that one wins and a real server can bind a port at generation time. This is rare (configuring the
  server via a `WebServerFactoryCustomizer` does not define a factory bean and is unaffected). To
  guarantee no port is bound, avoid defining such a bean or use `@OpenAPIDefinition(...)` to control
  the generated spec instead.
- The shared worker declares `spring-test` as a runtime dependency solely to build mock
  `ServerHttpRequest` / `HttpServletRequest`; it is pulled onto the fork classpath but never
  bundled into the worker jar.
- The `generateOpenApi` task is `@Cacheable`; its `javaExecutable` input is absolute (toolchain
  path), so remote build-cache hits are machine-specific.
- The fork-invocation logic (build `java -cp`, stream output, time out) is intentionally kept small
  and duplicated in the Gradle task and Maven Mojo rather than extracted into a shared helper, to
  avoid coupling the plugins to the worker jar's compile classpath. The shared worker still owns
  the actual generation.
- The offline spec's `servers` entry defaults to `http://localhost` (WebMvc) / a mock URL
  (WebFlux). Set `@OpenAPIDefinition(servers = @Server(...))` or a global `OpenApiCustomizer` to
  override it for your deployment.

### Generating apps that need infrastructure

The worker boots the application's real context, so beans that need external resources (a
database, JMS broker, external service) must be satisfiable at generation time. For example, a
JPA/Hibernate app with no reachable database will fail to refresh.

Use `systemProperties` to point generation at a test profile/overrides, exactly like a test:

```groovy
openApiGenerate {
    mainClass = 'com.example.App'
    systemProperties = [
        'spring.profiles.active': 'generation',
        'spring.autoconfigure.exclude':
            'org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration'
    ]
}
```

Spring Boot still reads `@Entity`/JPA annotations off the classpath, so the OpenAPI spec is
correct while no real database connection is needed.