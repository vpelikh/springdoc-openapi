# springdoc-openapi-maven-plugin

A Maven plugin that generates the [OpenAPI](https://swagger.io/specification/) specification
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

Reuses the shared worker module (`springdoc-openapi-generator-worker`) that the Gradle plugin
also uses, so the two build systems share one implementation.

## Usage

Bind the goal to the `package` (default) phase or invoke it directly:

```xml
<plugin>
    <groupId>io.github.vpelikh</groupId>
    <artifactId>springdoc-openapi-maven-plugin</artifactId>
    <version>5.1.0-SNAPSHOT</version>
    <configuration>
        <mainClass>com.example.YourApplication</mainClass>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Then:

```
mvn package          # or mvn springdoc:generate
```

The spec is written to `${project.build.directory}/docs/openapi.json` by default.

## Goal: `generate`

| Parameter        | Property             | Default                   | Description                              |
|------------------|----------------------|---------------------------|------------------------------------------|
| `mainClass`      | `springdoc.mainClass` | *required*                | Application's `@SpringBootApplication` base class |
| `outputDir`      | `springdoc.outputDir`  | `${project.build.directory}/docs` | Directory for the generated document |
| `outputFileName` | `springdoc.outputFileName` | `openapi`               | Base file name (extension appended)       |
| `format`         | `springdoc.format`    | `json`                     | `json` or `yaml`                         |
| `timeout`        | `springdoc.timeout`   | `120`                      | Worker time bound in seconds; aborts on timeout |
| `skip`           | `springdoc.skip`      | `false`                    | Skip generation entirely (`mvn package -Dspringdoc.skip=true`) |
| `systemProperties`| `<systemProperties>` | `{}`                       | Extra `-D` props for the worker JVM     |

## How it works

1. The Mojo collects the project's runtime classpath (plus compiled output and the shared
   `springdoc-openapi-generator-worker` jar, resolved at the plugin's own version).
2. It forks a JVM running `org.springdoc.generator.GeneratorWorkerMain`.
3. The worker detects the app's stack from its classpath and dispatches to the matching
   generator:
   - WebFlux: a no-op `ReactiveWebServerFactory` keeps springdoc active with **no port bound**.
   - WebMvc: the embedded server starts on an **ephemeral port** and is shut down immediately.
4. It invokes springdoc's matching `OpenApi*Resource` with a mock request, writes the JSON/YAML
   document, and shuts the context down.

### Generating apps that need infrastructure

The worker boots the application's real context, so beans needing external resources (a database,
JMS broker, external service) must be satisfiable at generation time. Use `systemProperties` to
point generation at a test profile/overrides, e.g. for a JPA/Hibernate app:

```xml
<configuration>
    <mainClass>com.example.App</mainClass>
    <systemProperties>
        <spring.profiles.active>generation</spring.profiles.active>
        <spring.autoconfigure.exclude>
            org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
        </spring.autoconfigure.exclude>
    </systemProperties>
</configuration>
```

The `-D` properties reach the forked worker (verified: `spring.profiles.active` activates in the
worker JVM), so Spring reads annotations off the classpath without needing a real connection.

## Notes / limitations

- **WebFlux "no port bound" caveat:** the no-op `ReactiveWebServerFactory` is registered with
  `@ConditionalOnMissingBean`. If the application itself defines a `ReactiveWebServerFactory` bean,
  that one wins and a real server can bind a port at generation time. This is rare (configuring the
  server via a `WebServerFactoryCustomizer` does not define a factory bean and is unaffected). To
  guarantee no port is bound, avoid defining such a bean.
- The fork-invocation logic (build `java -cp`, stream output, time out) is intentionally kept small
  and duplicated in the Gradle task and Maven Mojo rather than extracted into a shared helper, to
  avoid coupling the plugins to the worker jar's compile classpath. The shared worker still owns
  the actual generation.
- The offline spec's `servers` entry defaults to `http://localhost` (WebMvc) / a mock URL
  (WebFlux). Set `@OpenAPIDefinition(servers = @Server(...))` or a global `OpenApiCustomizer` to
  override it for your deployment.

## Building the plugin

From the repo root (with the fork modules already installed):

```
mvn -pl springdoc-openapi-generator-worker,springdoc-openapi-maven-plugin -am install -DskipTests
```