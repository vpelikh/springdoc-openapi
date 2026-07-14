package org.springdoc.core.configuration;

import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.KotlinRequiredPropertyCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for GitHub issue #54.
 *
 * <p>The {@code kotlinRequiredPropertyCustomizer} bean must only be created when
 * {@code kotlin-reflect} is on the classpath. Previously it was declared directly in
 * {@link SpringDocKotlinConfiguration} (gated only by kotlin-stdlib presence), which forced a
 * {@code NoClassDefFoundError: kotlin/reflect/full/KClasses} for pure-Java projects that have
 * kotlin-stdlib but not kotlin-reflect on the classpath.</p>
 */
class SpringDocKotlinConfigurationTest {

	private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
			.withPropertyValues("springdoc.api-docs.enabled=true")
			.withConfiguration(AutoConfigurations.of(
					WebMvcAutoConfiguration.class,
					SpringDocConfiguration.class,
					SpringDocConfigProperties.class,
					SpringDocKotlinConfiguration.class));

	@Test
	void kotlinRequiredPropertyCustomizerRegisteredWhenKotlinReflectPresent() {
		runner.run(context -> assertThat(context).hasBean("kotlinRequiredPropertyCustomizer"));
	}

	@Test
	void kotlinRequiredPropertyCustomizerNotRegisteredWhenKotlinReflectAbsent() {
		runner.withClassLoader(new FilteredClassLoader("kotlin.reflect.full.KClasses"))
				.run(context -> assertThat(context).doesNotHaveBean(KotlinRequiredPropertyCustomizer.class));
	}
}
