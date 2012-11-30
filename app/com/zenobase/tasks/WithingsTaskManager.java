package com.zenobase.tasks;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.SignatureType;
import org.scribe.model.Verb;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;

public class WithingsTaskManager extends OAuthTaskManager {

	@Inject
	public WithingsTaskManager(@Named("withings.api.key") String apiKey, @Named("withings.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(WithingsApi.class, apiKey, apiSecret, callbackUrl);
	}

	@Override
	public String getType() {
		return WithingsTask.TYPE;
	}

	@Override
	public Command authorize(Task task, ObjectNode config) {
		Preconditions.checkState(!task.isEnabled(), "Task is already enabled: %s", task.getId());
		return authorize(task.as(WithingsTask.class), config);
	}

	private Command authorize(WithingsTask task, ObjectNode config) {
		String token = config.get("oauth_token").getTextValue();
		String verifier = config.get("oauth_verifier").getTextValue();
		int userId = config.get("userid").asInt();
		Preconditions.checkState(task.getToken().getToken().equals(token),
			"Token matches in task %s, expected %s, got %s",
			task.getId(), task.getToken().getToken(), token);
		return UpdateTaskCommand.builder(task)
			.set(Task.ENABLED, task.isEnabled(), true)
			.set(OAuthTask.TOKEN, task.getToken(), getAccessToken(task, verifier)) // TODO CREDENTIALS
			.set(WithingsTask.USER_ID, task.getUserId(), userId) // TODO CREDENTIALS
			.build();
	}

	@Override
	public Command execute(Task task) {
		Preconditions.checkState(task.isEnabled(), "Task is not enabled: %s", task.getId());
		return execute(task.as(WithingsTask.class));
	}

	private Command execute(WithingsTask task) {
		OAuthRequest request = createRequest(task);
		getService(task).signRequest(task.getToken(), request);
		return createCommand(task, new WithingsResult(task.getPrincipal(), parseObject(request.send())));
	}

	private static OAuthRequest createRequest(WithingsTask task) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://wbsapi.withings.net/measure");
		request.addQuerystringParameter("userid", task.getUserId().toString());
		request.addQuerystringParameter("action", "getmeas");
		request.addQuerystringParameter("devtype", "1"); // weight scale data
		if (task.getMarker() != null) {
			request.addQuerystringParameter("lastupdate", task.getMarker().toString());
		}
		return request;
	}

	private static Command createCommand(WithingsTask task, WithingsResult result) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "imported events from withings", "removed events imported from withings");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), result.getMarker())
			.build());
		for (Event event : result.getEvents()) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}

	@Override
	protected void configure(ServiceBuilder builder) {
		builder.signatureType(SignatureType.QueryString);
	}
}
