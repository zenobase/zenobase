package com.zenobase.filters;

import com.zenobase.testing.ResultAssert;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.Test;

public class SecurityHeadersFilterTest extends FilterTestSupport {

	@Override
	protected void configureFilters(HttpRouting.Builder routing) {
		routing.addFilter(new SecurityHeadersFilter());
	}

	@Test
	public void testHeadersPresentOnResponse() {
		try (Http1ClientResponse r = client.get("/ping").request()) {
			ResultAssert.assertThat(r)
				.hasStatus(200)
				.hasHeader("Strict-Transport-Security", "max-age=31536000")
				.hasHeader("X-Content-Type-Options", "nosniff")
				.hasHeader("Cache-Control", "no-store");
		}
	}
}
