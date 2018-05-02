package com.zenobase.controllers;

import java.io.IOException;
import java.net.URI;

import javax.inject.Inject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import play.Logger;
import play.mvc.Result;

import com.zenobase.io.OpenGraph;

public class OpenGraphController extends ControllerSupport {

	@Inject
	public OpenGraphController(AuthorizationContext security) {
		super(security);
	}

	public Result get(String url) {
		if (!url.startsWith("http")) {
			return get("http://" + url);
		}
		if (!isValid(url)) {
			return badRequest("Invalid URL: " + url);
		}
		try {
			HttpResponse response = Request.Get(url).execute().returnResponse();
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				return badRequest("Couldn't retrieve resource: " + url);
			}
			HttpEntity entity = response.getEntity();
			try {
				return ok(OpenGraph.parse(url, entity.getContent()).toJson());
			} catch (IOException e) {
				String message = "Couldn't parse resource: " + url;
				Logger.warn(message);
				return badRequest(message);
			} finally {
				EntityUtils.consumeQuietly(entity);
			}
		} catch (IOException e) {
			return badRequest("Couldn't retrieve resource: " + url);
		}
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
