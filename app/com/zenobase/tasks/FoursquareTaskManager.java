package com.zenobase.tasks;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.builder.api.Foursquare2Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task.State;

public class FoursquareTaskManager extends OAuthTaskManager {

	private static final String API_VERSION = "20121128";
	private static final int LIMIT = 100;

	@Inject
	public FoursquareTaskManager(@Named("foursquare.api.key") String apiKey, @Named("foursquare.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(Foursquare2Api.class, apiKey, apiSecret, callbackUrl);
	}

	@Override
	public String getType() {
		return FoursquareTask.TYPE;
	}

	@Override
	public Task newTask(String bucketId, Identity principal) {
		FoursquareTask task = new FoursquareTask(bucketId, principal);
		task.setState(State.UNAUTHORIZED);
		return task;
	}

	@Override
	public Command authorize(Task task, ObjectNode config) {
		return authorize(task.as(OAuthTask.class), config);
	}

	private Command authorize(OAuthTask task, ObjectNode config) {
		String verifier = config.get("code").getTextValue();
		Token token = getAccessToken(task, verifier);
		return UpdateTaskCommand.builder(task)
			.set(OAuthTask.TOKEN, task.getToken(), token)
			.set(Task.STATE, task.getState(), Task.State.READY)
			.build();
	}

	@Override
	public Command execute(Task task) {
		return execute(task.as(FoursquareTask.class));
	}

	private Command execute(FoursquareTask task) {
		Preconditions.checkState(task.getState() == Task.State.READY, "Task is not ready: %s", task.getId());
		DateTime marker = new DateTime(DateTimeZone.UTC).minusMinutes(1);
		List<Event> events = Lists.newArrayList();
		for (int offset = 0; execute(task, marker, offset, events); offset += LIMIT) {}
		return createCommand(task, marker, events);
	}

	private boolean execute(FoursquareTask task, DateTime marker, int offset, List<Event> events) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.foursquare.com/v2/users/self/checkins");
		request.addQuerystringParameter("v", API_VERSION);
		request.addQuerystringParameter("oauth_token", task.getToken().getToken());
		if (task.getMarker() != null) {
			request.addQuerystringParameter("afterTimestamp", format(task.getMarker()));
		}
		request.addQuerystringParameter("beforeTimestamp", format(marker));
		request.addQuerystringParameter("offset", Integer.toString(offset));
		request.addQuerystringParameter("limit", Integer.toString(LIMIT));
		FoursquareResult result = new FoursquareResult(parseObject(request.send()), task.getPrincipal());
		Preconditions.checkState(result.getStatus() == 200);
		List<Event> found = result.getEvents();
		events.addAll(found);
		return found.size() > LIMIT && result.getTotal() > offset + LIMIT;
	}

	private static String format(DateTime time) {
		return Long.toString(time.getMillis() / 1000);
	}

	private Command createCommand(FoursquareTask task, DateTime marker, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "imported events from foursquare", "removed events imported from foursquare");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.UPDATED, task.getUpdated(), new DateTime(DateTimeZone.UTC))
			.set(FoursquareTask.MARKER, task.getMarker(), marker).build());
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
