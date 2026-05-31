/*
 *
 *  *
 *  *  *
 *  *  *  *
 *  *  *  *  *
 *  *  *  *  *  *
 *  *  *  *  *  *  * Copyright 2019-2026 the original author or authors.
 *  *  *  *  *  *  *
 *  *  *  *  *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  *  *  *  *  * you may not use this file except in compliance with the License.
 *  *  *  *  *  *  * You may obtain a copy of the License at
 *  *  *  *  *  *  *
 *  *  *  *  *  *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *  *  *  *  *  *
 *  *  *  *  *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  *  *  *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  *  *  *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  *  *  *  *  * See the License for the specific language governing permissions and
 *  *  *  *  *  *
 *  *  *  *  *
 *  *  *  *
 *  *  *
 *  *
 */
package org.springdoc.core.customizers

import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.Schema
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import org.springdoc.core.providers.ObjectMapperProvider
import tools.jackson.databind.JavaType
import kotlin.reflect.jvm.javaField

/**
 * Marks Kotlin data class constructor properties as required in the OpenAPI
 * schema when they are non-nullable and have no default value.
 *
 * Jackson 3's KotlinModule does not override [tools.jackson.databind.introspect.AnnotationIntrospector.hasRequiredMarker],
 * so [io.swagger.v3.core.jackson.ModelResolver] cannot determine required status for Kotlin
 * properties through introspection alone. This ModelConverter post-processor fills that gap.
 *
 * @author springdoc team
 */
class KotlinRequiredPropertyCustomizer(
	private val objectMapperProvider: ObjectMapperProvider
) : ModelConverter {

	override fun resolve(
		type: AnnotatedType,
		context: ModelConverterContext,
		chain: Iterator<ModelConverter>
	): Schema<*>? {
		if (!chain.hasNext()) return null
		val resolvedSchema = chain.next().resolve(type, context, chain)

		val javaType: JavaType =
			objectMapperProvider.jsonMapper().constructType(type.type)
		if (javaType.rawClass.packageName.startsWith("java.")) {
			return resolvedSchema
		}

		val kotlinClass = try {
			javaType.rawClass.kotlin
		} catch (_: Throwable) {
			return resolvedSchema
		}

		val targetSchema = if (resolvedSchema != null && resolvedSchema.`$ref` != null) {
			context.getDefinedModels()[resolvedSchema.`$ref`.substring(Components.COMPONENTS_SCHEMAS_REF.length)]
		} else {
			resolvedSchema
		}
		if (targetSchema == null) return resolvedSchema

		// Collect property names from both top-level properties and allOf branches
		val schemaProperties = mutableSetOf<String>()
		if (targetSchema.properties != null) {
			schemaProperties.addAll(targetSchema.properties.keys)
		}
		targetSchema.allOf?.forEach { allOfSchema ->
			if (allOfSchema?.properties != null) {
				allOfSchema.properties!!.keys.let { schemaProperties.addAll(it) }
			}
		}
		if (schemaProperties.isEmpty()) return resolvedSchema

		// Build a map from constructor parameter name -> KParameter for isOptional lookup
		val constructorParams: Map<String, KParameter>? =
			kotlinClass.primaryConstructor?.parameters?.associateBy { it.name ?: "" }
				?.takeIf { it.isNotEmpty() }

		val currentRequired: MutableSet<String> =
			targetSchema.required?.toMutableSet() ?: mutableSetOf()
		var changed = false

		for (prop in kotlinClass.memberProperties) {
			val fieldName = prop.name

			// Match schema property name, handling inline value class mangling
			// (e.g., singleId -> singleId-8-RKGlk)
			val schemaFieldName = if (fieldName in schemaProperties) {
				fieldName
			} else {
				schemaProperties.firstOrNull { it.startsWith("$fieldName-") }
			}
			if (schemaFieldName == null) continue

			// Skip if already in required (handled by ModelResolver from @Schema(required=true))
			if (schemaFieldName in currentRequired) continue

			// Nullable properties are never required
			if (prop.returnType.isMarkedNullable) continue

			// Check @Schema annotation for explicit NOT_REQUIRED or defaultValue.
			// @Schema() (no explicit required, no defaultValue) should NOT prevent nullability-based detection.
			val schemaAnnotation = prop.javaField?.getAnnotation(io.swagger.v3.oas.annotations.media.Schema::class.java)
			if (schemaAnnotation != null) {
				if (schemaAnnotation.requiredMode == io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED) continue
				// Properties with a Schema-level defaultValue are documented as optional
				val defaultVal = schemaAnnotation.defaultValue
				if (defaultVal != null && defaultVal.isNotEmpty()) continue
			}
			// Skip properties with default values (optional constructor params)
			val param = constructorParams?.get(fieldName)
			if (param != null && param.isOptional) continue

			currentRequired.add(schemaFieldName)
			changed = true
		}

		if (changed) {
			targetSchema.required = currentRequired.toList()
		}

		return resolvedSchema
	}
}
