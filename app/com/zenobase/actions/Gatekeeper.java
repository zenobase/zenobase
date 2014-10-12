package com.zenobase.actions;

import javax.inject.Inject;

import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Http.Context;
import play.mvc.Result;

import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.Bus;
import com.zenobase.services.UserRepository;

/**
 * Disables all requests other than GET or HEAD for everyone but superusers when in read-only mode.
 */
public class Gatekeeper extends Action.Simple {

	@Inject
	private Bus bus;

	@Inject
	private UserRepository users;

	@Inject
	private AuthorizationContext authContext;

	@Override
	public Promise<Result> call(Context context) throws Throwable {
		return !isSafe(context.request()) && bus.isReadOnly() && !isSuperuser(context)
			? Promise.<Result>pure(status(Http.Status.SERVICE_UNAVAILABLE))
			: delegate.call(context);
	}

	private boolean isSafe(Http.Request request) {
		return "GET".equals(request.method()) || "HEAD".equals(request.method());
	}

	private boolean isSuperuser(Context context) {
		Authorization auth = authContext.current(context);
		return auth != null && users.isSuperuser(auth.getPrincipal());
	}
}
