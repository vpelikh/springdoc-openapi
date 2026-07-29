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

package org.springdoc.core.customizers

import tools.jackson.databind.JavaType
import io.swagger.v3.core.util.KotlinDetector
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.providers.ObjectMapperProvider
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Kotlin Deprecated PropertyCustomizer to handle Kotlin Deprecated annotations.
 * @author bnasslahsen
 */
class KotlinDeprecatedPropertyCustomizer(
	private val objectMapperProvider: ObjectMapperProvider
) : ModelConverter {

	override fun resolve(
		type: AnnotatedType,
		context: ModelConverterContext,
		chain: Iterator<ModelConverter>
	): Schema<*>? {
		if (!chain.hasNext()) return null
		// Resolve the next model in the chain
		val resolvedSchema = chain.next().resolve(type, context, chain)

		val javaType: JavaType =
			objectMapperProvider.jsonMapper().constructType(type.type)

		// Not a Kotlin-compiled class - skip to avoid marking Java DTO properties as required
		if (!KotlinDetector.isKotlinClass(javaType.rawClass)) {
			return resolvedSchema
		}

		val kotlinClass = try {
			javaType.rawClass.kotlin
		} catch (_: Throwable) {
			return resolvedSchema
		}

		// Resolve target schema: for $ref look up the actual model, otherwise use the inline schema
		val targetSchema = if (resolvedSchema != null && resolvedSchema.`$ref` != null) {
			context.getDefinedModels()[resolvedSchema.`$ref`.substring(Components.COMPONENTS_SCHEMAS_REF.length)]
		} else {
			resolvedSchema
		}

		// Check each property of the class
		for (prop in kotlinClass.memberProperties) {
			val deprecatedAnnotation = prop.findAnnotation<Deprecated>()
			if (deprecatedAnnotation != null) {
				val fieldName = prop.name
				targetSchema?.properties?.get(fieldName)?.deprecated = true
				if (deprecatedAnnotation.message.isNotBlank()) {
					val currentDesc = targetSchema?.properties?.get(fieldName)?.description
					if (currentDesc.isNullOrBlank()) {
						targetSchema?.properties?.get(fieldName)?.description = deprecatedAnnotation.message
					}
				}
			}
		}
		return resolvedSchema
	}
}
