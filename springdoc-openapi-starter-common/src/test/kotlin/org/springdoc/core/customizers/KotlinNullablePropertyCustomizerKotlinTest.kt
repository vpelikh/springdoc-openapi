/*
 *
 *  *
 *  *  *
 *  *  *  *
 *  *  *  *  *
 *  *  *  *  *  *
 *  *  *  *  *  *  *
 *  *  *  *  *  *  *  * Copyright 2019-2026 the original author or authors.
 *  *  *  *  *  *  *  *
 *  *  *  *  *  *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  *  *  *  *  *  * you may not use this file except in compliance with the License.
 *  *  *  *  *  *  *  * You may obtain a copy of the License at
 *  *  *  *  *  *  *  *
 *  *  *  *  *  *  *  *      https://www.apache.org/licenses/LICENSE-2.0
 *  *  *  *  *  *  *  *
 *  *  *  *  *  *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  *  *  *  *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  *  *  *  *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  *  *  *  *  *  * See the License for the specific language governing permissions and
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
import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.KotlinDetector
import io.swagger.v3.oas.models.media.Schema
import org.junit.jupiter.api.Test
import org.springdoc.core.providers.ObjectMapperProvider
import java.util.List
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.ArgumentMatchers.any

/**
 * Verifies that [KotlinNullablePropertyCustomizer] correctly processes
 * Kotlin data classes via [io.swagger.v3.core.util.KotlinDetector.isKotlinClass].
 */
class KotlinNullablePropertyCustomizerKotlinTest {

    private val mapperProvider: ObjectMapperProvider = mock(ObjectMapperProvider::class.java)
    private val customizer = KotlinNullablePropertyCustomizer(mapperProvider)

    init {
        `when`(mapperProvider.jsonMapper()).thenReturn(Json.mapper())
    }

    @Test
    fun `should mark nullable field as nullable`() {
        val type = AnnotatedType().`type`(KotlinTestFixture::class.java)

        val unresolved = Schema<Any>().apply {
            name = "kotlinTestFixture"
            addProperty("nonNullField", io.swagger.v3.oas.models.media.StringSchema())
            addProperty("nullableField", io.swagger.v3.oas.models.media.StringSchema())
        }

        val mockConverter: ModelConverter = mock(ModelConverter::class.java)
        `when`(mockConverter.resolve(any(), any(), any())).thenReturn(unresolved)

        val chain = List.of(mockConverter).iterator()
        val context: ModelConverterContext = mock(ModelConverterContext::class.java)

        val result = customizer.resolve(type, context, chain)

        assertThat(result).isSameAs(unresolved)
        assertThat(result!!.properties!!["nullableField"]?.getNullable()).isTrue()
    }

    @Test
    fun `should not mark non-nullable field as nullable`() {
        val type = AnnotatedType().`type`(KotlinTestFixture::class.java)

        val unresolved = Schema<Any>().apply {
            name = "kotlinTestFixture"
            addProperty("nonNullField", io.swagger.v3.oas.models.media.StringSchema())
            addProperty("nullableField", io.swagger.v3.oas.models.media.StringSchema())
        }

        val mockConverter: ModelConverter = mock(ModelConverter::class.java)
        `when`(mockConverter.resolve(any(), any(), any())).thenReturn(unresolved)

        val chain = List.of(mockConverter).iterator()
        val context: ModelConverterContext = mock(ModelConverterContext::class.java)

        val result = customizer.resolve(type, context, chain)

        assertThat(result).isSameAs(unresolved)
        assertThat(result!!.properties!!["nonNullField"]?.getNullable()).isNull()
    }

    @Test
    fun `should not mark field with default value as nullable`() {
        val type = AnnotatedType().`type`(KotlinTestFixture::class.java)

        val unresolved = Schema<Any>().apply {
            name = "kotlinTestFixture"
            addProperty("fieldWithDefault", io.swagger.v3.oas.models.media.StringSchema())
        }

        val mockConverter: ModelConverter = mock(ModelConverter::class.java)
        `when`(mockConverter.resolve(any(), any(), any())).thenReturn(unresolved)

        val chain = List.of(mockConverter).iterator()
        val context: ModelConverterContext = mock(ModelConverterContext::class.java)

        val result = customizer.resolve(type, context, chain)

        assertThat(result).isSameAs(unresolved)
        assertThat(result!!.properties!!["fieldWithDefault"]?.getNullable()).isNull()
    }

    @Test
    fun kotlinDetectorAcceptsFixture() {
        assertThat(KotlinDetector.isKotlinClass(KotlinTestFixture::class.java)).isTrue()
    }
}
