package com.zenobase.filters;

import io.helidon.http.Method;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.Test;

public class TracingFilterTest extends FilterTestSupport {

	@Override
	protected void configureFilters(HttpRouting.Builder routing) {
		routing.addFilter(new TracingFilter());
	}

	@Override
	protected void configureRoutes(HttpRouting.Builder routing) {
		super.configureRoutes(routing);
		routing.route(Method.OPTIONS, "/ping", (req, res) -> res.send());
	}

	@Test
	public void test() {
		ping();
	}
}
