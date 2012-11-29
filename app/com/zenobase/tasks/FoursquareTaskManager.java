package com.zenobase.tasks;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.builder.api.Foursquare2Api;
import org.scribe.model.OAuthRequest;
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
		OAuthTask to = new OAuthTask(task.copy().toJson());
		String verifier = config.get("code").getTextValue();
		to.setToken(getAccessToken(to, verifier));
		to.setState(Task.State.READY);
		return new UpdateTaskCommand(task.getPrincipal(), task, to);
	}

	@Override
	public Command execute(Task task) {
		return execute(new FoursquareTask(task.toJson()));
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
		FoursquareCheckinsNode result = new FoursquareCheckinsNode(parseObject(request.send()), task.getPrincipal());
		Preconditions.checkState(result.getStatus() == 200);
		return events.addAll(result.getEvents()) && result.getCount() > offset + LIMIT;
	}

	private static String format(DateTime time) {
		return Long.toString(time.getMillis() / 1000);
	}

	private Command createCommand(FoursquareTask task, DateTime marker, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "imported events from foursquare", "removed events imported from foursquare");
		FoursquareTask to = task.copy();
		to.setUpdated(new DateTime(DateTimeZone.UTC));
		to.setMarker(marker);
		command.add(new UpdateTaskCommand(task.getPrincipal(), task, to));
		for (Event event : events) {
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
