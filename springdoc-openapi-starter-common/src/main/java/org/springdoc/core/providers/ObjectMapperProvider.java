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

package org.springdoc.core.providers;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.ObjectMapperFactory;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.configuration.SpringDocSealedClassModule;
import org.springdoc.core.mixins.SortedOpenAPIMixin;
import org.springdoc.core.mixins.SortedOpenAPIMixin31;
import org.springdoc.core.mixins.SortedSchemaMixin;
import org.springdoc.core.mixins.SortedSchemaMixin31;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SpringDocConfigProperties.ApiDocs.OpenApiVersion;

/**
 * The type Spring doc object mapper provider.
 */
public class ObjectMapperProvider extends ObjectMapperFactory {

	/**
	 * The Json mapper.
	 */
	private ObjectMapper jsonMapper;

	/**
	 * The Yaml mapper.
	 */
	private ObjectMapper yamlMapper;

	/**
	 * The Spring doc config properties.
	 */
	private final SpringDocConfigProperties springDocConfigProperties;

	/**
	 * Instantiates a new Spring doc object mapper.
	 *
	 * @param springDocConfigProperties the spring doc config properties
	 */
	public ObjectMapperProvider(SpringDocConfigProperties springDocConfigProperties) {
		this.springDocConfigProperties = springDocConfigProperties;
		OpenApiVersion openApiVersion = springDocConfigProperties.getApiDocs().getVersion();

		ObjectMapper baseJsonMapper;
		ObjectMapper baseYamlMapper;

		if (openApiVersion == OpenApiVersion.OPENAPI_3_1) {
			baseJsonMapper = Json31.mapper();
			baseYamlMapper = Yaml31.mapper();
			if (springDocConfigProperties.isUseArbitrarySchemas()) {
				System.setProperty(Schema.USE_ARBITRARY_SCHEMA_PROPERTY, "true");
			}
			if (springDocConfigProperties.isExplicitObjectSchema()) {
				System.setProperty(Schema.EXPLICIT_OBJECT_SCHEMA_PROPERTY, "true");
			}
			else {
				PrimitiveType.explicitObjectType = false;
			}
		}
		else {
			baseJsonMapper = Json.mapper();
			baseYamlMapper = Yaml.mapper();
			PrimitiveType.explicitObjectType = null;
		}

		jsonMapper = baseJsonMapper.rebuild()
				.addModule(new SpringDocSealedClassModule())
				.build();

		yamlMapper = baseYamlMapper.rebuild()
				.addModule(new SpringDocSealedClassModule())
				.build();
	}

	/**
	 * Create json object mapper.
	 *
	 * @param springDocConfigProperties the spring doc config properties
	 * @return the object mapper
	 */
	public static ObjectMapper createJson(SpringDocConfigProperties springDocConfigProperties) {
		OpenApiVersion openApiVersion = springDocConfigProperties.getApiDocs().getVersion();
		ObjectMapper objectMapper;
		if (openApiVersion == OpenApiVersion.OPENAPI_3_1)
			objectMapper = ObjectMapperFactory.createJson31();
		else
			objectMapper = ObjectMapperFactory.createJson();

		if (springDocConfigProperties.isWriterWithOrderByKeys())
			objectMapper = sortOutput(objectMapper, springDocConfigProperties);

		return objectMapper;
	}

	/**
	 * Sort output.
	 *
	 * @param objectMapper              the object mapper
	 * @param springDocConfigProperties the spring doc config properties
	 */
	public static ObjectMapper sortOutput(ObjectMapper objectMapper, SpringDocConfigProperties springDocConfigProperties) {
		MapperBuilder<ObjectMapper, ?> mapperBuilder = objectMapper
				.rebuild()
				.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
		if (OpenApiVersion.OPENAPI_3_1 == springDocConfigProperties.getApiDocs().getVersion()) {
			mapperBuilder.addMixIn(OpenAPI.class, SortedOpenAPIMixin31.class);
			mapperBuilder.addMixIn(Schema.class, SortedSchemaMixin31.class);
		}
		else {
			mapperBuilder.addMixIn(OpenAPI.class, SortedOpenAPIMixin.class);
			mapperBuilder.addMixIn(Schema.class, SortedSchemaMixin.class);
		}
		return mapperBuilder.build();
	}

	/**
	 * Mapper object mapper.
	 *
	 * @return the object mapper
	 */
	public ObjectMapper jsonMapper() {
		return jsonMapper;
	}

	public void setJsonMapper(ObjectMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	/**
	 * Yaml mapper object mapper.
	 *
	 * @return the object mapper
	 */
	public ObjectMapper yamlMapper() {
		return yamlMapper;
	}

	public void setYamlMapper(ObjectMapper yamlMapper) {
		this.yamlMapper = yamlMapper;
	}

	/**
	 * Is openapi 31 boolean.
	 *
	 * @return the boolean
	 */
	public boolean isOpenapi31() {
		return springDocConfigProperties.isOpenapi31();
	}
}
