package com.zenobase.filters;

import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.Test;

public class MetricsFilterTest extends FilterTestSupport {

	@Override
	protected void configureFilters(HttpRouting.Builder routing) {
		routing.addFilter(new MetricsFilter());
	}

	@Test
	public void test() {
		ping();
	}
}
