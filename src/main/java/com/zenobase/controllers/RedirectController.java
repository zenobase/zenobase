package com.zenobase.controllers;

import jakarta.inject.Inject;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

public class RedirectController extends ControllerSupport {

	@Inject
	public RedirectController(AuthorizationContext security) {
		super(security);
	}

	public void get(ServerRequest req, ServerResponse res) {
		String url = req.query().first("url").orElse(null);
    	sendRedirect(res, url);
    }
}
