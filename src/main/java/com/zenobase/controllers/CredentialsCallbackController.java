package com.zenobase.controllers;

import com.zenobase.common.Generator;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Set;

public class CredentialsCallbackController extends ControllerSupport {

	private final String webHostname;

	@Inject
	public CredentialsCallbackController(AuthorizationContext security, @Named("web.hostname") String webHostname) {
		super(security);
		this.webHostname = webHostname;
	}

	public void callback(ServerRequest req, ServerResponse res) {
		String id = req.path().pathParameters().get("id");
		if (!"-".equals(id) && !Generator.isValidId(id)) {
			sendNotFound(res);
			return;
		}
		res.status(Status.create(303));
		res.header(HeaderNames.LOCATION, String.format("%s/#/credentials/%s?%s", webHostname, id, toQueryString(req)));
		res.send();
	}

	private static String toQueryString(ServerRequest req) {
		StringBuilder builder = new StringBuilder();
		Set<String> names;
		try {
			names = req.query().names();
		} catch (NoSuchElementException e) {
			return "";
		}
		for (String name : names) {
			for (String value : req.query().all(name)) {
				if (!builder.isEmpty()) {
					builder.append('&');
				}
				builder.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8));
			}
		}
		return builder.toString();
	}
}
