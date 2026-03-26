package com.zenobase.tasks.foursquare;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class FoursquareTaskManager extends OAuthTaskManager {

	private static final String API_VERSION = "20121128";
	private static final int LIMIT = 100;

	@Inject
	public FoursquareTaskManager(FoursquareCredentialsManager credentialsManager) {
		super(FoursquareTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		return new FoursquareTask(bucketId, principal, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(FoursquareTask.class), credentials);
	}

	private Command execute(FoursquareTask task, OAuthCredentials credentials) {
		String marker = Objects.requireNonNull(
				formatMarker(DateTime.now(DateTimeZone.UTC).minusMinutes(1)));
		List<Event> events = new ArrayList<>();
		for (int offset = 0; execute(task, credentials, marker, offset, events); offset += LIMIT) {}
		return createCommand(task, marker, events);
	}

	static @Nullable DateTime parseMarker(@Nullable String marker) {
		return marker != null ? DateTime.parse(marker) : null;
	}

	static @Nullable String formatMarker(@Nullable DateTime time) {
		return time != null ? Long.toString(time.getMillis() / 1000) : null;
	}

	private boolean execute(
			FoursquareTask task, OAuthCredentials credentials, String marker, int offset, List<Event> events) {
		var request = new OAuthRequest(Verb.GET, "https://api.foursquare.com/v2/users/self/checkins");
		request.addQuerystringParameter("v", API_VERSION);
		if (task.getMarker() != null) {
			request.addQuerystringParameter("afterTimestamp", task.getMarker());
		}
		request.addQuerystringParameter("beforeTimestamp", marker);
		request.addQuerystringParameter("offset", Integer.toString(offset));
		request.addQuerystringParameter("limit", Integer.toString(LIMIT));
		Response response = send(request, credentials);
		var result = new FoursquareResult(task.getPrincipal(), parseObject(response));
		List<Event> found = result.getEvents();
		events.addAll(found);
		return found.size() == LIMIT && result.getTotal() > offset + LIMIT;
	}

	private Command createCommand(FoursquareTask task, String marker, List<Event> events) {
		var command = new CompoundCommand(task.getPrincipal(), "ran foursquare task", "reverted foursquare task");
		command.add(UpdateTaskCommand.builder(task)
				.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
				.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
				.set(Task.MARKER, task.getMarker(), marker)
				.set(Task.UNDO, task.getUndoId(), command.getId())
				.build());
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}
}
