package com.zenobase.actions;

import io.helidon.http.Status;
import io.helidon.webserver.http.Filter;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;

import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.Bus;
import com.zenobase.services.UserRepository;

/**
 * Disables all requests other than GET or HEAD for everyone but superusers when in read-only mode.
 */
public class GatekeeperFilter implements Filter {

	private final Bus bus;
	private final UserRepository users;
	private final AuthorizationContext authContext;

	public GatekeeperFilter(Bus bus, UserRepository users, AuthorizationContext authContext) {
		this.bus = bus;
		this.users = users;
		this.authContext = authContext;
	}

	@Override
	public void filter(FilterChain chain, RoutingRequest req, RoutingResponse res) {
		if (!isSafe(req) && bus.isReadOnly() && !isSuperuser(req)) {
			res.status(Status.SERVICE_UNAVAILABLE_503).send();
			return;
		}
		chain.proceed();
	}

	private boolean isSafe(RoutingRequest req) {
		String method = req.prologue().method().text();
		return "GET".equals(method) || "HEAD".equals(method);
	}

	private boolean isSuperuser(RoutingRequest req) {
		Authorization auth = authContext.current(req);
		return auth != null && users.isSuperuser(auth.getPrincipal());
	}
}
