package com.zenobase.filters;

import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.oauth.Authorization;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import jakarta.inject.Inject;
import org.slf4j.MDC;

public class LogContextFilter implements Filter {

	private final AuthorizationContext authContext;

	@Inject
	public LogContextFilter(AuthorizationContext authContext) {
		this.authContext = authContext;
	}

	@Override
	public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
		try {
			MDC.put("method", req.prologue().method().text());
			MDC.put("path", req.prologue().uriPath().rawPath());
			Authorization auth = authContext.current(req);
			if (auth != null) {
				MDC.put("userId", auth.getPrincipal().id());
			}
			chain.proceed();
		} finally {
			MDC.clear();
		}
	}
}
