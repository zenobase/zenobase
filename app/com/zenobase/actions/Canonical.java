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

	public Handler redirect(RequestHeader request) {
		Logger.info("redirect from " + request.host());
		return controllers.Default.redirect(baseUri + request.uri());
	}

	public boolean test(RequestHeader request) {
		String scheme = request.getHeader("X-Forwarded-Proto");
		return scheme == null || baseUri.equals(scheme + "://" + request.host());
	}
}
