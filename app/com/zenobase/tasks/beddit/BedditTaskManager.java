package com.zenobase.tasks.beddit;

import java.util.List;

import javax.inject.Inject;

import org.elasticsearch.common.joda.time.LocalDate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Ordering;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class BedditTaskManager extends OAuthTaskManager {

	@Inject
	public BedditTaskManager(BedditCredentialsManager credentialsManager) {
		super(BedditTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Preconditions.checkNotNull(settings.path("tag").textValue());
		return new BedditTask(bucketId, principal, tag);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(BedditTask.class), credentials);
	}

	private Command execute(BedditTask task, OAuthCredentials credentials) {
		DateTime from = task.getFrom();
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://cloudapi.beddit.com/api/v1/user/" + credentials.getScope() + "/sleep");
		request.addQuerystringParameter("start_date", from.toLocalDate().toString());
		request.addQuerystringParameter("end_date", LocalDate.now().plusMonths(1).toString());
		Response response = send(request, credentials);
		BedditResult result = new BedditResult(task.getTag(), task.getPrincipal(), from, parseArray(response));
		return createCommand(task, credentials, result.getEvents());
	}

	private Command createCommand(Task task, OAuthCredentials credentials, List<Event> events) {
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
