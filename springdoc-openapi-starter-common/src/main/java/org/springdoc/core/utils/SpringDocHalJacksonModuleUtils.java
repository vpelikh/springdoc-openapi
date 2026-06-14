/*
 *
 *  *
 *  *  *
 *  *  *  *
 *  *  *  *  *
 *  *  *  *  *  * Copyright 2019-2026 the original author or authors.
 *  *  *  *  *  *
 *  *  *  *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  *  *  *  * you may not use this file except in compliance with the License.
 *  *  *  *  *  * You may obtain a copy of the License at
 *  *  *  *  *  *
 *  *  *  *  *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *  *  *  *  *
 *  *  *  *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  *  *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  *  *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  *  *  *  * See the License for the specific language governing permissions and
 *  *  *  *  *  * limitations under the License.
 *  *  *  *  *
 *  *  *  *
 *  *  *
 *  *
 *
 */

package org.springdoc.core.utils;

import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.core.converters.HalPropertyNamingIntrospector;

/**
 * Utility for configuring {@code HalJacksonModule} on Jackson {@code ObjectMapper} instances.
 * <p>
 * Provides:
 * <ul>
 *   <li>{@link #configureHalOnBuilder(MapperBuilder)} — adds the HAL module and a custom
 *       {@code HalPropertyNamingIntrospector} to a {@code MapperBuilder}.</li>
 *   <li>{@link #ensureHalModuleRegistered(ObjectMapperProvider)} — idempotent helper that
 *       rebuilds an {@code ObjectMapperProvider}'s mapper with the HAL module if not already present.</li>
 *   <li>{@link #registerHalDirectMixins(MapperBuilder)} — registers HAL mixin classes directly
 *       on the builder for reliable annotation introspection.</li>
 * </ul>
 * Uses {@code Class.forName} instead of direct imports to avoid
 * {@code NoClassDefFoundError} when {@code spring-boot-hateoas} is optional.
 */
public final class SpringDocHalJacksonModuleUtils {

	private static final String HAL_MODULE_CLASS_NAME = "org.springframework.hateoas.mediatype.hal.HalJacksonModule";

	private static final boolean HAL_MODULE_AVAILABLE;

	static {
		boolean available = false;
		try {
			Class.forName(HAL_MODULE_CLASS_NAME);
			available = true;
		}
		catch (ClassNotFoundException e) {
			// HalJacksonModule not on classpath
		}
		HAL_MODULE_AVAILABLE = available;
	}

	private SpringDocHalJacksonModuleUtils() {
	}

	private static boolean isHalModuleRegistered(ObjectMapper mapper) {
		if (!HAL_MODULE_AVAILABLE) {
            return false;
        }
		for (JacksonModule module : mapper.registeredModules()) {
			if (HAL_MODULE_CLASS_NAME.equals(module.getClass().getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Configures a {@code MapperBuilder} with the HalJacksonModule and a custom
	 * {@code HalPropertyNamingIntrospector}.
	 * <p>
	 * If the module is not on the classpath, only the introspector is applied.
	 * Otherwise, the module is instantiated via reflection, registered, and HAL
	 * mixin classes are added directly on the builder.
	 *
	 * @param builder the MapperBuilder to configure
	 * @return the configured builder, for chaining
	 */
	public static MapperBuilder<ObjectMapper, ?> configureHalOnBuilder(MapperBuilder<ObjectMapper, ?> builder) {
		try {
			Class<?> halModuleClass = Class.forName(HAL_MODULE_CLASS_NAME);
			JacksonModule halModule = (JacksonModule) halModuleClass.getDeclaredConstructor().newInstance();
			builder = builder.addModule(halModule);
			registerHalDirectMixins(builder);
        } catch (Exception e) {
			// HalJacksonModule not on classpath — proceed without
		}
		builder.annotationIntrospector(
			AnnotationIntrospector.pair(new HalPropertyNamingIntrospector(), new JacksonAnnotationIntrospector()));
		return builder;
	}

	/**
	 * Ensures the HalJacksonModule is registered in the mapper from the given provider.
	 * <p>
	 * If the module is not on the classpath or is already registered, does nothing.
	 * Otherwise, rebuilds the mapper with the module added and sets it on the provider.
	 *
	 * @param provider the ObjectMapperProvider whose mapper should be updated
	 */
	public static void ensureHalModuleRegistered(ObjectMapperProvider provider) {
		ObjectMapper mapper = provider.jsonMapper();
		if (!HAL_MODULE_AVAILABLE || isHalModuleRegistered(mapper)) {
			return;
		}
		MapperBuilder<ObjectMapper, ?> builder = mapper.rebuild();
		configureHalOnBuilder(builder);
		provider.setJsonMapper(builder.build());
	}

	/**
	 * Registers HalJacksonModule's mixin classes directly on the MapperBuilder.
	 * <p>
	 * This supplements the HalJacksonModule registration via addModule() to ensure mixin
	 * annotations (@JsonProperty("_embedded"), @JsonProperty("_links")) are reliably
	 * available to Jackson 3's annotation introspection.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void registerHalDirectMixins(MapperBuilder<ObjectMapper, ?> builder) {
		try {
			Class<?> collectionModelClass = Class.forName("org.springframework.hateoas.CollectionModel");
			Class<?> representationModelClass = Class.forName("org.springframework.hateoas.RepresentationModel");
			Class<?> collectionModelMixinClass = Class.forName("org.springframework.hateoas.mediatype.hal.CollectionModelMixin");
			Class<?> representationModelMixinClass = Class.forName("org.springframework.hateoas.mediatype.hal.RepresentationModelMixin");
			builder.addMixIn(collectionModelClass, collectionModelMixinClass);
			builder.addMixIn(representationModelClass, representationModelMixinClass);
		}
		catch (ClassNotFoundException e) {
			// HalJacksonModule mixins not on classpath — proceed without
		}
	}
}
