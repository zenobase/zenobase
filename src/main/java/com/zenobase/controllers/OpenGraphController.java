package com.zenobase.controllers;

import java.io.IOException;
import java.net.URI;

import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
				get(res, "http://" + url);
				return;
			}
			sendBadRequest(res, "Invalid URL: " + url);
			return;
		}
		get(res, url);
	}

	private void get(ServerResponse res, String url) {
		if (!isValid(url)) {
			sendBadRequest(res, "Invalid URL: " + url);
			return;
		}
		try {
			var response = (ClassicHttpResponse) Request.get(url).execute().returnResponse();
			if (response.getCode() != HttpStatus.SC_OK) {
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
				EntityUtils.consume(entity);
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
