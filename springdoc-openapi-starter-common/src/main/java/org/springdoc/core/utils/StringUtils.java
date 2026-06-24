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

/**
 * Utility methods for String operations.
 * <p>
 * Replaces methods from {@code org.springdoc.core.utils.StringUtils} that
 * don't have equivalents in the Spring Framework or Java standard library.
 * All methods are null-safe and follow commons-lang3 contracts.
 * </p>
 */
public final class StringUtils {
	public static final String EMPTY = "";

	private StringUtils() {
	}

	/**
	 * Checks if a CharSequence is empty (null or length zero).
	 *
	 * @param cs the CharSequence to check, may be null
	 * @return {@code true} if the CharSequence is empty or null
	 */
	public static boolean isEmpty(final CharSequence cs) {
		return cs == null || cs.isEmpty();
	}

	/**
	 * Checks if a CharSequence is not empty (not null and not zero length).
	 *
	 * @param cs the CharSequence to check, may be null
	 * @return {@code true} if the CharSequence is not empty and not null
	 */
	public static boolean isNotEmpty(final CharSequence cs) {
		return !isEmpty(cs);
	}

	/**
	 * Checks if a CharSequence is blank (null, empty, or whitespace-only).
	 *
	 * @param cs the CharSequence to check, may be null
	 * @return {@code true} if the CharSequence is blank or null
	 */
	public static boolean isBlank(final CharSequence cs) {
		if (cs == null) {
			return true;
		}
		return cs.toString().isBlank();
	}

	/**
	 * Checks if a CharSequence is not blank (not null, not empty, not whitespace-only).
	 *
	 * @param cs the CharSequence to check, may be null
	 * @return {@code true} if the CharSequence is not blank and not null
	 */
	public static boolean isNotBlank(final CharSequence cs) {
		return !isBlank(cs);
	}

	/**
	 * Returns either the passed CharSequence, or if the CharSequence is
	 * empty or null, the value of {@code defaultStr}.
	 *
	 * @param str        the CharSequence to test, may be null
	 * @param defaultStr the default CharSequence to return, may be null
	 * @return the passed CharSequence if not empty, else the default
	 */
	public static String defaultIfEmpty(final String str, final String defaultStr) {
		return isEmpty(str) ? defaultStr : str;
	}

	/**
	 * Returns either the passed CharSequence, or if the CharSequence is
	 * blank or null, the value of {@code defaultStr}.
	 *
	 * @param str        the CharSequence to test, may be null
	 * @param defaultStr the default CharSequence to return, may be null
	 * @return the passed CharSequence if not blank, else the default
	 */
	public static String defaultIfBlank(final String str, final String defaultStr) {
		return isBlank(str) ? defaultStr : str;
	}

	/**
	 * Checks if CharSequence contains a search CharSequence, handling null.
	 *
	 * @param str    the CharSequence to check, may be null
	 * @param search the CharSequence to find, may be null
	 * @return true if the str contains the search CharSequence, false if null input
	 */
	public static boolean contains(final String str, final CharSequence search) {
		if (str == null || search == null) {
			return false;
		}
		return str.contains(search);
	}

	/**
	 * Capitalizes a String, changing the first character to upper case.
	 *
	 * @param str the String to capitalize, may be null
	 * @return the capitalized String, or null if null input
	 */
	public static String capitalize(final String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	/**
	 * Gets the substring after the last occurrence of a separator.
	 *
	 * @param str       the String to get a substring from, may be null
	 * @param separator the String to search for, may be null
	 * @return the substring after the last occurrence of the separator,
	 *         or the original string if the separator doesn't exist
	 */
	public static String substringAfterLast(final String str, final String separator) {
		if (isEmpty(str) || isEmpty(separator)) {
			return str;
		}
		int pos = str.lastIndexOf(separator);
		if (pos < 0) {
			return str;
		}
		if (pos == str.length() - separator.length()) {
			return "";
		}
		return str.substring(pos + separator.length());
	}

	/**
	 * Checks if CharSequence contains any of the given search strings.
	 *
	 * @param cs            the CharSequence to check, may be null
	 * @param searchStrings the CharSequences to search for, may be null or empty
	 * @return true if any of the search strings is found, false if null input
	 */
	public static boolean containsAny(final CharSequence cs, final CharSequence... searchStrings) {
		if (isEmpty(cs) || searchStrings == null || searchStrings.length == 0) {
			return false;
		}
		String str = cs.toString();
		for (CharSequence search : searchStrings) {
			if (search != null && str.contains(search)) {
				return true;
			}
		}
		return false;
	}
}
