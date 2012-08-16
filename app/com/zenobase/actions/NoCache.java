package com.zenobase.actions;

import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

public class NoCache extends Action.Simple {

	@Override
	public Result call(Context context) throws Throwable {
		context.response().setHeader("Cache-Control", "no-cache");
		return delegate.call(context);
	}
}
