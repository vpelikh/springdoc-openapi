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

import java.security.SecureRandom;
import java.util.Random;

/**
 * Utility methods for generating random strings.
 * <p>
 * Replaces {@code org.apache.commons.lang3.RandomStringUtils}.
 * </p>
 */
public final class RandomStringUtils {

	private static final char[] ALPHANUMERIC_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

	private static final Random RANDOM = new SecureRandom();

	private RandomStringUtils() {
	}

	/**
	 * Generates a random alphanumeric string of the specified length.
	 *
	 * @param count the length of the random string to create
	 * @return the random alphanumeric string
	 * @throws IllegalArgumentException if {@code count < 0}
	 */
	public static String randomAlphanumeric(int count) {
		if (count < 0) {
			throw new IllegalArgumentException("Count cannot be negative: " + count);
		}
		if (count == 0) {
			return "";
		}
		char[] buffer = new char[count];
		for (int i = 0; i < count; i++) {
			buffer[i] = ALPHANUMERIC_CHARS[RANDOM.nextInt(ALPHANUMERIC_CHARS.length)];
		}
		return new String(buffer);
	}
}
