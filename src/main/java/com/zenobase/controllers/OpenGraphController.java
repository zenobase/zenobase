package com.zenobase.controllers;

import java.io.IOException;
import java.net.URI;

import jakarta.inject.Inject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import com.zenobase.io.OpenGraph;

public class OpenGraphController extends ControllerSupport {

	private static final Logger logger = LoggerFactory.getLogger(OpenGraphController.class);

	@Inject
	public OpenGraphController(AuthorizationContext security) {
		super(security);
	}

	public void get(ServerRequest req, ServerResponse res) {
		String url = req.query().first("url").orElse(null);
		if (url == null || !url.startsWith("http")) {
			if (url != null) {
				get(req, res, "http://" + url);
				return;
			}
			sendBadRequest(res, "Invalid URL: " + url);
			return;
		}
		get(req, res, url);
	}

	private void get(ServerRequest req, ServerResponse res, String url) {
		if (!isValid(url)) {
			sendBadRequest(res, "Invalid URL: " + url);
			return;
		}
		try {
			HttpResponse response = Request.Get(url).execute().returnResponse();
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				sendBadRequest(res, "Couldn't retrieve resource: " + url);
				return;
			}
			HttpEntity entity = response.getEntity();
			try {
				sendOk(res, OpenGraph.parse(url, entity.getContent()).toJson());
			} catch (IOException e) {
				String message = "Couldn't parse resource: " + url;
				logger.warn(message);
				sendBadRequest(res, message);
			} finally {
				EntityUtils.consumeQuietly(entity);
			}
		} catch (IOException e) {
			sendBadRequest(res, "Couldn't retrieve resource: " + url);
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
