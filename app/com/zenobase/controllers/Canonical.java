package com.zenobase.controllers;

import play.mvc.Action.Simple;
import play.mvc.Http.Context;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Results;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class Canonical extends Simple {

	@Inject
	@Named("hostname")
	static String baseUri;

	@Override
	public Result call(Context context) throws Throwable {
		return isCanonical(context.request()) ? delegate.call(context) :
			Results.movedPermanently(baseUri + context.request().path());
	}

	private boolean isCanonical(Request request) {
		String originalUri = request.getHeader("X-Forwarded-Proto");
		return originalUri == null || originalUri.startsWith(baseUri);
	}
}
