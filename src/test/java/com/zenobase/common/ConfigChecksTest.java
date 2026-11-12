package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class ConfigChecksTest {

	@Test
	public void acceptsHttpAndHttps() {
		assertThat(ConfigChecks.requireHttpUrl("k", "https://a.example.com")).isEqualTo("https://a.example.com");
		assertThat(ConfigChecks.requireHttpUrl("k", "http://localhost:3000")).isEqualTo("http://localhost:3000");
	}

	@Test
	public void rejectsBlank() {
		assertThatThrownBy(() -> ConfigChecks.requireHttpUrl("auth0.domain", ""))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("auth0.domain is not configured");
	}

	@Test
	public void rejectsRelativeAndBareHost() {
		assertThatThrownBy(() -> ConfigChecks.requireHttpUrl("k", "/path"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("absolute http(s) URL");
		assertThatThrownBy(() -> ConfigChecks.requireHttpUrl("k", "example.com")).isInstanceOf(
			IllegalStateException.class
		);
	}
}
