package com.zenobase.tasks.bodymedia;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.RangeMap;

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

public class BodyMediaBurnTaskManager extends OAuthTaskManager {

	@Inject
	public BodyMediaBurnTaskManager(BodyMediaCredentialsManager credentialsManager) {
		super(BodyMediaBurnTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		return new BodyMediaBurnTask(bucketId, principal, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(BodyMediaBurnTask.class), credentials);
	}

	private Command execute(BodyMediaBurnTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		List<Event> events = Lists.newArrayList();
		RangeMap<LocalDateTime, DateTimeZone> timezones = getTimezones(task, credentials);
		LocalDate date = parseMarker(task.getMarker());
		while (true) {
			BodyMediaBurnResult result = execute(task, credentials, date, timezones);
			if (!events.addAll(result.getEvents())) {
				break;
			}
			date = date.plusDays(1);
		}
		return createCommand(task, credentials, date, events, token);
	}

	private RangeMap<LocalDateTime, DateTimeZone> getTimezones(BodyMediaBurnTask task, OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, String.format("http://api.bodymedia.com/v2/json/timezone"));
		Response response = send(request, credentials);
		return new BodyMediaTimezonesResult(parseObject(response)).getTimezones();
	}

	private BodyMediaBurnResult execute(BodyMediaBurnTask task, OAuthCredentials credentials, LocalDate date, RangeMap<LocalDateTime, DateTimeZone> timezones) {
		OAuthRequest request = new OAuthRequest(Verb.GET, String.format("http://api.bodymedia.com/v2/json/burn/day/minute/%s", formatMarker(date)));
		Response response = send(request, credentials);
		return new BodyMediaBurnResult(parseObject(response), task.getPrincipal(), timezones);
	}

	private static LocalDate parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker).toLocalDate() : LocalDate.now().withDayOfMonth(1);
	}

	private static String formatMarker(LocalDate date) {
		return date.toString("yyyyMMdd");
	}

	private static Command createCommand(BodyMediaBurnTask task, OAuthCredentials credentials, LocalDate lastSync, Iterable<Event> events, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran bodymedia burn task", "reverted bodymedia burn task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), lastSync.toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!credentials.getToken().equals(expiredToken)) {
			command.add(UpdateCredentialsCommand.builder(credentials)
				.with(Credentials.CREDENTIALS)
				.set(OAuthCredentials.TOKEN, expiredToken, credentials.getToken())
				.build());
		}
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
