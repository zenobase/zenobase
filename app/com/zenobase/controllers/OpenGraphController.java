package com.zenobase.controllers;

import java.io.IOException;
import java.net.URI;

import javax.inject.Inject;

import play.Logger;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.Result;

import com.zenobase.io.OpenGraph;

public class OpenGraphController extends ControllerSupport {

	@Inject
	public OpenGraphController(AuthorizationContext security) {
		super(security);
	}

	public Promise<Result> get(final String url) {
		if (!url.startsWith("http")) {
			return get("http://" + url);
		}
		if (!isValid(url)) {
			return Promise.<Result>pure(badRequest("Invalid URL: " + url));
		}
		return WS.url(url).get()
			.map(new F.Function<WSResponse, Result>() {
				@Override
				public Result apply(WSResponse response) {
					if (response.getStatus() != OK) {
						return badRequest("Couldn't retrieve resource: " + url);
					}
					try {
						return ok(OpenGraph.parse(url, response.getBodyAsStream()).toJson());
					} catch (IOException e) {
						String message = "Couldn't parse resource: " + url;
						Logger.warn(message);
						return badRequest(message);
					}
				}
			})
			.recover(new F.Function<Throwable, Result>() {
				@Override
				public Result apply(Throwable t) {
					String message = "Couldn't retrieve resource: " + url;
					Logger.warn(message);
					return badRequest(message);
				}
			}
		);
	}

	private static boolean isValid(String url) {
		try {
			URI.create(url);
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}
}
