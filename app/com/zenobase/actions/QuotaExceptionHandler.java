package com.zenobase.actions;

import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.SimpleResult;

import com.zenobase.controllers.ControllerSupport;
import com.zenobase.services.QuotaException;

public class QuotaExceptionHandler extends Action.Simple {

	@Override
	public Promise<SimpleResult> call(Context context) throws Throwable {
		try {
			return delegate.call(context);
		} catch (QuotaException e) {
			return Promise.<SimpleResult>pure(ControllerSupport.forbidden(e.getMessage()));
		}
	}
}
