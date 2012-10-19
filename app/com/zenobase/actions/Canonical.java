package com.zenobase.actions;
import java.util.Map;

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
		Logger.info("redirect " + request.host() + request.uri());
		return controllers.Default.redirect(baseUri + request.uri());
	}

	public boolean test(RequestHeader request) {
		if ("ELBLatencyCheck-1.0".equals(request.getHeader("User-Agent"))) {
			Logger.info("status check:");
			for (Map.Entry<String, String[]> entry : request.headers().entrySet()) {
				for (String value : entry.getValue()) {
					Logger.info("[http] " + entry.getKey() + ": " + value);
				}
			}
		}
		String scheme = request.getHeader("X-Forwarded-Proto");
		return scheme == null || baseUri.equals(scheme + "://" + request.host());
	}
}
