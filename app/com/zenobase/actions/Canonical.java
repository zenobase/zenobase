package com.zenobase.actions;

import javax.inject.Inject;
import javax.inject.Named;

import play.Logger;
import play.api.mvc.Handler;
import play.mvc.Http.RequestHeader;

public class Canonical {

	@Inject
	@Named("hostname")
	private String baseUri;

	@Inject
	@Named("api.hostname")
	private String apiUri;

	public Handler redirect(RequestHeader request) {
		Logger.info("Redirecting {}{}...", request.host(), request.uri());
		return controllers.Default.redirect(baseUri + request.uri());
	}

	public boolean test(RequestHeader request) {
		String agent = request.getHeader("User-Agent");
		String scheme = request.getHeader("X-Forwarded-Proto");
		String uri = scheme != null ? scheme + "://" + request.host() : null;
		return agent != null && agent.contains("CloudFront") || uri == null || baseUri.equals(uri) || apiUri.equals(uri);
	}
}
