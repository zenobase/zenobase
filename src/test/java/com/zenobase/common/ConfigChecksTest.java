package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class ConfigChecksTest {

	@Test
	public void requireNonBlankAcceptsValue() {
		assertThat(ConfigChecks.requireNonBlank("k", "v")).isEqualTo("v");
	}

	@Test
	public void requireNonBlankRejectsBlank() {
		assertThatThrownBy(() -> ConfigChecks.requireNonBlank("auth0.domain", " "))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("auth0.domain is not configured")
			.hasMessageContaining("application-local.yaml.template");
	}

	@Test
	public void requireHttpUrlAcceptsHttpAndHttps() {
		assertThat(ConfigChecks.requireHttpUrl("k", "https://a.example.com")).isEqualTo("https://a.example.com");
		assertThat(ConfigChecks.requireHttpUrl("k", "http://localhost:3000")).isEqualTo("http://localhost:3000");
	}

	@Test
	public void requireHttpUrlRejectsEmpty() {
		assertThatThrownBy(() -> ConfigChecks.requireHttpUrl("k", ""))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("k is not configured");
	}

	@Test
	public void requireHttpUrlRejectsRelativeAndBareHost() {
		assertThatThrownBy(() -> ConfigChecks.requireHttpUrl("k", "/path"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("absolute http(s) URL");
		assertThatThrownBy(() -> ConfigChecks.requireHttpUrl("k", "example.com")).isInstanceOf(
			IllegalStateException.class
		);
	}

	@Test
	public void requireHttpUrlRejectsOtherSchemes() {
		assertThatThrownBy(() -> ConfigChecks.requireHttpUrl("k", "ftp://example.com")).isInstanceOf(
			IllegalStateException.class
		);
	}

	@Test
	public void requireHttpUrlRejectsMalformed() {
		assertThatThrownBy(() -> ConfigChecks.requireHttpUrl("k", "http://exa mple.com"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("not a valid URL");
	}
}
