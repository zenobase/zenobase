package com.zenobase.tasks.strava;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class StravaTaskManager extends OAuthTaskManager {

	private static final String host = "https://www.strava.com/api/v3";

	@Inject
	public StravaTaskManager(StravaCredentialsManager credentialsManager) {
		super(StravaTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		boolean metric = settings.path("metric").booleanValue();
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		return new StravaTask(bucketId, principal, metric, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(StravaTask.class), credentials);
	}

	private Command execute(StravaTask task, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		DateTime from = parseMarker(task.getMarker());
		for (int i = 0; i < 10; ++i) {
			OAuthRequest request = new OAuthRequest(Verb.GET, host + "/athlete/activities");
			if (from != null) {
				request.addQuerystringParameter("after", Long.toString(from.getMillis() / 1000));
			}
			request.addQuerystringParameter("per_page", "100");
			request.addQuerystringParameter("page", Integer.toString(i + 1));
			Response response = send(request, credentials);
			StravaActivitiesResult result = new StravaActivitiesResult(parseArray(response), task.getPrincipal(), task.isMetric());
			if (!events.addAll(result.getEvents())) {
				break;
			}
		}
		return createCommand(task, credentials, events);
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
			DateTime time = event.getValue(Event.TIMESTAMP);
			if (latest == null || time.isAfter(latest)) {
				latest = time;
			}
		}
		return latest != null ? latest.toString() : null;
	}

	private Command createCommand(StravaTask task, OAuthCredentials credentials, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events).toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		return command;
	}
}
