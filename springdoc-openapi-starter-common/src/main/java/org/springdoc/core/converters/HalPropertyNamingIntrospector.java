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

import tools.jackson.core.Version;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedMethod;

/**
 * AnnotationIntrospector that provides HAL property naming ({@code _embedded}, {@code _links})
 * for HATEOAS types ({@link org.springframework.hateoas.CollectionModel CollectionModel},
 * {@link org.springframework.hateoas.RepresentationModel RepresentationModel}).
 * <p>
 * Registered as the <em>primary</em> introspector via {@link AnnotationIntrospector#pair}
 * so it is consulted first. For non-HAL properties it returns {@code null}, delegating
 * to the chained (secondary) {@link tools.jackson.databind.introspect.JacksonAnnotationIntrospector
 * JacksonAnnotationIntrospector}.
 * <p>
 * Uses class/method name string matching to avoid a compile-time dependency on Spring HATEOAS.
 *
 * @see tools.jackson.databind.introspect.JacksonAnnotationIntrospector
 */
public class HalPropertyNamingIntrospector extends AnnotationIntrospector {

	private static final String COLLECTION_MODEL_CLASS_NAME = "org.springframework.hateoas.CollectionModel";
	private static final String REPRESENTATION_MODEL_CLASS_NAME = "org.springframework.hateoas.RepresentationModel";

	@Override
	public Version version() {
		return Version.unknownVersion();
	}

	@Override
	public PropertyName findNameForSerialization(MapperConfig<?> config, Annotated annotated) {
		return resolveHalPropertyName(annotated);
	}

	@Override
	public PropertyName findNameForDeserialization(MapperConfig<?> config, Annotated annotated) {
		return resolveHalPropertyName(annotated);
	}

	/**
	 * Checks whether the given {@link Annotated} member is a no-arg getter of a
	 * HATEOAS HAL type and, if so, returns the HAL property name.
	 *
	 * @param annotated the annotated member (method) being introspected
	 * @return the HAL property name, or {@code null} if the member does not match
	 */
	private static PropertyName resolveHalPropertyName(Annotated annotated) {
		if (!(annotated instanceof AnnotatedMethod)) {
			return null;
		}
		AnnotatedMethod method = (AnnotatedMethod) annotated;
		if (method.getParameterCount() != 0) {
			return null;
		}
		String declaringClassName = method.getDeclaringClass().getName();
		String methodName = method.getName();

		if (COLLECTION_MODEL_CLASS_NAME.equals(declaringClassName)
				&& "getContent".equals(methodName)) {
			return PropertyName.construct("_embedded");
		}
		if (REPRESENTATION_MODEL_CLASS_NAME.equals(declaringClassName)
				&& "getLinks".equals(methodName)) {
			return PropertyName.construct("_links");
		}
		return null;
	}
}
