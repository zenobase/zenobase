package com.zenobase.tasks.bodymedia;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class BodyMediaSummaryTaskManager extends OAuthTaskManager {

	@Inject
	public BodyMediaSummaryTaskManager(BodyMediaCredentialsManager credentialsManager) {
		super(BodyMediaSummaryTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		return new BodyMediaSummaryTask(bucketId, principal, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(BodyMediaSummaryTask.class), credentials);
	}

	private Command execute(BodyMediaSummaryTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		OAuthRequest request = createRequest(task);
		Response response = send(request, credentials);
		BodyMediaSummaryResult result = new BodyMediaSummaryResult(parseObject(response), task.getPrincipal());
		return createCommand(task, credentials, result, token);
	}

	private OAuthRequest createRequest(BodyMediaSummaryTask task) {
		return new OAuthRequest(Verb.GET, String.format("https://api.bodymedia.com/v2/json/summary/day/%s/%s", formatMarker(getFromDate(task)), formatMarker(new LocalDate())));
	}

	private static LocalDate getFromDate(Task task) {
		return LocalDate.parse(task.getMarker());
	}

	private static LocalDate parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker).toLocalDate() : LocalDate.now().withDayOfMonth(1);
	}

	private static String formatMarker(LocalDate date) {
		return date.toString("yyyyMMdd");
	}

	private static Command createCommand(BodyMediaSummaryTask task, OAuthCredentials credentials, BodyMediaSummaryResult result, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran bodymedia task", "reverted bodymedia task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), result.getLastSyncDate().toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!credentials.getToken().equals(expiredToken)) {
			command.add(UpdateCredentialsCommand.builder(credentials)
				.with(Credentials.CREDENTIALS)
				.set(OAuthCredentials.TOKEN, expiredToken, credentials.getToken())
				.build());
		}
		for (Event event : result.getEvents()) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
