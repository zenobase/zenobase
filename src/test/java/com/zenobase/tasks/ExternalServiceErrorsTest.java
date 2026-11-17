package com.zenobase.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.scribe.exceptions.OAuthConnectionException;

public class ExternalServiceErrorsTest {

	@ParameterizedTest
	@CsvSource({ "503, true", "500, true", "429, true", "404, false", "400, false" })
	public void testIsTransientStatus(int status, boolean expected) {
		assertEquals(
			expected,
			ExternalServiceErrors.isTransient(new InvalidStatusException("https://example.com", status, ""))
		);
	}

	@Test
	public void testIsTransientConnectionFailure() {
		assertTrue(ExternalServiceErrors.isTransient(new OAuthConnectionException(new IOException("timeout"))));
		assertFalse(ExternalServiceErrors.isTransient(new RuntimeException("boom")));
	}
}
