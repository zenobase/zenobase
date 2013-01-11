package com.zenobase.tasks;

import javax.inject.Inject;
import javax.inject.Named;
import javax.measure.quantity.Mass;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.SignatureType;
import org.scribe.model.Verb;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

public class WithingsTaskManager extends OAuthTaskManager {

	@Inject
	public WithingsTaskManager(@Named("withings.api.key") String apiKey, @Named("withings.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(new WithingsApi(), apiKey, apiSecret, callbackUrl);
	}

	@Override
	public String getType() {
		return WithingsTask.TYPE;
	}

	@Override
	public WithingsTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		WithingsTask task = super.newTask(bucketId, principal, settings).as(WithingsTask.class);
		task.setTag(Objects.firstNonNull(settings.path("tag").getTextValue(), "steps"));
		task.setUnit(Measures.<Mass>parseUnit(Objects.firstNonNull(settings.path("unit").getTextValue(), "kg")));
		return task;
	}

	@Override
	public Command authorize(Task task, ObjectNode config) {
		try {
			Preconditions.checkState(!task.isEnabled(), "Task is already enabled: %s", task.getId());
			return authorize(task.as(WithingsTask.class), config);
		} catch (InvalidTokenException e) {
			return createCommand(e);
		}
	}

	private Command authorize(WithingsTask task, ObjectNode config) {
		String token = config.get("oauth_token").getTextValue();
		String verifier = config.get("oauth_verifier").getTextValue();
		int userId = config.get("userid").asInt();
		Preconditions.checkState(task.getToken().getToken().equals(token),
			"Token matches in task %s, expected %s, got %s",
			task.getId(), task.getToken().getToken(), token);
		return UpdateTaskCommand.builder(task)
			.set(Task.AUTHORIZATION_URL, task.getAuthorizationUrl(), null)
			.with(Task.CREDENTIALS)
			.set(OAuthTask.TOKEN, task.getToken(), getAccessToken(task, verifier))
			.set(WithingsTask.USER_ID, task.getUserId(), userId)
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
		Response response = request.send();
		checkResponse(task, request, response);
		WithingsResult result = new WithingsResult(parseObject(response), task.getPrincipal(), task.getTag(), task.getUnit());
		Preconditions.checkState(result.getStatus() == 0, "Expected status <0> but got <%s> for task <%s>", result.getStatus(), task.getId());
		return createCommand(task, result);
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
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran withings task", "reverted withings task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), result.getMarker())
			.set(Task.UNDO, task.getUndoId(), command.getId())
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
