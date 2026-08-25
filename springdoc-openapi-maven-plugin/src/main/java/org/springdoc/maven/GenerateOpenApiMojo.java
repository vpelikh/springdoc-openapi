package org.springdoc.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

/**
 * Maven goal that generates the OpenAPI specification for a Spring Boot application without
 * leaving a web server running. It forks a dedicated JVM (using the project's runtime
 * classpath) that boots the application's reactive web context on an ephemeral port, lets
 * springdoc-openapi build the document, writes it to disk, and shuts the context down.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateOpenApiMojo extends AbstractMojo {

	/**
	 * The Maven project.
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * The Maven session.
	 */
	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

	/**
	 * Aether repository system for resolving the worker artifact.
	 */
	@Component
	private RepositorySystem repositorySystem;

	/**
	 * Fully-qualified name of the application's {@code @SpringBootApplication} class.
	 */
	@Parameter(property = "springdoc.mainClass", required = true)
	private String mainClass;

	/**
	 * Directory for the generated document. Defaults to {@code ${project.build.directory}/docs}.
	 */
	@Parameter(defaultValue = "${project.build.directory}/docs", property = "springdoc.outputDir")
	private File outputDir;

	/**
	 * Base file name of the generated document (without extension).
	 */
	@Parameter(defaultValue = "openapi", property = "springdoc.outputFileName")
	private String outputFileName;

	/**
	 * Output format: {@code json} or {@code yaml}.
	 */
	@Parameter(defaultValue = "json", property = "springdoc.format")
	private String format;

	/**
	 * The plugin's own version, used to resolve the matching generator-worker artifact.
	 */
	@Parameter(defaultValue = "${plugin.version}", readonly = true, required = true)
	private String pluginVersion;

	/**
	 * Upper bound (seconds) for the forked worker. Defaults to 120.
	 */
	@Parameter(defaultValue = "120", property = "springdoc.timeout")
	private int timeout;

	/**
	 * Skip generation entirely. Useful to disable the bound goal without removing the plugin,
	 * e.g. {@code mvn package -Dspringdoc.skip=true}.
	 */
	@Parameter(defaultValue = "false", property = "springdoc.skip")
	private boolean skip;

	/**
	 * Additional system properties ({@code -D}) passed to the forked worker JVM, e.g.
	 * {@code spring.profiles.active=generation} or {@code spring.autoconfigure.exclude=...}.
	 */
	@Parameter
	private Map<String, String> systemProperties;

	@Override
	public void execute() throws MojoExecutionException {
		if (skip) {
			getLog().info("Springdoc: generation skipped (springdoc.skip=true)");
			return;
		}
		if (mainClass == null || mainClass.isBlank()) {
			throw new MojoExecutionException("springdoc.mainClass must be set to the application's @SpringBootApplication class");
		}
		if (!outputDir.exists() && !outputDir.mkdirs()) {
			throw new MojoExecutionException("Could not create output dir " + outputDir);
		}

		List<String> classpath = new ArrayList<>();
		try {
			classpath.add(project.getBuild().getOutputDirectory());
			classpath.addAll(project.getRuntimeClasspathElements());
		}
		catch (DependencyResolutionRequiredException e) {
			throw new MojoExecutionException("Could not resolve runtime classpath", e);
		}

		// The fork uses the thin shared generator-worker jar (boots the app, exposes
		// GeneratorWorkerMain) plus its transitive runtime dependencies (spring-test for the
		// mock request). Resolved at the plugin's own version.
		List<String> workerJars = resolveWorkerClasspath();
		classpath.addAll(workerJars);

		// keep order & dedupe
		Set<String> dedup = new LinkedHashSet<>(classpath);
		String cp = String.join(File.pathSeparator, dedup);

		String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		List<String> command = new ArrayList<>();
		command.add(javaBin);
		// Pass through user-supplied system properties (e.g. spring.profiles.active,
		// spring.autoconfigure.exclude) so infra-dependent apps can be generated against
		// a generation-time profile/overrides.
		if (systemProperties != null) {
			systemProperties.forEach((k, v) -> command.add("-D" + k + "=" + v));
		}
		command.add("-cp");
		command.add(cp);
		command.add("org.springdoc.generator.GeneratorWorkerMain");
		command.add(mainClass);
		command.add(outputDir.getAbsolutePath());
		command.add(outputFileName);
		command.add(format);

		getLog().info("Springdoc: generating OpenAPI spec for main class " + mainClass);
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(true);
		Process process = null;
		try {
			process = pb.start();
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					getLog().info(line);
				}
			}
			boolean finished = process.waitFor(timeout, java.util.concurrent.TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				throw new MojoExecutionException("Springdoc generator worker did not finish within "
						+ timeout + "s and was terminated");
			}
			int exit = process.exitValue();
			if (exit != 0) {
				throw new MojoExecutionException("Springdoc generator worker exited with code " + exit);
			}
		}
		catch (IOException e) {
			if (process != null) {
				process.destroyForcibly();
			}
			throw new MojoExecutionException("Failed to launch springdoc generator worker", e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			if (process != null) {
				process.destroyForcibly();
			}
			throw new MojoExecutionException("Springdoc generator worker interrupted", e);
		}
	}

	/**
	 * Resolves the {@code springdoc-openapi-generator-worker} artifact and its transitive runtime
	 * dependencies from the build session's repositories (or the reactor). Fails the build if it
	 * cannot be resolved, since generation is impossible without it.
	 *
	 * @return absolute paths to the worker jar and its runtime dependencies
	 * @throws MojoExecutionException if resolution fails
	 */
	private List<String> resolveWorkerClasspath() throws MojoExecutionException {
		try {
			Dependency root = new Dependency(
					new DefaultArtifact("io.github.vpelikh",
							"springdoc-openapi-generator-worker", "jar", pluginVersion),
					"runtime");
			java.util.List<RemoteRepository> repos =
					session.getCurrentProject().getRemoteProjectRepositories();
			CollectRequest collect = new CollectRequest(root, repos);
			DependencyRequest request = new DependencyRequest(collect, null);
			DependencyResult result = repositorySystem.resolveDependencies(session.getRepositorySession(), request);
			List<String> paths = new ArrayList<>();
			for (ArtifactResult artifact : result.getArtifactResults()) {
				paths.add(artifact.getArtifact().getFile().getAbsolutePath());
			}
			if (paths.isEmpty()) {
				throw new MojoExecutionException("Could not resolve springdoc generator worker at "
						+ pluginVersion);
			}
			return paths;
		}
		catch (DependencyResolutionException e) {
			throw new MojoExecutionException(
					"Could not resolve springdoc-openapi-generator-worker:" + pluginVersion
							+ ". Ensure the artifact is installed (e.g. via the springdoc Maven build) "
							+ "or available in the configured repositories.",
					e);
		}
	}
}