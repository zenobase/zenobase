package com.zenobase.tasks.dash;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Ordering;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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

public class DashTaskManager extends OAuthTaskManager {

	@Inject
	public DashTaskManager(DashCredentialsManager credentialsManager) {
		super(DashTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = settings.path("tag").textValue();
		DateTimeZone timezone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		return new DashTask(bucketId, principal, tag, timezone, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(DashTask.class), credentials);
	}

	private Command execute(DashTask task, OAuthCredentials credentials) {
		DateTime from = parseMarker(task.getMarker());
		UserSettings settings = retrieveSettings(credentials);
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://dash.by/api/chassis/v1/trips");
		request.addQuerystringParameter("startTime", Long.toString(from.getMillis()));
		request.addQuerystringParameter("endTime", Long.toString(System.currentTimeMillis()));
		request.addQuerystringParameter("paged", "false");
		Response response = send(request, credentials);
		TripsResult result = new TripsResult(parse(response), task.getPrincipal(), settings, task.getTag(), task.getTimezone());
		return createCommand(task, result.getTrips());
	}

	private UserSettings retrieveSettings(OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://dash.by/api/chassis/v1/user");
		Response response = send(request, credentials);
		return new UserSettingsResult(parse(response)).get();
	}

	static DateTime parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker) : null;
	}

	static String formatMarker(DateTime time) {
		return time != null ? time.toString() : null;
	}

	static String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime time = Ordering.natural().max(event.getValues(Event.TIMESTAMP));
			if (latest == null || time.isAfter(latest)) {
				latest = time;
			}
		}
		return latest != null ? latest.plusMinutes(1).toString() : null;
	}

	private Command createCommand(Task task, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events))
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}

	@Override
	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		request.addHeader("Accept", "*/*");
		return super.send(request, credentials);
	}
}
