package com.zenobase.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class OpenSearchClientFactoryTest {

	@Test
	public void testHttpsRequiresRegion() {
		assertThatThrownBy(() -> OpenSearchClientFactory.createClient("https://search.example.com", null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("AWS_REGION");
	}
}
