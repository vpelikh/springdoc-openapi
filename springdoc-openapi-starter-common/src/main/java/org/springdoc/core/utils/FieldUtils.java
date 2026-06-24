/*
 * Copyright 2019-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springdoc.core.utils;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for reflection operations.
 * <p>
 * Replaces methods from {@code org.apache.commons.lang3.reflect.FieldUtils}.
 * Delegates to Spring's {@code org.springframework.util.ReflectionUtils}
 * internally where appropriate.
 * </p>
 */
public final class FieldUtils {

	private FieldUtils() {
	}

	/**
	 * Gets a field from a class. The field may be declared in the class
	 * or inherited from its ancestors.
	 *
	 * @param cls         the class to inspect
	 * @param fieldName   the field name
	 * @param forceAccess whether to make inaccessible fields accessible
	 * @return the Field object, or null if not found
	 */
	public static Field getField(final Class<?> cls, final String fieldName, final boolean forceAccess) {
		Field field = ReflectionUtils.findField(cls, fieldName);
		if (field != null && forceAccess) {
			ReflectionUtils.makeAccessible(field);
		}
		return field;
	}

	/**
	 * Gets a field from a class, looking only at the class's declared fields.
	 *
	 * @param cls         the class to inspect
	 * @param fieldName   the field name
	 * @param forceAccess whether to make inaccessible fields accessible
	 * @return the Field object, or null if not found
	 */
	public static Field getDeclaredField(final Class<?> cls, final String fieldName, final boolean forceAccess) {
		for (Field field : cls.getDeclaredFields()) {
			if (field.getName().equals(fieldName)) {
				if (forceAccess) {
					ReflectionUtils.makeAccessible(field);
				}
				return field;
			}
		}
		return null;
	}

	/**
	 * Reads a named field from an object.
	 *
	 * @param obj         the object to read from
	 * @param fieldName   the field name
	 * @param forceAccess whether to make inaccessible fields accessible
	 * @return the field value, or null if field not found
	 */
	public static Object readField(final Object obj, final String fieldName, final boolean forceAccess) {
		Field field = getField(obj.getClass(), fieldName, forceAccess);
		if (field == null) {
			return null;
		}
		return ReflectionUtils.getField(field, obj);
	}

	/**
	 * Reads a named declared field from an object.
	 *
	 * @param obj         the object to read from
	 * @param fieldName   the field name
	 * @param forceAccess whether to make inaccessible fields accessible
	 * @return the field value, or null if field not found
	 */
	public static Object readDeclaredField(final Object obj, final String fieldName, final boolean forceAccess) {
		Field field = getDeclaredField(obj.getClass(), fieldName, forceAccess);
		if (field == null) {
			return null;
		}
		return ReflectionUtils.getField(field, obj);
	}

	/**
	 * Gets all fields for a class, including inherited fields.
	 *
	 * @param cls the class to inspect
	 * @return a list of Fields (possibly empty)
	 */
	public static List<Field> getAllFieldsList(final Class<?> cls) {
		final List<Field> fields = new ArrayList<>();
		ReflectionUtils.doWithFields(cls, fields::add);
		return fields;
	}
}
