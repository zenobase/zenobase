package com.zenobase.tasks;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import play.Logger;
import play.mvc.Http;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;

public abstract class OAuthTaskManager extends TaskManager {

	private final Api provider;
	private final String apiKey;
	private final String apiSecret;
	private final String callbackUrl;

	protected OAuthTaskManager(Api provider, String apiKey, String apiSecret, String callbackUrl) {
		this.provider = provider;
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.callbackUrl = callbackUrl;
	}

	@Override
	public OAuthTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		OAuthTask task = new OAuthTask(getType(), bucketId, principal);
		task.setToken(getRequestToken(task));
		task.setAuthorizationUrl(getService(task).getAuthorizationUrl(task.getToken()));
		return task;
	}

	protected Token getRequestToken(OAuthTask task) {
		return getService(task).getRequestToken();
	}

	@Override
	public Command authorize(Task task, ObjectNode config) {
		Preconditions.checkState(!task.isEnabled(), "Task is already enabled: %s", task.getId());
		return authorize(task.as(OAuthTask.class), config);
	}

	private Command authorize(OAuthTask task, ObjectNode config) {
		String token = config.path("oauth_token").getTextValue();
		String verifier = config.path("oauth_verifier").asText();
		if (token == null) {
			Logger.warn(String.format("Couldn't authorize %s task <%s>: %s",
				task.getType(), task.getId(), config));
			return null;
		}
		Preconditions.checkState(task.getToken().getToken().equals(token),
			"Token matches in task %s, expected %s, got %s",
			task.getId(), task.getToken().getToken(), token);
		return UpdateTaskCommand.builder(task)
			.set(Task.AUTHORIZATION_URL, task.getAuthorizationUrl(), null)
			.with(Task.CREDENTIALS)
			.set(OAuthTask.TOKEN, task.getToken(), getAccessToken(task, verifier))
			.build();
	}

	protected final Token getAccessToken(OAuthTask task, String verifier) {
		return getService(task).getAccessToken(task.getToken(), new Verifier(verifier));
	}

	protected final OAuthService getService(OAuthTask task) {
		ServiceBuilder builder = new ServiceBuilder()
			.provider(provider)
			.apiKey(apiKey)
			.apiSecret(apiSecret)
			.callback(String.format("%s/oauth/callback/%s", callbackUrl, task.getId()));
		configure(builder);
		return builder.build();
	}

	protected void configure(ServiceBuilder builder) {

	}

	protected static ObjectNode parseObject(Response response) {
		// System.out.println("parse: " + response.getBody());
		return Nodes.readObject(response.getBody());
	}

	protected static ArrayNode parseArray(Response response) {
		// System.out.println("parse: " + response.getBody());
		return Nodes.readArray(response.getBody());
	}

	protected void checkResponse(OAuthTask task, OAuthRequest request, Response response) throws OAuthException {
		if (!response.isSuccessful()) {
			if (isTokenInvalid(response)) {
				throw new InvalidTokenException(task, request);
			} else {
				throw new InvalidStatusException(task, request, response.getCode());
			}
		}
	}

	protected boolean isTokenInvalid(Response response) {
		return response.getCode() == Http.Status.UNAUTHORIZED;
	}

	protected Command createCommand(InvalidTokenException e) {
		Token requestToken = getRequestToken(e.getTask());
		return UpdateTaskCommand.builder(e.getTask())
			.set(Task.COMPLETED, e.getTask().getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, e.getTask().getStatus(), Task.Status.FAILED)
			.set(Task.UNDO, e.getTask().getUndoId(), null)
			.set(Task.AUTHORIZATION_URL, e.getTask().getAuthorizationUrl(), getService(e.getTask()).getAuthorizationUrl(requestToken))
			.with(Task.CREDENTIALS).set(OAuthTask.TOKEN, e.getTask().getToken(), requestToken).build();
	}
}
