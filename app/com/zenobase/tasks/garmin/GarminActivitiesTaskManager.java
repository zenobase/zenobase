package com.zenobase.tasks.garmin;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class GarminActivitiesTaskManager extends OAuthTaskManager {

	private static final String HOST = "https://healthapi.garmin.com";

	@Inject
	public GarminActivitiesTaskManager(GarminCredentialsManager credentialsManager) {
		super(GarminActivitiesTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTime marker = parseMarker(settings.path("marker").textValue());
		return new GarminActivitiesTask(bucketId, principal, marker != null ? marker.toString() : null);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(GarminActivitiesTask.class), credentials);
	}

	private Command execute(GarminActivitiesTask task, OAuthCredentials credentials) {
		DateTime from = parseMarker(task.getMarker());
		DateTime to = from.plusDays(1);
		//DateTime to = parseMarker("2020-03-16T00:00:00-07:00"); // from.plusWeeks(2);
		OAuthRequest request = new OAuthRequest(Verb.GET, HOST + "/wellness-api/rest/backfill/epochs");
		request.addQuerystringParameter("summaryStartTimeInSeconds", Long.toString(from.getMillis() / 1000));
		request.addQuerystringParameter("summaryEndTimeInSeconds", Long.toString(to.getMillis() / 1000));
		send(request, credentials);
		return createCommand(task, to);
	}

	private static DateTime parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker) : null;
	}

	private Command createCommand(Task task, DateTime marker) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), marker.toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		return command;
	}
}
