package com.zenobase.tasks;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;

public abstract class OAuthTaskManager extends TaskManager {

	private final Class<? extends Api> apiClass;
	private final String apiKey;
	private final String apiSecret;
	private final String callbackUrl;

	protected OAuthTaskManager(Class<? extends Api> apiClass, String apiKey, String apiSecret, String callbackUrl) {
		this.apiClass = apiClass;
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.callbackUrl = callbackUrl;
	}

	@Override
	public OAuthTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		OAuthTask task = new OAuthTask(getType(), bucketId, principal);
		task.setToken(getService(task).getRequestToken());
		task.setAuthorizationUrl(getService(task).getAuthorizationUrl(task.getToken()));
		return task;
	}

	@Override
	public Command authorize(Task task, ObjectNode config) {
		Preconditions.checkState(!task.isEnabled(), "Task is already enabled: %s", task.getId());
		return authorize(task.as(OAuthTask.class), config);
	}

	private Command authorize(OAuthTask task, ObjectNode config) {
		String token = config.get("oauth_token").getTextValue();
		String verifier = config.get("oauth_verifier").getTextValue();
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
			.provider(apiClass)
			.apiKey(apiKey)
			.apiSecret(apiSecret)
			.callback(String.format("%s/#/buckets/%s/tasks/%s/auth", callbackUrl, task.getBucketId(), task.getId()));
		configure(builder);
		return builder.build();
	}

	protected void configure(ServiceBuilder builder) {

	}

	protected static ObjectNode parseObject(Response response) {
		// System.out.println("parse: " + response.getBody());
		return Nodes.readObject(response.getBody().getBytes());
	}

	protected static ArrayNode parseArray(Response response) {
		// System.out.println("parse: " + response.getBody());
		return Nodes.readArray(response.getBody().getBytes());
	}
}
