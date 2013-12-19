package com.zenobase.tasks.runkeeper;

import java.util.List;

import javax.inject.Inject;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class RunkeeperTaskManager extends OAuthTaskManager {

	private static final String host = "https://api.runkeeper.com";

	@Inject
	public RunkeeperTaskManager(RunkeeperCredentialsManager credentialsManager) {
		super(RunkeeperTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		RunkeeperTask task = new RunkeeperTask(bucketId, principal, marker);
		task.setTimezone(DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC")));
		task.setUnit(Measures.<Length>parseUnit(Objects.firstNonNull(settings.path("unit").textValue(), "km")));
		return task;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(RunkeeperTask.class), credentials);
	}

	private Command execute(RunkeeperTask task, OAuthCredentials credentials) {
		String path = "/fitnessActivities";
		List<Event> events = Lists.newArrayList();
		LocalDateTime from = parseMarker(task.getMarker());
		while (path != null) {
			OAuthRequest request = new OAuthRequest(Verb.GET, host + path);
			request.addHeader("Accept", "application/vnd.com.runkeeper.FitnessActivityFeed+json");
			if (from != null) {
				request.addQuerystringParameter("noEarlierThan", from.toLocalDate().toString());
			}
			request.addQuerystringParameter("pageSize", "100");
			Response response = send(request, credentials);
			ActivitiesResult result = new ActivitiesResult(parseObject(response), task.getPrincipal(), task.getUnit(), task.getTimezone());
			for (Event event : result.getEvents()) {
				if (from == null || event.getValue(Event.TIMESTAMP).toLocalDateTime().isAfter(from)) {
					events.add(event);
				}
			}
			path = result.getNext();
		}
		for (Event event : events) {
			addDetails(event, task.getHeightUnit(), credentials);
		}
		return createCommand(task, credentials, events);
	}

	private void addDetails(Event event, Unit<Length> heightUnit, OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, host + event.getValue(Event.SOURCE).getUrl());
		request.addHeader("Accept", "application/vnd.com.runkeeper.FitnessActivity+json");
		Response response = send(request, credentials);
		new ActivityResult(parseObject(response), heightUnit).addDetails(event);
	}

	static LocalDateTime parseMarker(String marker) {
		return marker != null ? LocalDateTime.parse(marker.replaceAll("Z", "")) : null;
	}

	static String formatMarker(LocalDateTime time) {
		return time != null ? time.toString() : null;
	}

	static String getMarker(Iterable<Event> events) {
		return events.iterator().next().getValue(Event.TIMESTAMP).plusSeconds(1).toLocalDateTime().toString();
	}

	private Command createCommand(RunkeeperTask task, OAuthCredentials credentials, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events).toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
