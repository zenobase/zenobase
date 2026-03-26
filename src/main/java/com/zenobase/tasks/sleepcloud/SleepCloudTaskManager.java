package com.zenobase.tasks.sleepcloud;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Ordering;
import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;
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
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		List<Event> events = new ArrayList<>();
		String cursor = null;
		do {
			var request = new OAuthRequest(Verb.GET, "https://sleep-cloud.appspot.com/fetchRecords");
			request.addQuerystringParameter("tags", "true");
			request.addQuerystringParameter("comments", "true");
			// request.addQuerystringParameter("sample", "true"); // test data
			DateTime from = task.getFrom();
			if (from != null) {
				request.addQuerystringParameter("timestamp", Long.toString(from.getMillis()));
			}
			if (cursor != null) {
				request.addQuerystringParameter("cursor", cursor);
			}
			Response response = send(request, credentials);
			var result =
					new SleepCloudResult(task.getTag(), task.getPrincipal(), task.useRanges(), parseObject(response));
			if (!events.addAll(result.getEvents())) {
				break;
			}
			cursor = result.getCursor();
		} while (cursor != null);
		return createCommand(task, credentials, events, token);
	}

	private Command createCommand(Task task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
		var command = new CompoundCommand(task.getPrincipal(), "ran sleepcloud task", "reverted sleepcloud task");
		command.add(UpdateTaskCommand.builder(task)
				.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
				.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
				.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events))
				.set(Task.UNDO, task.getUndoId(), command.getId())
				.build());
		if (!Objects.equal(credentials.getToken(), expiredToken)) {
			command.add(UpdateCredentialsCommand.builder(credentials)
					.with(Credentials.CREDENTIALS)
					.set(OAuthCredentials.TOKEN, expiredToken, credentials.getToken())
					.build());
		}
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}

	private static String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime end = Ordering.natural().max(event.getValues(Event.TIMESTAMP));
			if (latest == null || end.isAfter(latest)) {
				latest = end;
			}
		}
		return latest != null ? latest.toString() : null;
	}
}
