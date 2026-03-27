package com.zenobase.actions;

import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import io.sentry.Sentry;
import io.sentry.protocol.Request;
import io.sentry.protocol.User;
import jakarta.inject.Inject;

import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.oauth.Authorization;

public class SentryFilter implements Filter {

	private final AuthorizationContext authContext;

	@Inject
	public SentryFilter(AuthorizationContext authContext) {
		this.authContext = authContext;
	}

	@Override
	public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
		try {
			Authorization auth = authContext.current(req);
			if (auth != null) {
				User user = new User();
				user.setId(auth.getPrincipal().getId());
				Sentry.setUser(user);
			}
			Sentry.configureScope(scope -> {
				Request request = new Request();
				request.setMethod(req.prologue().method().text());
				request.setUrl(req.prologue().uriPath().rawPath());
				scope.setRequest(request);
			});
			chain.proceed();
		} finally {
			Sentry.configureScope(scope -> {
				scope.setUser(null);
				scope.setRequest(null);
			});
		}
	}
}
