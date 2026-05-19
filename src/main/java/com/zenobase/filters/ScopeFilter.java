package com.zenobase.filters;

import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import io.sentry.Sentry;
import io.sentry.protocol.Request;
import io.sentry.protocol.User;
import jakarta.inject.Inject;

public class ScopeFilter implements Filter {

	private static final String CLIENT_ID_TAG = "auth.client_id";

	private final AuthorizationContext authContext;

	@Inject
	public ScopeFilter(AuthorizationContext authContext) {
		this.authContext = authContext;
	}

	@Override
	public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
		try {
			Authorization auth = authContext.current(req);
			if (auth != null) {
				User user = new User();
				user.setId(auth.getPrincipal().id());
				Sentry.setUser(user);
				Identity client = auth.getClient();
				if (client != null) {
					Sentry.configureScope(scope -> scope.setTag(CLIENT_ID_TAG, client.id()));
				}
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
				scope.removeTag(CLIENT_ID_TAG);
			});
		}
	}
}
