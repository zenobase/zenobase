package com.zenobase.filters;

import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;

public class SecurityHeadersFilter implements Filter {

	@Override
	public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
		res.header(HeaderNames.create("Strict-Transport-Security"), "max-age=31536000");
		res.header(HeaderNames.create("X-Content-Type-Options"), "nosniff");
		res.header(HeaderNames.CACHE_CONTROL, "no-store");
		chain.proceed();
	}
}
