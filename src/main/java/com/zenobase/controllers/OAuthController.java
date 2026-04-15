package com.zenobase.controllers;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import org.joda.time.Duration;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.auth.local.LocalTokenService;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.UserRepository;

public class OAuthController extends ControllerSupport {

	private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);

	static final String RESPONSE_TYPE_TOKEN = "token";
	static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
	static final String INVALID_REQUEST = "invalid_request";
	static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
	static final String INVALID_REDIRECT_URI = "invalid_redirect_uri";
	static final String INVALID_SCOPE = "invalid_scope";

	static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";

	static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

	private final LocalTokenService localTokenService;
	private final UserRepository users;

	@Inject
	public OAuthController(AuthorizationContext security, LocalTokenService localTokenService, UserRepository users) {
		super(security);
		this.localTokenService = localTokenService;
		this.users = users;
	}

	public void authorize(ServerRequest req, ServerResponse res) {
		authorize(res, req, new AuthorizeForm(body(req)));
	}

	private void authorize(ServerResponse res, ServerRequest req, AuthorizeForm form) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null || auth.getScope() != null) {
			sendUnauthorized(res);
			return;
		}
		if (!RESPONSE_TYPE_TOKEN.equals(form.getResponseType())) {
			deny(res, UNSUPPORTED_RESPONSE_TYPE, String.format("response_type must be '%s'", RESPONSE_TYPE_TOKEN));
			return;
		}
		if (form.getClient() == null) {
			deny(res, INVALID_REQUEST, "client_id is required");
			return;
		}
		if (form.getRedirectUri() == null) {
			deny(res, INVALID_REQUEST, "redirect_uri is required");
			return;
		}
		User client = users.find(form.getClient());
		if (client == null || !client.isVerified() || client.isSuspended()) {
			deny(res, UNAUTHORIZED_CLIENT, "client account must be enabled and verified");
			return;
		}
		if (!new OAuthRedirectValidator(client).valid(form.getRedirectUri())) {
			deny(res, INVALID_REDIRECT_URI, "domain must match the domain of the email address");
			return;
		}
		if (form.getScope() == null) {
			// TODO: check that the bucket exists and that the principal has access
			deny(res, INVALID_SCOPE, "scope must be a bucket");
			return;
		}
		String token = localTokenService.createScopedToken(auth.getPrincipal(), form.getClient(), form.getScope());
		ObjectNode result = Nodes.newObject();
		result.put("access_token", token);
		result.put("client_id", auth.getPrincipal().id());
		result.put("scope", form.getScope());
		sendOk(res, result);
	}

	public void token(ServerRequest req, ServerResponse res) {
		TokenForm form = parseTokenForm(req);
		token(res, form);
	}

	private TokenForm parseTokenForm(ServerRequest req) {
		String contentType =
				req.headers().first(io.helidon.http.HeaderNames.CONTENT_TYPE).orElse("");
		if (contentType.contains("application/json")) {
			ObjectNode node = body(req);
			return new TokenForm(node.has("grant_type") ? node.get("grant_type").asText() : null);
		}
		String body = req.content().as(String.class);
		Map<String, String> params = parseFormEncoded(body);
		return new TokenForm(params.get("grant_type"));
	}

	private static Map<String, String> parseFormEncoded(@Nullable String body) {
		Map<String, String> params = new LinkedHashMap<>();
		if (body == null || body.isEmpty()) {
			return params;
		}
		for (String pair : body.split("&")) {
			String[] kv = pair.split("=", 2);
			String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
			String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
			params.put(key, value);
		}
		return params;
	}

	private void token(ServerResponse res, TokenForm form) {
		if (GRANT_TYPE_CLIENT_CREDENTIALS.equals(form.getGrant_type())) {
			String token = localTokenService.createGuestToken();
			Identity guest = new Identity(com.auth0.jwt.JWT.decode(token).getSubject());
			ObjectNode result = Nodes.newObject();
			result.put("access_token", token);
			result.put("client_id", guest.id());
			result.put("expires_in", Duration.standardDays(31).getStandardSeconds());
			sendOk(res, result);
			return;
		}
		deny(
				res,
				UNSUPPORTED_GRANT_TYPE,
				String.format("grant_type must be '%s', got %s", GRANT_TYPE_CLIENT_CREDENTIALS, form.getGrant_type()));
	}

	private void deny(ServerResponse res, String errorCode, String errorDescription) {
		logger.warn("Denied: {}", errorDescription);
		ObjectNode result = Nodes.newObject();
		result.put("error", errorCode);
		result.put("error_description", errorDescription);
		sendBadRequest(res, result);
	}

	public void callback(ServerRequest req, ServerResponse res) {
		String id = req.path().pathParameters().get("id");
		if (!"-".equals(id) && !Generator.isValidId(id)) {
			sendNotFound(res);
			return;
		}
		res.status(Status.create(303));
		res.header(HeaderNames.LOCATION, String.format("/#/credentials/%s?%s", id, toQueryString(req)));
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
