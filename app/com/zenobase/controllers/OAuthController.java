package com.zenobase.controllers;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import play.data.Form;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.With;
import com.google.common.base.Charsets;

import com.zenobase.actions.Timed;
import com.zenobase.commands.CreateAuthorizationCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

@With(Timed.class)
public class OAuthController extends ControllerSupport {

	static String RESPONSE_TYPE_TOKEN = "token";
	static String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
	static String INVALID_REQUEST = "invalid_request";
	static String UNAUTHORIZED_CLIENT = "unauthorized_client";
	static String INVALID_REDIRECT_URI = "invalid_redirect_uri";
	static String INVALID_SCOPE = "invalid_scope";

	static String GRANT_TYPE_PASSWORD = "password";
	static String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
	static String ACCESS_DENIED = "access_denied";

	static String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

	private final CommandDispatcher dispatcher;
	private final UserRepository users;

	@Inject
	public OAuthController(AuthorizationContext security, CommandDispatcher dispatcher, UserRepository users) {
		super(security);
		this.dispatcher = dispatcher;
		this.users = users;
	}

    @BodyParser.Of(BodyParser.Json.class)
	public Result authorize() {
        return authorize(new AuthorizeForm(body()));
    }

	private Result authorize(AuthorizeForm form) {
    	Authorization auth = getCurrentAuthorization();
    	if (auth == null || auth.getScope() != null) {
    		return unauthorized();
    	}
    	if (!RESPONSE_TYPE_TOKEN.equals(form.getResponseType())) {
    		return deny(UNSUPPORTED_RESPONSE_TYPE, String.format("response_type must be '%s'", RESPONSE_TYPE_TOKEN));
    	}
    	if (form.getClient() == null) {
    		return deny(INVALID_REQUEST, "client_id is required");
    	}
    	if (form.getRedirectUri() == null) {
    		return deny(INVALID_REQUEST, "redirect_uri is required");
    	}
    	User client = users.find(form.getClient());
    	if (client == null || !client.isVerified() || client.isSuspended()) {
    		return deny(UNAUTHORIZED_CLIENT, "client account must be enabled and verified");
    	}
    	if (!sameDomain(client.getEmail(), URI.create(form.getRedirectUri()))) {
    		return deny(INVALID_REDIRECT_URI, "domain must match the domain of the email address");
    	}
    	if (form.getScope() == null) {
    		// TODO: check that the bucket exists and that the principal has access
    		return deny(INVALID_SCOPE, "scope must be a bucket");
    	}
        return grant(auth.getPrincipal(), form.getClient(), form.getScope());
    }

	private static boolean sameDomain(String email, URI uri) {
		String domain = email.substring(email.indexOf('@') + 1);
    	return uri.getHost().endsWith(domain);
	}

    public Result token() {
    	return token(Form.form(TokenForm.class).bindFromRequest().get());
    }

    private Result token(TokenForm form) {
    	if (GRANT_TYPE_CLIENT_CREDENTIALS.equals(form.getGrant_type())) {
            return grant(new Identity(), null, null);
    	}
    	if (GRANT_TYPE_PASSWORD.equals(form.getGrant_type())) {
    		if (form.getUsername() == null) {
    			return deny(INVALID_REQUEST, "username is required");
    		}
    		if (form.getPassword() == null) {
    			return deny(INVALID_REQUEST, "password is required");
    		}
    		User user = users.find(form.getUsername());
    		if (user == null || !user.passwordEquals(form.getPassword())) {
    			return deny(ACCESS_DENIED, "invalid username or password");
    		}
    		if (user.isSuspended()) {
    			return deny(ACCESS_DENIED, "user suspended");
    		}
    		return grant(user.asIdentity(), null, null);
    	}
		return deny(UNSUPPORTED_GRANT_TYPE, String.format("grant_type must be '%s', got %s", GRANT_TYPE_PASSWORD, form.getGrant_type()));
    }

    private Result deny(String errorCode, String errorDescription) {
    	ObjectNode result = Nodes.newObject();
    	result.put("error", errorCode);
    	result.put("error_description", errorDescription);
    	return badRequest(result);
    }

    private Result grant(Identity principal, Identity client, String scope) {
		Authorization auth = new Authorization(principal, client, scope);
    	dispatcher.dispatch(new CreateAuthorizationCommand(principal, auth));
    	ObjectNode result = Nodes.newObject();
    	result.put("access_token", auth.getId());
    	result.put("client_id", principal.getId());
    	if (scope != null) {
    		result.put("scope", scope);
    	}
    	return ok(result);
    }

    public Result callback(String taskId) {
    	return redirect(String.format("/#/tasks/%s?%s", taskId, toString(request().queryString())));
    }

	private static String toString(Map<String, String[]> params) {
		try {
			StringBuilder builder = new StringBuilder();
			for (Map.Entry<String, String[]> entry : params.entrySet()) {
				for (String value : entry.getValue()) {
					if (builder.length() > 0) {
						builder.append('&');
					}
					builder.append(entry.getKey()).append('=').append(URLEncoder.encode(value, Charsets.UTF_8.name()));
				}
			}
			return builder.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
