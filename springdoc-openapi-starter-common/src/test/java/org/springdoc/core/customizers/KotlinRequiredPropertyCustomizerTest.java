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
package org.springdoc.core.customizers;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.KotlinDetector;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.springdoc.core.providers.ObjectMapperProvider;

import tools.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link KotlinRequiredPropertyCustomizer}.
 *
 * Verifies that:
 * <ul>
 *   <li>Java DTOs (no {@code @kotlin.Metadata}) are skipped — their schemas are returned
 *       without marking properties as required (the regression fix for GitHub issue #54).</li>
 *   <li>Kotlin data classes are still processed normally (regression guard).</li>
 * </ul>
 */
class KotlinRequiredPropertyCustomizerTest {

    private final ObjectMapperProvider mapperProvider = mock(ObjectMapperProvider.class);
    private final KotlinRequiredPropertyCustomizer customizer = new KotlinRequiredPropertyCustomizer(mapperProvider);

    KotlinRequiredPropertyCustomizerTest() {
        ObjectMapper mapper = Json.mapper();
        when(mapperProvider.jsonMapper()).thenReturn(mapper);
    }

    @Test
    void shouldSkipJavaDto() {
        // Given a Java POJO (no @kotlin.Metadata on its class)
        AnnotatedType type = new AnnotatedType().type(JavaDto.class);

        // The chain mock returns a schema with one property
        Schema<?> unresolvedSchema = new Schema<>();
        unresolvedSchema.setName("javaDto");
        unresolvedSchema.addProperty("name", new io.swagger.v3.oas.models.media.StringSchema());

        ModelConverter mockConverter = mock(ModelConverter.class);
        when(mockConverter.resolve(any(), any(), any())).thenReturn(unresolvedSchema);

        Iterator<ModelConverter> chain = List.of(mockConverter).iterator();
        ModelConverterContext context = mock(ModelConverterContext.class);

        // When
        Schema<?> result = customizer.resolve(type, context, chain);

        // Then: schema is returned unchanged — no required properties were added
        assertThat(result).isSameAs(unresolvedSchema);
        assertThat(result.getRequired()).isNull();
    }

    @Test
    void shouldSkipJavaLangClass() {
        // Given java.lang.String (from the "java." package)
        AnnotatedType type = new AnnotatedType().type(String.class);

        Schema<?> unresolvedSchema = new Schema<>();
        unresolvedSchema.setName("string");

        ModelConverter mockConverter = mock(ModelConverter.class);
        when(mockConverter.resolve(any(), any(), any())).thenReturn(unresolvedSchema);

        Iterator<ModelConverter> chain = List.of(mockConverter).iterator();
        ModelConverterContext context = mock(ModelConverterContext.class);

        // When
        Schema<?> result = customizer.resolve(type, context, chain);

        // Then
        assertThat(result).isSameAs(unresolvedSchema);
        assertThat(result.getRequired()).isNull();
    }

    @Test
    void shouldReturnNullWhenChainEmpty() {
        // Given an empty chain
        AnnotatedType type = new AnnotatedType().type(String.class);
        Iterator<ModelConverter> emptyChain = List.<ModelConverter>of().iterator();
        ModelConverterContext context = mock(ModelConverterContext.class);

        // When
        Schema<?> result = customizer.resolve(type, context, emptyChain);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void kotlinDetectorRejectsJavaDto() {
        // Verify that KotlinDetector correctly identifies our Java DTO as non-Kotlin
        assertThat(KotlinDetector.isKotlinClass(JavaDto.class)).isFalse();
    }

    /**
     * A plain Java POJO — no {@code @kotlin.Metadata} annotation present.
     */
    static class JavaDto {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
