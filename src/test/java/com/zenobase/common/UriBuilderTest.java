package com.zenobase.common;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class UriBuilderTest {

	@Test
	public void test() {
		UriBuilder uri = new UriBuilder("https://localhost:9000/xyz")
				.addParameter("foo", "a")
				.addParameter("bar", "b c")
				.addParameter("baz", "~d");
		Assertions.assertThat(uri.build()).isEqualTo("https://localhost:9000/xyz?foo=a&bar=b+c&baz=%7Ed");
	}
}
