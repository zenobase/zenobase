package com.zenobase.tasks.sleepcloud;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.google.GoogleCredentialsManager;

public class SleepCloudTaskManager extends OAuthTaskManager {

	@Inject
	public SleepCloudTaskManager(GoogleCredentialsManager credentialsManager) {
		super(SleepCloudTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Preconditions.checkNotNull(settings.path("tag").textValue());
		return new SleepCloudTask(bucketId, principal, tag);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(SleepCloudTask.class), credentials);
	}

	private Command execute(SleepCloudTask task, OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://sleep-cloud.appspot.com/fetchRecords");
		DateTime from = task.getFrom();
		if (from != null) {
			request.addQuerystringParameter("timestamp", Long.toString(from.getMillis()));
		}
		// request.addQuerystringParameter("sample", "true"); // test data
		Response response = send(request, credentials);
		List<Event> events = new SleepsResult(task.getTag(), task.getPrincipal(), parseObject(response)).getEvents();
		return createCommand(task, events);
	}

	private Command createCommand(Task task, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran sleepcloud task", "reverted sleepcloud task");
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

	private static String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime begin = event.getValue(Event.TIMESTAMP);
			Duration duration = event.getValue(Event.DURATION);
			DateTime end = begin.plus(duration);
			if (latest == null || end.isAfter(latest)) {
				latest = end;
			}
		}
		return latest != null ? latest.toString() : null;
	}
}
