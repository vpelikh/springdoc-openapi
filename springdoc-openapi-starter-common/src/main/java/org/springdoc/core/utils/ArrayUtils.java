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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Utility methods for array operations.
 * <p>
 * Replaces methods from {@code org.apache.commons.lang3.ArrayUtils}.
 * All methods are null-safe and follow commons-lang3 contracts.
 * </p>
 */
public final class ArrayUtils {

	private ArrayUtils() {
	}

	/**
	 * Checks if an array is empty or null.
	 *
	 * @param array the array to check, may be null
	 * @param <T>   the array component type
	 * @return {@code true} if the array is empty or null
	 */
	public static <T> boolean isEmpty(final T[] array) {
		return array == null || array.length == 0;
	}

	/**
	 * Checks if an array is not empty and not null.
	 *
	 * @param array the array to check, may be null
	 * @param <T>   the array component type
	 * @return {@code true} if the array is not empty and not null
	 */
	public static <T> boolean isNotEmpty(final T[] array) {
		return !isEmpty(array);
	}

	/**
	 * Copies the given array and adds the given element at the end.
	 *
	 * @param array   the array to add the element to, may be null
	 * @param element the element to add, may be null
	 * @param <T>     the component type of the array
	 * @return a new array containing the existing elements plus the new element,
	 *         or a new array containing only the element if the input array is null
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] add(final T[] array, final T element) {
		if (array == null) {
			@SuppressWarnings("unchecked")
			T[] result = (T[]) Array.newInstance(element != null ? element.getClass() : Object.class, 1);
			result[0] = element;
			return result;
		}
		T[] result = Arrays.copyOf(array, array.length + 1);
		result[array.length] = element;
		return result;
	}

	/**
	 * Adds all the elements of the given arrays into a new array.
	 *
	 * @param array1 the first array, may be null
	 * @param array2 the second array, may be null
	 * @param <T>    the component type of the arrays
	 * @return a new array with all elements from both arrays
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] addAll(final T[] array1, final T[] array2) {
		if (array1 == null && array2 == null) {
			return (T[]) Array.newInstance(Object.class, 0);
		}
		if (array1 == null) {
			return Arrays.copyOf(array2, array2.length);
		}
		if (array2 == null) {
			return Arrays.copyOf(array1, array1.length);
		}
		T[] result = Arrays.copyOf(array1, array1.length + array2.length);
		System.arraycopy(array2, 0, result, array1.length, array2.length);
		return result;
	}

	/**
	 * Removes the first occurrence of the specified element from the array.
	 *
	 * @param array   the array to remove the element from, may be null
	 * @param element the element to be removed, may be null
	 * @param <T>     the component type of the array
	 * @return a new array with the element removed, or the original array if
	 *         the element is not found
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] removeElement(final T[] array, final T element) {
		if (isEmpty(array)) {
			return array;
		}
		List<T> list = new ArrayList<>(Arrays.asList(array));
		// Remove first occurrence, matching commons-lang3 behavior
		// which uses indexOf + System.arraycopy
		int index = -1;
		for (int i = 0; i < array.length; i++) {
			if (Objects.equals(array[i], element)) {
				index = i;
				break;
			}
		}
		if (index < 0) {
			return array;
		}
		T[] result = Arrays.copyOf(array, array.length - 1);
		if (index < array.length - 1) {
			System.arraycopy(array, index + 1, result, index, array.length - index - 1);
		}
		return result;
	}

	/**
	 * Create an empty array of the specified type.
	 *
	 * @param <T> the array component type
	 * @return an empty array
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] toArray() {
		return (T[]) new Object[0];
	}

}
