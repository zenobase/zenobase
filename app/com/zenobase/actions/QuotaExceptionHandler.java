package com.zenobase.actions;

import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

import com.zenobase.controllers.ControllerSupport;
import com.zenobase.services.QuotaException;

public class QuotaExceptionHandler extends Action.Simple {

	@Override
	public Result call(Context context) throws Throwable {
		try {
			return delegate.call(context);
		} catch (QuotaException e) {
			return ControllerSupport.forbidden(e.getMessage());
		}
	}
}
