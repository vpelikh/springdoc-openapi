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

package org.springdoc.core.converters;

import java.util.List;
import java.util.Optional;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperBuilder;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.utils.SpringDocHalJacksonModuleUtils;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.HateoasHalProvider;
import org.springdoc.core.configuration.SpringDocSealedClassModule;
/**
 * Wrapper for model converters to only register converters once
 *
 * @author bnasslahsen
 */
public class ModelConverterRegistrar {

	/**
	 * The constant LOGGER.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelConverterRegistrar.class);

	/**
	 * The constant modelConvertersInstance.
	 */
	private final ModelConverters modelConvertersInstance;

	/**
	 * Instantiates a new Model converter registrar.
	 *
	 * @param modelConverters           spring registered model converter beans which have to be registered in {@link ModelConverters} instance
	 * @param springDocConfigProperties the spring doc config properties
	 * @param halProvider               the hal provider (nullable, absent when HAL is not on the classpath)
	 */
	public ModelConverterRegistrar(List<ModelConverter> modelConverters, SpringDocConfigProperties springDocConfigProperties,
	                               HateoasHalProvider halProvider) {
		modelConvertersInstance = ModelConverters.getInstance(springDocConfigProperties.isOpenapi31());

		replaceDefaultModelResolver(springDocConfigProperties.isOpenapi31(), halProvider == null || halProvider.isHalEnabled());

		for (ModelConverter modelConverter : modelConverters) {
			Optional<ModelConverter> registeredConverterOptional = getRegisteredConverterSameAs(modelConverter);
			registeredConverterOptional.ifPresent(modelConvertersInstance::removeConverter);
			modelConvertersInstance.addConverter(modelConverter);
		}
	}

	private void replaceDefaultModelResolver(boolean openapi31, boolean configureHal) {
		List<ModelConverter> existingConverters = modelConvertersInstance.getConverters();
		for (ModelConverter converter : existingConverters) {
			if (converter instanceof ModelResolver) {
				modelConvertersInstance.removeConverter(converter);
			}
		}
		ObjectMapper baseMapper = openapi31 ? Json31.mapper() : Json.mapper();
		MapperBuilder<ObjectMapper, ?> builder = baseMapper.rebuild()
			.addModule(new SpringDocSealedClassModule());
		if (configureHal) {
			SpringDocHalJacksonModuleUtils.configureHalOnBuilder(builder);
		}
		ObjectMapper modelResolverMapper = builder.build();
		ModelResolver modelResolver = new ModelResolver(modelResolverMapper);
		if (openapi31) {
			modelResolver = modelResolver.openapi31(true);
		}
		modelConvertersInstance.addConverter(modelResolver);
	}

	/**
	 * Gets registered converter same as.
	 *
	 * @param modelConverter the model converter
	 * @return the registered converter same as
	 */
	@SuppressWarnings("unchecked")
	private Optional<ModelConverter> getRegisteredConverterSameAs(ModelConverter modelConverter) {
		try {
			List<ModelConverter> modelConverters = (List<ModelConverter>) FieldUtils.readDeclaredField(modelConvertersInstance, "converters", true);
			return modelConverters.stream()
					.filter(registeredModelConverter -> isSameConverter(registeredModelConverter, modelConverter))
					.findFirst();
		}
		catch (IllegalAccessException exception) {
			LOGGER.warn(exception.getMessage());
		}
		return Optional.empty();
	}

	/**
	 * Is same converter boolean.
	 *
	 * @param modelConverter1 the model converter 1
	 * @param modelConverter2 the model converter 2
	 * @return the boolean
	 */
	private boolean isSameConverter(ModelConverter modelConverter1, ModelConverter modelConverter2) {
		// comparing by the converter type
		Class<? extends ModelConverter> modelConverter1Class = modelConverter1.getClass();
		Class<? extends ModelConverter> modelConverter2Class = modelConverter2.getClass();
		return modelConverter1Class.getName().equals(modelConverter2Class.getName());
	}
}
