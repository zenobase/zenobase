package com.zenobase.actions;

import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

/**
 * Protection against clickjacking attacks.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/HTTP/X-Frame-Options">The X-Frame-Options response header</a>
 */
public class NoFrame extends Action.Simple {

	@Override
	public Promise<Result> call(Context context) throws Throwable {
		context.response().setHeader("X-Frame-Options", "SAMEORIGIN");
		return delegate.call(context);
	}
}
