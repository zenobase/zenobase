package com.zenobase.controllers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import play.Logger;
import play.libs.F;
import play.libs.WS;
import play.mvc.Result;
import play.mvc.With;

import com.zenobase.actions.Timed;
import com.zenobase.io.OpenGraph;

@With(Timed.class)
public class OpenGraphController extends ControllerSupport {

	public static Result get(final String url) {
		if (!url.startsWith("http")) {
			return get("http://" + url);
		}
		if (!isValid(url)) {
			return badRequest(notification("Invalid URL: " + url));
		}
		return async(WS.url(url).get().map(new F.Function<WS.Response, Result>() {
			@Override
			public Result apply(WS.Response response) {
				if (response.getStatus() != OK) {
					return badRequest(notification("Couldn't retrieve resource: " + url));
				}
				try {
					return ok(OpenGraph.parse(url, response.getBodyAsStream()).toJson());
				} catch (IOException e) {
					String message = "Couldn't parse resource: " + url;
					Logger.warn(message);
					return badRequest(notification(message));
				}
			}
		}));
	}

	private static boolean isValid(String url) {
		try {
			new URL(url);
		} catch (MalformedURLException e) {
			return false;
		}
		return true;
	}
}
