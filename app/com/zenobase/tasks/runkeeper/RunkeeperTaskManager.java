package com.zenobase.tasks.runkeeper;

import java.util.List;

import javax.inject.Inject;
import javax.measure.quantity.Length;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
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
		List<Event> events = Lists.newArrayList();
		String host = "https://api.runkeeper.com";
		String path = "/fitnessActivities";
		LocalDate to = new DateTime(task.getTimezone()).toLocalDate().minusDays(1);
		while (path != null) {
			OAuthRequest request = new OAuthRequest(Verb.GET, host + path);
			request.addHeader("Accept", "application/vnd.com.runkeeper.FitnessActivityFeed+json");
			request.addQuerystringParameter("noEarlierThan", task.getMarker());
			request.addQuerystringParameter("noLaterThan", to.toString());
			request.addQuerystringParameter("pageSize", "100");
			Response response = send(request, credentials);
			ActivitiesResult result = new ActivitiesResult(parseObject(response), task.getPrincipal(), task.getUnit(), task.getTimezone());
			events.addAll(result.getEvents());
			path = result.getNext();
		}
		return createCommand(task, credentials, events, to.plusDays(1));
	}

	static DateTime parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker) : null;
	}

	static String formatMarker(DateTime time) {
		return time != null ? time.toLocalDate().toString() : null;
	}

	private Command createCommand(RunkeeperTask task, OAuthCredentials credentials, List<Event> events, LocalDate marker) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : marker.toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
