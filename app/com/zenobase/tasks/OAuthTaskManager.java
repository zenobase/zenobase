package com.zenobase.tasks;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.Response;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.json.Nodes;

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

	public final String getAuthorizationUrl(OAuthTask task) {
		OAuthService service = getService(task);
		if (task.getToken() == null) {
			task.setToken(service.getRequestToken());
		}
		return service.getAuthorizationUrl(task.getToken());
	}

	@Override
	public Command configure(Task task, ObjectNode config) {
		OAuthTask to = new OAuthTask(task.copy().toJson());
		setToken(to, config.get("oauth_verifier").getTextValue());
		return new UpdateTaskCommand(task.getPrincipal(), task.getBucketId(), task, to);
	}

	public final void setToken(OAuthTask task, String verifier) {
		task.setToken(getService(task).getAccessToken(task.getToken(), new Verifier(verifier)));
	}

	protected final OAuthService getService(OAuthTask task) {
		ServiceBuilder builder = new ServiceBuilder()
			.provider(apiClass)
			.apiKey(apiKey)
			.apiSecret(apiSecret)
			.callback(callbackUrl + task.getId());
		configure(builder);
		return builder.build();
	}

	protected void configure(ServiceBuilder builder) {

	}

	protected static ObjectNode parseObject(Response response) {
		System.out.println("parse: " + response.getBody());
		return Nodes.readObject(response.getBody().getBytes());
	}

	protected static ArrayNode parseArray(Response response) {
		System.out.println("parse: " + response.getBody());
		return Nodes.readArray(response.getBody().getBytes());
	}
}
