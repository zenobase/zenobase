package com.zenobase.tasks;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.SignatureType;
import org.scribe.model.Verb;
import com.google.common.base.Preconditions;
import com.google.inject.name.Named;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;

public class WithingsTaskManager extends OAuthTaskManager {

	public WithingsTaskManager(@Named("withings.api.key") String apiKey, @Named("withings.api.secret") String apiSecret, @Named("hostname") String callbackUrl) {
		super(WithingsApi.class, apiKey, apiSecret, callbackUrl);
	}

	@Override
	public String getType() {
		return WithingsTask.TYPE;
	}

	@Override
	public Command configure(Task task, ObjectNode config) {
		WithingsTask to = new WithingsTask(task.copy().toJson());
		String token = config.get("oauth_token").getTextValue();
		String verifier = config.get("oauth_verifier").getTextValue();
		Preconditions.checkState(to.getToken().getToken().equals(token),
			"Token matches in task %s, expected %s, got %s",
			task.getId(), to.getToken().getToken(), token);
		to.setUserId(config.get("userid").asInt());
		to.setToken(getAccessToken(to, verifier));
		to.setState(Task.State.READY);
		return new UpdateTaskCommand(task.getPrincipal(), task.getBucketId(), task, to);
	}

	@Override
	public Command execute(Task task) {
		return execute(new WithingsTask(task.toJson()));
	}

	private Command execute(WithingsTask task) {
		OAuthRequest request = createRequest(task);
		getService(task).signRequest(task.getToken(), request);
		Response response = request.send();
		for (Event event : process(task, response)) {
			System.out.println("event: " + event.toJson());
		}

		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "imported events from Withings", "dropped events from Withings");
		WithingsTask to = task.copy();
		// TODO to.setMarker();
		command.add(new UpdateTaskCommand(task.getPrincipal(), task.getBucketId(), task, to));
		for (Event event : process(task, response)) {
			System.out.println("event: " + event.toJson());
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}

	private static OAuthRequest createRequest(WithingsTask task) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://wbsapi.withings.net/measure");
		request.addQuerystringParameter("userid", Integer.toString(task.getUserId()));
		request.addQuerystringParameter("action", "getmeas");
		request.addQuerystringParameter("devtype", "1"); // weight scale data
		if (task.getMarker() != null) {
			request.addQuerystringParameter("lastupdate", task.getMarker());
		}
		return request;
	}

	public static List<Event> process(WithingsTask task, Response response) {
		WithingsResultNode result = new WithingsResultNode(parseObject(response));
		task.setMarker(result.getMarker());
		System.out.println("marker: " + task.getMarker());
		return result.getEvents();
	}

	@Override
	protected void configure(ServiceBuilder builder) {
		builder.signatureType(SignatureType.QueryString);
	}
}