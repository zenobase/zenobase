package com.zenobase.actions;

import play.Logger;
import play.api.mvc.Handler;
import play.mvc.Http.RequestHeader;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class Canonical {

	@Inject
	@Named("hostname")
	private String baseUri;

	@Inject
	@Named("api.hostname")
	private String apiUri;

	public Handler redirect(RequestHeader request) {
		Logger.info("redirect " + request.host() + request.uri());
		return controllers.Default.redirect(baseUri + request.uri());
	}

	public boolean test(RequestHeader request) {
		String scheme = request.getHeader("X-Forwarded-Proto");
		String uri = scheme != null ? scheme + "://" + request.host() : null;
		return uri == null || baseUri.equals(uri) || apiUri.equals(uri);
	}
}
