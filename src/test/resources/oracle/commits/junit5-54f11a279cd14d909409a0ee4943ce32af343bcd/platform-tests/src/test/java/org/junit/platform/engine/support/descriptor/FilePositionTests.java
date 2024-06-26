/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine.support.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.PreconditionViolationException;

/**
 * Unit tests for {@link FilePosition}.
 *
 * @since 1.0
 */
@DisplayName("FilePosition unit tests")
class FilePositionTests extends AbstractTestSourceTests {

	@Test
	@DisplayName("factory method preconditions")
	void preconditions() {
		assertThrows(PreconditionViolationException.class, () -> FilePosition.from(-1));
		assertThrows(PreconditionViolationException.class, () -> FilePosition.from(0, -1));
	}

	@Test
	@DisplayName("create FilePosition from factory method with line number")
	void filePositionFromLine() {
		FilePosition filePosition = FilePosition.from(42);

		assertThat(filePosition.getLine()).isEqualTo(42);
		assertThat(filePosition.getColumn()).isEmpty();
	}

	@Test
	@DisplayName("create FilePosition from factory method with line number and column number")
	void filePositionFromLineAndColumn() {
		FilePosition filePosition = FilePosition.from(42, 99);

		assertThat(filePosition.getLine()).isEqualTo(42);
		assertThat(filePosition.getColumn()).contains(99);
	}

	@ParameterizedTest
	@MethodSource
	void filePositionFromQuery(String query, int expectedLine, int expectedColumn) {
		Optional<FilePosition> optionalFilePosition = FilePosition.fromQuery(query);
		if (optionalFilePosition.isPresent()) {
			FilePosition filePosition = optionalFilePosition.get();

			assertThat(filePosition.getLine()).isEqualTo(expectedLine);
			assertThat(filePosition.getColumn().orElse(-1)).isEqualTo(expectedColumn);
			return;
		}

		assertEquals(-1, expectedLine);
		assertEquals(-1, expectedColumn);
	}

	@SuppressWarnings("unused")
	static Stream<Arguments> filePositionFromQuery() {
		return Stream.of( //
			Arguments.of(null, -1, -1), //
			Arguments.of("?!", -1, -1), //
			Arguments.of("line=ZZ", -1, -1), //
			Arguments.of("line=42", 42, -1), //
			Arguments.of("line=42&line=24", 24, -1), //
			Arguments.of("line=42&column=99", 42, 99), //
			Arguments.of("line=42&column=ZZ", 42, -1) //
		);
	}

	@Test
	@DisplayName("equals() and hashCode() with column number cached by Integer.valueOf()")
	void equalsAndHashCode() {
		FilePosition same = FilePosition.from(42, 99);
		FilePosition sameSame = FilePosition.from(42, 99);
		FilePosition different = FilePosition.from(1, 2);

		assertEqualsAndHashCode(same, sameSame, different);
	}

	@Test
	@DisplayName("equals() and hashCode() with column number not cached by Integer.valueOf()")
	void equalsAndHashCodeWithColumnNumberNotCachedByJavaLangIntegerDotValueOf() {
		FilePosition same = FilePosition.from(42, 99999);
		FilePosition sameSame = FilePosition.from(42, 99999);
		FilePosition different = FilePosition.from(1, 2);

		assertEqualsAndHashCode(same, sameSame, different);
	}

}
